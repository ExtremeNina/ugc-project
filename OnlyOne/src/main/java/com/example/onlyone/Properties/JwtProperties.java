package com.example.onlyone.Properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    private String secret = "your-very-long-secret-key-that-is-at-least-32-characters-long";
    private long expiration = 86400000L; // 24小时
    private long refreshExpiration = 604800000L; // 7天
}
