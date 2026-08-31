package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.onlyone.Entity.Follow;
import com.example.onlyone.Entity.PrivateMessage;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.FollowMapper;
import com.example.onlyone.Mapper.PrivateMessageMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.PrivateService;
import com.example.onlyone.Utils.SecurityUtils;
import com.example.onlyone.VO.ChatHistoryVO;
import com.example.onlyone.VO.FriendVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 私信服务实现
 *
 * 本类包含的改造（对应优化路线第二批、第三批）：
 *  1. getFollows 消灭 N+1：原来每个好友要 1 次 getLastText SQL + 1 次在线状态 Redis 查询，
 *     100 个好友 = 200 次往返；现在 = 1 条窗口函数 SQL + 1 次 pipeline + 1 次 hGetAll；
 *  2. 在线状态从共享 Set 改为读 per-user 租约 key（user:online:{userId}，带 TTL 自愈）；
 *  3. getChatHistory 游标分页：不再全量拉取聊天记录，前端上滑时传 lastId 翻历史；
 *  4. 移除整包打印聊天内容的日志（隐私 + 性能）。
 */
@Service
@Slf4j
public class PrivateServiceImpl implements PrivateService {

    @Resource
    private FollowMapper followMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private PrivateMessageMapper privateMessageMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 会话列表/聊天记录单页大小 */
    private static final int PAGE_SIZE = 20;
    /** 在线租约 key 前缀，必须与 ForumWebSocketHandler 保持一致 */
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    /** 未读数 hash 前缀：userId -> (发送者userId -> 未读条数) */
    private static final String UNREAD_HASH_PREFIX = "unread:hash:";


