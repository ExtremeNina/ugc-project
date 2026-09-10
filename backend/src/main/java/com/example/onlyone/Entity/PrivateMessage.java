package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    // [实时通信升级] 客户端消息唯一号，配合发送者实现幂等
    private String clientMessageId;
    private Long userId;
    private Long receiveId;
    private String content;
    //用于排序
    private LocalDateTime dateTime;
    //0为未读1为已读
    private Long status;
    // [实时通信升级] 投递状态：0持久化，1已送达，2已读；status 仍仅表示接收方已读
    private Integer deliveryStatus;

}
