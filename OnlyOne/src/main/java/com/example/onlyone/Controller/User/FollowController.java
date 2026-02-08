package com.example.onlyone.Controller.User;

import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.FollowService;
import com.example.onlyone.Utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Interaction/follow")
@Slf4j
public class FollowController {

    @Resource
    private FollowService followService;


    /**
     * 关注/取消关注用户
     * @param userId 要关注的用户ID
     * @return 关注状态（true=关注成功，false=取消关注成功）
     */
    @PostMapping("/{userId}")
    public Result toggleFollow(@PathVariable Long userId) {

        log.info("Received follow request for userId: {}", userId);

        boolean isFollow = followService.toggleFollow(userId);
        return Result.success(isFollow);
    }


    /**
     * 检查是否关注了某用户
     * @param userId 目标用户ID
     * @return 是否已关注
     */
    @GetMapping("/is-following/{userId}")
    public Result<Boolean> isFollowing(@PathVariable Long userId) {
        try {
            // 注意：这里需要实现相应的方法
            // Boolean isFollowing = followService.isFollowing(userId);
            // return Result.success(isFollowing);

            // 或者如果只有当前用户ID，可以使用：
            Long currentUserId = SecurityUtils.getCurrentUserId();
            Boolean isFollowing = followService.isFollowing(currentUserId, userId);
            return Result.success(isFollowing);
        } catch (Exception e) {
            log.error("检查关注状态失败: userId={}", userId, e);
            return Result.error("检查关注状态失败");
        }
    }


    /**
     * 获取用户的粉丝数
     * @param userId 用户ID
     * @return 粉丝数
     */
    @GetMapping("/followers/count/{userId}")
    public Result<Long> getFansCount(@PathVariable Long userId) {
        try {
            Long count = followService.getFansCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取粉丝数失败: userId={}", userId, e);
            return Result.error("获取粉丝数失败");
        }
    }


    /**
     * 获取用户的关注数
     * @param userId 用户ID
     * @return 关注数
     */
    @GetMapping("/following/count/{userId}")
    public Result<Long> getFollowingCount(@PathVariable Long userId) {
        try {
            Long count = followService.getFollowingCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取关注数失败: userId={}", userId, e);
            return Result.error("获取关注数失败");
        }
    }






}
