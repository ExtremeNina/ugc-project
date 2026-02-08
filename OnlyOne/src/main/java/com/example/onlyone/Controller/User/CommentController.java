package com.example.onlyone.Controller.User;

import com.example.onlyone.DTO.CommentDTO;
import com.example.onlyone.VO.CommentVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.CommentService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
@Slf4j
public class CommentController {

    @Resource
    private CommentService commentService;


    //用户基本信息
    @GetMapping("/userProfile")
    public Result userProfile() {
        UserProfileVO userProfileVO = commentService.getUserProfile();
        return Result.success(userProfileVO);
    }

    //发表评论
    @PostMapping("/publish")
    private Result publishComment(@RequestBody CommentDTO commentDTO) {
        log.info("评论传输数据: {}", commentDTO);
        CommentVO commentVO = commentService.publishComment(commentDTO);
        if (commentVO == null) {
            log.info("该文章不允许评论");
            return Result.success(false);
        }
        return Result.success(commentVO);
    }

    //获取评论列表
    @GetMapping("{articleId}")
    private Result<PageInfo<CommentVO>> getCommentList(@PathVariable Long articleId) {
        PageInfo<CommentVO> commentVOList = commentService.getCommentList(articleId);
        log.info("当前文章的评论列表: {}", commentVOList);
        return Result.success(commentVOList);

    }

}
