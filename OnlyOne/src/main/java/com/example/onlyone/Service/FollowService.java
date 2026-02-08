package com.example.onlyone.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.Entity.Follow;

import java.util.List;

public interface FollowService extends IService<Follow> {

    boolean toggleFollow(Long userId);

    Boolean isFollowing(Long currentUserId, Long userId);

    Long getFansCount(Long userId);

    Long getFollowingCount(Long userId);

    List<Long> getFanList(Long userId);
}
