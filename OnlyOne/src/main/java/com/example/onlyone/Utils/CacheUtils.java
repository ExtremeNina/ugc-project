package com.example.onlyone.Utils;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.onlyone.VO.LogicalExpire;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


//缓存工具类
@Component
@Slf4j
public class CacheUtils {

    //重建缓存线程池
    public ExecutorService threadPool;

    //初始化线程池
    @PostConstruct
    private void initThreadPool() {
        threadPool = new ThreadPoolExecutor(
                3,
                5,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(50),
                new ThreadFactory() {
                    private final AtomicInteger atomicInteger = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("cache-record-" + atomicInteger.getAndIncrement());
                        return thread;
                    }
                },
                //拒绝策略，由主线程执行
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        LogicalExpire logicalExpire = new LogicalExpire();
        logicalExpire.setDate(value);
        logicalExpire.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(logicalExpire));
    }



    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public <R,ID> R queryByPassThrough(String KeyPrefix , ID id , Class<R> clazz,
                                       Function<ID,R> dbFallback, Long time, TimeUnit timeUnit) {

        String key = KeyPrefix + id;
        //获取缓存值
        String value = stringRedisTemplate.opsForValue().get(key);
        //判断缓存是否为空或者为空字符串
        if(!StrUtil.isNotBlank(value)) {
            //缓存命中,序列化后返回
            return JSONUtil.toBean(value, clazz);
        }
        //缓存未命中则查询数据库
        R r = dbFallback.apply(id);
        if (r == null) {
            //数据库未命中，缓存穿透,redis写入空字符,设置过期时间
            stringRedisTemplate.opsForValue().set(key, "",time, timeUnit);
            return null;
        }else {
            //数据库命中,写入缓存
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r));
            return r;
        }
    }

    public <R,ID> R queryByLogicalExpire(String KeyPrefix , ID id , Class<R> clazz,
                                         Function<ID,R> dbFallback, Long time, TimeUnit timeUnit) {

        String key = KeyPrefix + id;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(value)) {
            //缓存没命中
            return null;
        }
        //将字符串序列化
        LogicalExpire logicalExpire = JSONUtil.toBean(value,LogicalExpire.class);
        //获取数据
        R r = JSONUtil.toBean((JSONObject) logicalExpire.getDate(),clazz);
        //获取过期时间
        LocalDateTime expireTime = logicalExpire.getExpireTime();
        //判断是否过期
        if (expireTime != null && expireTime.isAfter(LocalDateTime.now())) {
            //未过期返回数据
            return r;
        }
        //获取分布式锁
        String lockKey = "cache:restart:" + id;
        Boolean isLock = tryLock(lockKey);
        if (isLock) {
            //获取锁成功
            threadPool.submit(() -> {
                try {
                    //查询数据库
                    R newR = dbFallback.apply(id);
                    //写入缓存
                    this.setWithLogicalExpire(key, newR, time, timeUnit);
                }catch (Exception e) {
                    throw new RuntimeException("重建缓存失败");
                }finally {
                    unlock(lockKey);
                }
            });
        }
        // 7.1 再次查询缓存（可能在异步重建期间已更新）
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(jsonStr)) {
            // 缓存命中，解析并返回
            logicalExpire = JSONUtil.toBean(jsonStr, LogicalExpire.class);
            r = JSONUtil.toBean((JSONObject) logicalExpire.getDate(), clazz);
            expireTime = logicalExpire.getExpireTime();
            if (expireTime.isAfter(LocalDateTime.now())) {
                // 当前缓存数据未过期（可能是其他线程已重建）
                return r;
            }
        }
        //返回过期数据
        return r;
    }



    private Boolean tryLock(String key) {
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "", 5, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isLock);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

}
