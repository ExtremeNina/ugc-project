package com.example.onlyone.VO;

import lombok.Data;

import java.time.LocalDateTime;

//逻辑过期类
@Data
public class LogicalExpire {

    private Object date;
    private LocalDateTime expireTime;
}
