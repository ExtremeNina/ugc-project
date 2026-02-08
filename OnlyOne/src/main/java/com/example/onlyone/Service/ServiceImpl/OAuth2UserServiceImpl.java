package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.Entity.*;
import com.example.onlyone.Mapper.*;
import com.example.onlyone.Utils.SpringContextUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {




    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        UserMapper userMapper = SpringContextUtils.getBean(UserMapper.class);
        UserAndRoleMapper userAndRoleMapper = SpringContextUtils.getBean(UserAndRoleMapper.class);

        //获取github用户信息
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.info("获取github用户信息:{}", attributes);

        //获取用户名
        String username = (String) attributes.get("login");
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            User newUser = new User();
            newUser.setId(((Number) attributes.get("id")).longValue()); //修复Integer不能转换出Long类型
            newUser.setUsername(username);
            newUser.setPassword("123456");
            newUser.setCreateTime(LocalDateTime.now());
            newUser.setUpdateTime(LocalDateTime.now());
            userMapper.insert(newUser);
            //给用户赋予角色
            UserAndRole newUserAndRole = new UserAndRole();
            newUserAndRole.setUserId(newUser.getId());
            newUserAndRole.setRoleId(1L);
            newUserAndRole.setCreateTime(LocalDateTime.now());

            userAndRoleMapper.insert(newUserAndRole);


            log.info("github用户信息存入数据库");

        } else {
            //更新该用户用户名
            user.setUsername(username);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("GitHub用户信息更新成功");
        }

        // 关键修改：创建并返回 CustomOAuth2User
        // 将 DefaultOAuth2User 的权限集合转换为 List
        List<GrantedAuthority> authorities = new ArrayList<>(oAuth2User.getAuthorities());


        log.info("oAuth2User的类型为: {}", oAuth2User.getClass().getName());
        return new CustomOAuth2User(attributes,authorities);


    }
}
