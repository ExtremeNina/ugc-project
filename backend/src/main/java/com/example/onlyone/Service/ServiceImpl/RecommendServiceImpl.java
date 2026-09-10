package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.Entity.Article;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.UserActionMapper;
import com.example.onlyone.Properties.RecommendProperties;
import com.example.onlyone.Service.RecallService;
import com.example.onlyone.Service.RecommendService;
import com.example.onlyone.Utils.RedisKeyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐服务实现 —— 按需补货 + 缓存分页 + 异步补货 + 分布式锁
 *
 * <h3>核心流程</h3>
 * <ol>
 *   <li>缓存缺失：同步执行多路召回生成候选集，写入 ZSet，30 分钟过期自动轮换</li>
 *   <li>后续翻页：直接读 ZSet 分页，剩余不足阈值时触发异步补货</li>
 *   <li>异步补货：Redisson 非阻塞锁防并发，当前请求不等待补货结果</li>
 *   <li>兜底：候选数 &lt; 20 时降级为纯热门推荐</li>
 * </ol>
 *
 * <h3>缓存键</h3>
 * <ul>
 *   <li>候选集：rec:candidate:{userId}（ZSet，score 为排名分）</li>
 *   <li>已展示列表：rec:displayed:{userId}（List，用于去重）</li>
 *   <li>补货锁：rec:lock:refill:{userId}（Redisson 分布式锁）</li>
 * </ul>
 */
