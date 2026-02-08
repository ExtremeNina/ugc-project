package com.example.onlyone.VO;

import lombok.Data;

@Data
public class FriendVO {

    private Long userId;
    private String userName;
    private String icon;
    private Boolean isOnline;
    private String lastText;
    private String lastTime;

}
