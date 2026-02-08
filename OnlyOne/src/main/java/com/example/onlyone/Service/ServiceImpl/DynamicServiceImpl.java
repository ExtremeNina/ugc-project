package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.FollowMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Rabbitmq.ArticleMessage;
import com.example.onlyone.Service.DynamicService;
import com.example.onlyone.Service.FollowService;
import com.example.onlyone.Service.LoveService;
import com.example.onlyone.Utils.SecurityUtils;
import com.example.onlyone.VO.DyArticleVO;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DynamicServiceImpl implements DynamicService {


    @Resource
    private FollowService followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private LoveService loveService;
    @Resource
    private UserMapper userMapper;

    private static final long TIMELINE_TTL_DAYS = 7;
    private static final String TIMELINE_KEY_PREFIX = "user:timeline:";

    //获取最新文章
    @Override
    public List<DyArticleVO> getNewArticles() {

        Long userId = SecurityUtils.getCurrentUserId();
        //获取时间流key
        String timelineKey = TIMELINE_KEY_PREFIX + userId;
        //判断时间流key是否存在
        if (!stringRedisTemplate.hasKey(timelineKey)) {
            //不存在则是活跃用户，从数据库中加载
            return getArticlesFromDB(userId);
        }
        //获取七天前的时间戳
        long sevenDaysAgo = System.currentTimeMillis() - (TIMELINE_TTL_DAYS * 24 * 60 * 60 * 1000L);
        //获取最新7天内的文章id
        Set<String> articleIdSet = stringRedisTemplate.opsForZSet().reverseRangeByScore(
                timelineKey,
                sevenDaysAgo,
                System.currentTimeMillis());
        //转换成list结构
        if (CollectionUtils.isEmpty(articleIdSet)) {
            log.info("用户 {} 的时间流中没有最近7天的文章", userId);
            return List.of();
        }
        List<Long> articleIds = articleIdSet.stream().map(Long::parseLong).toList();
        log.debug("从Redis获取用户 {} 的时间流，共 {} 篇文章", userId, articleIds.size());
        //查询对于的文章信息
        List<Article> articleList = articleMapper.BatchArticles(articleIds);
        //构建队列并且返回
        List<DyArticleVO> dyArticleVOList = convertToVOList(articleList);
        log.info("文章查询成功,如下：{}",dyArticleVOList);
        return dyArticleVOList;
    }


    /**
     * 监听文章推送队列
     */
    @RabbitListener(queues = "articlePushQueue")
    private void articlePushListener(ArticleMessage articleMessage
    , Channel channel, Message message) throws IOException {

        Long articleId = articleMessage.getArticleId();
        Long authorId = articleMessage.getAuthorId();
        log.info("开始处理文章推送，文章ID: {}，作者ID: {}", articleId, authorId);

        //使用分布式锁防止消息被重复消费
        String lockKey = "push:listener:"+articleMessage.getMessageId();
        Boolean isLock = tryLock(lockKey);
        if (!isLock) {
            //说明此时消息正在被处理或者已经被处理完
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("消息正在被处理或已处理，直接确认，消息ID: {}", articleMessage.getMessageId());
            return;
        }

        try {
            //获取粉丝id列表
            List<Long> fanIds = followService.getFanList(authorId);
            if (CollectionUtils.isEmpty(fanIds)) {
                log.info("作者 {} 没有粉丝，无需推送", authorId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }
            log.info("找到 {} 个在线粉丝，准备推送文章 {}", fanIds.size(), articleId);

            //在线用户使用zSET构建时间流存入文章id，显示7天内的新文章
            pushToTimeline(articleId, fanIds);

            log.info("文章 {} 推送完成，成功推送给 {} 个在线粉丝", articleId, fanIds.size());

        }catch (Exception e) {
            log.error("处理文章推送消息失败，文章ID: {}, 作者ID: {}", articleId, authorId, e);
            //重试机制,到达次数最大进入死信队列
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }finally {
            //释放锁，防止阻塞
            releaseLock(lockKey);
        }
        // 消息处理成功，确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        //收到处理消息确认
        log.info("消息确认成功");

    }


    //把文章id推送进用户时间流
    private void pushToTimeline(Long articleId, List<Long> fanIds) {

        long currentTime = System.currentTimeMillis();
        //时间限制为7天
        long sevenDaysAgo = currentTime - (TIMELINE_TTL_DAYS * 24 * 60 * 60 * 1000L);

        stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                StringRedisTemplate template = (StringRedisTemplate) operations;
                //为每个粉丝设置时间流
                for (Long fanId : fanIds) {
                    String timelineKey = TIMELINE_KEY_PREFIX + fanId;
                    template.opsForZSet().add(timelineKey, articleId.toString(), currentTime);
                    //设置过期时间
                    template.expire(timelineKey, TIMELINE_TTL_DAYS, TimeUnit.DAYS);
                    //清除7天前的文章
                    template.opsForZSet().removeRangeByScore(timelineKey, 0, sevenDaysAgo);
                }
                return null;
            }
        });
    }



    //从数据库中加载最新文章
    private List<DyArticleVO> getArticlesFromDB(Long userId) {
        log.info("从数据库为用户 {} 加载最新文章", userId);

        // 1. 获取用户关注的所有作者
        List<Long> authorIds = followMapper.getFollowedAuthorIds(userId);

        if (CollectionUtils.isEmpty(authorIds)) {
            log.info("用户 {} 没有关注任何作者", userId);
            return List.of();
        }

        // 2. 计算7天前的时间
        long sevenDaysAgo = System.currentTimeMillis() - (TIMELINE_TTL_DAYS * 24 * 60 * 60 * 1000L);
        Date sevenDaysAgoDate = new Date(sevenDaysAgo);

        // 3. 构建查询条件
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("author_id", authorIds)  // 作者ID在关注列表中
                .ge("create_time", sevenDaysAgoDate)// 发布时间在7天内
                .eq("status", 3)
                .orderByDesc("create_time");  // 按时间倒序

        // 4. 执行查询
        List<Article> articles = articleMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(articles)) {
            log.info("用户 {} 关注的作者最近7天没有发布文章", userId);
            return List.of();
        }

        log.info("从数据库为用户 {} 查询到 {} 篇文章", userId, articles.size());

        // 5. 使用已有的转换方法转换为VO列表
        return convertToVOList(articles);

    }


    //article转换出DyArticleVO对象
    private DyArticleVO convertArticleToDyArticleVO(Article article) {

        DyArticleVO dyArticleVO = new DyArticleVO();
        User author = userMapper.selectById(article.getAuthorId());
        //封装作者名称
        dyArticleVO.setAuthor(author.getUsername());
        dyArticleVO.setUserIcon(author.getIcon());
        Boolean isLove = loveService.isEntityLovedByUser(article.getId(),1L);
        //封装点赞情况
        dyArticleVO.setIsLove(isLove);
        BeanUtils.copyProperties(article, dyArticleVO);
        // 将LocalDateTime转换为String格式的publishTime
        if (article.getCreateTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dyArticleVO.setPublishTime(article.getCreateTime().format(formatter));
        }
        dyArticleVO.setViewCount(article.getPageview());
        return dyArticleVO;
    }

    /**
     * 将文章列表转换为VO列表
     */
    private List<DyArticleVO> convertToVOList(List<Article> articles) {
        return articles.stream()
                .map(this::convertArticleToDyArticleVO)
                .collect(Collectors.toList());
    }


    /**
     * 获取锁锁
     */
    private Boolean tryLock(String lockKey) {
        try {
            Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            return isLock;
        }catch (Exception e) {
            log.info("获取分布式锁失败");
            return false;
        }
    }

    /**
     * 释放锁
     */
    private void releaseLock(String lockKey) {
        try {
            stringRedisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("释放锁失败", e);
        }
    }


}
