package com.example.onlyone.Service;

import com.example.onlyone.VO.ChatHistoryVO;
import com.example.onlyone.VO.FriendVO;

import java.util.List;

public interface PrivateService {
    List<FriendVO> getFollows(Long userId);

    List<ChatHistoryVO> getChatHistory(Long id);
}
