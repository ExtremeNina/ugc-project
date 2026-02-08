package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.RoleAndPermission;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleAndPermissionMapper extends BaseMapper<RoleAndPermissionMapper> {

    @Select("select  * from role_and_permission where role_id = #{roleId}")
    List<RoleAndPermission> queryPermission(Long roleId);
}
