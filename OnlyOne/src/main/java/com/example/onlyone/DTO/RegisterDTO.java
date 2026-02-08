package com.example.onlyone.DTO;
import lombok.Data;

//注册信息
@Data
public class RegisterDTO {

    private String username;
    private String mailbox;
    private String onePassword;
    private String twoPassword;
    private String code;
}
