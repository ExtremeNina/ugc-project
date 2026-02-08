package com.example.onlyone.Service.ServiceImpl;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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


    @Override
    public List<FriendVO> getFollows(Long userId) {
        if (userId == null) {
            log.error("用户id不能为空");
            return null;
        }
        List<Follow> followList = followMapper.selectByFollowingId(userId);
        //获取粉丝id
        List<Long> followingIds = followList.stream().map(Follow::getFollowerId).toList();

        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<User> follows = userMapper.selectBatchIds(followingIds);
        List<FriendVO> friendVOList = new ArrayList<>();
        for (User user : follows) {
            FriendVO friendVO = new FriendVO();

            //判断好友是否在线
            String key = "user:online";
            if (Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, user.getId().toString()))) {
                friendVO.setIsOnline(true);
            }else {
                friendVO.setIsOnline(false);
            }

            //获取上一次的最后发言
            PrivateMessage lastText = privateMessageMapper.getLastText(userId,user.getId());
            if (lastText == null) {
                friendVO.setLastText(null);
                friendVO.setLastTime(null);
            }else {
                friendVO.setLastTime(lastText.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                friendVO.setLastText(lastText.getContent());
            }

            friendVO.setUserId(user.getId());
            friendVO.setIcon(user.getIcon());
            friendVO.setUserName(user.getUsername());
            friendVOList.add(friendVO);
        }
        return friendVOList;
    }

    //获取历史聊天记录
    @Override
    public List<ChatHistoryVO> getChatHistory(Long id) {

        //获取当前用户id
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        //标记所以聊天记录为已读
        privateMessageMapper.updateStatus(userId,id);

        //获取当前用户和聊天用户的全部消息
        List<PrivateMessage> privateMessageList = privateMessageMapper.selectByCurrent(userId,id);
        List<ChatHistoryVO> chatHistoryVOList = new ArrayList<>();
        for (PrivateMessage privateMessage : privateMessageList) {

            ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
            chatHistoryVO.setId(privateMessage.getId());
            chatHistoryVO.setTime(privateMessage.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
            chatHistoryVO.setContent(privateMessage.getContent());
            //判断，如果当userId等于当前用户id，则设置为当前用户所发
            if (privateMessage.getUserId().equals(userId)) {
                //构建返回对象
                chatHistoryVO.setSenderId(userId);
                chatHistoryVO.setReceiverId(id);
                chatHistoryVO.setIsOwn(true);
                chatHistoryVOList.add(chatHistoryVO);
            }else {
                chatHistoryVO.setSenderId(id);
                chatHistoryVO.setReceiverId(userId);
                chatHistoryVO.setIsOwn(false);
                chatHistoryVOList.add(chatHistoryVO);
            }
        }

        //构建当前聊天对象用于判断是否已读
        String currentKey = "current:chat:"+userId;
        //设置两个小时过期时间
        stringRedisTemplate.opsForValue().set(currentKey,id.toString(),24, TimeUnit.HOURS);

        //删除未读消息数
        String unreadHashKey = "unread:hash:" + userId;
        stringRedisTemplate.opsForHash().delete(unreadHashKey,id.toString());

        log.info("返回的聊天记录为：{}", chatHistoryVOList);
        return chatHistoryVOList;
    }
}
