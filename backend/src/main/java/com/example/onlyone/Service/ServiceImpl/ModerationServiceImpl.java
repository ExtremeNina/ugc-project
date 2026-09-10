package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.DTO.ModerationResult;
import com.example.onlyone.DTO.ModerationTask;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.Comment;
import com.example.onlyone.Entity.ContentModerationRecord;
import com.example.onlyone.Entity.ModerationRecordStatus;
import com.example.onlyone.Entity.ModerationTaskOutbox;
import com.example.onlyone.Mapper.ModerationTaskOutboxMapper;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.CommentMapper;
import com.example.onlyone.Mapper.ContentModerationRecordMapper;
import com.example.onlyone.Service.AliyunModerationService;
import com.example.onlyone.Service.ModerationService;
import com.example.onlyone.websocket.ForumWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ModerationServiceImpl implements ModerationService {

    // [审核修复 P0] 通过代理调用事务方法，避免 self-invocation 绕过 @Transactional。
    @Lazy
    @Resource
    private ModerationServiceImpl self;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private AliyunModerationService aliyunModerationService;

    @Resource
    private ModerationDecider moderationDecider;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private ContentModerationRecordMapper contentModerationRecordMapper;

    @Lazy
    @Resource
    private ForumWebSocketHandler forumWebSocketHandler;

    @Resource
    private ObjectMapper objectMapper;
    @Resource private ModerationTaskOutboxMapper outboxMapper;

    @Override
    public void submitTask(ModerationTask task) {
        // [审核修复 P1] 只写 Outbox；调用方事务提交后由调度器可靠投递 MQ。
        ModerationTaskOutbox row = new ModerationTaskOutbox();
        row.setTargetType(task.getTargetType()); row.setTargetId(task.getTargetId());
        row.setRevision(task.getRevision()); row.setContent(task.getContentSnapshot() != null ? task.getContentSnapshot() : task.getContent());
        row.setUserId(task.getUserId()); row.setSent(0); row.setRetryCount(0); row.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(row);
    }

    @Override
    public void publishTask(ModerationTask task) {
        rabbitTemplate.convertAndSend("moderation.exchange", "moderation.request", task);
        log.info("审核任务已提交: targetType={}, targetId={}", task.getTargetType(), task.getTargetId());
    }

    @RabbitListener(queues = "moderation.request.queue")
    public void handleRequest(ModerationTask task, com.rabbitmq.client.Channel channel,
                              org.springframework.amqp.core.Message message) throws java.io.IOException {
        log.info("处理审核任务: targetType={}, targetId={}", task.getTargetType(), task.getTargetId());

        try {
            ContentModerationRecord duplicate = contentModerationRecordMapper.selectByTargetRevision(task.getTargetType(), task.getTargetId(), task.getRevision());
            if (duplicate != null) {
                // [审核修复 P0] 业务事务已成功但 ACK 丢失时，重复任务直接确认。
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            validateTask(task);
            self.processModeration(task);
            // [审核修复 P0] processModeration 正常返回代表事务已提交，此时才 ACK。
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

            log.info("审核完成: targetType={}, targetId={}, revision={}", task.getTargetType(), task.getTargetId(), task.getRevision());
        } catch (Exception e) {
            log.error("审核处理异常: targetType={}, targetId={}", task.getTargetType(), task.getTargetId(), e);
            // [审核修复 P0] 并发重复任务触发唯一键冲突时，若结果已落库则视为幂等成功。
            if (contentModerationRecordMapper.selectByTargetRevision(task.getTargetType(), task.getTargetId(), task.getRevision()) != null) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            // [审核修复 P0] 重新抛出，让 Spring 事务回滚，避免 ACK/DLQ 与数据库状态不一致。
            if (e instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("审核处理失败", e);
        }
    }

    // [审核修复 P0] 数据库审核变更置于独立事务；监听器在事务返回后再 ACK。
    @Transactional
    public void processModeration(ModerationTask task) {
        ModerationResult result = aliyunModerationService.checkText(task.getContent());
        String businessStatus = moderationDecider.decideBusinessStatus(result);
        String recordStatus = moderationDecider.decideRecordStatus(result);
        updateBusinessStatus(task.getTargetType(), task.getTargetId(), businessStatus);
        insertModerationRecord(task, result, recordStatus);
        sendResultNotification(task, businessStatus, recordStatus, result.getReason());
    }

    private void updateBusinessStatus(String targetType, Long targetId, String status) {
        long statusVal = Long.parseLong(status);
        if ("article".equals(targetType)) {
            Article article = new Article();
            article.setId(targetId);
            article.setStatus(statusVal);
            if (articleMapper.updateById(article) != 1) throw new IllegalStateException("文章不存在或已删除");
        } else if ("comment".equals(targetType)) {
            Comment comment = new Comment();
            comment.setId(targetId);
            comment.setStatus(statusVal);
            if (commentMapper.updateById(comment) != 1) throw new IllegalStateException("评论不存在或已删除");
        } else {
            throw new IllegalArgumentException("不支持的审核目标类型: " + targetType);
        }
    }

    private void insertModerationRecord(ModerationTask task, ModerationResult result, String recordStatus) {
        ContentModerationRecord record = new ContentModerationRecord();
        record.setTargetType(task.getTargetType());
        record.setTargetId(task.getTargetId());
        record.setRevision(task.getRevision());
        record.setStatus(recordStatus);
        record.setModelScore(result.getConfidence() != null ? result.getConfidence() : BigDecimal.ZERO);
        try {
            record.setModelLabels(result.getLabels() != null ? objectMapper.writeValueAsString(result.getLabels()) : null);
        } catch (Exception e) { throw new IllegalStateException("审核标签序列化失败", e); }
        record.setModelReason(result.getReason());
        record.setContentSnapshot(task.getContentSnapshot());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        contentModerationRecordMapper.insert(record);
    }

    // [审核修复 P0] 任务必须绑定合法目标和版本，旧版本任务不得覆盖当前内容。
    private void validateTask(ModerationTask task) {
        if (!("article".equals(task.getTargetType()) || "comment".equals(task.getTargetType()))
                || task.getTargetId() == null || task.getRevision() == null || task.getRevision() < 1) {
            throw new IllegalArgumentException("审核任务参数非法");
        }
        // [审核修复 P0] 已存在更高版本时，当前任务已过期，不能覆盖新审核结果。
        Integer maxRevision = contentModerationRecordMapper.selectMaxRevision(task.getTargetType(), task.getTargetId());
        if (maxRevision != null && task.getRevision() <= maxRevision) throw new IllegalStateException("审核任务版本已过期");
    }

    private void sendResultNotification(ModerationTask task, String businessStatus, String recordStatus, String reason) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("targetType", task.getTargetType());
            data.put("targetId", task.getTargetId());
            data.put("status", recordStatus.startsWith("auto_approved") ? "approved" :
                    recordStatus.startsWith("auto_rejected") ? "rejected" : "human_review");
            data.put("reason", reason);

            Map<String, Object> message = new HashMap<>();
            message.put("type", "moderation_result");
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());

            String jsonMsg = objectMapper.writeValueAsString(message);
            forumWebSocketHandler.pushModerationResult(task.getUserId(), jsonMsg);
        } catch (Exception e) {
            log.error("发送审核结果通知失败: userId={}", task.getUserId(), e);
            // [审核修复 P1] 通知失败不能静默确认消息，交给事务与 DLQ 处理。
            throw new IllegalStateException("审核结果通知失败", e);
        }
    }
}
