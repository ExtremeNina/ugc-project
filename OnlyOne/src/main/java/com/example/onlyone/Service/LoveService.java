package com.example.onlyone.Service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.DTO.LoveDTO;
import com.example.onlyone.Entity.UserLove;

public interface LoveService extends IService<UserLove> {


    void toggleLike(LoveDTO loveDTO);

    Boolean isEntityLovedByUser(Long entityId, Long loveTypeId);

    String getEntityLoveCount(Long entityId, Long loveTypeId);
}
