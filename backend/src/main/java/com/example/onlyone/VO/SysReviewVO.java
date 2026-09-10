package com.example.onlyone.VO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SysReviewVO {

    private Long recordId;
    private String targetType;
    private Long targetId;
    private String status;
    private BigDecimal modelScore;
    private String modelLabels;
    private String modelReason;
    private String sensitiveWordsHit;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
    // [审核修复 P1] 审核内容快照
    private String contentSnapshot;
    private Integer revision;
}
