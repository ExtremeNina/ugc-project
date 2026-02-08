package com.example.onlyone.Utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.List;


@Component
@Slf4j
public class FollowLuaScript {

    private final DefaultRedisScript<Long> followScript;
    private final DefaultRedisScript<Long> unfollowScript;

    public FollowLuaScript() {
        followScript = new DefaultRedisScript<>();
        followScript.setResultType(Long.class);

        unfollowScript = new DefaultRedisScript<>();
        unfollowScript.setResultType(Long.class);

    }

    /**
     * 执行关注脚本
     */
    public Long executeFollow(StringRedisTemplate redisTemplate,
                              List<String> keys, Object... args) {
        return redisTemplate.execute(followScript, keys, args);
    }

    /**
     * 执行取消关注脚本
     */
    public Long executeUnfollow(StringRedisTemplate redisTemplate,
                                List<String> keys, Object... args) {
        return redisTemplate.execute(unfollowScript, keys, args);
    }



}
