package com.example.onlyone.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails 实现
 *
 * 注意：不使用 @Data，因为 Lombok 生成的 equals()/hashCode()/toString()
 * 会包含 passwordEncoder 字段和 User 对象（含密码），
 * 这会导致 Spring Security 认证/授权过程中的对象比较异常（403）。
 */
@Getter
@Setter
@NoArgsConstructor
public class UserDetail implements UserDetails {

    private User user;
    private PasswordEncoder passwordEncoder;
    private List<GrantedAuthority> authorities;

    public UserDetail(User user, PasswordEncoder passwordEncoder, List<GrantedAuthority> authorities) {
        this.user = user;
        this.passwordEncoder = passwordEncoder;
        this.authorities = authorities;
    }

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

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}
