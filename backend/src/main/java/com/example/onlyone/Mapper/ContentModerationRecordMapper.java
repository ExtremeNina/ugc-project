package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.ContentModerationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContentModerationRecordMapper extends BaseMapper<ContentModerationRecord> {

    // [审核修复 P1] 仅允许 human_review 状态被一次性抢占，避免并发审核覆盖。
    @Update("UPDATE content_moderation_record SET human_decision=#{decision}, reviewed_by=#{reviewedBy}, human_comment=#{comment}, status=#{status}, updated_at=NOW() WHERE id=#{recordId} AND status='human_review'")
    int updateHumanDecision(@Param("recordId") Long recordId,
                            @Param("decision") String decision,
                            @Param("reviewedBy") Long reviewedBy,
                            @Param("comment") String comment,
                            @Param("status") String status);

    @Select("select coalesce(max(revision), 0) from content_moderation_record where target_type = #{targetType} and target_id = #{targetId}")
    Integer selectMaxRevision(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    // [审核修复 P0] 重复投递时按业务版本幂等确认，避免 ACK 丢失导致重复处理。
    @Select("select * from content_moderation_record where target_type=#{targetType} and target_id=#{targetId} and revision=#{revision} limit 1")
    ContentModerationRecord selectByTargetRevision(@Param("targetType") String targetType,
                                                    @Param("targetId") Long targetId,
                                                    @Param("revision") Integer revision);

    @Select("select * from content_moderation_record where status = #{status} and target_type = #{targetType} order by created_at asc limit #{offset}, #{limit}")
    List<ContentModerationRecord> selectByStatusAndType(@Param("status") String status,
                                                         @Param("targetType") String targetType,
                                                         @Param("offset") int offset,
                                                         @Param("limit") int limit);
}
