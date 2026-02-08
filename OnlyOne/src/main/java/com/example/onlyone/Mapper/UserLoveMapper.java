package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.UserLove;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

public interface UserLoveMapper extends BaseMapper<UserLove> {

    @Select("select * from user_love where user_id = #{userId} and entity_id = #{entityId} and love_type_id = #{loveTypeId}")
    UserLove selectByUserAndEntity(Long userId, Long entityId, Long loveTypeId);


    @Delete("delete from user_love where user_id = #{userId} and entity_id = #{entityId} and love_type_id = #{loveTypeId}")
    void deleteByUserAndEntity(Long userId, Long entityId, Long loveTypeId);


    @Select("select COUNT(*) from follow where following_id = #{userId}")
    Long selectByUserId(Long userId);
}
