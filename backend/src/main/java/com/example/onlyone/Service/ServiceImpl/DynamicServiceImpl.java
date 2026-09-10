package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.ContentStatus;
import com.example.onlyone.Entity.Follow;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.FollowMapper;
import com.example.onlyone.Mapper.UserLoveMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Rabbitmq.ArticleMessage;
import com.example.onlyone.Service.DynamicService;
import com.example.onlyone.Service.FollowService;
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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private UserMapper userMapper;
    @Resource
    private UserLoveMapper userLoveMapper;
    @Resource
    private FeedAsyncService feedAsyncService;

    /** 时间线收件箱 TTL：7 天 */
    private static final long TIMELINE_TTL_DAYS = 7;
    private static final String TIMELINE_KEY_PREFIX = "user:timeline:";

    /**
     * 推拉结合阈值：粉丝数超过该值的作者视为"大V"，发布时不推入收件箱，
     * 改为用户读 Feed 时现查（拉模式），避免大V发一篇文章产生上万次写放大
     */
    private static final int PUSH_FAN_THRESHOLD = 10_000;

    /** 单个用户收件箱最大保留条数，防止重度用户的时间线 ZSet 无限膨胀 */
    private static final int INBOX_MAX_SIZE = 1000;

    /** 点赞类型：1 = 文章点赞（与 LoveServiceImpl 的调用约定保持一致） */
    private static final Long LOVE_TYPE_ARTICLE = 1L;

    /** 用户点赞集合的 Redis Key 模板（与 LoveServiceImpl 中的格式保持一致） */
    private static final String LOVE_USER_KEY_FMT = "love:user:%s:%s";

    // ==================================================================================
    // 读路径：分页 + 推拉结合
    // ==================================================================================

    /**
     * 获取关注动态 Feed（分页）
     *
     * 整体流程：
     *  1. 收件箱不存在（新用户 / 7 天没动静过期）→ DB 回源，并异步回填收件箱；
     *  2. 收件箱存在 → 读推模式数据（普通作者文章，只取 id+时间分数，成本低）；
     *  3. 同时拉模式现查关注的大V最近文章（大V文章从未进过收件箱）；
     *  4. 两路按发布时间合并、按文章id去重 → 截取当前页 → 批量补齐文章详情。
     *
     * 注意：本方法只对"最终这一页"的文章做批量 DB 查询，排序分页全部基于轻量的 id+score 完成。
     */
    @Override
    public List<DyArticleVO> getNewArticles(int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        String timelineKey = TIMELINE_KEY_PREFIX + userId;

        // ---------- 1. 冷启动 / 收件箱过期：DB 回源 + 异步回填 ----------
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(timelineKey))) {
            log.info("用户 {} 收件箱不存在，从数据库回源加载", userId);
            List<Article> fromDb = loadRecentArticlesFromDB(userId);
            // 回源结果异步写回收件箱：不阻塞本次请求，下次即可命中 Redis 热路径
            feedAsyncService.backfillTimeline(userId, fromDb);
            return convertToVOList(pageOf(fromDb, page, size));
        }

        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - TIMELINE_TTL_DAYS * 24 * 60 * 60 * 1000L;

        // ---------- 2. 推模式：读收件箱 ZSet（只拿 id + score，无 DB 访问） ----------
        // articleId -> 排序分数（发布时间毫秒值）。用 Map 收集，后面和大V文章统一合并
        Map<Long, Double> candidates = new HashMap<>();
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(timelineKey, sevenDaysAgo, now);
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String value = tuple.getValue();
                if (value == null) {
                    continue;
                }
                // merge：同一篇文章若多来源，保留最新时间分数
                candidates.merge(Long.parseLong(value), tuple.getScore(), Math::max);
            }
        }

        // ---------- 3. 拉模式：现查关注的大V最近 7 天的文章 ----------
        List<Long> followedIds = getFollowedAuthorIds(userId);
        if (!CollectionUtils.isEmpty(followedIds)) {
            List<Long> bigVIds = filterBigVIds(followedIds);
            if (!bigVIds.isEmpty()) {
                // 大V文章从未被推入收件箱，这里读时现查（一条 SQL，带 limit）
                List<Article> bigVArticles = articleMapper.selectRecentByAuthors(
                        bigVIds, new Date(sevenDaysAgo), size);
                for (Article article : bigVArticles) {
                    if (article.getCreateTime() == null) {
                        continue;
                    }
                    // 与收件箱使用同一套分数体系（发布时间毫秒值），保证合并后排序一致
                    long score = article.getCreateTime()
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    candidates.merge(article.getId(), (double) score, Math::max);
                }
            }
        }

        // 收件箱和大V都没有内容 → 走兜底：每个关注作者的最新文章
        if (candidates.isEmpty()) {
            log.info("用户 {} 的时间流中没有最近7天的文章，查询关注作者的最新文章", userId);
            return getLatestArticlesFromFollowedAuthors(userId, page, size);
        }

        // ---------- 4. 合并排序 + 截取当前页 ----------
        // 按分数（发布时间）倒序排，跳过前 (page-1)*size 条，取当前页的 size 条 id
        List<Long> pageIds = candidates.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .skip((long) (page - 1) * size)
                .limit(size)
                .toList();
        if (pageIds.isEmpty()) {
            return List.of();
        }

        // ---------- 5. 只对这一页的 id 做一次批量 DB 查询，并按排序恢复顺序 ----------
        //（selectBatchIds 不保证返回顺序，必须按 pageIds 顺序显式还原）
        Map<Long, Article> articleMap = batchGetArticles(pageIds);
        List<Article> orderedArticles = pageIds.stream()
                .map(articleMap::get)
                .filter(Objects::nonNull)
                .toList();
        return convertToVOList(orderedArticles);
    }

    /**
     * 查询用户关注的所有作者 id（MP lambda 查询，替代 FollowMapper 里的手写 @Select）
     * .select(Follow::getFollowingId) 只查需要的一列，减少数据传输
     */
    private List<Long> getFollowedAuthorIds(Long userId) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .select(Follow::getFollowingId)
                        .eq(Follow::getFollowerId, userId))
                .stream()
                .map(Follow::getFollowingId)
                .toList();
    }

    /**
     * 从关注列表中筛出"大V"（fan 数超过推拉阈值）
     * fan 是 user 表的冗余粉丝数字段，一次 selectBatchIds 全部拿回，无需逐个 count
     */
    private List<Long> filterBigVIds(List<Long> followedIds) {
        List<User> users = userMapper.selectBatchIds(followedIds);
        return users.stream()
                // fan 是 int 基本类型（非空），直接与阈值比较
                .filter(user -> user.getFan() > PUSH_FAN_THRESHOLD)
                .map(User::getId)
                .toList();
    }

    // ==================================================================================
    // DB 回源与兜底
    // ==================================================================================

    /**
     * DB 回源：关注作者 7 天内已发布的文章（新用户 / 收件箱过期时走这里）
     * 查询结果由调用方异步回填收件箱，避免收件箱一失效每次请求都打 DB
     */
    private List<Article> loadRecentArticlesFromDB(Long userId) {
        List<Long> authorIds = getFollowedAuthorIds(userId);
        if (CollectionUtils.isEmpty(authorIds)) {
            log.info("用户 {} 没有关注任何作者", userId);
            return List.of();
        }
        long sevenDaysAgo = System.currentTimeMillis() - TIMELINE_TTL_DAYS * 24 * 60 * 60 * 1000L;
        // MP lambda 查询：列名由方法引用提供，重构改字段名时编译器直接报错（原来是字符串列名）
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Article::getAuthorId, authorIds)              // 作者在关注列表中
                .ge(Article::getCreateTime, new Date(sevenDaysAgo))   // 7 天内发布
                .eq(Article::getStatus, ContentStatus.APPROVED.value()) // 已发布
                .eq(Article::getIsDraft, 0)                           // 非草稿
                .orderByDesc(Article::getCreateTime)
                // 上限保护：回源最多带回收件箱容量条，配合异步回填刚好填满收件箱
                .last("LIMIT " + INBOX_MAX_SIZE);
        List<Article> articles = articleMapper.selectList(queryWrapper);
        log.info("从数据库为用户 {} 回源 {} 篇文章", userId, articles.size());
        return articles;
    }

    /**
     * 兜底：时间线为空时，查每个关注作者的最新文章
     * 优化点：原来这里是"每个作者一条 LIMIT 1 的 SQL"，关注 100 人就查 100 次；
     * 现在用 selectRecentByAuthors 合并为一条 SQL
     */
    private List<DyArticleVO> getLatestArticlesFromFollowedAuthors(Long userId, int page, int size) {
        log.info("为用户 {} 查询关注作者的最新文章（兜底）", userId);
        List<Long> authorIds = getFollowedAuthorIds(userId);
        if (CollectionUtils.isEmpty(authorIds)) {
            return List.of();
        }
        // 每人最新 1 篇 ≈ 取最近 authorIds.size() 篇再按作者去重，一条 SQL 完成
        long sevenDaysAgo = System.currentTimeMillis() - TIMELINE_TTL_DAYS * 24 * 60 * 60 * 1000L;
        List<Article> latest = articleMapper.selectRecentByAuthors(
                authorIds, new Date(sevenDaysAgo), authorIds.size());

        // 按作者去重，每个作者只留最新一篇（列表已按时间倒序，先出现的即最新）
        Map<Long, Article> latestPerAuthor = new HashMap<>();
        for (Article article : latest) {
            latestPerAuthor.putIfAbsent(article.getAuthorId(), article);
        }
        List<Article> articles = new ArrayList<>(latestPerAuthor.values());
        // 兜底结果同样分页返回
        return convertToVOList(pageOf(articles, page, size));
    }

    // ==================================================================================
    // VO 组装：批量化改造（原来的 N+1 查询是本接口最大的性能黑洞）
    // ==================================================================================

    /**
     * 批量组装 VO 列表
     *
     * 优化点（对照旧版 convertArticleToDyArticleVO 的逐篇处理）：
     *  - 作者信息：原来每篇文章 selectById 查一次 user → 现在 selectBatchIds 一次查回整页作者；
     *  - 点赞状态：原来每篇文章单独查 Redis（+可能查 DB）→ 现在 pipeline 一次网络往返批量查，
     *    缓存未命中的再用一条批量 SQL 兜底，语义与 isEntityLovedByUser 完全一致；
     *  - N 篇文章的组装成本从 2N 次查询降为 2 次查询，与文章数无关。
     */
    private List<DyArticleVO> convertToVOList(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) {
            return List.of();
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 1) 作者信息批量查：收集去重后的 authorId，一次 selectBatchIds
        Set<Long> authorIds = articles.stream()
                .map(Article::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> authorMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        // 2) 点赞状态批量查（pipeline + 批量 DB 兜底）
        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        Set<Long> lovedIds = batchGetLovedArticleIds(currentUserId, articleIds);

        // 3) 纯内存组装 VO
        return articles.stream()
                .map(article -> convertArticleToDyArticleVO(article, authorMap, lovedIds))
                .collect(Collectors.toList());
    }

    /**
     * 批量查询"当前用户对这些文章的点赞状态"
     *
     * 步骤：
     *  a. pipeline 批量 SMEMBER：所有文章的点赞判断合并为一次 Redis 网络往返；
     *  b. 缓存未命中的部分，用一条批量 SQL 查 user_love 表兜底（与单个查询的回退逻辑一致），
     *     保证缓存丢失时结果依然正确。
     */
    private Set<Long> batchGetLovedArticleIds(Long userId, List<Long> articleIds) {
        if (userId == null || CollectionUtils.isEmpty(articleIds)) {
            return Set.of();
        }
        String userLikeKey = String.format(LOVE_USER_KEY_FMT, userId, LOVE_TYPE_ARTICLE);

        // a. pipeline 批量 SMEMBER：结果顺序与 articleIds 一一对应
        List<Object> pipelineResults = stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                StringRedisTemplate template = (StringRedisTemplate) operations;
                for (Long articleId : articleIds) {
                    template.opsForSet().isMember(userLikeKey, articleId.toString());
                }
                return null;
            }
        });

        Set<Long> lovedIds = new HashSet<>();
        List<Long> cacheMissIds = new ArrayList<>();
        for (int i = 0; i < articleIds.size(); i++) {
            if (Boolean.TRUE.equals(pipelineResults.get(i))) {
                lovedIds.add(articleIds.get(i));
            } else {
                // Redis 未命中：可能真的没点赞，也可能只是缓存丢失，交给 DB 兜底判断
                cacheMissIds.add(articleIds.get(i));
            }
        }

        // b. 未命中部分批量查 DB（一条 SQL），保证语义与原 isEntityLovedByUser 一致
        if (!cacheMissIds.isEmpty()) {
            lovedIds.addAll(userLoveMapper.selectLovedEntityIds(
                    userId, LOVE_TYPE_ARTICLE, cacheMissIds));
        }
        return lovedIds;
    }

    /**
     * 单篇文章组装 VO（纯内存操作，查询已全部在批量阶段完成）
     */
    private DyArticleVO convertArticleToDyArticleVO(Article article,
                                                    Map<Long, User> authorMap,
                                                    Set<Long> lovedIds) {
        DyArticleVO dyArticleVO = new DyArticleVO();
        // 作者信息直接取批量查询的结果，不再访问数据库
        User author = authorMap.get(article.getAuthorId());
        if (author != null) {
            dyArticleVO.setAuthor(author.getUsername());
            dyArticleVO.setUserIcon(author.getIcon());
        }
        // 点赞状态直接取批量查询的结果，不再访问 Redis/DB
        dyArticleVO.setIsLove(lovedIds.contains(article.getId()));
        BeanUtils.copyProperties(article, dyArticleVO);
        // LocalDateTime -> 前端需要的字符串格式
        if (article.getCreateTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dyArticleVO.setPublishTime(article.getCreateTime().format(formatter));
        }
        dyArticleVO.setViewCount(article.getPageview());
        return dyArticleVO;
    }

    // ==================================================================================
    // 写路径：RabbitMQ 消费者（推模式，带推拉分级）
    // ==================================================================================

    /**
     * 监听文章推送队列
     *
     * 推拉结合关键点：拿到粉丝列表后先判断粉丝规模——
     *  - 普通作者：照旧 pipeline 写入每个粉丝的收件箱（写扩散）；
     *  - 大V（粉丝数 > PUSH_FAN_THRESHOLD）：直接 ACK 跳过，不做任何写入。
     *    其文章由读路径的"拉模式"在用户请求时现查，避免单条消息产生海量写。
     */
    @RabbitListener(queues = "articlePushQueue")
    private void articlePushListener(ArticleMessage articleMessage
            , Channel channel, Message message) throws IOException {

        Long articleId = articleMessage.getArticleId();
        Long authorId = articleMessage.getAuthorId();
        log.info("开始处理文章推送，文章ID: {}，作者ID: {}", articleId, authorId);

        // 使用分布式锁防止消息被重复消费（幂等保护）
        String lockKey = "push:listener:" + articleMessage.getMessageId();
        Boolean isLock = tryLock(lockKey);
        if (!isLock) {
            // 说明此时消息正在被处理或者已经被处理完，直接确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("消息正在被处理或已处理，直接确认，消息ID: {}", articleMessage.getMessageId());
            return;
        }

        try {
            // 获取粉丝 id 列表
            List<Long> fanIds = followService.getFanList(authorId);
            if (CollectionUtils.isEmpty(fanIds)) {
                log.info("作者 {} 没有粉丝，无需推送", authorId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // ===== 推拉结合分级：大V走拉模式，这里直接跳过推送 =====
            // 写放大分析：粉丝 10 万的作者发 1 篇 = 10 万次 ZADD，消费端会被拖垮；
            // 大V的文章改由读路径 selectRecentByAuthors 现查，写成本降为 0
            if (fanIds.size() > PUSH_FAN_THRESHOLD) {
                log.info("作者 {} 粉丝数 {} 超过阈值 {}，走拉模式，跳过推送，文章 {}",
                        authorId, fanIds.size(), PUSH_FAN_THRESHOLD, articleId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            log.info("找到 {} 个粉丝，准备推送文章 {}", fanIds.size(), articleId);
            pushToTimeline(articleId, fanIds);
            log.info("文章 {} 推送完成，成功推送给 {} 个粉丝", articleId, fanIds.size());

        } catch (Exception e) {
            log.error("处理文章推送消息失败，文章ID: {}, 作者ID: {}", articleId, authorId, e);
            // 重试机制：不重回队列，到达次数上限后进入死信队列
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            // 注意：异常路径必须先释放锁再返回，避免锁泄漏（原代码异常时仍会走到 finally）
            return;
        } finally {
            // 释放锁，防止阻塞
            releaseLock(lockKey);
        }
        // 消息处理成功，确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("消息确认成功");
    }

    /**
     * 把文章 id 推送进每个粉丝的时间流（收件箱）
     *
     * pipeline 一次网络往返完成 N 个粉丝的写入；每条时间线额外做两件事：
     *  - 清理 7 天前的旧文章（滚动过期）；
     *  - 按排名裁剪到 INBOX_MAX_SIZE 条以内（防止重度用户 ZSet 无限膨胀）。
     */
    private void pushToTimeline(Long articleId, List<Long> fanIds) {
        long currentTime = System.currentTimeMillis();
        // 时间限制为 7 天
        long sevenDaysAgo = currentTime - (TIMELINE_TTL_DAYS * 24 * 60 * 60 * 1000L);

        stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                StringRedisTemplate template = (StringRedisTemplate) operations;
                for (Long fanId : fanIds) {
                    String timelineKey = TIMELINE_KEY_PREFIX + fanId;
                    // 写入文章 id，score = 发布时间毫秒值（读路径按此分数排序）
                    template.opsForZSet().add(timelineKey, articleId.toString(), currentTime);
                    // 设置过期时间：7 天没有任何新内容则整个收件箱过期（下次走 DB 回源）
                    template.expire(timelineKey, TIMELINE_TTL_DAYS, TimeUnit.DAYS);
                    // 清除 7 天前的文章
                    template.opsForZSet().removeRangeByScore(timelineKey, 0, sevenDaysAgo);
                    // 收件箱容量保护：只保留最新 INBOX_MAX_SIZE 条（按分数排名裁剪）
                    template.opsForZSet().removeRange(timelineKey, 0, -(INBOX_MAX_SIZE + 1));
                }
                return null;
            }
        });
    }

    // ==================================================================================
    // 通用小工具
    // ==================================================================================

    /** 内存分页：对已排好序的列表截取当前页，越界自动收敛 */
    private List<Article> pageOf(List<Article> list, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = Math.min((safePage - 1) * safeSize, list.size());
        int to = Math.min(from + safeSize, list.size());
        return list.subList(from, to);
    }

    /** 批量查文章并转为 id -> Article 的 Map（供按排序结果恢复顺序使用）
     *  使用 MP 内置的 selectBatchIds，不再需要自定义 BatchArticles SQL */
    private Map<Long, Article> batchGetArticles(List<Long> articleIds) {
        return articleMapper.selectBatchIds(articleIds).stream()
                .filter(article -> ContentStatus.APPROVED.value().equals(article.getStatus())
                        && Integer.valueOf(0).equals(article.getIsDraft()))
                .collect(Collectors.toMap(Article::getId, article -> article, (a, b) -> a));
    }

    /**
     * 获取锁：SETNX + 10 秒自动过期（防止消费者宕机后锁永不释放）
     */
    private Boolean tryLock(String lockKey) {
        try {
            return stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
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
