package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String mailbox;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private int status;
    private String password;
    private int fan;
    private String description;
    private int love;
    private String icon;
    private String location;
    private LocalDateTime lastLoginTime;
}
