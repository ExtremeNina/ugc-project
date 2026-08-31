package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.UserLove;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户点赞 Mapper
 *
 * 软删除操作说明：
 * - selectByUserAndEntity：只查 status=1 的有效点赞记录
 * - cancelLove：软删除，将 status 设为 0（不物理删除）
 * - reLove：恢复点赞，将 status 重新设为 1 并更新创建时间（复用历史记录）
 */
public interface UserLoveMapper extends BaseMapper<UserLove> {

    @Select("select * from user_love where user_id = #{userId} and entity_id = #{entityId} and love_type_id = #{loveTypeId} and status = 1")
    UserLove selectByUserAndEntity(Long userId, Long entityId, Long loveTypeId);

    @Update("update user_love set status = 0 where user_id = #{userId} and entity_id = #{entityId} and love_type_id = #{loveTypeId}")
    void cancelLove(Long userId, Long entityId, Long loveTypeId);

    @Update("update user_love set status = 1, create_time = NOW() where user_id = #{userId} and entity_id = #{entityId} and love_type_id = #{loveTypeId}")
    int reLove(@Param("userId") Long userId, @Param("entityId") Long entityId, @Param("loveTypeId") Long loveTypeId);

    @Select("select COUNT(*) from follow where following_id = #{userId}")
    Long selectByUserId(Long userId);

    /**
     * 批量查询：用户在这些实体中已点赞（status=1）的实体 id 集合
     * 用于 Feed 流组装 VO 时替代"逐篇单查"，一条 SQL 搞定整页点赞状态
     */
    @Select("<script>" +
            "select entity_id from user_love where user_id = #{userId} and love_type_id = #{loveTypeId} and status = 1 " +
            "and entity_id in " +
            "<foreach collection='entityIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> selectLovedEntityIds(@Param("userId") Long userId,
                                    @Param("loveTypeId") Long loveTypeId,
                                    @Param("entityIds") List<Long> entityIds);
}
