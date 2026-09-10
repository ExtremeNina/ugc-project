package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.DTO.CommentDTO;
import com.example.onlyone.DTO.ModerationTask;
import com.example.onlyone.VO.CommentVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Entity.Comment;
import com.example.onlyone.Entity.ContentModerationRecord;
import com.example.onlyone.Entity.ContentStatus;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.CommentMapper;
import com.example.onlyone.Mapper.ContentModerationRecordMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.CommentService;
import com.example.onlyone.Service.LoveService;
import com.example.onlyone.Service.ModerationService;
import com.example.onlyone.Service.ServiceImpl.SensitiveWordEngine;
import com.example.onlyone.Utils.SecurityUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.onlyone.Service.ServiceImpl.LoveServiceImpl.LOVE_TYPE_COMMENT;

/**
 * 评论服务实现
 *
 * 评论树形结构说明：
 * 根评论 (root_id=0, parent_id=null)  ← 直接对文章发表的评论
 *   └── 回复A (root_id=根评论ID, parent_id=根评论ID)  ← 回复根评论
 *         └── 回复a1 (root_id=根评论ID, parent_id=回复A的ID)  ← 回复回复，root_id 始终指向根
 *
 * 前端折叠展示：根评论列表返回 replyCount 和 replyList，子评论全部下发，
 * 由前端控制"展开/收起"的 UI 层级。
 *
 * 优化点：
 * 1. getReplyList 中原有的 N+1 查询（每条回复单独查父评论用户名）已优化为
 *    selectBatchIds 批量查询，将 O(N) 次 DB 查询降为 O(1)。
 * 2. 点赞类型用常量 LOVE_TYPE_COMMENT 替代魔法数字 2L。
 * 3. buildCommentVO 提取为公共方法，消除 getCommentList 和 getReplyList 中的重复代码。
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private LoveService loveService;

    @Resource
    private SensitiveWordEngine sensitiveWordEngine;

    @Resource
    private ModerationService moderationService;

    @Resource
    private ContentModerationRecordMapper contentModerationRecordMapper;

    // [审核修复 P1] 独立事务保存敏感词拦截审计
    @Resource
    private ModerationAuditService moderationAuditService;

    @Override
    public UserProfileVO getUserProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);

        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setUserId(user.getId());
        userProfileVO.setUsername(user.getUsername());
        userProfileVO.setIcon(user.getIcon());

        log.info("评论模块用户基本信息:{}", userProfileVO);
        return userProfileVO;
    }

    /**
     * 发表评论
     *
     * 逻辑分支：
     * - isCommented=0：该文章不允许评论，直接返回 null
     * - parentId 为空：发表根评论，root_id 设为 0
     * - parentId 不为空且父评论 root_id=0：回复的是根评论，root_id 指向父评论的 id
     * - parentId 不为空且父评论 root_id>0：回复的是子评论，root_id 继承父评论的 root_id
     */
    @Override
    public CommentVO publishComment(CommentDTO commentDTO) {
        if (commentDTO.getIsCommented() == 0) {
            return null;
        }

        log.info("评论传输数据：{}", commentDTO);

        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);

        Comment comment = new Comment();
        comment.setUserId(user.getId());
        comment.setIcon(user.getIcon());
        comment.setUsername(user.getUsername());
        comment.setContent(commentDTO.getContent());

        Set<String> hitWords = sensitiveWordEngine.findAllSensitiveWords(commentDTO.getContent());
        if (!hitWords.isEmpty()) {
            comment.setStatus(ContentStatus.REJECTED.value());
            comment.setLove(0L);
            comment.setReplyCount(0L);
            comment.setArticleId(commentDTO.getArticleId());
            comment.setCreateTime(LocalDateTime.now());
            comment.setParentId(commentDTO.getParentId());

            CommentVO commentVO = new CommentVO();
            if (comment.getParentId() != null && comment.getParentId() > 0) {
                Comment parent = commentMapper.selectById(comment.getParentId());
                if (parent == null) {
                    throw new RuntimeException("父评论不存在");
                }
                if (parent.getRootId() == 0) {
                    comment.setRootId(parent.getId());
                } else {
                    comment.setRootId(parent.getRootId());
                }
                commentVO.setReplyName(parent.getUsername());
            } else {
                comment.setRootId(0L);
            }

            commentMapper.insert(comment);

            ContentModerationRecord record = new ContentModerationRecord();
            record.setTargetType("comment");
            record.setTargetId(comment.getId());
            record.setStatus(com.example.onlyone.Entity.ModerationRecordStatus.AUTO_REJECTED.value());
            record.setRevision(1);
            record.setContentSnapshot(comment.getContent());
            try { record.setSensitiveWordsHit(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(hitWords)); }
            catch (Exception ex) { throw new IllegalStateException("敏感词序列化失败", ex); }
            record.setModelReason("命中敏感词: " + hitWords);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            moderationAuditService.saveRejectedRecord(record);

            log.warn("用户 {} 评论命中敏感词，已拦截: {}", user.getUsername(), hitWords);
            throw new RuntimeException("内容包含违规词，评论失败");
        }

        comment.setStatus(ContentStatus.PENDING.value());
        comment.setLove(0L);
        comment.setReplyCount(0L);
        comment.setArticleId(commentDTO.getArticleId());
        comment.setCreateTime(LocalDateTime.now());
        comment.setParentId(commentDTO.getParentId());

        CommentVO commentVO = new CommentVO();

        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent == null) {
                throw new RuntimeException("父评论不存在");
            }
            // 父评论的 root_id=0 说明父评论本身就是根评论，将 root_id 指向它
            // 否则继承父评论的 root_id，保证同一条评论线程下的所有回复 root_id 一致
            if (parent.getRootId() == 0) {
                comment.setRootId(parent.getId());
            } else {
                comment.setRootId(parent.getRootId());
            }
            commentVO.setReplyName(parent.getUsername());
        } else {
            // 根评论：root_id 和 parent_id 均为 0/null
            comment.setRootId(0L);
        }

        commentMapper.insert(comment);

        // 更新直接父评论的回复数
        if (comment.getParentId() != null) {
            commentMapper.updateReplyCount(comment.getParentId(), 1);
        }

        ModerationTask task = new ModerationTask();
        task.setTargetType("comment");
        task.setTargetId(comment.getId());
        task.setContent(comment.getContent());
        task.setUserId(user.getId());
        // [审核修复 P0] 任务绑定内容版本和快照。
        task.setRevision(1);
        task.setContentSnapshot(comment.getContent());
        moderationService.submitTask(task);

        commentVO.setCreateTime(comment.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        BeanUtils.copyProperties(comment, commentVO);
        log.info("用户：{}评论了：{}", user.getUsername(), commentVO);

        return commentVO;
    }

    /**
     * 获取文章的根评论列表（分页，每页 10 条）
     *
     * 每条根评论包含：
     * - 自身信息 + 点赞状态/数量
     * - replyList：该根评论下的所有子回复（前端折叠展示用）
     * - replyCount：子回复总数（由 replyList.size() 计算）
     */
    @Override
    public PageInfo<CommentVO> getCommentList(Long articleId) {
        PageHelper.startPage(1, 10);

        List<Comment> commentList = commentMapper.selectAllComment(articleId);
        if (commentList == null || commentList.isEmpty()) {
            return null;
        }

        List<CommentVO> commentVOList = new ArrayList<>();
        for (Comment comment : commentList) {
            CommentVO commentVO = buildCommentVO(comment);
            BeanUtils.copyProperties(comment, commentVO);

            List<CommentVO> replyList = getReplyList(comment.getId());
            commentVO.setReplyList(replyList);
            commentVO.setReplyCount(replyList != null ? (long) replyList.size() : 0L);
            commentVOList.add(commentVO);
        }

        return new PageInfo<>(commentVOList);
    }

    /**
     * 获取根评论的所有子回复
     *
     * 优化说明：
     * 原实现对每条回复调用 selectById 查询父评论用户名（N+1 查询），现已改为：
     * 1. 先收集所有 parentId（去重）
     * 2. 使用 MyBatis Plus 的 selectBatchIds 一次性批量查询所有父评论
     * 3. 构建 id → username 映射，循环中直接取用
     *
     * @param rootCommentId 根评论的 ID（非 root_id 字段值），对应 SQL: WHERE root_id = #{rootCommentId}
     */
    private List<CommentVO> getReplyList(Long rootCommentId) {
        if (rootCommentId == null) {
            return Collections.emptyList();
        }

        List<Comment> commentList = commentMapper.selectAllReply(rootCommentId);
        if (commentList == null || commentList.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询父评论用户名，消除 N+1 查询
        Map<Long, String> parentUsernameMap = buildParentUsernameMap(commentList);

        List<CommentVO> commentVOList = new ArrayList<>();
        for (Comment comment : commentList) {
            CommentVO commentVO = buildCommentVO(comment);
            // 从批量查询的 Map 中获取父评论用户名，避免逐条查 DB
            commentVO.setReplyName(parentUsernameMap.getOrDefault(comment.getParentId(), ""));
            BeanUtils.copyProperties(comment, commentVO);
            commentVOList.add(commentVO);
        }
        return commentVOList;
    }

    /**
     * 构建 CommentVO 公共方法
     * 封装点赞状态和点赞数的查询，消除 getCommentList 和 getReplyList 中的重复代码
     */
    private CommentVO buildCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setCreateTime(comment.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setIsLove(loveService.isEntityLovedByUser(comment.getId(), LOVE_TYPE_COMMENT));
        Long loveCount = Long.valueOf(loveService.getEntityLoveCount(comment.getId(), LOVE_TYPE_COMMENT));
        vo.setLove(loveCount);
        return vo;
    }

    /**
     * 批量构建 parentId → username 映射
     *
     * 收集当前回复列表中所有 parentId → 去重 → selectBatchIds 一次查询 → 构建 Map
     * 将原有的 O(N) 次 selectById 降为 O(1) 次 selectBatchIds
     */
    private Map<Long, String> buildParentUsernameMap(List<Comment> commentList) {
        List<Long> parentIds = commentList.stream()
                .map(Comment::getParentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (parentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return commentMapper.selectBatchIds(parentIds).stream()
                .collect(Collectors.toMap(Comment::getId, Comment::getUsername, (a, b) -> a));
    }
}
