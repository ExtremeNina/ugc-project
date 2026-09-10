package com.example.onlyone.Controller.User;


import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.PrivateService;
import com.example.onlyone.VO.ChatHistoryVO;
import com.example.onlyone.VO.FriendVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/private")
@Slf4j
public class PrivateController {

    @Resource
    private PrivateService privateService;


    //获取关注用户
    @GetMapping("/{userId}")
    public Result getFollows(@PathVariable Long userId) {
        List<FriendVO> friendList = privateService.getFollows(userId);
        return Result.success(friendList);
    }


    /**
     * 获取和某个好友的聊天记录（游标分页）
     *
     * @param id     好友用户 id
     * @param lastId 翻页游标，可选：不传返回最新 20 条；上滑加载历史时传上一页最小消息 id
     */
    @GetMapping("/ChatHistory/{id}")
    public Result getChatHistory(@PathVariable Long id,
                                 @RequestParam(value = "lastId", required = false) Long lastId) {
        List<ChatHistoryVO> chatHistoryVOS = privateService.getChatHistory(id, lastId);
        return Result.success(chatHistoryVOS);
    }

    // [实时通信升级] WebSocket 重连后的断点续传：拉取 afterId 之后的消息
    @GetMapping("/ChatHistory/{id}/sync")
    public Result syncChatHistory(@PathVariable Long id,
                                  @RequestParam(value = "afterId", required = false) Long afterId) {
        return Result.success(privateService.getMessagesAfter(id, afterId));
    }


}
