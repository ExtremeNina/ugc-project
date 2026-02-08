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

}
