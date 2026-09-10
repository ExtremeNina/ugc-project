package com.example.onlyone.Service;

import com.example.onlyone.VO.ChatHistoryVO;
import com.example.onlyone.VO.FriendVO;

import java.util.List;

public interface PrivateService {

    /**
     * 获取好友（粉丝）列表：批量组装在线状态、最后一条消息、未读数
     */
    List<FriendVO> getFollows(Long userId);

    /**
     * 获取与某好友的聊天记录（游标分页）
     *
     * @param id     好友用户 id
     * @param lastId 翻页游标：传 null 时返回最新一页；上滑加载更多时传本页最小消息 id
     */
    List<ChatHistoryVO> getChatHistory(Long id, Long lastId);

    // [实时通信升级] 重连后按服务端消息序号增量同步
    List<ChatHistoryVO> getMessagesAfter(Long id, Long afterId);
}
