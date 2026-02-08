package com.example.onlyone.Utils;

import com.example.onlyone.Entity.UserDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


//security工具类
@Slf4j
@Component
public class SecurityUtils {

    /**
     * 获取当前认证信息
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("用户未登录，请先登录");
        }
        return auth.getName();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("用户未登录，请先登录");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetail) {
            return ((UserDetail) principal).getUserId();
        } else {
            log.info("无法获取用户ID，认证信息不完整");
            return null;
            //throw new SecurityException("无法获取用户ID，认证信息不完整");
        }
    }

    /**
     * 获取当前用户详情
     */
    public static UserDetail getCurrentUserDetail() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("用户未登录，请先登录");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetail) {
            return (UserDetail) principal;
        } else {
            throw new SecurityException("无法获取用户详情，认证信息不完整");
        }
    }

    /**
     * 验证用户是否已认证
     */
    public static void validateAuthentication() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("用户未登录，请先登录");
        }
    }

    /**
     * 检查用户是否有指定权限
     */
    public static boolean hasAuthority(String authority) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    /**
     * 检查用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * 获取当前用户的权限列表
     */
    public static List<String> getCurrentUserAuthorities() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Collections.emptyList();
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

}
