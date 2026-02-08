package com.example.onlyone.DTO;

import lombok.Data;
import java.util.List;


//创作中心编写文章
@Data
public class ArticleDTO {

    private Long id;
    private String title;
    private Long categoryId;
    private List<Long> label;
    private String content;
    private String summary;
    private String coverImageUrl;
    private Long isCommented;

}
