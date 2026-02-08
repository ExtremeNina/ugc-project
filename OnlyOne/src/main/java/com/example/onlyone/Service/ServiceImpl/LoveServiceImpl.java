package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.DTO.LoveDTO;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.Comment;
import com.example.onlyone.Entity.UserDetail;
import com.example.onlyone.Entity.UserLove;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.CommentMapper;
import com.example.onlyone.Mapper.UserLoveMapper;
import com.example.onlyone.Service.LoveService;
import com.example.onlyone.Utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoveServiceImpl extends ServiceImpl<UserLoveMapper, UserLove> implements LoveService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserLoveMapper userLoveMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private CommentMapper commentMapper;

    // 缓存过期时间
    private static final long CACHE_TTL = 24 * 60 * 60L; // 1天
    //点赞锁过期时间
    private static final long Love_Lock_Timeout = 5L;


    //关注功能的lua脚本
    private static final DefaultRedisScript<Long> LOVE_SCRIPT;
    private static final DefaultRedisScript<Long> UNLOVE_SCRIPT;
    static {

        LOVE_SCRIPT = new DefaultRedisScript<>();
        LOVE_SCRIPT.setLocation(new ClassPathResource("LoveLuaScript.lua"));
        LOVE_SCRIPT.setResultType(Long.class);

        UNLOVE_SCRIPT = new DefaultRedisScript<>();
        UNLOVE_SCRIPT.setLocation(new ClassPathResource("unLoveLuaScript.lua"));
        UNLOVE_SCRIPT.setResultType(Long.class);

    }

    //点赞||取消点赞
    @Transactional
    @Override
    public void toggleLike(LoveDTO loveDTO) {

        if (loveDTO.getId() == null) {
            throw new RuntimeException("该评论或文章已被删除");
        }

        Long entityId = loveDTO.getId();
        //点赞锁
        String lockKey = String.format("love:lock:%s", entityId);

        //获取分布式锁
        Boolean lock = tryLock(lockKey);

        try {
            if (!lock) {
                throw new RuntimeException("操作过于频繁");
            }
            //获取用户id
            Long userId = SecurityUtils.getCurrentUserId();

            //执行点赞操作
            doToggleLike(loveDTO);

            log.info("用户 {} 操作实体 {} 成功", userId, loveDTO.getId());
        }finally {
            releaseLock(lockKey);
        }

    }



    public void doToggleLike(LoveDTO loveDTO) {

        log.info("loveDTO:{}", loveDTO);

        //实体id
        Long entityId = loveDTO.getId();
        //点赞类型id
        Long loveTypeId = loveDTO.getLoveTypeId();
        //查询具体点赞类型
        //String typeName = loveTypeMapper.selectById(loveTypeId).getTypeName();
        if (!isValidLoveTypeId(loveTypeId)) {
            throw new IllegalArgumentException("无效的点赞类型ID: " + loveTypeId);
        }

        //获取当前用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //获取detail类
        UserDetail userDetail = (UserDetail) auth.getPrincipal();
        //获取userId;
        Long userId = userDetail.getUserId();

        //判断用户是否登录
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        //查询对应的用户点赞表信息
        //UserLove userLove = userLoveMapper.selectByUserAndEntity(userId,entityId,loveTypeId);
        //查询不到信息
        //数据库唯一索引
        try {
            //执行关注操作,封装对象
           // 尝试插入，如果已存在会抛出DuplicateKeyException
            UserLove newLove = new UserLove();
            newLove.setUserId(userId);
            newLove.setEntityId(entityId);
            newLove.setLoveTypeId(loveTypeId);
            newLove.setCreateTime(LocalDateTime.now());

            // 插入数据库
            userLoveMapper.insert(newLove);

            // 更新文章点赞数 +1
            if (loveTypeId == 1) { // 假设1是文章点赞类型
                updateArticleLoveCount(entityId, 1);
            }else {
                updateCommentLoveCount(entityId,1);
            }

            //在事务提交后同步更新缓存
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            updateLoveCache(entityId, loveTypeId,false);
                        }
                    }
            );


            log.info("用户 {} 点赞实体 {} 成功", userId, entityId);

        } catch (DuplicateKeyException e){

            // 插入失败 → 已点赞，执行取消点赞

            //删除用户点赞表中的对应数据
            userLoveMapper.deleteByUserAndEntity(userId, entityId, loveTypeId);

            // 更新文章点赞数 -1
            if (loveTypeId == 1) { // 假设1是文章点赞类型
                updateArticleLoveCount(entityId, -1);
            }else {
                updateCommentLoveCount(entityId,-1);
            }

            //在事务提交后同步更新缓存
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            updateLoveCache(entityId, loveTypeId,true);
                        }
                    }
            );

            log.info("用户 {} 取消点赞实体 {}", userId, entityId);
        }
    }


    // 检查用户是否对某实体点过赞
    @Override
    @Transactional
    public Boolean isEntityLovedByUser(Long entityId, Long loveTypeId) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (entityId == null || loveTypeId == null || userId == null) {
            log.info("传入的实体id为空");
            return false;
        }

        // Redis键设计：love:user:{userId}:{loveTypeId}
        String userLikeKey = String.format("love:user:%s:%s", userId, loveTypeId);

        try {
            // 检查用户点赞集合中是否包含该实体
            Boolean isLoved = stringRedisTemplate.opsForSet().isMember(userLikeKey, entityId.toString());

            // 缓存中查不到或者返回false
            if (isLoved == null || !isLoved) {
                log.info("文章：{}点赞信息没有被缓存",entityId);
                // 从数据库中加载用户点赞关系
                UserLove userLove = userLoveMapper.selectByUserAndEntity(userId, entityId, loveTypeId);
                boolean lovedInDB = userLove != null;

                // 如果数据库中存在点赞关系，更新到Redis缓存中
                if (lovedInDB) {
                    log.info("数据库中查询到数据，缓存到redis中");
                    stringRedisTemplate.opsForSet().add(userLikeKey, entityId.toString());
                    stringRedisTemplate.expire(userLikeKey, CACHE_TTL, TimeUnit.SECONDS);
                }
                return lovedInDB;
            } else {
                return true;
            }
        } catch (Exception e) {
            log.error("检查用户点赞状态失败: userId={}, entityId={}, loveTypeId={}",
                    userId, entityId, loveTypeId, e);
            // 发生异常时尝试从数据库直接获取
            try {
                UserLove userLove = userLoveMapper.selectByUserAndEntity(userId, entityId, loveTypeId);
                return userLove != null;
            } catch (Exception ex) {
                log.error("从数据库检查用户点赞状态也失败", ex);
                return false; // 最终保底返回false
            }
        }
    }


    // 获取实体的点赞数
    @Override
    @Transactional
    public String getEntityLoveCount(Long entityId, Long loveTypeId) {
        if (entityId == null || loveTypeId == null) {
            return "0";
        }

        String countKey = String.format("love:count:%s:%s", loveTypeId, entityId);
        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        log.info("redis中的缓存的点赞数:{}", countStr);

        try {
            //缓存中查不到
            if (countStr == null) {
                //判断是文章还评论
                if (loveTypeId == 1) {
                    //从数据库中加载
                    Article article = articleMapper.selectById(entityId);
                    //获取点赞数
                    Long loveCount = article.getLove();
                    log.info("(文章)数据库中的点赞数:{}", loveCount);
                    //更新到redis中
                    stringRedisTemplate.opsForValue().set(countKey, String.valueOf(loveCount), CACHE_TTL, TimeUnit.SECONDS);
                    log.info("(文章)更新到redis中的点赞数:{}", loveCount);
                    return loveCount.toString();
                }else {
                    //从数据库中加载
                    Comment comment = commentMapper.selectById(entityId);
                    //获取点赞数
                    Long loveCount = comment.getLove();
                    log.info("(评论)数据库中的点赞数:{}", loveCount);
                    //更新到redis中
                    stringRedisTemplate.opsForValue().set(countKey, String.valueOf(loveCount), CACHE_TTL, TimeUnit.SECONDS);
                    log.info("(评论)更新到redis中的点赞数:{}", loveCount);
                    return loveCount.toString();
                }

            }else {
                return countStr;
            }
        }catch (Exception e) {
            log.error("获取实体点赞数失败: entityId={}, loveTypeId={}", entityId, loveTypeId, e);
            // 发生异常时尝试从数据库直接获取
            try {
                if (Objects.equals(loveTypeId, 1L)) {
                    Article article = articleMapper.selectById(entityId);
                    return article == null ? "0" : String.valueOf(article.getLove());
                } else {
                    Comment comment = commentMapper.selectById(entityId);
                    return comment == null ? "0" : String.valueOf(comment.getLove());
                }
            } catch (Exception ex) {
                log.error("从数据库获取点赞数也失败", ex);
                return "0";
            }
        }
    }


    private boolean isValidLoveTypeId(Long loveTypeId) {
        // 示例：检查是否在有效范围内
        return loveTypeId != null && loveTypeId >= 1 && loveTypeId <= 10;
    }




    //同步更新点赞缓存
    protected void updateLoveCache(Long entityId, Long loveTypeId,boolean isLoved) {

        Long userId = SecurityUtils.getCurrentUserId();
        try {
            //当前用户id
            //redis键设计
            //点赞过的用户集合,将点赞后的用户添加进集合里: love:user:userId:loveTypeId,value为实体id
            String userLikeKey = String.format("love:user:%s:%s", userId, loveTypeId);
            //实体（也就说文章或者评论）被点赞的集合:love:type:loveTypeId:entityId,value为用户id
            String loveTypeLikeKey = String.format("love:type:%s:%s", loveTypeId, entityId);
            // 实体点赞数键: love:count:{entityType}:{entityId},value为实体id
            String countKey = String.format("love:count:%s:%s", loveTypeId, entityId);

            List<String> keys = Arrays.asList(userLikeKey, loveTypeLikeKey, countKey);

            Object[] args = new Object[] {
                    entityId.toString(),
                    userId.toString(),
                    String.valueOf(CACHE_TTL)
            };


            //判断当前用户有没有点赞
            if (isLoved) {

                Long result =  stringRedisTemplate.execute(
                        UNLOVE_SCRIPT,keys, args);;

                log.info("当前用户的点赞数更新为:{}",result);

            } else {

                Long result =  stringRedisTemplate.execute(
                        LOVE_SCRIPT,keys, args);;

                log.info("当前用户的点赞数更新为:{}",result);
            }

        } catch (Exception e) {
            log.error("更新点赞缓存失败: userId={}, entityId={}, loveTypeId={}",
                    userId, entityId, loveTypeId, e);
        }
    }

    /**
     * 更新文章点赞数
     */
    private void updateArticleLoveCount(Long articleId, int delta) {
        try {
            // 先查询当前文章
            Article article = articleMapper.selectById(articleId);
            if (article != null) {
                // 计算新的点赞数，确保不会小于0
                int newLoveCount = (int) Math.max(0, article.getLove() + delta);
                article.setLove((long) newLoveCount);
                articleMapper.updateById(article);
                log.info("更新文章 {} 点赞数: {}", articleId, newLoveCount);
            } else {
                log.warn("文章不存在: {}", articleId);
            }
        } catch (Exception e) {
            log.error("更新文章点赞数失败: articleId={}, delta={}", articleId, delta, e);
        }
    }

    /**
     * 更新文章点赞数
     */
    private void updateCommentLoveCount(Long commentId, int delta) {
        try {
            // 先查询当前文章
            Comment comment = commentMapper.selectById(commentId);
            if (comment != null) {
                // 计算新的点赞数，确保不会小于0
                Long newLoveCount = Math.max(0, comment.getLove() + delta);
                comment.setLove(newLoveCount);
                commentMapper.updateById(comment);
                log.info("更新文章 {} 点赞数: {}", commentId, newLoveCount);
            } else {
                log.warn("文章不存在: {}", commentId);
            }
        } catch (Exception e) {
            log.error("更新文章点赞数失败: articleId={}, delta={}", commentId, delta, e);
        }
    }

    /**
     * 获取锁
     */
    private Boolean tryLock(String lockKey) {
        try{
            Boolean isLocked = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey,"1",Love_Lock_Timeout, TimeUnit.SECONDS);
            return isLocked;

        }catch (Exception e){
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
