package com.example.onlyone.VO;

import lombok.Data;

//编辑个人资料模块返回的vo对象
@Data
public class UpdateUserMessageVO {

    private String username;
    private String password;
    private String userSummary;
    private String mailbox;
    private String location;
    private String phone;
    private String icon;

}
