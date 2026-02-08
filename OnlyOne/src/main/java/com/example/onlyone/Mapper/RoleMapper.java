package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.Role;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    List<Role> selectAllRoles(List<Long> roleIds);
}
