package com.example.onlyone.VO;


import lombok.Data;

//动态最新文章
@Data
public class DyArticleVO {

    //文章id
    private Long id;
    private String title;
    private String publishTime;
    private String userIcon;
    private String coverImageUrl;
    private String url;
    private String author;
    //点赞数
    private Long love;
    private Boolean isLove;
    private Long viewCount;

}
