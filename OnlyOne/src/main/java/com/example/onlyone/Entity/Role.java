package com.example.onlyone.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Role {

    private Long id;
    private String roleName;
    private String displayName;
    private String description;
    private int level;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
