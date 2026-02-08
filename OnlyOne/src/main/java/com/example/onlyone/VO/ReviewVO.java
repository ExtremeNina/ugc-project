package com.example.onlyone.VO;

import lombok.Data;

//创作中心文章查看
@Data
public class ReviewVO {

    private Long id;
    private String title;
    private String author;
    private Long category_id;
    private String content;
    private String summary;
    private int status;
    private String createTime;
    private String coverImageUrl;

    //后续补充的内容
    private Long loveCount;     // 文章总点赞数
    private Long viewCount;     // 文章浏览数
}
