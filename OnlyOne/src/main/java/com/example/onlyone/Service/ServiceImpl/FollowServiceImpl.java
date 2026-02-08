package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.Entity.Follow;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Entity.UserDetail;
import com.example.onlyone.Mapper.FollowMapper;
import com.example.onlyone.Service.FollowService;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Resource
    private FollowMapper followMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 缓存过期时间
    private static final long CACHE_TTL = 7 * 24 * 60 * 60L;


    //关注功能的lua脚本
    private static final DefaultRedisScript<Long> FOlLOW_SCRIPT;
    private static final DefaultRedisScript<Long> UNFOlLOW_SCRIPT;
    static {

        FOlLOW_SCRIPT = new DefaultRedisScript<>();
        FOlLOW_SCRIPT.setLocation(new ClassPathResource("FollowLuaScript.lua"));
        FOlLOW_SCRIPT.setResultType(Long.class);

        UNFOlLOW_SCRIPT = new DefaultRedisScript<>();
        UNFOlLOW_SCRIPT.setLocation(new ClassPathResource("unFollowLuaScript.lua"));
        UNFOlLOW_SCRIPT.setResultType(Long.class);

    }


    @Override
    @Transactional
    public boolean toggleFollow(Long authorId) {

        //获取当前用户id
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetail userDetail = (UserDetail) auth.getPrincipal();
        Long currentUserId = userDetail.getUserId();

        if (authorId == null) {
            throw new RuntimeException("目标用户ID不能为空");
        }

        //不能关注自己
        if (currentUserId.equals(authorId)) {
            throw new RuntimeException("不能关注自己");
        }


        //当前用户未关注
        try {
            //构建对象
            Follow follow = new Follow();
            follow.setFollowerId(currentUserId);  // 当前用户是关注者
            follow.setFollowingId(authorId);      // 目标用户是被关注者
            follow.setCreateTime(LocalDateTime.now());
            //插入用户关注表
            followMapper.insert(follow);

            // 在事务提交后更新缓存 - 新API
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            updateFollowCache(currentUserId, authorId,true);
                        }
                    }
            );

            return true;

        }catch (DuplicateKeyException e){
            //取消关注
            followMapper.deleteByUserId(currentUserId,authorId);
            // 在事务提交后更新缓存
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            updateFollowCache(currentUserId, authorId, false);
                        }
                    }
            );

            return false;

        }

    }


    //同步更新关注缓存
    protected void updateFollowCache(Long followerId, Long followingId,boolean isFollow) {

        //followerId 为当前用户
        //following 为目标用户


        // 当前用户的关注列表（他关注了谁）- 存储他关注的所有用户ID
        String userFollowingKey = String.format("user:following:%s", followerId);

        // 目标用户的粉丝列表（谁关注了他）- 存储关注他的所有用户ID
        String userFollowersKey = String.format("user:followers:%s", followingId);

        // 目标用户的粉丝数
        String fansCountKey = String.format("user:fansCount:%s", followingId);

        // 当前用户的关注数
        String followingCountKey = String.format("user:followingCount:%s", followerId);

        List<String> keys = Arrays.asList(
                userFollowingKey,      // KEYS[1] - 关注者关注列表
                userFollowersKey,      // KEYS[2] - 被关注者粉丝列表
                fansCountKey,          // KEYS[3] - 被关注者粉丝计数
                followingCountKey      // KEYS[4] - 关注者关注计数
        );

        Object[] args;
        Long result;

        // 判断用户是否关注
        if (isFollow) {
            // 执行lua脚本，保证原子性

            args = new Object[]{
                    followingId.toString(),      // ARGV[1] - 被关注者ID
                    followerId.toString(),       // ARGV[2] - 关注者ID
                    String.valueOf(CACHE_TTL)    // ARGV[3] - TTL
            };

            result = stringRedisTemplate.execute(FOlLOW_SCRIPT, keys, args);

            if (result != null) {
                if (result == -1) {
                    log.info("用户 {} 已关注用户 {}，无需重复关注", followerId, followingId);
                } else {
                    log.info("用户 {} 关注用户 {} 成功，新粉丝数: {}", followerId, followingId, result);
                }
            } else {
                log.error("关注操作失败，Lua脚本返回null");
            }
        } else {

            args = new Object[]{
                    followingId.toString(),      // ARGV[1] - 被关注者ID
                    followerId.toString(),       // ARGV[2] - 关注者ID
                    String.valueOf(CACHE_TTL)    // ARGV[3] - TTL
            };

            // 取消关注操作
            result = stringRedisTemplate.execute(UNFOlLOW_SCRIPT, keys, args);

            if (result != null) {
                if (result == -1) {
                    log.info("用户 {} 未关注用户 {}，无需取消关注", followerId, followingId);
                } else {
                    log.info("用户 {} 取消关注用户 {}，新粉丝数: {}", followerId, followingId, result);
                }
            } else {
                log.error("取消关注操作失败，Lua脚本返回null");
            }
        }
    }



    /**
     * 检查用户是否关注了目标用户
     */
    @Transactional
    public Boolean isFollowing(Long followerId, Long followingId) {


        try {
            // 当前用户的关注列表（他关注了谁）- 存储他关注的所有用户ID
            String userFollowingKey = String.format("user:following:%s", followerId);

            // 先查缓存
            Boolean isMember = stringRedisTemplate.opsForSet()
                    .isMember(userFollowingKey, followingId.toString());


            if (Boolean.TRUE.equals(isMember)) {
                return isMember;
            }

            // 缓存没有，查数据库
            Follow follow = followMapper.selectByUserId(followerId, followingId);
            boolean result = follow != null;

            // 更新缓存
            if (result) {
                //同步写入redis
                stringRedisTemplate.opsForSet().add(userFollowingKey, followingId.toString());
                stringRedisTemplate.expire(userFollowingKey, CACHE_TTL, TimeUnit.SECONDS);

                log.debug("修复关注关系缓存: followerId={}, followingId={}",
                        followerId, followingId);
            }

            return result;

        } catch (Exception e) {
            log.error("检查关注关系失败: followerId={}, followingId={}",
                    followerId, followingId, e);
            // 降级：直接查数据库
            Follow follow = followMapper.selectByUserId(followerId, followingId);
            return follow != null;
        }
    }

    /**
     * 获取用户的粉丝数
     */
    public Long getFansCount(Long userId) {
        try {
            String fansCountKey = String.format("user:fansCount:%s", userId);
            String countStr = stringRedisTemplate.opsForValue().get(fansCountKey);

            if (countStr != null) {
                return Long.parseLong(countStr);
            }

            // 缓存没有，查数据库
            Long count = followMapper.countFollowers(userId);

            // 更新缓存
            stringRedisTemplate.opsForValue()
                    .set(fansCountKey, String.valueOf(count), CACHE_TTL, TimeUnit.SECONDS);

            return count;

        } catch (Exception e) {
            log.error("获取粉丝数失败: userId={}", userId, e);
            return followMapper.countFollowers(userId);
        }
    }

    /**
     * 获取用户的关注数
     */
    public Long getFollowingCount(Long userId) {
        try {
            String followingCountKey = String.format("user:followingCount:%s", userId);
            String countStr = stringRedisTemplate.opsForValue().get(followingCountKey);

            if (countStr != null) {
                return Long.parseLong(countStr);
            }

            // 缓存没有，查数据库
            Long count = followMapper.countFollowing(userId);

            // 更新缓存
            stringRedisTemplate.opsForValue()
                    .set(followingCountKey, String.valueOf(count), CACHE_TTL, TimeUnit.SECONDS);

            return count;

        } catch (Exception e) {
            log.error("获取关注数失败: userId={}", userId, e);
            return followMapper.countFollowing(userId);
        }
    }

    /**
     * 获取用户的粉丝列表
     */
    public List<Long> getFanList(Long userId) {

        //用户粉丝列表的key
        String userFollowersKey = String.format("user:followers:%s", userId);
        Set<String> fanIds = stringRedisTemplate.opsForSet().members(userFollowersKey);

        // 判断缓存是否存在且不为空
        if (fanIds != null && !fanIds.isEmpty()) {
            // 将Set<String>转换为List<Long>
            return fanIds.stream()
                    .filter(idStr -> !idStr.isEmpty()) // 过滤空字符串
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }

        List<Long> followerIds;
        //不存在从数据库中加载
        List<Follow> followList = followMapper.selectByFollowingId(userId);
        //获取所有粉丝id
        followerIds = followList.stream().map(Follow::getFollowerId).toList();

        //写入缓存
        if (!followerIds.isEmpty()) {
            // 将Long类型转换为String类型存入Redis
            String[] memberArray = followerIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);

            stringRedisTemplate.opsForSet().add(userFollowersKey, memberArray);
            stringRedisTemplate.expire(userFollowersKey, CACHE_TTL, TimeUnit.SECONDS);
        }
        return followerIds;

    }



}
