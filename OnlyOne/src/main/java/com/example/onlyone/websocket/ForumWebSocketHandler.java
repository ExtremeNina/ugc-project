package com.example.onlyone.websocket;
import com.example.onlyone.DTO.PrivateDTO;
import com.example.onlyone.Entity.PrivateMessage;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.PrivateMessageMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Utils.SpringContextUtils;
import com.example.onlyone.VO.PrivateMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ForumWebSocketHandler extends TextWebSocketHandler {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public ForumWebSocketHandler(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String username = (String) session.getAttributes().get("username");
        Long userId = (Long) session.getAttributes().get("userId");
        //判断用户是否认证
        if (username != null) {
            //更新用户状态
            userSessions.put(userId, session);
            updateOnlineStatus(userId, true);
            //发送离线未读消息
            log.info("用户 {} (ID:{}) 建立WebSocket连接", username, userId);
        }else {
            log.info("用户 {} (ID:{}) 建立WebSocket连接异常", username, userId);
        }
    }


    @Override
    @Transactional
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        ObjectMapper objectMapper = SpringContextUtils.getBean(ObjectMapper.class);

        //将json格式的消息转换成java对象
        PrivateDTO privateDTO = objectMapper.readValue(message.getPayload(), PrivateDTO.class);
        log.info("发送的私信为:{}", privateDTO);

        Long userId = (Long) session.getAttributes().get("userId");
        //根据type进行判断离开页面请求
        if (Objects.equals(privateDTO.getType(), "leave_chat")) {
            log.info("离开聊天页面");
            return;
        }

        //构建消息并保存到数据库
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setUserId(userId);
        privateMessage.setContent(privateDTO.getContent());
        privateMessage.setDateTime(LocalDateTime.now());
        privateMessage.setReceiveId(privateDTO.getReceiveId());

        sendMessageToUser(privateMessage);

    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        Long userId = (Long) session.getAttributes().get("userId");

        if (userId != null) {
            //移除在线状态
            userSessions.remove(userId);
            updateOnlineStatus(userId, false);

            //移除私聊状态
            String key = "current:chat:"+userId;
            stringRedisTemplate.delete(key);

            log.info("用户 ID:{} 断开连接", userId);
        }
    }



    private void updateOnlineStatus(Long userId,Boolean isOnline) {
        String key = "user:online";
        if (isOnline) {
            stringRedisTemplate.opsForSet().add(key, userId.toString());
        }else {
            stringRedisTemplate.opsForSet().remove(key, userId.toString());
        }
    }

    //向好友发送私信
    public void sendMessageToUser(PrivateMessage privateMessage) throws IOException {


        ObjectMapper objectMapper = SpringContextUtils.getBean(ObjectMapper.class);
        UserMapper userMapper = SpringContextUtils.getBean(UserMapper.class);
        PrivateMessageMapper privateMessageMapper = SpringContextUtils.getBean(PrivateMessageMapper.class);

        //获取基本信息
        Long receiveId = privateMessage.getReceiveId();
        WebSocketSession receiveSession = userSessions.get(receiveId);

        //判断好友是否在线
        if (Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember("user:online", receiveId.toString()))
        || receiveSession != null) {


            //构建返回对象
            //Long userId = (Long) receiveSession.getAttributes().get("userId");
            Long userId = privateMessage.getUserId();
            User user = userMapper.selectById(userId);
            String icon = user.getIcon();

            //提前new出来，获取未读消息数
            PrivateMessageVO privateMessageVO = new PrivateMessageVO();

            String key = "current:chat:"+ receiveId;
            //证明对方正在当前的聊天页面
            if (Objects.equals(stringRedisTemplate.opsForValue().get(key), userId.toString())) {
                privateMessage.setStatus(1L);
                privateMessageVO.setNotReadCount(0L);
            }else {
                //不在则设置为未读
                privateMessage.setStatus(0L);
                //记录当前聊天页面的未读数
                String unreadHashKey = "unread:hash:" + receiveId;
                Long notReadCount = stringRedisTemplate.opsForHash()
                        .increment(unreadHashKey, privateMessage.getUserId().toString(), 1L);
                //设置过期时间
                stringRedisTemplate.expire(unreadHashKey, 7, TimeUnit.DAYS);
                //设置未读消息数
                privateMessageVO.setNotReadCount(notReadCount);
            }

            privateMessageVO.setContent(privateMessage.getContent());
            privateMessageVO.setSenderId(privateMessage.getUserId());
            privateMessageVO.setIcon(icon);
            privateMessageVO.setDateTime(privateMessage.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            //把java对象转换出json格式
            String message =  objectMapper.writeValueAsString(privateMessageVO);
            log.info("发送方的信息:{}",privateMessageVO);
            //发送消息
            receiveSession.sendMessage(new TextMessage(message));
            log.info("发送成功");
        }else {
            log.info("当前用户不在线");
            //设置为未读
            privateMessage.setStatus(0L);
        }
        //将消息保存到数据库
        privateMessageMapper.insert(privateMessage);
    }






}
