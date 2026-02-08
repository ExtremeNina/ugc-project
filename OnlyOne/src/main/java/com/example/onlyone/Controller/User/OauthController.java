package com.example.onlyone.Controller.User;

import com.example.onlyone.Common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth")
public class OauthController {


    @PostMapping("/github/login")
    public Result githubPage() {

        Map<String, Object> response = new HashMap<>();
        response.put("message", "请使用GitHub登录");
        response.put("github_login_url", "/oauth2/authorization/github");

        return Result.success(response);
    }



}
