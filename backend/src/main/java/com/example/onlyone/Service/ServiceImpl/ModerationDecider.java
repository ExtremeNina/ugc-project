package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.DTO.ModerationResult;
import com.example.onlyone.Entity.ContentStatus;
import com.example.onlyone.Entity.ModerationRecordStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ModerationDecider {

    public String decideBusinessStatus(ModerationResult result) {
        return switch (result.getSuggestion()) {
            case "pass" -> ContentStatus.APPROVED.value().toString();
            case "block" -> ContentStatus.REJECTED.value().toString();
            case "review" -> ContentStatus.HUMAN_REVIEW.value().toString();
            default -> ContentStatus.HUMAN_REVIEW.value().toString();
        };
    }

    public String decideRecordStatus(ModerationResult result) {
        return switch (result.getSuggestion()) {
            case "pass" -> ModerationRecordStatus.AUTO_APPROVED.value();
            case "block" -> ModerationRecordStatus.AUTO_REJECTED.value();
            case "review" -> ModerationRecordStatus.HUMAN_REVIEW.value();
            default -> ModerationRecordStatus.HUMAN_REVIEW.value();
        };
    }
}
