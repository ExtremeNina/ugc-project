package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.onlyone.Entity.Follow;
import com.example.onlyone.Entity.UserDetail;
import com.example.onlyone.Exception.BusinessException;
import com.example.onlyone.Mapper.FollowMapper;
import com.example.onlyone.Service.FollowService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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

/**
 * 关注服务实现
 *
 * 优化说明：
 * 1. toggleFollow 由原来的异常驱动（try-catch DuplicateKeyException）改为先查后判：
 *    先 selectByUserId 查询是否已存在关注记录，存在则删除（取消关注），不存在则插入（关注）。
 *    消除了每次取消关注都会抛异常的堆栈开销。
 * 2. 用 BusinessException 替代 RuntimeException，提供明确的 HTTP 状态码（400）。
 * 3. Redis 缓存采用写后异步更新（TransactionSynchronization.afterCommit），事务提交后才写缓存，
 *    避免 DB 回滚后缓存数据不一致。
 */
@Service
@Slf4j
public class FollowServiceImpl implements FollowService {

    @Resource
    private FollowMapper followMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final long CACHE_TTL = 7 * 24 * 60 * 60L;

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

    /**
     * 关注/取消关注（toggle 模式）
     *
     * 优化前：依赖数据库唯一索引 + try-catch DuplicateKeyException 驱动 toggle。
     * 优化后：先查 selectByUserId，存在则删除（取关），不存在则插入（关注）。
     *
     * 缓存更新策略：事务提交后（afterCommit）通过 Lua 脚本原子更新 Redis。
     *
     * @param authorId 被关注的用户 ID
     * @return true = 关注成功，false = 取消关注成功
     */
    @Override
    @Transactional
    public boolean toggleFollow(Long authorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetail userDetail = (UserDetail) auth.getPrincipal();
        Long currentUserId = userDetail.getUserId();

        if (authorId == null) {
            throw BusinessException.badRequest("目标用户ID不能为空");
        }
        if (currentUserId.equals(authorId)) {
            throw BusinessException.badRequest("不能关注自己");
        }

        // 先查后判：查询当前是否已存在关注关系
        Follow existing = followMapper.selectByUserId(currentUserId, authorId);

        if (existing == null) {
            // 未关注 → 执行关注
            Follow follow = new Follow();
            follow.setFollowerId(currentUserId);
            follow.setFollowingId(authorId);
            follow.setCreateTime(LocalDateTime.now());
            followMapper.insert(follow);

            // 事务提交后异步更新 Redis 缓存，保证数据一致性
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            updateFollowCache(currentUserId, authorId, true);
                        }
                    }
            );
            log.info("用户 {} 关注了用户 {}", currentUserId, authorId);
            return true;
        } else {
            // 已关注 → 执行取消关注
            followMapper.deleteByUserId(currentUserId, authorId);

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            updateFollowCache(currentUserId, authorId, false);
                        }
                    }
            );
            log.info("用户 {} 取消关注了用户 {}", currentUserId, authorId);
            return false;
        }
    }

    /**
     * 通过 Lua 脚本原子更新关注缓存
     *
     * 操作四个 Redis 键：
     * - user:following:{followerId}  ← 我关注的人集合 (SET)
     * - user:followers:{followingId} ← 关注我的人集合 (SET)
     * - user:fansCount:{followingId} ← 粉丝数 (STRING)
     * - user:followingCount:{followerId} ← 关注数 (STRING)
     */
    protected void updateFollowCache(Long followerId, Long followingId, boolean isFollow) {
        String userFollowingKey = String.format("user:following:%s", followerId);
        String userFollowersKey = String.format("user:followers:%s", followingId);
        String fansCountKey = String.format("user:fansCount:%s", followingId);
        String followingCountKey = String.format("user:followingCount:%s", followerId);

        List<String> keys = Arrays.asList(
                userFollowingKey,
                userFollowersKey,
                fansCountKey,
                followingCountKey
        );

        Object[] args = new Object[]{
                followingId.toString(),
                followerId.toString(),
                String.valueOf(CACHE_TTL)
        };

        Long result;
        if (isFollow) {
            result = stringRedisTemplate.execute(FOlLOW_SCRIPT, keys, args);
            if (result != null && result != -1) {
                log.info("用户 {} 关注用户 {} 成功，新粉丝数: {}", followerId, followingId, result);
            }
        } else {
            result = stringRedisTemplate.execute(UNFOlLOW_SCRIPT, keys, args);
            if (result != null && result != -1) {
                log.info("用户 {} 取消关注用户 {}，新粉丝数: {}", followerId, followingId, result);
            }
        }
    }

    /**
     * 判断是否已关注
     *
     * 缓存优先策略：先用 Redis SET 的 isMember 判断，命中则直接返回。
     * 未命中时查 DB 并回写缓存（Cache-Aside 模式）。
     */
    @Override
    public Boolean isFollowing(Long followerId, Long followingId) {
        try {
            String userFollowingKey = String.format("user:following:%s", followerId);
            Boolean isMember = stringRedisTemplate.opsForSet()
                    .isMember(userFollowingKey, followingId.toString());

            if (Boolean.TRUE.equals(isMember)) {
                return true;
            }

            Follow follow = followMapper.selectByUserId(followerId, followingId);
            boolean result = follow != null;

            if (result) {
                stringRedisTemplate.opsForSet().add(userFollowingKey, followingId.toString());
                stringRedisTemplate.expire(userFollowingKey, CACHE_TTL, TimeUnit.SECONDS);
            }
            return result;
        } catch (Exception e) {
            log.error("检查关注关系失败: followerId={}, followingId={}", followerId, followingId, e);
            Follow follow = followMapper.selectByUserId(followerId, followingId);
            return follow != null;
        }
    }

    @Override
    public Long getFansCount(Long userId) {
        try {
            String fansCountKey = String.format("user:fansCount:%s", userId);
            String countStr = stringRedisTemplate.opsForValue().get(fansCountKey);

            if (countStr != null) {
                return Long.parseLong(countStr);
            }

            Long count = followMapper.countFollowers(userId);
            stringRedisTemplate.opsForValue()
                    .set(fansCountKey, String.valueOf(count), CACHE_TTL, TimeUnit.SECONDS);
            return count;
        } catch (Exception e) {
            log.error("获取粉丝数失败: userId={}", userId, e);
            return followMapper.countFollowers(userId);
        }
    }

    @Override
    public Long getFollowingCount(Long userId) {
        try {
            String followingCountKey = String.format("user:followingCount:%s", userId);
            String countStr = stringRedisTemplate.opsForValue().get(followingCountKey);

            if (countStr != null) {
                return Long.parseLong(countStr);
            }

            Long count = followMapper.countFollowing(userId);
            stringRedisTemplate.opsForValue()
                    .set(followingCountKey, String.valueOf(count), CACHE_TTL, TimeUnit.SECONDS);
            return count;
        } catch (Exception e) {
            log.error("获取关注数失败: userId={}", userId, e);
            return followMapper.countFollowing(userId);
        }
    }

    @Override
    public List<Long> getFanList(Long userId) {
        String userFollowersKey = String.format("user:followers:%s", userId);
        Set<String> fanIds = stringRedisTemplate.opsForSet().members(userFollowersKey);

        if (fanIds != null && !fanIds.isEmpty()) {
            return fanIds.stream()
                    .filter(idStr -> !idStr.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }

        // MP lambda 查询（替代手写 @Select）：列名由方法引用保证类型安全
        List<Follow> followList = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowingId, userId));
        List<Long> followerIds = followList.stream().map(Follow::getFollowerId).toList();

        if (!followerIds.isEmpty()) {
            String[] memberArray = followerIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(userFollowersKey, memberArray);
            stringRedisTemplate.expire(userFollowersKey, CACHE_TTL, TimeUnit.SECONDS);
        }
        return followerIds;
    }
}
