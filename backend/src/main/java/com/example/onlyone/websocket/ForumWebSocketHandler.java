package com.example.onlyone.websocket;
import com.example.onlyone.DTO.PrivateDTO;
import com.example.onlyone.Entity.PrivateMessage;
import com.example.onlyone.Entity.OfflineNotification;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.OfflineNotificationMapper;
import com.example.onlyone.Mapper.PrivateMessageMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Utils.SpringContextUtils;
import com.example.onlyone.VO.PrivateMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 私信 WebSocket 处理器
 *
 * 本类包含的正确性改造（对应优化路线第一批、第二批）：
 *  1. 先落库、后发送：消息以数据库为准，insert 拿到 msgId 后再推送 + 回 ACK，
 *     杜绝"对方看到了、记录却没有"的不一致；
 *  2. 移除失效的 @Transactional（self-invocation 不走代理，且事务不应包裹 WebSocket IO）；
 *  3. 在线判断统一以本地 session 为准，彻底消除"Redis 说在线但 session 为空"的 NPE；
 *  4. 多端登录：新连接顶掉旧连接前先 close 旧连接，避免死连接泄漏；
 *     所有 session 用 ConcurrentWebSocketSessionDecorator 包装，防止并发写抛异常；
 *  5. 在线状态改为"心跳租约"：per-user key 带 60s TTL，客户端 30s 心跳续期，
 *     服务器崩溃后 60s 自动转为离线，无需人工清理（自愈）；
 *  6. 未读数 hash 不再设置 7 天过期——未读数随消息生命周期管理，不随时间丢失；
 *  7. 发送方头像在连接建立时查一次并缓存进 session attributes，
 *     每条消息不再单独 selectById 查用户表。
 */
@Component
@Slf4j
public class ForumWebSocketHandler extends TextWebSocketHandler {

    private final StringRedisTemplate stringRedisTemplate;

    /** 用户 id -> 该用户的 WebSocket 会话（经并发安全包装） */
    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /** 在线状态 Key 前缀：每个用户独立 key，靠 TTL 自愈（旧方案是共享 Set，崩溃后残留） */
    public static final String ONLINE_KEY_PREFIX = "user:online:";
    /** 在线租约时长：客户端每 30s 心跳一次，60s 内没续期即视为离线 */
    private static final long ONLINE_TTL_SECONDS = 60;

    /** 客户端心跳消息类型（前端 index.vue 每 30s 发送 {type:"heartbeat"}） */
    private static final String TYPE_HEARTBEAT = "heartbeat";
    /** 服务端回给发送方的消息回执类型（前端可据此做"发送中/失败/重发"） */
    private static final String TYPE_MSG_ACK = "msg_ack";

    /** 并发写保护参数：单条消息发送最长阻塞 2s，缓冲区上限 512KB，超限断开该连接 */
    private static final int SEND_TIME_LIMIT_MS = 2000;
    private static final int SEND_BUFFER_SIZE_LIMIT = 512 * 1024;

    public ForumWebSocketHandler(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // ==================================================================================
    // 连接生命周期
    // ==================================================================================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String username = (String) session.getAttributes().get("username");
        Long userId = (Long) session.getAttributes().get("userId");
        if (username == null || userId == null) {
            // 握手时未通过鉴权，直接关闭，不进入会话表
            log.warn("未鉴权的 WebSocket 连接被拒绝");
            session.close();
            return;
        }

        // ---- 多端登录处理：同一用户新连接顶掉旧连接，必须先关闭旧连接，防止死连接泄漏 ----
        WebSocketSession oldSession = userSessions.put(userId, wrapConcurrent(session));
        if (oldSession != null && oldSession.isOpen()) {
            log.info("用户 {} (ID:{}) 在新端登录，关闭旧连接", username, userId);
            try {
                oldSession.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception e) {
                log.warn("关闭旧连接失败, userId={}", userId, e);
            }
        }

        // ---- 在线租约：SET key EX 60，之后靠心跳续期（见 handleTextMessage 的 heartbeat 分支） ----
        markOnline(userId);

        // ---- 头像缓存：连接建立时查一次用户信息存进 attributes，之后每条消息直接取，不再查库 ----
        cacheUserInfo(session, userId);

        // 补推离线期间积压的审核结果等通知
        pushOfflineNotifications(userId);

        log.info("用户 {} (ID:{}) 建立WebSocket连接", username, userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }

        // 只有"当前登记的连接"是自己时才清理，防止新端登录后旧连接的 close 事件误删新端状态
        if (userSessions.get(userId) == session) {
            userSessions.remove(userId);
            // 正常断开：立刻删除在线租约（若进程是崩溃状态，key 会在 60s 后自动过期，同样自愈）
            stringRedisTemplate.delete(ONLINE_KEY_PREFIX + userId);
            // 清除"正在和谁聊天"状态
            stringRedisTemplate.delete("current:chat:" + userId);
            log.info("用户 ID:{} 断开连接", userId);
        }
    }

