package com.example.onlyone.Utils;

import com.example.onlyone.Service.ServiceImpl.OAuth2UserServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

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

            //判断当前用是否处于登录状态
            String jwt = getJwtFromRequest(request);
            log.info("认证的JWT: {}", jwt);

            if (!StringUtils.hasText(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            if(!jwtProvider.validateToken(jwt)) {
                log.warn("JWT令牌无效: {}", jwt);
                return;
            }

            String username = jwtProvider.getUsernameFromToken(jwt);
            String jwtKey = "black:jwt"+ DigestUtils.md5DigestAsHex(jwt.getBytes());

            //黑名单中则代表退出登录，放行
            if (stringRedisTemplate.hasKey(jwtKey)) {
                log.info("您当前不在登录状态中");
                filterChain.doFilter(request, response);
                return;
            }

            //查询用户信息封装成userDetail对象
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            //封装成认证后对象
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            //封装到上下文之中
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.info("保存的上下文：{}",SecurityContextHolder.getContext().getAuthentication());

            //放行
            filterChain.doFilter(request, response);

        }catch (Exception e) {
            log.error("JWT认证出现异常",e);
            //清理上下文防止错误信息
            SecurityContextHolder.clearContext();
            //filterChain.doFilter(request, response);
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
