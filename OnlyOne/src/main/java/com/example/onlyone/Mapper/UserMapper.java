package com.example.onlyone.Mapper;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    @Select("select * from user")
    List<User> selectAll();

    org.springframework.security.core.userdetails.User selectOne(LambdaQueryWrapper<org.springframework.security.core.userdetails.User> queryWrapper);

    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);


    @Update(
            "update `user` u set u.last_login_time = #{loginTime} where u.username = #{username} "
    )
    void updateLastLoginTime(String username, LocalDateTime loginTime);


    @Select("select * from user where mailbox = #{mail}")
    User selectByMail(String mail);

    /**
     * 查询指定时间前未登录的用户ID
     */
    @Select("SELECT id FROM user WHERE last_login_time < #{dateTime} OR last_login_time IS NULL")
    List<Long> selectInactiveUserIds(@Param("dateTime") LocalDateTime dateTime);
}