@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    /** 兜底降级阈值：候选数少于此值时降级为纯热门推荐 */
    private static final int FALLBACK_LIMIT = 20;

    @Resource
    private RecallService recallService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserActionMapper userActionMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private RecommendProperties props;
    @Resource
    private RedissonClient redissonClient;
    /** 自注入代理，用于调用异步方法（绕开 Spring AOP 代理限制） */
    @Resource
    @Lazy
    private RecommendServiceImpl self;
    /** 推荐补货专用线程池：Core=2, Max=4, Queue=100, CallerRunsPolicy */
    @Resource
    private ThreadPoolTaskExecutor recRefillExecutor;

    /**
     * 登录用户个性化推荐流
     * <p>
     * 候选集由 30 分钟 TTL 自动轮换，异步补货在剩余不足时自动触发，
     * 无需手动刷新。
     *
     * @param userId 用户 ID
     * @param offset 分页偏移量（第 offset 条开始）
     * @param limit  每页数量
     * @return 当前页的 Article 列表
     */
    @Override
    public List<Article> getFeedList(Long userId, int offset, int limit) {
        String cacheKey = RedisKeyUtils.candidateKey(userId);
        String displayKey = RedisKeyUtils.displayedKey(userId);

        // 1. 缓存缺失：同步生成候选集
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(cacheKey))) {
            generateCandidates(userId, cacheKey, displayKey, null);
        }

        // 2. 按需补货：候选集剩余不足阈值时，触发异步补货
        Long totalSize = stringRedisTemplate.opsForZSet().size(cacheKey);
        if (totalSize != null && totalSize > 0) {
            long remaining = totalSize - offset;
            if (remaining < props.getRefillThreshold()) {
                // 提取当前 offset 之后的剩余笔记作为种子（补货时保留，保证连续性）
                Set<String> seedSet = stringRedisTemplate.opsForZSet()
                        .reverseRange(cacheKey, offset, -1);
                List<Long> seedIds = seedSet != null
                        ? seedSet.stream().map(Long::valueOf).toList()
                        : Collections.emptyList();
                log.info("用户 {} 候选集剩余 {} 条（阈值 {}），触发异步补货，种子笔记 {} 条",
                        userId, remaining, props.getRefillThreshold(), seedIds.size());
                self.asyncRefillCandidate(userId, cacheKey, displayKey, seedIds);
            }
        }

        // 3. 从 ZSet 按分数倒序分页取当前页笔记 ID
        Set<String> pageSet = stringRedisTemplate.opsForZSet()
                .reverseRange(cacheKey, offset, offset + limit - 1);
        if (pageSet == null || pageSet.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> noteIds = pageSet.stream().map(Long::valueOf).collect(Collectors.toList());
        log.info("用户 {} 推荐笔记 offset={} limit={}: {}", userId, offset, limit, noteIds);

        // 4. 记录已展示（用于后续去重）
        recordDisplayed(displayKey, noteIds);

        // 5. 批量查询 Article 并保持顺序返回
        return mapToArticles(noteIds);
    }

    /**
     * 未登录用户纯热门推荐流
     * <p>
     * 直接从 hot:notes ZSet 分页取数据，无个性化、无缓存、无去重。
     *
     * @param offset 分页偏移
     * @param limit  每页数量
     * @return 当前页的热门 Article 列表
     */
    @Override
    public List<Article> getHotFeed(int offset, int limit) {
        Set<String> pageSet = stringRedisTemplate.opsForZSet()
                .reverseRange(RedisKeyUtils.hotNotesKey(), offset, offset + limit - 1);
        if (pageSet == null || pageSet.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> noteIds = pageSet.stream().map(Long::valueOf).collect(Collectors.toList());
        return mapToArticles(noteIds);
    }

    /**
     * 异步补货：在候选集即将耗尽时，后台重新生成候选集并替换缓存
     *
     * <h3>关键设计</h3>
     * <ul>
     *   <li>使用 CompletableFuture.runAsync 提交到专用线程池，不阻塞主线程</li>
     *   <li>Redisson 非阻塞锁 tryLock(0, 30s)，防止并发重复补货</li>
     *   <li>种子笔记（当前 offset 后的剩余笔记）赋予高优先级，确保翻页连续性</li>
     *   <li>当前请求不等待补货结果，直接基于旧缓存返回</li>
     * </ul>
     *
     * @param userId     用户 ID
     * @param cacheKey   候选集 Redis Key
     * @param displayKey 已展示列表 Redis Key
     * @param seeds      种子笔记 ID 列表（当前 offset 后尚未翻页的笔记）
     */
    public void asyncRefillCandidate(Long userId, String cacheKey, String displayKey, List<Long> seeds) {
        CompletableFuture.runAsync(() -> {
            String lockKey = RedisKeyUtils.refillLockKey(userId);
            RLock lock = redissonClient.getLock(lockKey);
            try {
                // tryLock(waitTime=0, leaseTime=30s)：非阻塞，未获取到锁立即返回 false
                if (!lock.tryLock(0, props.getRefillLockTimeout(), TimeUnit.SECONDS)) {
                    log.info("用户 {} 补货被跳过（其他线程正在补货中）", userId);
                    return;
                }
                log.info("用户 {} 开始异步补货，种子笔记数: {}", userId, seeds.size());
                generateCandidates(userId, cacheKey, displayKey, seeds);
                log.info("用户 {} 异步补货完成", userId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("用户 {} 补货被中断", userId, e);
            } catch (Exception e) {
                log.error("用户 {} 补货异常", userId, e);
            } finally {
                // 只有当前线程持有锁时才释放，避免误释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }, recRefillExecutor);
    }

    /**
     * 生成候选集（同步执行，由首次请求或异步补货调用）
     *
     * <h3>流程</h3>
     * <ol>
     *   <li>多路召回：协同过滤 + 热门 + 标签</li>
     *   <li>合并得分：取各召回路径的最大值</li>
     *   <li>去重：过滤已交互笔记 + 已展示笔记</li>
     *   <li>排序截断：按得分降序，截取前 candidatePoolSize 条</li>
     *   <li>兜底：候选数 &lt; 20 时降级为纯热门</li>
     *   <li>种子融合：补货时种子笔记放在列表最前面（保证翻页连续性）</li>
     *   <li>写入 ZSet：score = 排名分（第 1 名最高分，第 N 名 1 分），设置 TTL</li>
     * </ol>
     *
     * @param userId     用户 ID
     * @param cacheKey   候选集 Redis Key
     * @param displayKey 已展示列表 Redis Key
     * @param seeds      种子笔记 ID 列表（异步补货时传入，首次生成时为 null）
     */
    private void generateCandidates(Long userId, String cacheKey, String displayKey, List<Long> seeds) {
        // ---- 多路召回 ----
        Map<Long, Double> collab = recallService.collaborativeRecall(userId); // 协同过滤
        Map<Long, Double> hot = recallService.hotRecall();                    // 热门笔记
        Map<Long, Double> tag = recallService.tagRecall(userId);              // 标签匹配

        // ---- 合并得分（取最大值）----
        Map<Long, Double> merged = new HashMap<>();
        collab.forEach((k, v) -> merged.merge(k, v, Math::max));
        hot.forEach((k, v) -> merged.merge(k, v, Math::max));
        tag.forEach((k, v) -> merged.merge(k, v, Math::max));

        // ---- 去重：已交互 + 已展示 ----
        Set<Long> interacted = new HashSet<>(userActionMapper.selectInteractedNoteIds(userId));
        List<String> displayed = stringRedisTemplate.opsForList().range(displayKey, 0, -1);
        Set<Long> displayedSet = displayed != null
                ? displayed.stream().map(Long::valueOf).collect(Collectors.toSet())
                : Collections.emptySet();
        merged.keySet().removeIf(id -> interacted.contains(id) || displayedSet.contains(id));

        // ---- 排序截断 ----
        List<Long> candidateIds = merged.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(props.getCandidatePoolSize())
                .map(Map.Entry::getKey)
                .toList();

        // ---- 兜底：候选数不足时降级为纯热门 ----
        if (candidateIds.size() < FALLBACK_LIMIT) {
            hot.keySet().removeIf(id -> interacted.contains(id) || displayedSet.contains(id));
            candidateIds = hot.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(props.getCandidatePoolSize())
                    .map(Map.Entry::getKey)
                    .toList();
            log.info("用户 {} 候选数不足，降级为纯热门，得 {} 条", userId, candidateIds.size());
        }

        // ---- 种子融合：补货时种子笔记置顶，保证翻页连续性 ----
        if (seeds != null && !seeds.isEmpty()) {
            List<Long> finalIds = new ArrayList<>(seeds);
            for (Long id : candidateIds) {
                if (!finalIds.contains(id)) {
                    finalIds.add(id);
                }
            }
            if (finalIds.size() > props.getCandidatePoolSize()) {
                finalIds = finalIds.subList(0, props.getCandidatePoolSize());
            }
            candidateIds = finalIds;
        }

        // ---- 写入 Redis ZSet ----
        stringRedisTemplate.delete(cacheKey);
        if (!candidateIds.isEmpty()) {
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
            // score = candidateIds.size() - i，排名越高分数越大
            for (int i = 0; i < candidateIds.size(); i++) {
                tuples.add(new DefaultTypedTuple<>(
                        candidateIds.get(i).toString(),
                        (double) (candidateIds.size() - i)
                ));
            }
            stringRedisTemplate.opsForZSet().add(cacheKey, tuples);
            stringRedisTemplate.expire(cacheKey, Duration.ofMinutes(props.getCandidateCacheTtl()));
            log.info("用户 {} 候选集已写入 Redis，共 {} 条，TTL {} 分钟",
                    userId, candidateIds.size(), props.getCandidateCacheTtl());
        }
    }

    /**
     * 记录已展示笔记：将当前页笔记 ID 追加入已展示列表，并修剪长度
     * <p>
     * 已展示列表用于后续候选集生成时去重，避免重复推荐。
     *
     * @param displayKey 已展示列表 Redis Key
     * @param noteIds    当前页笔记 ID 列表
     */
    private void recordDisplayed(String displayKey, List<Long> noteIds) {
        for (Long id : noteIds) {
            stringRedisTemplate.opsForList().rightPush(displayKey, id.toString());
        }
        // 修剪列表长度，只保留最近 N 条
        stringRedisTemplate.opsForList().trim(displayKey, -props.getDisplayedMaxSize(), -1);
    }

    /**
     * 批量查询 Article 并保持与输入列表相同的顺序
     * <p>
     * MyBatis-Plus 的 selectBatchIds 底层使用 IN 查询，返回顺序不保证与输入一致，
     * 因此借助 Map 重排，确保最终返回顺序严格符合推荐排名。
     *
     * @param noteIds 已排好序的笔记 ID 列表
     * @return 与输入顺序对应的 Article 列表（已剔除数据库中不存在的 ID）
     */
    private List<Article> mapToArticles(List<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Article> articles = articleMapper.selectBatchIds(noteIds);
        // 转为 Map<id, Article> 方便 O(1) 查找
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        // 按原始 noteIds 顺序取出，过滤掉数据库中已删除的文章（null）
        return noteIds.stream()
                .map(articleMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
