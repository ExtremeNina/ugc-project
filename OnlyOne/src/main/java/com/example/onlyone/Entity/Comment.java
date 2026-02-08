package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long articleId;
    private Long rootId;
    private Long parentId;
    private String username;
    private String icon;
    private String content;
    private Long love;
    private Long replyCount;
    private Long status;
    private String ip;
    private LocalDateTime createTime;

}
