package com.example.onlyone.VO;


import lombok.Data;

import java.util.List;

//查看文章详细
@Data
public class ArticleDetailVO {


    //文章部分
    private Long id;
    private String author;
    private String title;
    private String category;
    private List<String> label;
    private String content;
    private String summary;
    private String coverImageUrl;
    private String publishTime;

    //用户部分
    private UserProfileVO userProfileVO;

    // 交互状态
    private Boolean isLoved;    // 当前用户是否点赞
    //private Boolean isFollowed; // 当前用户是否关注作者
    private Long loveCount;     // 文章总点赞数
    private Long viewCount;     //文章浏览数
    private Long comments;   // 文章评论数
}
