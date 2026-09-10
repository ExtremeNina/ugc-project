package com.example.onlyone.DTO;


import lombok.Data;

@Data
public class PrivateDTO {

    private String type;
    private Long receiveId;
    private String receiveName;
    private Long senderId;
    private String senderIcon;
    private String content;
    // [实时通信升级] 客户端幂等键；重试时必须保持不变
    private String clientMessageId;
    // [实时通信升级] 已读回执携带的消息水位
    private Long maxReadMessageId;
    // [实时通信升级] 接收方确认已收到的服务端消息号
    private Long messageId;

}
