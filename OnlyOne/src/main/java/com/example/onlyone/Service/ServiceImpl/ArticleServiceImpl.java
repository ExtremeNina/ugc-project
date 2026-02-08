package com.example.onlyone.Service.ServiceImpl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.Entity.*;
import com.example.onlyone.Rabbitmq.ArticleMessage;
import com.example.onlyone.VO.ArticleDetailVO;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Mapper.*;
import com.example.onlyone.Service.ArticleService;
import com.example.onlyone.Service.FollowService;
import com.example.onlyone.Service.LoveService;
import com.example.onlyone.Service.UserProfileService;
import com.example.onlyone.Utils.ArticleDataConverterUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Resource
    private LoveService loveService;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleAndLabelMapper articleAndLabelMapper;
    @Resource
    private LabelMapper labelMapper;
    @Resource
    private FollowService followService;
    @Resource
    private UserProfileService userProfileService;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private ArticleDataConverterUtils articleDataConverterUtils;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private CategoryMapper categoryMapper;
    // 假设文章对应的点赞类型ID为1（根据你的实际情况调整）
    private static final Long ARTICLE_LOVE_TYPE_ID = 1L;


    @Override
    @Transactional
    public ArticleDetailVO getArticleDetail(Long articleId) {

        // 1. 获取文章基本信息
        Article article = articleMapper.selectByArticleId(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        // 2. 获取当前登录用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userMapper.selectByUsername(currentUsername);

        // 3. 获取文章作者信息
        User author = userMapper.selectById(article.getAuthorId());
        UserProfileVO authorProfile = new UserProfileVO();
        authorProfile.setUserId(author.getId());

        Boolean isFollow = null;
        if (currentUser != null) {
            isFollow = followService.isFollowing(currentUser.getId(), author.getId());
            log.info("是否关注了该作者：{}", isFollow);
        } else {
            isFollow = false;
        }
        authorProfile.setIsFollow(isFollow);
        //获取作者粉丝数
        authorProfile.setFan(userProfileService.getFansCountFromCacheOrDB(author.getId()));
        //获取作者关注数
        authorProfile.setFollows(userProfileService.getFollowingCountFromCacheOrDB(author.getId()));
        //获取作者总点赞数
        authorProfile.setLove(userProfileService.getAllLoveCount(author.getId()));

        BeanUtils.copyProperties(author, authorProfile);


        // 4. 获取文章标签和分类信息
        List<String> labelList = selectArticleOfLabel(articleId);
        Long categoryId = article.getCategoryId();
        Category category = categoryMapper.selectById(categoryId);


        // 5. 检查点赞状态
        Boolean isLoved = loveService.isEntityLovedByUser(articleId, ARTICLE_LOVE_TYPE_ID);
        log.info("当前文章的点赞情况{}", isLoved);

        // 6. 获取文章点赞数
        Long loveCount = Long.valueOf(loveService.getEntityLoveCount(articleId, ARTICLE_LOVE_TYPE_ID));

        // 7. 构建返回对象
        ArticleDetailVO articleDetailVO = new ArticleDetailVO();

        // 复制文章基本信息
        BeanUtils.copyProperties(article, articleDetailVO);
        articleDetailVO.setCategory(article.getCategoryId().toString());
        articleDetailVO.setLabel(labelList);
        articleDetailVO.setAuthor(author.getUsername());
        articleDetailVO.setCategory(category.getCategoryName());

        // 8. 时间处理：优先显示更新时间，如果没有则显示创建时间
        String displayTime = formatDisplayTime(article);
        articleDetailVO.setPublishTime(displayTime);

        //9.获取文章评论树
        Long comments = commentMapper.countByArticleId(articleId);
        articleDetailVO.setComments(comments);


        // 设置作者信息
        articleDetailVO.setUserProfileVO(authorProfile);

        // 设置交互状态
        articleDetailVO.setIsLoved(isLoved);
        articleDetailVO.setLoveCount(loveCount);
        articleDetailVO.setViewCount((long) article.getPageview());
        log.info("文章的浏览量{}", article.getPageview());

        // 9. 增加文章浏览数
        articleMapper.incrementViewCount(articleId);

        log.info("当前作者的粉丝数：{}", articleDetailVO.getUserProfileVO().getFan());
        log.info("当前作者的关注数：{}", articleDetailVO.getUserProfileVO().getFollows());
        log.info("当前作者的总点赞数：{}", articleDetailVO.getUserProfileVO().getLove());
        log.info("是否关注该作者:{}", articleDetailVO.getUserProfileVO().getIsFollow());
        log.info("文章详情查询成功，文章ID: {}, 显示时间: {}", articleId, displayTime);
        log.info("当前文章的评论数：{}", articleDetailVO.getComments());
        return articleDetailVO;
    }




    //查看待审核和已发布文章(管理)
    @Override
    public PageInfo<ArticleVO> getAllArticle() {

        PageHelper.startPage(1, 10);

        // 定义要查询的状态：2（待审核）和 3（已发布）
        List<Integer> statuses = Arrays.asList(2, 3);

        // 调用Mapper查询文章列表
        List<Article> articles = articleMapper.selectArticlesByStatuses(statuses);

        // 使用已有的转换器转换为VO对象
        List<ArticleVO> articleVOList = articleDataConverterUtils.convertToArticleVOList(articles);
        for (ArticleVO articleVO : articleVOList) {
            log.info("查看到的文章状态{}",articleVO.getStatus());
        }
        return new PageInfo<>(articleVOList);
    }



    //更改文章状态（审核 ——> 已发布）
    @Override
    public void updateArticleStatus(Long articleId,Integer status) {
        if (articleId == null || status == null) {
            throw new RuntimeException("文章id不能为空");
        }
        articleMapper.updateArticleByStatus(articleId, status);
        //异步执行文章推送
        sendArticlePushMessage(articleId);
    }


    /**
     * 格式化显示时间：优先使用更新时间，如果为null则使用创建时间
     */
    private String formatDisplayTime(Article article) {
        LocalDateTime timeToDisplay = article.getUpdateTime() != null ?
                article.getUpdateTime() :
                article.getCreateTime();
        if (timeToDisplay != null) {
            return timeToDisplay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        return "未知时间";
    }


    //查询一篇文章对应的标签
    private List<String> selectArticleOfLabel(Long articleId) {

        //查询全部文章标签表的数据
        List<ArticleAndLabel> list = articleAndLabelMapper.selectAll(articleId);
        //获取全部标签id
        List<Long> labelIdList = list.stream().map(ArticleAndLabel::getLabelId).toList();
        if (labelIdList == null || labelIdList.isEmpty()) {
            return new ArrayList<>(); // 返回空列表而不是执行SQL
        }
        //查询到全部的label内容
        List<Label> labelList =  labelMapper.selectLabelList(labelIdList);
        //获取label表中的标签名
        List<String> labelNameList = labelList.stream().map(Label::getName).toList();
        return labelNameList;
    }


    //异步推送文章信息（生产者）
    private void sendArticlePushMessage(Long articleId) {

        Article article = articleMapper.selectById(articleId);
        Long authorId = article.getAuthorId();

        ArticleMessage articleMessage = new ArticleMessage();
        //使用uuid随机生成消息id
        Long messageId = RandomUtil.randomLong(6);
        //构建消息体
        articleMessage.setMessageId(messageId);
        articleMessage.setAuthorId(authorId);
        articleMessage.setArticleId(articleId);

        try {

            rabbitTemplate.convertAndSend("articlePushExchange","articlePush", articleMessage,

            message -> {
                //消息持久化
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                //设置消息id防止重复消费
                message.getMessageProperties().setMessageId(articleMessage.getMessageId().toString());
                return message;
            }
            );

            log.info("文章推送消息发送成功, articleId: {}, authorId: {}", articleId, authorId);
        }catch (Exception e) {
            log.info("文章推送消息发送失败, articleId: {}, authorId: {}", articleId, authorId);
        }


    }





}
