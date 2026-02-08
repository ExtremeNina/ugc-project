package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.DTO.CommentDTO;
import com.example.onlyone.VO.CommentVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Entity.Comment;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.CommentMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.CommentService;
import com.example.onlyone.Service.LoveService;
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
import java.util.List;

@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private LoveService loveService;

    //获取用户基本信息
    @Override
    public UserProfileVO getUserProfile() {

        //封装id，用户名和头像
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);

        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setUserId(user.getId());
        userProfileVO.setUsername(user.getUsername());
        userProfileVO.setIcon(user.getIcon());

        log.info("评论模块用户基本信息:{}", userProfileVO);

        return userProfileVO;
    }

    @Override
    public CommentVO publishComment(CommentDTO commentDTO) {

        if (commentDTO.getIsCommented() == 0) {
            return null;
        }

        //封装基本信息
        log.info("评论传输数据：{}", commentDTO);

        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);

        Comment comment = new Comment();
        comment.setUserId(user.getId());
        comment.setIcon(user.getIcon());
        comment.setUsername(user.getUsername());
        comment.setContent(commentDTO.getContent());
        comment.setStatus(3L);
        comment.setLove(0L);
        comment.setReplyCount(0L);
        comment.setArticleId(commentDTO.getArticleId());
        comment.setCreateTime(LocalDateTime.now());

        comment.setParentId(commentDTO.getParentId());

        CommentVO commentVO = new CommentVO();

        //判断是否有父评论
       if (comment.getParentId() != null && comment.getParentId() > 0) {
           //查询父评论是否还存在，此时的parentId就为父评论的id
           Comment parent = commentMapper.selectById(comment.getParentId());
           if (parent == null) {
               throw new RuntimeException("父评论不存在");
           }
           //如果父评论的rootID为null，则父评论则为根评论
           if (parent.getRootId() == 0) {
                comment.setRootId(parent.getId());

           }else {
               //设置根评论为父评论的根评论
               comment.setRootId(parent.getRootId());
           }
           //设置回复的用户名
           commentVO.setReplyName(parent.getUsername());


       }else {
           comment.setRootId(0L);
       }

       //插入评论表
        commentMapper.insert(comment);

       //如果是回复评论
       if (comment.getParentId() != null) {
           //回复数+1
           commentMapper.updateReplyCount(comment.getParentId(),1);
       }

        commentVO.setCreateTime(comment.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        BeanUtils.copyProperties(comment, commentVO);
        log.info("用户：{}评论了：{}",user.getUsername(),commentVO);

        return commentVO;
    }

    //获取评论列表
    @Override
    public PageInfo<CommentVO> getCommentList(Long articleId) {

        //使用分页插件
        PageHelper.startPage(1, 10);

        List<Comment> commentList = commentMapper.selectAllComment(articleId);
        if (commentList == null || commentList.isEmpty()) {
            return null;
        }

        List<CommentVO> commentVOList = new ArrayList<>();
        for (Comment comment : commentList) {

            CommentVO commentVO = new CommentVO();
            commentVO.setCreateTime(comment.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            //判断是否点过赞
            Boolean isLove = loveService.isEntityLovedByUser(comment.getId(),2L);
            commentVO.setIsLove(isLove);

            //获取点赞数
            Long loveCount = Long.valueOf(loveService.getEntityLoveCount(comment.getId(),2L));
            commentVO.setLove(loveCount);
            log.info("当前评论的点赞数为：{}",loveCount);

            //获取当前评论的全部回复
            Long rootId = comment.getRootId();
            List<CommentVO> replyList = getReplyList(comment.getId());
            commentVO.setReplyList(replyList);
            if (replyList != null) {
                commentVO.setReplyCount((long) replyList.size());
            }else {
                commentVO.setReplyCount(0L);
            }


            BeanUtils.copyProperties(comment, commentVO);
            commentVOList.add(commentVO);
            log.info("当前根评论：{}的回复{}",rootId,commentVO.getReplyList());
        }

        //log.info("当前文章的评论列表：{}", commentVOList);

        return new PageInfo<>(commentVOList);
    }

    //获取回复列表
    private List<CommentVO> getReplyList(Long rootId) {

        if (rootId == null) {
            log.info("当前评论不存在");
            return null;
        }
        List<Comment> commentList = commentMapper.selectAllReply(rootId);
        List<CommentVO> commentVOList = new ArrayList<>();
        for (Comment comment : commentList) {
            CommentVO commentVO = new CommentVO();

            //获取父评论用户名
            Long parentId = comment.getParentId();
            if (parentId == null) {
                throw new RuntimeException("回复的评论不存在");
            }
            Comment parent = commentMapper.selectById(parentId);
            commentVO.setReplyName(parent.getUsername());

            //判断是否点过赞
            Boolean isLove = loveService.isEntityLovedByUser(comment.getId(),2L);
            commentVO.setIsLove(isLove);

            //封装点赞数
            Long loveCount = Long.valueOf(loveService.getEntityLoveCount(comment.getId(),2L));
            commentVO.setLove(loveCount);
            log.info("当前评论的点赞数为：{}",loveCount);

            commentVO.setCreateTime(comment.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            BeanUtils.copyProperties(comment, commentVO);
            commentVOList.add(commentVO);
        }
        return commentVOList;
    }


}
