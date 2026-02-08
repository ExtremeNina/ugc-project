package com.example.onlyone.Controller.User;

import com.example.onlyone.DTO.QQLoginDTO;
import com.example.onlyone.DTO.RegisterDTO;
import com.example.onlyone.DTO.LoginDTO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.LoginService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthUserController {

    @Resource
    private LoginService userService;


    //普通登录
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO userDTO) {
        String JwtToken = userService.login(userDTO);
        return Result.success(JwtToken);
    }

    @PostMapping("/QQ/sendCode")
    public Result SendQQEmail(@RequestBody Map<String, String> emailMap) {
        String email = emailMap.get("email");
        Boolean isSend = userService.sendQQEmail(email);
        return Result.success(isSend);
    }

    @PostMapping("/QQ/login")
    public Result loginQQ(@RequestBody QQLoginDTO qqLoginDTO) {
        String JWT = userService.qqLogin(qqLoginDTO);
        return Result.success(JWT);
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        userService.registerUser(registerDTO);
        return Result.success();
    }

    @PostMapping("/verify-code")
    public Result setCode(@RequestBody RegisterDTO registerDTO) {
        String code = userService.setCode(registerDTO);
        return Result.success(code);
    }

    @PostMapping("/loginOut")
    public Result loginOut(HttpServletRequest request, HttpServletResponse response) {
        userService.loginOut(request);
        return Result.success();
    }





}
