# Redis 使用点与业务层优化建议

本文档只关注业务层（代码/缓存策略/缓存问题）优化，不涉及 Redis 服务端的配置。

## 一、代码中使用 Redis 的位置（摘要）

- `src/main/java/com/example/onlyone/websocket/ForumWebSocketHandler.java`
  - 用途：记录在线用户集合（`user:online`）、当前聊天页面标记（`current:chat:{userId}`）、未读消息哈希（`unread:hash:{receiveId}`）、删除键、设置过期等。
- `src/main/java/com/example/onlyone/Utils/JwtAuthenticationFilter.java`
  - 用途：把 JWT 黑名单存入 Redis（`black:jwt{md5}`），用于判断已登出/作废的 token。
- `src/main/java/com/example/onlyone/Utils/LoveLuaScript.java`
  - 用途：封装点赞/取消/切换的 Lua 脚本，保证集合与计数的原子性操作。
- `src/main/java/com/example/onlyone/Service/ServiceImpl/LoveServiceImpl.java`
  - 用途：缓存点赞集合与计数（键如 `love:user:{userId}:{loveTypeId}`、`love:type:{loveTypeId}:{entityId}`、`love:count:{loveTypeId}:{entityId}`），使用分布式锁（`love:lock:{entityId}`）、Lua 脚本同步缓存与事务后刷新缓存。
- `src/main/java/com/example/onlyone/Service/ServiceImpl/FollowServiceImpl.java`
  - 用途：缓存关注关系（`user:following:{userId}`、`user:followers:{userId}`）、粉丝数/关注数键（`user:fansCount:{id}`、`user:followingCount:{id}`），使用 Lua 脚本保证关注/取消关注的原子性。
- 资源文件：`src/main/resources/*.lua`（`LoveLuaScript.lua`、`FollowLuaScript.lua`、`unLoveLuaScript.lua`、`unFollowLuaScript.lua`）
  - 用途：在 ServiceImpl 中载入并执行，保证复杂变更的原子性与一致性。

（以上为主要发现的业务层 Redis 用例；项目中可能还有其它轻量缓存/会话写法，但核心用法集中在上述文件。）

## 二、业务层常见的三大缓存问题及针对性建议

下面针对缓存穿透、缓存击穿（热点/单点失效）与缓存雪崩分别给出业务层可执行的建议。

1) 缓存穿透（大量请求查询不存在的 key，直接穿透到 DB）

- 建议：实现负缓存（对不存在的数据也缓存空或标记），在 Service 层读取 DB 后，将“空结果”以短 TTL 缓存（例如 1~5 分钟），避免重复穿透。
  - 在 `FollowServiceImpl.getFanList` / `isFollowing`、`LoveServiceImpl.isEntityLovedByUser`、`getEntityLoveCount` 等返回空或 null 的分支，写入负缓存（值为特殊占位符或空集合）并设置短 TTL。
- 建议：在业务层在外层加一层快速校验（可选）——使用布隆过滤器缓存存在性判断（适合用户/文章等固定存在集合），可以在应用内存或 Redis 中实现，但这里仅建议在业务层调用布隆过滤器查询，若返回“可能存在”再走缓存/DB。

2) 缓存击穿（热点 key 大量并发失效导致瞬间击穿 DB）

- 建议：对热点数据采用以下任一或组合策略：
  - 使用互斥锁 / 双重检查：在缓存未命中时，先尝试获取业务层分布式锁（`love:lock:{entityId}` 已用于限流/防重），只有持锁者去 DB 读取并回填缓存，其他线程等待或短时重试；注意业务中锁的超时与释放保证。
  - 使用逻辑过期 + 后台异步刷新：把缓存值封装为 {data, expireAt}，当发现逻辑过期后立即返回旧数据（降级可用），异步触发刷新任务去 DB 更新缓存，保证短时间内仍能服务。
  - 对“极热”数据采用恒定更新策略（如定期预热或主动更新），避免 TTL 同时到期。

- 在代码位置建议：
  - `LoveServiceImpl.getEntityLoveCount` 可改为先返回缓存值并在后台刷新（若缓存存在但接近过期），或在 DB 回填时使用事务后的同步回写（当前代码已在事务后更新缓存，保持此逻辑并加入锁/双检以避免并发重建）。

3) 缓存雪崩（大量 key 同时过期或 Redis 不可用导致流量全部打到 DB）

- 建议：
  - 随机化 TTL（在原有 TTL 基础上加上随机抖动值），防止大量 key 同时过期。
  - 对非必须强一致的缓存设置合理较短 TTL 并允许异步重建；对必须强一致的数据走同步更新而非纯缓存（例如交易类、计费类）。
  - 在业务层实现降级策略：当 Redis 异常或超时，直接降级为从数据库读取并限流（快速失败或返回友好错误提示），避免放大故障。

- 在代码位置建议：
  - `FollowServiceImpl`、`LoveServiceImpl` 在写入 `expire` 时应带上随机抖动（例如在 1 天 TTL 上随机 ±10%），并考虑对集合批量写入时分散过期时间。

## 三、针对当前项目发现的具体优化建议（按文件/场景）

