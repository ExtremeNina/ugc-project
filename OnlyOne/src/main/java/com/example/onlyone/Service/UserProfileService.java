package com.example.onlyone.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.DTO.UpdateUserMessageDTO;
import com.example.onlyone.VO.UpdateUserMessageVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserProfileService extends IService<User> {

    UserProfileVO getUserMessage();

    void updateUserMessage(UpdateUserMessageDTO updateUserMessageDTO);

    Map<String, String> uploadImage(MultipartFile file);

    UpdateUserMessageVO showUserMessage();

    Long getFansCountFromCacheOrDB(Long userId);

    Long getFollowingCountFromCacheOrDB(Long userId);

    Long getAllLoveCount(Long userId);

}
