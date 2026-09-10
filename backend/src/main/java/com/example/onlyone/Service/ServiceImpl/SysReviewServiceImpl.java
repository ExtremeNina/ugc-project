package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.Comment;
import com.example.onlyone.Entity.ContentModerationRecord;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Entity.ContentStatus;
import com.example.onlyone.Entity.ModerationRecordStatus;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.CommentMapper;
import com.example.onlyone.Mapper.ContentModerationRecordMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.SysReviewService;
import com.example.onlyone.VO.SysReviewVO;
import com.example.onlyone.websocket.ForumWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SysReviewServiceImpl implements SysReviewService {

    @Resource
    private ContentModerationRecordMapper contentModerationRecordMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private UserMapper userMapper;

    @Lazy
    @Resource
    private ForumWebSocketHandler forumWebSocketHandler;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public List<SysReviewVO> getPendingList(int page, int size, String type) {
        int offset = (page - 1) * size;
        List<ContentModerationRecord> records = contentModerationRecordMapper.selectByStatusAndType(ModerationRecordStatus.HUMAN_REVIEW.value(), type, offset, size);

        List<SysReviewVO> result = new ArrayList<>();
        for (ContentModerationRecord record : records) {
            SysReviewVO vo = new SysReviewVO();
            vo.setRecordId(record.getId());
            vo.setTargetType(record.getTargetType());
            vo.setTargetId(record.getTargetId());
            vo.setStatus(record.getStatus());
            vo.setModelScore(record.getModelScore());
            vo.setModelLabels(record.getModelLabels());
            vo.setModelReason(record.getModelReason());
            vo.setSensitiveWordsHit(record.getSensitiveWordsHit());
            vo.setCreatedAt(record.getCreatedAt());
            vo.setContentSnapshot(record.getContentSnapshot());
            vo.setRevision(record.getRevision());

            if ("article".equals(record.getTargetType())) {
                Article article = articleMapper.selectById(record.getTargetId());
                if (article != null) {
                    vo.setContent(record.getContentSnapshot() != null ? record.getContentSnapshot() : article.getContent());
                    User author = userMapper.selectById(article.getAuthorId());
                    vo.setAuthorName(author != null ? author.getUsername() : "未知");
                }
            } else if ("comment".equals(record.getTargetType())) {
                Comment comment = commentMapper.selectById(record.getTargetId());
                if (comment != null) {
                    vo.setContent(record.getContentSnapshot() != null ? record.getContentSnapshot() : comment.getContent());
                    User author = userMapper.selectById(comment.getUserId());
                    vo.setAuthorName(author != null ? author.getUsername() : "未知");
                }
            }

            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void approve(Long recordId, String remark) {
        ContentModerationRecord record = contentModerationRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("审核记录不存在");
        }
        Long reviewer = com.example.onlyone.Utils.SecurityUtils.getCurrentUserId();
        if (contentModerationRecordMapper.updateHumanDecision(recordId, "approved", reviewer, remark, ModerationRecordStatus.HUMAN_APPROVED.value()) != 1)
            throw new IllegalStateException("审核记录已被其他管理员处理");
        updateBusinessStatus(record.getTargetType(), record.getTargetId(), ContentStatus.APPROVED.value());
        record.setStatus(ModerationRecordStatus.HUMAN_APPROVED.value()); record.setHumanDecision("approved"); record.setReviewedBy(reviewer); record.setHumanComment(remark);
        sendResultNotification(record, "approved", remark);

        log.info("人工审核通过: recordId={}", recordId);
    }

    @Override
    @Transactional
    public void reject(Long recordId, String remark) {
        ContentModerationRecord record = contentModerationRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("审核记录不存在");
        }

        Long reviewer = com.example.onlyone.Utils.SecurityUtils.getCurrentUserId();
        if (contentModerationRecordMapper.updateHumanDecision(recordId, "rejected", reviewer, remark, ModerationRecordStatus.HUMAN_REJECTED.value()) != 1)
            throw new IllegalStateException("审核记录已被其他管理员处理");
        updateBusinessStatus(record.getTargetType(), record.getTargetId(), ContentStatus.REJECTED.value());
        record.setStatus(ModerationRecordStatus.HUMAN_REJECTED.value()); record.setHumanDecision("rejected"); record.setReviewedBy(reviewer); record.setHumanComment(remark);
        sendResultNotification(record, "rejected", remark);

        log.info("人工审核拦截: recordId={}", recordId);
    }

    private void updateBusinessStatus(String targetType, Long targetId, Long status) {
        if ("article".equals(targetType)) {
            Article article = new Article();
            article.setId(targetId);
            article.setStatus(status);
            if (articleMapper.updateById(article) != 1) throw new IllegalStateException("文章不存在或已删除");
        } else if ("comment".equals(targetType)) {
            Comment comment = new Comment();
            comment.setId(targetId);
            comment.setStatus(status);
            if (commentMapper.updateById(comment) != 1) throw new IllegalStateException("评论不存在或已删除");
        } else {
            throw new IllegalArgumentException("不支持的审核目标类型");
        }
    }

    private void sendResultNotification(ContentModerationRecord record, String decision, String remark) {
        try {
            Long userId = getUserId(record.getTargetType(), record.getTargetId());

            Map<String, Object> data = new HashMap<>();
            data.put("targetType", record.getTargetType());
            data.put("targetId", record.getTargetId());
            data.put("status", decision);
            data.put("reason", remark);

            Map<String, Object> message = new HashMap<>();
            message.put("type", "moderation_result");
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());

            String jsonMsg = objectMapper.writeValueAsString(message);
            forumWebSocketHandler.pushModerationResult(userId, jsonMsg);
        } catch (Exception e) {
            log.error("发送人工审核结果通知失败: recordId={}", record.getId(), e);
            // [审核修复 P1] 通知失败必须暴露给调用方，避免审核结果不可感知。
            throw new IllegalStateException("人工审核结果通知失败", e);
        }
    }

    private Long getUserId(String targetType, Long targetId) {
        if ("article".equals(targetType)) {
            Article article = articleMapper.selectById(targetId);
            return article != null ? article.getAuthorId() : 0L;
        } else if ("comment".equals(targetType)) {
            Comment comment = commentMapper.selectById(targetId);
            return comment != null ? comment.getUserId() : 0L;
        }
        return 0L;
    }
}
