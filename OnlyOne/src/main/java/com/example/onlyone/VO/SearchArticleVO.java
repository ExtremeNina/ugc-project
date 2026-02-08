package com.example.onlyone.VO;

import lombok.Data;

//搜索页面查询文章列表返回对象
@Data
public class SearchArticleVO {

    //文章id
    private Long id;
    private String title;
    private String publishTime;
    private String userIcon;
    private String coverImageUrl;
    private String url;
    private String author;
    private String category;
    private String loveCount;
    private Boolean isLove;
    private Long viewCount;

}