    // ==================================================================================
    // 消息处理（先落库，后发送）
    // ==================================================================================

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        ObjectMapper objectMapper = SpringContextUtils.getBean(ObjectMapper.class);
        // 将 json 格式的消息转换成 java 对象（解析失败直接丢弃，不影响连接）
        PrivateDTO privateDTO;
        try {
            privateDTO = objectMapper.readValue(message.getPayload(), PrivateDTO.class);
        } catch (Exception e) {
            log.warn("无法解析的 WebSocket 消息，已丢弃: {}", message.getPayload());
            return;
        }

        Long userId = (Long) session.getAttributes().get("userId");

        // ---- 心跳：只续租，不落库不转发。客户端 30s 一次，60s 没续期即自动下线 ----
        if (TYPE_HEARTBEAT.equals(privateDTO.getType())) {
            renewOnlineLease(userId);
            return;
        }

        // 根据type进行判断离开页面请求（清除"正在和谁聊天"状态，对方再来消息将累计未读数）
        if (Objects.equals(privateDTO.getType(), "leave_chat")) {
            log.info("离开聊天页面");
            stringRedisTemplate.delete("current:chat:" + userId);
            return;
        }

        // [实时通信升级] 接收方明确确认已读，按消息水位更新，避免仅依赖页面状态推测
        if (Objects.equals(privateDTO.getType(), "read_ack")) {
            if (privateDTO.getReceiveId() == null || privateDTO.getMaxReadMessageId() == null) return;
            privateMessageMapperForAck().update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getUserId, privateDTO.getReceiveId())
                    .eq(PrivateMessage::getReceiveId, userId)
                    .le(PrivateMessage::getId, privateDTO.getMaxReadMessageId())
                    .eq(PrivateMessage::getStatus, 0L)
                    .set(PrivateMessage::getStatus, 1L)
                    .set(PrivateMessage::getDeliveryStatus, 2));
            stringRedisTemplate.opsForHash().delete("unread:hash:" + userId, privateDTO.getReceiveId().toString());
            notifySenderMessageStatus(privateDTO.getReceiveId(), privateDTO.getMaxReadMessageId(), "read");
            return;
        }

        // [实时通信升级] 接收方 ACK：只有消息确实写入客户端后才标记已送达
        if (Objects.equals(privateDTO.getType(), "delivery_ack")) {
            if (privateDTO.getMessageId() == null) return;
            PrivateMessage delivered = privateMessageMapperForAck().selectById(privateDTO.getMessageId());
            if (delivered != null && Objects.equals(delivered.getReceiveId(), userId)) {
                delivered.setDeliveryStatus(Math.max(1, delivered.getDeliveryStatus() == null ? 0 : delivered.getDeliveryStatus()));
                privateMessageMapperForAck().updateById(delivered);
                notifySenderMessageStatus(delivered.getUserId(), delivered.getId(), "delivered");
            }
            return;
        }

        // 基础校验：必须有接收人和内容
        if (privateDTO.getReceiveId() == null || privateDTO.getContent() == null
                || privateDTO.getContent().isBlank() || privateDTO.getContent().length() > 255
                || Objects.equals(privateDTO.getReceiveId(), userId)) {
            log.warn("非法的私信消息（缺少接收人或内容），已丢弃, userId={}", userId);
            return;
        }
        // [实时通信升级] 不信任客户端携带的任何用户资料，只以用户表中的有效接收者为准
        User receiver = SpringContextUtils.getBean(UserMapper.class).selectById(privateDTO.getReceiveId());
        if (receiver == null || receiver.getStatus() != 1) {
            sendProtocolError(session, privateDTO.getClientMessageId(), "receiver unavailable");
            return;
        }

        // ---- 1. 先确定状态再落库：消息以数据库为准 ----
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setUserId(userId);
        privateMessage.setContent(privateDTO.getContent());
        privateMessage.setDateTime(LocalDateTime.now());
        privateMessage.setReceiveId(privateDTO.getReceiveId());
        privateMessage.setClientMessageId(privateDTO.getClientMessageId());
        privateMessage.setDeliveryStatus(0);
        // [实时通信升级] 初始统一未读；是否已读仅由接收方明确的 read_ack 决定。
        privateMessage.setStatus(0L);

        // ---- 2. 落库（insert 后 MyBatis-Plus 会回填自增 id）----
        // 落库失败直接抛异常结束：此时谁也没收到消息，发送方前端可提示重发，不会出现"单边可见"
        PrivateMessageMapper privateMessageMapper = SpringContextUtils.getBean(PrivateMessageMapper.class);
        try {
            if (privateDTO.getClientMessageId() == null || privateDTO.getClientMessageId().isBlank()) {
                sendAck(session, privateMessage, false, "clientMessageId required");
                return;
            }
            PrivateMessage existing = privateMessageMapper.selectByClientMessageId(userId, privateDTO.getClientMessageId());
            if (existing != null) {
                sendAck(session, existing, true, null);
                return;
            }
            privateMessageMapper.insert(privateMessage);
        } catch (Exception e) {
            log.error("私信落库失败，消息未投递, sender={}, receiver={}", userId, privateDTO.getReceiveId(), e);
            sendAck(session, privateMessage, false, "persist failed");
            return;
        }

        // ---- 3. 落库成功后再投递 + 接收方未读数累计 ----
        // 发送方头像从发送方自己的 session attributes 取（连接时已缓存），避免每条消息查库
        String senderIcon = (String) session.getAttributes().get("senderIcon");
        deliverToReceiver(privateMessage, isReceiverOnChatPage(privateDTO.getReceiveId(), userId), senderIcon);

        // ---- 4. 给发送方回 ACK（带 msgId），前端可据此把消息标记为"发送成功" ----
        sendAck(session, privateMessage, true, null);
    }

    // ==================================================================================
    // 私信投递
    // ==================================================================================

    /**
     * 把已落库的消息推送给接收方，并维护未读数
     * 前提：消息已经 insert 成功，这里任何失败都不影响消息的持久化
     *
     * @param senderIcon 发送方头像（取自发送方 session 的连接期缓存）
     */
    private void deliverToReceiver(PrivateMessage privateMessage, boolean receiverOnChatPage,
                                   String senderIcon) throws IOException {

        Long receiveId = privateMessage.getReceiveId();
        // ---- 在线判断只认本地 session：这是唯一能真正投递消息的凭据，且不可能为脏数据 ----
        WebSocketSession receiveSession = userSessions.get(receiveId);
        if (receiveSession == null || !receiveSession.isOpen()) {
            // 接收方离线：数据库里 status=0 已是未读，再累计未读 hash（供会话列表展示）
            incrementUnread(receiveId, privateMessage.getUserId());
            log.info("接收方 {} 不在线，消息 {} 已落库待拉取", receiveId, privateMessage.getId());
            return;
        }

        // ---- 接收方在线但不在当前聊天页：累计未读数（不再设置过期时间！旧方案 7 天后未读数会凭空消失）----
        if (!receiverOnChatPage) {
            incrementUnread(receiveId, privateMessage.getUserId());
        }

        // ---- 组装推送 VO（发送方头像直接取自缓存，不再查库）----
        ObjectMapper objectMapper = SpringContextUtils.getBean(ObjectMapper.class);
        PrivateMessageVO privateMessageVO = new PrivateMessageVO();
        privateMessageVO.setNotReadCount(0L); // 接收方在线时前端不依赖该字段，保持兼容
        privateMessageVO.setContent(privateMessage.getContent());
        privateMessageVO.setMessageId(privateMessage.getId());
        privateMessageVO.setClientMessageId(privateMessage.getClientMessageId());
        privateMessageVO.setSenderId(privateMessage.getUserId());
        // 头像必须是发送方的（前端用它渲染对方的头像）
        privateMessageVO.setIcon(senderIcon);
        privateMessageVO.setDeliveryStatus(0);
        privateMessageVO.setDateTime(privateMessage.getDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String message = objectMapper.writeValueAsString(privateMessageVO);
        try {
            // session 已经用 ConcurrentWebSocketSessionDecorator 包装，并发写安全
            receiveSession.sendMessage(new TextMessage(message));
            log.info("消息 {} 已投递给接收方 {}", privateMessage.getId(), receiveId);
        } catch (IOException e) {
            // 推送失败不影响消息已落库的事实：接收方下次打开会话仍能从 DB 拉到
            log.error("消息推送失败（消息已落库）, msgId={}, receiver={}", privateMessage.getId(), receiveId, e);
        }
    }

    /**
     * 给发送方回 ACK：{type:"msg_ack", msgId, receiveId, dateTime, success}
     * 前端当前的消息分发逻辑会安全忽略未知 type，后续可据此实现"发送中/失败/重发"状态
     */
    private void sendAck(WebSocketSession senderSession, PrivateMessage privateMessage, boolean success, String error) {
        try {
            Map<String, Object> ack = new HashMap<>();
            ack.put("type", TYPE_MSG_ACK);
            ack.put("msgId", privateMessage.getId());
            ack.put("messageId", privateMessage.getId());
            ack.put("clientMessageId", privateMessage.getClientMessageId());
            ack.put("receiveId", privateMessage.getReceiveId());
            ack.put("dateTime", privateMessage.getDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ack.put("success", success);
            ack.put("error", error);
            String json = SpringContextUtils.getBean(ObjectMapper.class).writeValueAsString(ack);
            senderSession.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("发送 ACK 失败（不影响消息投递）, msgId={}", privateMessage.getId());
        }
    }

    // [实时通信升级] WebSocket 回调中通过 SpringContext 获取 Mapper，避免引入 IO 事务
    private PrivateMessageMapper privateMessageMapperForAck() {
        return SpringContextUtils.getBean(PrivateMessageMapper.class);
    }

    // [实时通信升级] 已送达/已读状态变化推送给发送方，发送方 UI 无需轮询数据库
    private void notifySenderMessageStatus(Long senderId, Long messageId, String status) {
        WebSocketSession sender = userSessions.get(senderId);
        if (sender == null || !sender.isOpen()) return;
        try {
            sender.sendMessage(new TextMessage(SpringContextUtils.getBean(ObjectMapper.class).writeValueAsString(Map.of(
                    "type", "message_status", "messageId", messageId, "status", status))));
        } catch (IOException e) {
            log.debug("消息状态推送失败，用户下次拉取历史可恢复, senderId={}, messageId={}", senderId, messageId);
        }
    }

    private void sendProtocolError(WebSocketSession session, String clientMessageId, String error) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "msg_ack");
            payload.put("clientMessageId", clientMessageId);
            payload.put("success", false);
            payload.put("error", error);
            session.sendMessage(new TextMessage(SpringContextUtils.getBean(ObjectMapper.class).writeValueAsString(payload)));
        } catch (IOException ignored) {
            // 连接关闭时无需额外处理
        }
    }

    // ==================================================================================
    // 在线状态：心跳租约（TTL 自愈）
    // ==================================================================================

    /** 连接建立时标记在线：SET user:online:{userId} "1" EX 60 */
    private void markOnline(Long userId) {
        stringRedisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, "1",
                ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 心跳续租：只重置 TTL，key 不存在（极端情况）则重建 */
    private void renewOnlineLease(Long userId) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(ONLINE_KEY_PREFIX + userId))) {
            stringRedisTemplate.expire(ONLINE_KEY_PREFIX + userId, ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            markOnline(userId);
        }
    }

    /** 判断接收方是否正停留在与发送方的聊天页（旧逻辑保留：current:chat 标记） */
    private boolean isReceiverOnChatPage(Long receiveId, Long senderId) {
        return Objects.equals(
                stringRedisTemplate.opsForValue().get("current:chat:" + receiveId),
                senderId.toString());
    }

    /** 接收方未读数 +1（hash 字段：发送者userId -> 未读条数；不设 TTL，已读时由 getChatHistory 删除） */
    private void incrementUnread(Long receiveId, Long senderId) {
        String unreadHashKey = "unread:hash:" + receiveId;
        stringRedisTemplate.opsForHash().increment(unreadHashKey, senderId.toString(), 1L);
    }

    // ==================================================================================
    // 会话工具
    // ==================================================================================

    /**
     * 用 ConcurrentWebSocketSessionDecorator 包装原始 session：
     * 同一 session 的 sendMessage 会串行化，避免并发写抛 IllegalStateException；
     * 发送阻塞超过 2s 或缓冲超 512KB 时自动断开该慢连接，保护容器线程
     */
    private WebSocketSession wrapConcurrent(WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_LIMIT);
    }

    /**
     * 连接建立时缓存发送者头像到 session attributes，
     * 之后每条消息的 VO 组装直接读取，省去每次 selectById
     */
    private void cacheUserInfo(WebSocketSession session, Long userId) {
        try {
            User user = SpringContextUtils.getBean(UserMapper.class).selectById(userId);
            if (user != null) {
                session.getAttributes().put("senderIcon", user.getIcon());
            }
        } catch (Exception e) {
            log.warn("缓存用户信息失败（不影响连接）, userId={}", userId, e);
        }
    }

    // ==================================================================================
    // 审核结果推送与离线补推（原有逻辑保留）
    // ==================================================================================

    public void pushModerationResult(Long userId, String jsonMsg) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(jsonMsg));
                log.info("审核结果已推送: userId={}", userId);
            } catch (IOException e) {
                log.error("审核结果推送失败，存入离线消息: userId={}", userId, e);
                saveOfflineNotification(userId, jsonMsg);
            }
        } else {
            saveOfflineNotification(userId, jsonMsg);
        }
    }

    private void saveOfflineNotification(Long userId, String jsonMsg) {
        OfflineNotificationMapper mapper = SpringContextUtils.getBean(OfflineNotificationMapper.class);
        OfflineNotification notification = new OfflineNotification();
        notification.setUserId(userId);
        notification.setMessageType("moderation_result");
        notification.setMessageContent(jsonMsg);
        notification.setIsSent(0);
        notification.setCreatedAt(LocalDateTime.now());
        mapper.insert(notification);
        log.info("审核结果已存入离线消息: userId={}", userId);
    }

    private void pushOfflineNotifications(Long userId) {
        OfflineNotificationMapper mapper = SpringContextUtils.getBean(OfflineNotificationMapper.class);
        List<OfflineNotification> notifications = mapper.selectByUserIdAndUnsent(userId);
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        for (OfflineNotification notification : notifications) {
            try {
                WebSocketSession session = userSessions.get(userId);
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(notification.getMessageContent()));
                } else {
                    continue;
                }
                notification.setIsSent(1);
                mapper.updateById(notification);
            } catch (Exception e) {
                log.error("离线消息补推失败: notificationId={}", notification.getId(), e);
            }
        }
        log.info("离线消息补推完成: userId={}, count={}", userId, notifications.size());
    }
}
