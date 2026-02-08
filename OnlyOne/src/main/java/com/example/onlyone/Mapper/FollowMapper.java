package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.Follow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FollowMapper extends BaseMapper<Follow> {

    @Select("select * from follow where follower_id = #{currentUserId} and following_id = #{authorId}")
    Follow selectByUserId(Long currentUserId,Long authorId);

    @Delete("delete  from follow where follower_id = #{currentUserId} and following_id = #{authorId}")
    void deleteByUserId(Long currentUserId,Long authorId);


    @Select("SELECT COUNT(*) FROM follow WHERE following_id = #{userId}")
    Long countFollowers(Long userId);

    @Select("SELECT COUNT(*) FROM follow WHERE follower_id = #{userId}")
    Long countFollowing(Long userId);

    //获取粉丝id集合
    @Select("select * from follow where following_id = #{userId}")
    List<Follow> selectByFollowingId(Long userId);

    //获取所有关注作者的id集合
    @Select("SELECT following_id FROM follow WHERE follower_id = #{followerId}")
    List<Long> getFollowedAuthorIds(@Param("followerId") Long followerId);


}
