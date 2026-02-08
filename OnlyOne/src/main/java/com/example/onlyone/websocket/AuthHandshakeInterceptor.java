package com.example.onlyone.websocket;
import com.example.onlyone.Utils.JwtProvider;
import com.example.onlyone.Utils.SpringContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Slf4j
public class AuthHandshakeInterceptor implements HandshakeInterceptor {



    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {

            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 从查询参数获取token
            String token = httpRequest.getParameter("token");
            log.info("websocket握手的token: {}", token);

            //判断token的格式
            if (StringUtils.hasText(token)) {
                try {

                    JwtProvider jwtProvider = SpringContextUtils.getBean(JwtProvider.class);

                    if (jwtProvider.validateToken(token)) {
                        String username = jwtProvider.getUsernameFromToken(token);

                        Long userId = jwtProvider.getUserIdFromToken(token);

                        // 将用户信息存入attributes，在WebSocketHandler中可以使用
                        attributes.put("userId", userId);
                        attributes.put("username", username);
                        attributes.put("token", token);
                        log.info("用户：{}({})websocket握手成功",username,userId);
                        return true;
                    }


                } catch (Exception e) {
                    log.info("WebSocket握手时验证token失败", e);
                }
            }
        }
        log.info("WebSocket握手失败，未提供有效token");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {

    }


}