    @Override
    public List<FriendVO> getFollows(Long userId) {
        if (userId == null) {
            log.error("用户id不能为空");
            return List.of();
        }
        List<Follow> followList = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowingId, userId));
        //获取粉丝id（沿用原语义：好友列表 = 关注我的人）
        List<Long> peerIds = followList.stream().map(Follow::getFollowerId).toList();

        if (peerIds.isEmpty()) {
            return new ArrayList<>();
        }

        // ---- 1. 好友基本信息：一次批量查（原实现已是批量，保留）----
        List<User> follows = userMapper.selectBatchIds(peerIds);

        // ---- 2. 每个会话的最新一条消息：窗口函数一条 SQL 搞定（原实现每个好友查一次）----
        Map<Long, PrivateMessage> lastMsgMap = new HashMap<>();
        List<PrivateMessage> lastMessages =
                privateMessageMapper.selectLastMessagePerPeer(userId, peerIds);
        for (PrivateMessage pm : lastMessages) {
            // 该条消息的另一端就是对应好友
            Long peerId = pm.getUserId().equals(userId) ? pm.getReceiveId() : pm.getUserId();
            lastMsgMap.put(peerId, pm);
        }

        // ---- 3. 在线状态：pipeline 一次批量 EXISTS（原实现每个好友查一次 Redis）----
        // per-user 租约 key 带 60s TTL：服务器崩溃后自动过期，无需人工清理
        Map<Long, Boolean> onlineMap = batchGetOnline(peerIds);

        // ---- 4. 未读数：一次 hGetAll 拿到所有好友的未读计数（原实现完全没查）----
        Map<Object, Object> unreadMap = stringRedisTemplate.opsForHash()
                .entries(UNREAD_HASH_PREFIX + userId);

        // ---- 5. 纯内存组装 VO，不再有任何逐好友查询 ----
        List<FriendVO> friendVOList = new ArrayList<>();
        for (User user : follows) {
            FriendVO friendVO = new FriendVO();

            friendVO.setUserId(user.getId());
            friendVO.setIcon(user.getIcon());
            friendVO.setUserName(user.getUsername());
            friendVO.setIsOnline(onlineMap.getOrDefault(user.getId(), false));

            // 最后一条发言：直接取批量查询结果
            PrivateMessage lastText = lastMsgMap.get(user.getId());
            if (lastText != null) {
                friendVO.setLastTime(lastText.getDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                friendVO.setLastText(lastText.getContent());
            }

            // 未读数：hash 字段缺失按 0 处理
            Object unread = unreadMap.get(user.getId().toString());
            friendVO.setUnreadCount(unread == null ? 0L : Long.parseLong(unread.toString()));

            friendVOList.add(friendVO);
        }
        return friendVOList;
    }

    /**
     * 批量判断一批用户的在线状态
     * pipeline 把 N 次 EXISTS 合并为 1 次网络往返
     */
    private Map<Long, Boolean> batchGetOnline(List<Long> userIds) {
        List<Object> results = stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                StringRedisTemplate template = (StringRedisTemplate) operations;
                for (Long id : userIds) {
                    template.hasKey(ONLINE_KEY_PREFIX + id);
                }
                return null;
            }
        });
        Map<Long, Boolean> onlineMap = new HashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            onlineMap.put(userIds.get(i), Boolean.TRUE.equals(results.get(i)));
        }
        return onlineMap;
    }

    //==================================================================================
    // 聊天记录：游标分页
    //==================================================================================

    @Override
    public List<ChatHistoryVO> getChatHistory(Long id, Long lastId) {

        //获取当前用户id
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }

        //标记所有聊天记录为已读（打开聊天页即视为已读，同时清掉未读 hash 字段）
        //MP lambda 更新：替代手写 @Update，条件与赋值都用方法引用，避免字符串列名拼错
        privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getReceiveId, userId)
                .eq(PrivateMessage::getUserId, id)
                .eq(PrivateMessage::getStatus, 0L)
                .set(PrivateMessage::getStatus, 1L));
        stringRedisTemplate.opsForHash().delete(UNREAD_HASH_PREFIX + userId, id.toString());

        // ---- 游标分页：lastId 为 null 取最新一页，翻历史时传上一页最小 id ----
        // MP lambda 查询 + last 限页：不用 Page 分页插件是为了省掉多余的 count 查询
        // SQL 里/此处均按 id 倒序取最新一页，下面反转成时间正序返回，前端渲染顺序不变
        List<PrivateMessage> privateMessageList = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .and(w -> w.eq(PrivateMessage::getUserId, userId)
                                .eq(PrivateMessage::getReceiveId, id))
                        .or(w -> w.eq(PrivateMessage::getUserId, id)
                                .eq(PrivateMessage::getReceiveId, userId))
                        .lt(lastId != null, PrivateMessage::getId, lastId)
                        .orderByDesc(PrivateMessage::getId)
                        .last("LIMIT " + PAGE_SIZE));
        Collections.reverse(privateMessageList);

        List<ChatHistoryVO> chatHistoryVOList = new ArrayList<>();
        for (PrivateMessage privateMessage : privateMessageList) {

            ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
            chatHistoryVO.setId(privateMessage.getId());
            chatHistoryVO.setTime(privateMessage.getDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            chatHistoryVO.setContent(privateMessage.getContent());
            //判断，如果当userId等于当前用户id，则设置为当前用户所发
            if (privateMessage.getUserId().equals(userId)) {
                chatHistoryVO.setSenderId(userId);
                chatHistoryVO.setReceiverId(id);
                chatHistoryVO.setIsOwn(true);
                chatHistoryVOList.add(chatHistoryVO);
            } else {
                chatHistoryVO.setSenderId(id);
                chatHistoryVO.setReceiverId(userId);
                chatHistoryVO.setIsOwn(false);
                chatHistoryVOList.add(chatHistoryVO);
            }
        }

        //构建当前聊天对象用于判断是否已读（供服务端投递时判断"对方正在聊天页"）
        String currentKey = "current:chat:" + userId;
        stringRedisTemplate.opsForValue().set(currentKey, id.toString(), 24, java.util.concurrent.TimeUnit.HOURS);

        // 注意：不再整包打印聊天内容（隐私 + 日志膨胀），只打条数
        log.info("返回用户 {} 与 {} 的聊天记录 {} 条, lastId={}", userId, id, chatHistoryVOList.size(), lastId);
        return chatHistoryVOList;
    }
}
