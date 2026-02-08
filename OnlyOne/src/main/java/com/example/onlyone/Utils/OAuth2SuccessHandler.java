package com.example.onlyone.Utils;

import com.example.onlyone.Entity.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException{

        //对外是new一个新对象所以不能依赖注入
        JwtProvider jwtProvider = SpringContextUtils.getBean(JwtProvider.class);

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomOAuth2User) {
            String jwtToken = jwtProvider.generateToken(authentication);
            log.info("github用户jwt生成成功：{}", jwtToken);
            // 直接返回JSON给前端
            String redirectUrl = String.format("%s/oauth/callback?token=%s",
                    "http://localhost:8080",
                    URLEncoder.encode(jwtToken, StandardCharsets.UTF_8));

            response.sendRedirect(redirectUrl);
        }else {
            log.info("Authentication 类型: {}", authentication.getClass().getName());
            log.info("principal类型为: {}", principal.getClass().getName());
        }
    }
}
