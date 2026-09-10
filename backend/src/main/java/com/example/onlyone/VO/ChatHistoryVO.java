package com.example.onlyone.VO;

import lombok.Data;


//聊天记录的返回对象
@Data
public class ChatHistoryVO {

    private Long id;
    // [实时通信升级] 用于前端 ACK 关联和断点续传
    private String clientMessageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    //间隔时间显示
    private String time;
    private Boolean isOwn;
    private Integer deliveryStatus;
}
