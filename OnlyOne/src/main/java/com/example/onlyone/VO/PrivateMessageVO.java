package com.example.onlyone.VO;

import lombok.Data;

@Data
public class PrivateMessageVO {

    private Long senderId;
    private String icon;
    private String content;
    private String dateTime;
    private Long notReadCount;


}
