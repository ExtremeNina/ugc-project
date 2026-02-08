package com.example.onlyone.Configuration;

import com.example.onlyone.websocket.AuthHandshakeInterceptor;
import com.example.onlyone.websocket.ForumWebSocketHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ForumWebSocketHandler(stringRedisTemplate),"/ws")
                .addInterceptors(new AuthHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
