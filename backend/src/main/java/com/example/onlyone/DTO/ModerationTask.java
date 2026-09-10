package com.example.onlyone.DTO;

import lombok.Data;

@Data
public class ModerationTask {

    private String targetType;
    private Long targetId;
    private String content;
    private Long userId;
    // [审核修复 P0] 任务绑定提交时的审核版本，防止旧任务覆盖新版本
    private Integer revision;
    // [审核修复 P1] 审核时使用的内容快照
    private String contentSnapshot;
}
