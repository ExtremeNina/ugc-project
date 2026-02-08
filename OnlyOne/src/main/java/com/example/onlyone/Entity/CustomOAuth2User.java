package com.example.onlyone.Entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class CustomOAuth2User implements OAuth2User {

    private Map<String, Object> attributes;
    List<GrantedAuthority> authorities;

    public CustomOAuth2User(Map<String,Object> attributes,List<GrantedAuthority> authorities) {
        this.attributes = attributes;
        this.authorities = authorities;
    }


    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return attributes.get("login").toString();
    }
}
