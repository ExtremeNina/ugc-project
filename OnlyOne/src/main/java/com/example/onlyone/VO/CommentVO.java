package com.example.onlyone.VO;

import lombok.Data;

import java.util.List;

@Data
public class CommentVO {

    private Long id;
    private Long userId;
    private Long articleId;
    private String username;
    private String icon;
    private String content;
    private Long love;
    private Long replyCount;
    private String createTime;

    private Boolean isLove;
    //回复评论
    private List<CommentVO> replyList;
    //回复的用户名
    private String replyName;

}
