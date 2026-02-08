package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.UserAndRole;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserAndRoleMapper extends BaseMapper<UserAndRole> {

    @Select("select * from user_and_role where user_id = #{userId}")
    List<UserAndRole> queryRoles(Long userId);

}
