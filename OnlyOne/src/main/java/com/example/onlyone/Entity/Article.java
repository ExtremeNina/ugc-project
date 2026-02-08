package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long authorId;
    private Long categoryId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long pageview;
    private Long status;
    private String content;
    private String summary;
    private String coverImageUrl;
    private Long love;
    private Long isCommented;
}
