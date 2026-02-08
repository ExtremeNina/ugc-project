package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long receiveId;
    private String content;
    //用于排序
    private LocalDateTime dateTime;
    //0为未读1为已读
    private Long status;

}
