package com.example.onlyone.VO;

import lombok.Data;

//个人界面用户信息VO对象
@Data
public class UserProfileVO {

    private Long userId;
    private String icon;
    private String username;
    private String description;
    private Long fan;
    private Long love;

    //添加字段
    private Boolean isFollow;
    //关注数
    private Long follows;
}
