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
    /** 该好友发来的未读消息条数（Redis unread:hash 维护，打开聊天页即清零） */
    private Long unreadCount;

}
