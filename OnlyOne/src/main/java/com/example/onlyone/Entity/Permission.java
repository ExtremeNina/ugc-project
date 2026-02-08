package com.example.onlyone.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Permission {

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String category;
    private String module;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
