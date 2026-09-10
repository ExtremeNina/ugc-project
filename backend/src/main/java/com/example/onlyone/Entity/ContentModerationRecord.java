package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内容审核记录表
 * <p>
 * 记录每次内容审核的全流程信息：AI 模型审核 → 敏感词过滤 → 人工复核。
 * 一条业务记录（article/comment）只对应一条审核记录（uk_target 唯一约束）。
 */
@Data
public class ContentModerationRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容类型：article（笔记）/ comment（评论） */
    private String targetType;

    /** 对应业务表 ID（article.id 或 comment.id） */
    private Long targetId;

    /** 同一内容的审核版本号；target_type + target_id + revision 唯一。 */
    private Integer revision;

    /** [审核修复 P0] 审核状态统一由 ModerationRecordStatus 管理。 */
    private String status;

    /** [审核修复 P1] 提交审核时的内容快照，保证人工审核看到原始版本。 */
    private String contentSnapshot;

    /** 阿里云内容安全返回的置信度（0.00 ~ 1.00） */
    private BigDecimal modelScore;

    /** 违规标签列表，JSON 数组，如 ["spam","porn"] */
    private String modelLabels;

    /** 阿里云返回的详细原因描述 */
    private String modelReason;

    /** AC 自动机命中的敏感词列表，JSON 数组，如 ["敏感词A","敏感词B"] */
    private String sensitiveWordsHit;

    /** 人工复核最终判定：approved（通过）/ rejected（拒绝） */
    private String humanDecision;

    /** 人工复核备注 */
    private String humanComment;

    /** 复核人用户 ID */
    private Long reviewedBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间（每次记录变更自动更新） */
    private LocalDateTime updatedAt;
}
