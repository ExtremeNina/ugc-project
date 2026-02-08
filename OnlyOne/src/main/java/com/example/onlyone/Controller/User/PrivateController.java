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
        log.info("当前用户的好友列表为: {}", friendList);
        return Result.success(friendList);
    }


    //获取和某个好友的聊天记录
    @GetMapping("/ChatHistory/{id}")
    public Result getChatHistory(@PathVariable Long id) {
        List<ChatHistoryVO> chatHistoryVOS = privateService.getChatHistory(id);
        return Result.success(chatHistoryVOS);
    }


}
