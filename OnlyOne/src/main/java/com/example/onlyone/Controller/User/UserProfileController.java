package com.example.onlyone.Controller.User;

import com.example.onlyone.DTO.UpdateUserMessageDTO;
import com.example.onlyone.VO.UpdateUserMessageVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.UserProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@Slf4j
public class UserProfileController {

    @Resource
    private UserProfileService userProfileService;

    //获取用户基本信息
    @GetMapping
    public Result getUserProfile() {
        UserProfileVO userProfileVO = userProfileService.getUserMessage();
        return Result.success(userProfileVO);
    }

    //编辑个人资料按钮时的回显
    @GetMapping("/edit")
    public Result showUserMessage() {
        UpdateUserMessageVO showUserMessage = userProfileService.showUserMessage();
        return Result.success(showUserMessage);
    }

    //更新用户信息
    @PutMapping
    public void updateUserProfile(@RequestBody UpdateUserMessageDTO updateUserMessageDTO) {
        userProfileService.updateUserMessage(updateUserMessageDTO);
    }

    //上传图片
    @PostMapping("/upload/image")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String,String> map =  userProfileService.uploadImage(file);
        String ImageUrl = map.get("url");
        log.info("当前用户上传的头像:{}",ImageUrl);
        return Result.success(ImageUrl);
    }

}
