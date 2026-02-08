package com.example.onlyone.VO;

import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String password;
    private String icon;
    private String createTime;

}
