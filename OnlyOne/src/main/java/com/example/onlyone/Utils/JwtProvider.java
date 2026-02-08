package com.example.onlyone.Utils;

import com.example.onlyone.Entity.CustomOAuth2User;
import com.example.onlyone.Entity.UserDetail;
import com.example.onlyone.Properties.JwtProperties;
import io.jsonwebtoken.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final Key key;

    // 通过构造函数注入 JwtProperties
    public JwtProvider(JwtProperties jwtProperties, Key key) {
        this.jwtProperties = jwtProperties;
        this.key = key;
    }


    //生成token
    public String generateToken(Authentication authenticate) {

        String username = authenticate.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        Long userId = null;
        String source = "local";

        //判断认证信息来源,是否是本地或者第三方用户
        Object principal = authenticate.getPrincipal();
        if (principal instanceof UserDetail) {
            userId = ((UserDetail) principal).getUserId();
        }else {
            //获取用户对象
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authenticate.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();
            if (attributes.containsKey("login")) {
                //设置用户标准
                source = "github";
                //设置用户id
                userId = ((Number) attributes.get("id")).longValue();
            }
        }

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("source", source)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(this.key,SignatureAlgorithm.HS256)
                .compact();
    }

    // 从 Token 中获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(this.key)  // 使用注入的 Key 对象
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 验证 Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(this.key)  // 使用注入的 Key 对象
                    .build()
                    .parseClaimsJws(token);
            return true;
        }  catch (ExpiredJwtException ex) {
            log.warn("Token 已过期: {}", ex.getMessage());
            return false;

        } catch (UnsupportedJwtException ex) {
            log.warn("不支持的 Token 格式: {}", ex.getMessage());
            return false;

        } catch (MalformedJwtException ex) {
            log.warn("Token 格式错误: {}", ex.getMessage());
            return false;

        } catch (SignatureException ex) {
            log.warn("Token 签名验证失败: {}", ex.getMessage());
            return false;

        } catch (IllegalArgumentException ex) {
            log.warn("Token 参数异常: {}", ex.getMessage());
            return false;

        } catch (Exception ex) {
            log.error("Token 验证发生未知错误: {}", ex.getMessage(), ex);
            return false;
        }
    }

    // 新增：从 Token 中获取用户ID
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 从claims中获取userId
            Object userIdObj = claims.get("userId");

            if (userIdObj == null) {
                log.warn("Token中没有包含userId: {}", token);
                return null;
            }

            // 处理不同类型的数字（Integer, Long等）
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            } else if (userIdObj instanceof String) {
                try {
                    return Long.parseLong((String) userIdObj);
                } catch (NumberFormatException e) {
                    log.warn("Token中的userId格式错误: {}", userIdObj);
                    return null;
                }
            } else {
                log.warn("Token中的userId类型不支持: {}", userIdObj.getClass());
                return null;
            }

        } catch (ExpiredJwtException ex) {
            // 对于过期的token，我们仍然可以解析claims
            log.warn("Token已过期，但尝试解析其中的userId");
            Object userIdObj = ex.getClaims().get("userId");
            return parseUserIdFromObject(userIdObj);

        } catch (Exception ex) {
            log.error("获取用户ID时发生错误: {}", ex.getMessage());
            return null;
        }
    }

    // 辅助方法：从对象解析用户ID
    private Long parseUserIdFromObject(Object userIdObj) {
        if (userIdObj == null) {
            return null;
        }

        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        } else if (userIdObj instanceof String) {
            try {
                return Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // 1. 从 JWT 获取用户来源（source）
    public String getSourceFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 从 claims 中获取 source，如果没有则返回 "local"
            return claims.get("source", String.class);
        } catch (Exception e) {
            log.warn("从 JWT 获取用户来源失败: {}", e.getMessage());
            return "local"; // 默认返回本地用户
        }
    }



    //获取jwt令牌剩余时间
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

}


