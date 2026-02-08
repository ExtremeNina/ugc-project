package com.example.onlyone.Controller.User;

import com.example.onlyone.DTO.LoveDTO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.LoveService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


//互动模块
@RestController
@RequestMapping("user/api/Interaction")
@Slf4j
public class LoveController {

    @Resource
    private LoveService loveService;

    /**
     * 点赞/取消点赞
     * @param loveDTO 点赞参数
     * @return 操作结果
     */
    @PostMapping("/likes")
    public Result toggleLike(@RequestBody LoveDTO loveDTO) {
        loveService.toggleLike(loveDTO);
        return Result.success();
    }

    /**
     * 检查用户是否对某实体点过赞
     * @param entityId 实体ID（文章ID、评论ID等）
     * @param loveTypeId 点赞类型ID
     * @return 是否点过赞
     */
    @GetMapping("/is-liked")
    public Result<Boolean> isEntityLovedByUser(
            @RequestParam Long entityId,
            @RequestParam Long loveTypeId) {
        try {
            Boolean isLoved = loveService.isEntityLovedByUser(entityId, loveTypeId);
            return Result.success(isLoved);
        } catch (Exception e) {
            log.error("检查点赞状态失败: entityId={}, loveTypeId={}", entityId, loveTypeId, e);
            return Result.error("检查点赞状态失败");
        }
    }

    /**
     * 获取实体的点赞数
     * @param entityId 实体ID
     * @param loveTypeId 点赞类型ID
     * @return 点赞数
     */
    @GetMapping("/count")
    public Result<String> getEntityLoveCount(
            @RequestParam Long entityId,
            @RequestParam Long loveTypeId) {
        try {
            String count = loveService.getEntityLoveCount(entityId, loveTypeId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取点赞数失败: entityId={}, loveTypeId={}", entityId, loveTypeId, e);
            return Result.error("获取点赞数失败");
        }
    }







}
