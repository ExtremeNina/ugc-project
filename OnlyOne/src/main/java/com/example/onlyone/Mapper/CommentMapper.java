package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.Comment;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {

    @Update("update comment set reply_count = reply_count + #{increment} where id = #{id}")
    void updateReplyCount(Long id , Integer increment);

    @Select("select * from comment where article_id = #{articleId} and status = 3 and parent_id is null order by create_time asc")
    List<Comment> selectAllComment(Long articleId);

    //查看当前评论的全部回复
    @Select("select * from comment where root_id = #{rootId}")
    List<Comment> selectAllReply(Long rootId);


    @Select("select count(*) from comment where article_id = #{articleId}")
    Long countByArticleId(Long articleId);
}
