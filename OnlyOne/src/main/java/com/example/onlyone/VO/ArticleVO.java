package com.example.onlyone.VO;

import lombok.Data;

//创作中心文章列表展示
@Data
public class ArticleVO {

    private Long id;
    private String title;
    private Long authorId;
    private Long categoryId;
    private Long pageview;
    private Long status;
    private String createTime;
    private String updateTime;
    private String coverImageUrl;


    //后续补充的内容
    private Long loveCount;     // 文章总点赞数
    private Long viewCount;     // 文章浏览数
}
