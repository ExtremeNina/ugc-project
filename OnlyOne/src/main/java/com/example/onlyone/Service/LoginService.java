package com.example.onlyone.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.DTO.QQLoginDTO;
import com.example.onlyone.DTO.RegisterDTO;
import com.example.onlyone.DTO.LoginDTO;
import com.example.onlyone.Entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface LoginService extends IService<User> {

    void insertUser(LoginDTO userdto);

    String login(LoginDTO userDTO);

    String setCode(RegisterDTO registerDTO);

    void registerUser(RegisterDTO registerDTO);

    void loginOut(HttpServletRequest request);

    Boolean sendQQEmail(String email);

    String qqLogin(QQLoginDTO qqLoginDTO);
}
