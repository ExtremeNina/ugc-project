package com.example.onlyone.DTO;

import lombok.Data;

@Data
public class CommentDTO {

    //文章id
    private Long articleId;
    //父评论id
    private Long parentId;
    //评论内容
    private String content;
    //该文章是否允许评论
    private Long isCommented;
}
