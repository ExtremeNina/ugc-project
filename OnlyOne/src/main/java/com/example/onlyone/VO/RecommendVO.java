package com.example.onlyone.VO;

import lombok.Data;

//首页推荐文章返回类
@Data
public class RecommendVO {

    private Long id;
    private String author;
    private String icon;
    private String title;
    private String summary;
    private String coverImageUrl;
    private String publishTime;
    private Boolean isLoved;    // 当前用户是否点赞
    //private Boolean isFollowed; // 当前用户是否关注作者
    private Long loveCount;     // 文章总点赞数
    private Long viewCount;     //文章浏览数
    private Long comments;   // 文章评论数
}
