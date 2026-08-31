package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.Entity.Article;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Feed 流异步任务（手动线程池版，不使用 @Async 注解）
 *
 * 为什么用手动提交代替 @Async：
 *  - @Async 只能" fire-and-forget "，提交策略、拒绝处理、返回值都由框架接管，不够灵活；
 *  - 手动持有线程池引用后：
 *      1) 可以自由选择提交时机（比如攒批后再提交）；
 *      2) 可以自定义拒绝策略（本类的兜底是记日志放弃，也可改为 CallerRuns 同步执行）；
 *      3) 换成 submit/CompletableFuture 即可拿到 Future 做结果回调；
 *      4) 线程池参数全部集中在 AsyncConfig，便于统一调优和监控。
 *
 * 线程池来源：复用 AsyncConfig 中已定义的 "cacheExecutor"（core 10 / max 30 / queue 500），
 * 与其它缓存类异步任务共用资源池，避免线程数膨胀。
 */
@Service
@Slf4j
public class FeedAsyncService {

    /** 注入统一管理的线程池，代替 @Async 的隐式代理调度 */
    @Resource(name = "cacheExecutor")
    private Executor cacheExecutor;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 时间线收件箱 Key 前缀与 TTL，必须与 DynamicServiceImpl 保持一致 */
    public static final String TIMELINE_KEY_PREFIX = "user:timeline:";
    public static final long TIMELINE_TTL_DAYS = 7;

    /**
     * 异步回填用户收件箱（冷启动 / 收件箱过期回源 DB 后调用）
     *
     * 调用方语义：submitBackfill 提交任务后立即返回，不阻塞当前请求线程；
     * 真正的 Redis 写入在 cacheExecutor 线程池中由 doBackfill 执行。
     *
     * @param userId   用户 id
     * @param articles 回源查到的文章列表（ZSet score 用发布时间毫秒值）
     */
    public void backfillTimeline(Long userId, List<Article> articles) {
        // 空参数直接返回，避免无意义的任务提交
        if (userId == null || CollectionUtils.isEmpty(articles)) {
            return;
        }

        try {
            // 将回填任务提交到线程池：主请求线程到此结束，写入由后台线程完成
            cacheExecutor.execute(() -> doBackfillTimeline(userId, articles));
        } catch (RejectedExecutionException e) {
            // 线程池队列已满（默认 AbortPolicy 会抛此异常）：
            // 回填是"锦上添花"的优化，放弃即可，下次请求会再次回源，不影响正确性
            log.warn("回填任务被线程池拒绝（队列已满），放弃本次回填, userId={}", userId);
        }
    }

    /**
     * 真正的回填逻辑，运行在 cacheExecutor 线程池的工作线程中
     */
    private void doBackfillTimeline(Long userId, List<Article> articles) {
        try {
            String timelineKey = TIMELINE_KEY_PREFIX + userId;
            // pipeline：把所有文章的 (id, 发布时间) 一次网络往返写入 ZSet
            stringRedisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                    StringRedisTemplate template = (StringRedisTemplate) operations;
                    for (Article article : articles) {
                        // 没有发布时间的文章跳过（无法作为 ZSet 排序分数）
                        if (article.getCreateTime() == null) {
                            continue;
                        }
                        long score = article.getCreateTime()
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        template.opsForZSet().add(timelineKey, article.getId().toString(), score);
                    }
                    // 重置 TTL，与推送路径保持一致的 7 天过期策略
                    template.expire(timelineKey, TIMELINE_TTL_DAYS, TimeUnit.DAYS);
                    return null;
                }
            });
            log.info("异步回填用户 {} 收件箱完成，共 {} 篇文章", userId, articles.size());
        } catch (Exception e) {
            // 回填失败只影响下次仍走 DB 回源，不影响本次响应
            log.error("异步回填收件箱失败, userId={}", userId, e);
        }
    }
}
