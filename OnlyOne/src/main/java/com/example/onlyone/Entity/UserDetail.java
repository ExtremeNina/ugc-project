package com.example.onlyone.Entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class UserDetail implements UserDetails {


    private User user;

    private PasswordEncoder passwordEncoder;
    List<GrantedAuthority> authorities;


    public UserDetail(User user,PasswordEncoder passwordEncoder,List<GrantedAuthority> authorities) {
        this.user = user;
        this.passwordEncoder = passwordEncoder;
        this.authorities = authorities;
    }
    // 添加获取userId的方法
    public Long getUserId() {
        return user.getId();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return passwordEncoder.encode(user.getPassword());
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }


    // 建议实现剩余方法，避免潜在问题
    @Override
    public boolean isAccountNonExpired() {
        return true; // 根据业务需求实现
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 根据业务需求实现
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 根据业务需求实现
    }


}
