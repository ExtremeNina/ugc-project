package com.example.onlyone.Utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LoveLuaScript {


    // Lua脚本：点赞操作
    private static final String LIKE_SCRIPT =
            // KEYS[1]: userLikeKey (love:user:{userId}:{loveTypeId})
            // KEYS[2]: loveTypeLikeKey (love:type:{loveTypeId}:{entityId})
            // KEYS[3]: countKey (love:count:{loveTypeId}:{entityId})
            // ARGV[1]: entityId
            // ARGV[2]: userId
            // ARGV[3]: TTL (过期时间，单位秒)
            // Lua脚本：点赞操作 - 修正版
            "local userKey = KEYS[1] " +
            "local typeKey = KEYS[2] " +  // 变量名统一为 typeKey
            "local countKey = KEYS[3] " +
            "local entityId = ARGV[1] " +
            "local userId = ARGV[2] " +
            "local ttl = ARGV[3] " +  // 统一使用小写 ttl
            "" +
            "-- 执行点赞操作 " +
            "redis.call('SADD', userKey, entityId) " +
            "redis.call('SADD', typeKey, userId) " +
            "local count = redis.call('INCR', countKey) " +
            "" +
            "-- 设置过期时间 " +
            "redis.call('EXPIRE', userKey, ttl) " +
            "redis.call('EXPIRE', typeKey, ttl) " +
            "redis.call('EXPIRE', countKey, ttl) " +
            "" +
            "-- 返回新的点赞数 " +
            "return count";



    // Lua脚本：取消点赞操作
    private static final String UNLIKE_SCRIPT =
            "local userKey = KEYS[1] " +
            "local typeKey = KEYS[2] " +
            "local countKey = KEYS[3] " +
            "local entityId = ARGV[1] " +
            "local userId = ARGV[2] " +
            "local ttl = ARGV[3] " +
            "" +
            "-- 执行取消点赞操作 " +
            "redis.call('SREM', userKey, entityId) " +
            "redis.call('SREM', typeKey, userId) " +
            "local count = redis.call('DECR', countKey) " +
            "" +
            "-- 确保计数不小于0 " +
            "if count < 0 then " +
            "   count = 0 " +
            "   redis.call('SET', countKey, count) " +
            "end " +
            "" +
            "-- 设置过期时间 " +
            "redis.call('EXPIRE', userKey, ttl) " +
            "redis.call('EXPIRE', typeKey, ttl) " +
            "redis.call('EXPIRE', countKey, ttl) " +
            "" +
            "-- 返回新的点赞数 " +
            "return count";




    private static final String TOGGLE_LIKE_SCRIPT =
            "local userKey = KEYS[1] " +
            "local typeKey = KEYS[2] " +
            "local countKey = KEYS[3] " +
            "local entityId = ARGV[1] " +
            "local userId = ARGV[2] " +
            "local ttl = ARGV[3] " +
            "" +
            "-- 检查用户是否点赞了这个实体 " +
            "local isMember = redis.call('SISMEMBER', userKey, entityId) " +
            "" +
            "if isMember == 1 then " +
            "   -- 已点赞，执行取消点赞 " +
            "   redis.call('SREM', userKey, entityId) " +
            "   redis.call('SREM', typeKey, userId) " +
            "   local count = redis.call('DECR', countKey) " +
            "   if count < 0 then " +
            "       count = 0 " +
            "       redis.call('SET', countKey, count) " +
            "   end " +
            "   -- 设置过期时间 " +
            "   redis.call('EXPIRE', userKey, ttl) " +
            "   redis.call('EXPIRE', typeKey, ttl) " +
            "   redis.call('EXPIRE', countKey, ttl) " +
            "   return {0, count} " +
            "else " +
            "   -- 未点赞，执行点赞 " +
            "   redis.call('SADD', userKey, entityId) " +
            "   redis.call('SADD', typeKey, userId) " +
            "   local count = redis.call('INCR', countKey) " +
            "   -- 设置过期时间 " +
            "   redis.call('EXPIRE', userKey, ttl) " +
            "   redis.call('EXPIRE', typeKey, ttl) " +
            "   redis.call('EXPIRE', countKey, ttl) " +
            "   return {1, count} " +
            "end";

    private final DefaultRedisScript<Long> likeScript;
    private final DefaultRedisScript<Long> unlikeScript;
    private final DefaultRedisScript<List> toggleScript;

    public LoveLuaScript() {

        // 初始化点赞脚本
        likeScript = new DefaultRedisScript<>();
        likeScript.setScriptText(LIKE_SCRIPT);
        likeScript.setResultType(Long.class);

        // 初始化取消点赞脚本
        unlikeScript = new DefaultRedisScript<>();
        unlikeScript.setScriptText(UNLIKE_SCRIPT);
        unlikeScript.setResultType(Long.class);


        // 初始化切换脚本
        // 初始化切换脚本
        toggleScript = new DefaultRedisScript<>();
        toggleScript.setScriptText(TOGGLE_LIKE_SCRIPT);
        toggleScript.setResultType(List.class);

    }


    public Long executeLike(StringRedisTemplate redisTemplate,
                            List<String> keys, Object... args) {
        try {
            return redisTemplate.execute(likeScript, keys, args);
        } catch (Exception e) {
            log.error("执行点赞Lua脚本失败: keys={}, args={}", keys, args, e);
            return null;
        }
    }

    public Long executeUnlike(StringRedisTemplate redisTemplate,
                              List<String> keys, Object... args) {
        try {
            return redisTemplate.execute(unlikeScript, keys, args);
        } catch (Exception e) {
            log.error("执行取消点赞Lua脚本失败: keys={}, args={}", keys, args, e);
            return null;
        }
    }

    public List executeToggle(StringRedisTemplate redisTemplate,
                              List<String> keys, Object... args) {
        try {
            return redisTemplate.execute(toggleScript, keys, args);
        } catch (Exception e) {
            log.error("执行切换点赞Lua脚本失败: keys={}, args={}", keys, args, e);
            return null;
        }
    }

}
