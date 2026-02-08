package com.example.onlyone.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.DTO.CommentDTO;
import com.example.onlyone.VO.CommentVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Entity.Comment;
import com.github.pagehelper.PageInfo;


public interface CommentService extends IService<Comment> {

    UserProfileVO getUserProfile();

    CommentVO publishComment(CommentDTO commentDTO);

    PageInfo<CommentVO> getCommentList(Long articleId);
}
