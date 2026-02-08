package com.example.onlyone.Configuration;

import com.example.onlyone.Properties.JwtProperties;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Configuration
public class JwtConfig {

    @Resource
    private JwtProperties jwtProperties;

    @Bean
    public Key jwtKey() {
        // 方法1：直接使用字符串生成密钥（确保字符串足够长）
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
