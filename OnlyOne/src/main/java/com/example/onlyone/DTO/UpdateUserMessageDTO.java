package com.example.onlyone.DTO;

import lombok.Data;

//个人界面更新
@Data
public class UpdateUserMessageDTO {

    private String username;
    private String mailbox;
    private String password;
    private String description;
    private String icon;
    private String location;
    private String phone;
}
