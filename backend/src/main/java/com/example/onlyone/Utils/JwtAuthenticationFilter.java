package com.example.onlyone.Utils;

import cn.hutool.json.JSONUtil;
import com.example.onlyone.Common.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtProvider jwtProvider;
    @Resource
    private UserDetailsService userDetailsService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (!StringUtils.hasText(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtProvider.isTokenExpired(jwt)) {
                log.info("Access Token 已过期，返回 401 提示前端刷新");
                writeJsonResponse(response, 401, "TOKEN_EXPIRED", "Access Token 已过期，请刷新");
                return;
            }

            if (!jwtProvider.validateToken(jwt)) {
                log.warn("JWT令牌无效: {}", jwt);
                filterChain.doFilter(request, response);
                return;
            }

            // Bearer 认证只接受 Access Token，Refresh Token 只能用于刷新接口。
            if (!"access".equals(jwtProvider.getTokenType(jwt))) {
                writeJsonResponse(response, 401, "INVALID_TOKEN_TYPE", "仅允许使用 Access Token");
                return;
            }

            String jti = jwtProvider.getJtiFromToken(jwt);
            if (jti != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey("blacklist:at:" + jti))) {
                log.info("Token 已在黑名单中");
                writeJsonResponse(response, 401, "TOKEN_REVOKED", "Token 已被吊销");
                return;
            }

            String username = jwtProvider.getUsernameFromToken(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT认证出现异常", e);
            SecurityContextHolder.clearContext();
        }
    }

    private void writeJsonResponse(HttpServletResponse response, int status, String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<String> result = new Result<>();
        result.setCode(status);
        result.setMsg(errorCode + ": " + message);
        result.setData(errorCode);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
