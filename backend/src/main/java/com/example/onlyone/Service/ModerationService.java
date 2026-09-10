package com.example.onlyone.Service;

import com.example.onlyone.DTO.ModerationTask;

public interface ModerationService {

    void submitTask(ModerationTask task);
    // [审核修复 P1] 由 Outbox 调用的实际 MQ 投递方法
    void publishTask(ModerationTask task);
}