- `ForumWebSocketHandler`（在线/未读逻辑）
  - 问题：`unread:hash:{receiveId}` 每次未读自增并设置固定 7 天过期；若用户高并发聊天，会频繁写 Redis；同时 `current:chat:{userId}` 为当前页面状态，读取/写入频繁。
  - 建议：
    - 对未读计数使用哈希键是合理，建议在业务上合并短时间内的小量更新（批量、异步写入）或使用局部内存队列，定期落盘到 Redis，减小写压力。
    - `current:chat` 值为短期状态，可使用短 TTL 并在写入时加入抖动或避免频繁覆盖相同值。
    - 当消息量大时，考虑将未读计数维护为最终一致（DB + Redis 双写，或 DB 主导，Redis 为加速层），并在 websocket 层做好失败回退。

- `JwtAuthenticationFilter`（JWT 黑名单）
  - 问题：黑名单 key 存在性检查直接依赖 Redis；若 Redis 不可用，可能造成无法识别已登出令牌。
  - 建议：
    - 在业务层允许 Redis 故障时的降级：若 Redis 读失败，可继续基于 JWT 本身（签名/过期）判断，并记录事件以便后续审计；写黑名单时应保证事务或异步重试确保可靠性。

- `LoveServiceImpl`（点赞缓存 + 锁 + Lua）
  - 问题：已使用锁（`love:lock`）和 Lua 脚本，设计良好，但存在：缓存穿透（检查时直接走 DB）、并发下锁竞争、计数一致性风险（DB 与 Redis 双写）。
  - 建议：
    - 将 `isEntityLovedByUser` 中的缺失情况写入负缓存（短 TTL）以防穿透。
    - 对热点文章的 `love:count` 可以采用逻辑过期 + 异步刷新，减少并发回填。
    - 分布式锁使用上：当前 `setIfAbsent` + TTL 做法可行，但要确保 release 在正确时机（防止误删他人锁），可改用带唯一值的锁（检查值再删除）或使用 Redisson/稳定实现。
    - 保持事务后回写缓存的模式（代码中已有 TransactionSynchronizationManager 注册），并在回写时做重试与幂等保护。

- `FollowServiceImpl`（关注缓存 + Lua）
  - 问题：关注关系与粉丝计数都由 Lua 脚本原子更新，良好；但读取缓存不存在时会回库并全量写入 set（可能造成大集合写入延迟）。
  - 建议：
    - 对大集合（粉丝列表）采用分页缓存或部分缓存（TopN）而非一次性全量写入 Redis，以防一次性写入大量成员导致阻塞。
    - 对粉丝列表的写缓存操作，考虑异步补偿（先返回 DB 写入结果，事务提交后异步触发缓存更新任务），或在写入 Redis 时分批提交。

## 四、可执行检查清单（业务层，仅含需开发/代码验证项）

1. 缓存穿透
- [ ] 在 `LoveServiceImpl.isEntityLovedByUser` 中为“无结果”写入负缓存（短 TTL）。
- [ ] 在 `FollowServiceImpl.isFollowing` / `getFanList` 中为不存在结果写入负缓存或特定占位符。
- [ ] 对高频查询的资源（用户、文章）评估是否适合布隆过滤器防穿透（如适用，列出需求）。

2. 缓存击穿（热点）
- [ ] 在 `LoveServiceImpl.getEntityLoveCount` 增加双重检查/互斥锁或逻辑过期 + 后台刷新策略。
- [ ] 对极热 key（热门文章/大V 用户）预热并避免同时到期（随机化 TTL 或统一刷新计划）。
- [ ] 确认 `love:lock:{id}` 的锁释放机制是安全的（建议使用带标识值的删除或成熟库实现）。

3. 缓存雪崩
- [ ] 为主要计数/集合写入操作加上 TTL 随机抖动（在业务层设置 expire 时添加 ±10% 随机值）。
- [ ] 在 Redis 不可用时实现降级策略：短路到 DB 并对外返回限流/提示，避免流量激增。

4. 原子性与一致性
- [ ] 对复杂多键变更（关注/点赞）保持 Lua 脚本或事务后的回写逻辑（当前项目已有实现，但需代码审计确认没有遗漏路径）。
- [ ] 对事务后缓存刷新确认使用 `TransactionSynchronizationManager` 的所有分支都能覆盖（异常分支、回滚场景）。

5. 大集合与热点列表
- [ ] 对 `user:followers:{id}` 的全量缓存写入改为分批写或分页缓存（避免一次写入大量成员）。
- [ ] 对粉丝/关注列表频繁变更场景，考虑只缓存前 N 或最近活跃的子集，按需回 DB 获取完整列表。

6. 读写路径健壮性
- [ ] 对 Redis 操作统一封装（统一捕获异常、降级、重试和监控），避免散落在业务代码中频繁重复处理。
- [ ] 实现统一的缓存工具类（例如 `CacheUtils`），包含：安全设置 TTL（带抖动）、负缓存写入、原子更新辅助、分布式锁封装、默认重试策略、故障降级入口。

## 五、结论与下一步建议

- 项目当前在关注/点赞等核心场景采用了 Lua 脚本与事务后回写，这在保证原子性和一致性上是良好实践；建议补齐负缓存、TTL 随机抖动、锁的安全实现与大集合分批写入等业务层策略。
- 推荐实现一个小型 `CacheUtils`/`CacheService` 层，统一封装上文提到的常用策略（负缓存、随机 TTL、逻辑过期、幂等写回、分布式锁封装），逐步替换散落的 Redis 调用以降低重复代码并便于审计。

---
_本文件已自动生成，若需我把上述检查清单拆分为具体 issue/PR 模板并自动修改代码片段（例如在 `LoveServiceImpl` 增加负缓存实现），我可以继续执行。_
