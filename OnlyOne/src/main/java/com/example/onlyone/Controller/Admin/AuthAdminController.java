package com.example.onlyone.Controller.Admin;

import com.example.onlyone.DTO.RegisterDTO;
import com.example.onlyone.DTO.LoginDTO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.LoginService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin")
public class AuthAdminController {

    @Resource
    private LoginService userService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO userDTO) {
        String JwtToken = userService.login(userDTO);
        return Result.success(JwtToken);
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {

        return null;
    }



}
