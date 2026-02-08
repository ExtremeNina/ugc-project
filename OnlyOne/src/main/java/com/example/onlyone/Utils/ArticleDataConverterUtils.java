package com.example.onlyone.Utils;


import com.example.onlyone.Mapper.CommentMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.VO.RecommendVO;
import com.example.onlyone.VO.ReviewVO;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Service.LoveService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


//数据转换工具类
@Component
@Slf4j
public class ArticleDataConverterUtils {


    // 定义文章点赞类型ID
    private static final Long ARTICLE_LOVE_TYPE = 1L;
    // 假设文章对应的点赞类型ID为1（根据你的实际情况调整）
    private static final Long ARTICLE_LOVE_TYPE_ID = 1L;

    @Resource
    private LoveService loveService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CommentMapper commentMapper;

    /**
     * 将Article转换为ArticleVO，并根据状态设置额外字段
     */
    public ArticleVO convertToArticleVO(Article article) {
        ArticleVO articleVO = new ArticleVO();

        // 复制基本属性
        BeanUtils.copyProperties(article, articleVO);

        // 格式化日期
        formatDateTimeFields(article, articleVO);

        // 设置点赞数和浏览量
        setLoveAndViewCount(articleVO, article);

        return articleVO;
    }

    private RecommendVO convertToRecommendVO(Article article) {
        RecommendVO recommendVO = new RecommendVO();

        Long authorId = article.getAuthorId();
        User author = userMapper.selectById(authorId);

        recommendVO.setAuthor(author.getUsername());
        recommendVO.setIcon(author.getIcon());
        String displayTime = formatDisplayTime(article);
        recommendVO.setPublishTime(displayTime);
        recommendVO.setLoveCount(article.getLove());
        recommendVO.setViewCount(article.getPageview());

        Boolean isLoved = loveService.isEntityLovedByUser(article.getId(), ARTICLE_LOVE_TYPE_ID);
        recommendVO.setIsLoved(isLoved);

        Long comments = commentMapper.countByArticleId(article.getId());
        recommendVO.setComments(comments);

        BeanUtils.copyProperties(article, recommendVO);
        return recommendVO;

    }

    /**
     * 将Article转换为ReviewVO，并根据状态设置额外字段
     */
    public ReviewVO convertToReviewVO(Article article, User user) {
        ReviewVO reviewVO = new ReviewVO();

        // 复制基本属性
        BeanUtils.copyProperties(article, reviewVO);

        // 设置作者信息
        if (user != null) {
            reviewVO.setAuthor(user.getUsername());
        }

        // 格式化日期
        formatDateTimeFields(article, reviewVO);

        // 设置点赞数和浏览量
        setLoveAndViewCount(reviewVO, article);

        return reviewVO;
    }

    /**
     * 将Article列表转换为ArticleVO列表
     */
    public List<ArticleVO> convertToArticleVOList(List<Article> articleList) {
        if (articleList == null || articleList.isEmpty()) {
            return Collections.emptyList();
        }

        return articleList.stream()
                .map(this::convertToArticleVO)
                .collect(Collectors.toList());
    }

    /**
     * 将Article列表转换为RecommendVO列表
     */
    public List<RecommendVO> convertToReCommendVOList(List<Article> articleList) {
        if (articleList == null || articleList.isEmpty()) {
            return Collections.emptyList();
        }

        return articleList.stream()
                .map(this::convertToRecommendVO)
                .collect(Collectors.toList());
    }


    /**
     * 格式化日期字段
     */
    private void formatDateTimeFields(Article article, Object vo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            if (article.getCreateTime() != null) {
                String createTimeStr = article.getCreateTime().format(formatter);
                if (vo instanceof ArticleVO) {
                    ((ArticleVO) vo).setCreateTime(createTimeStr);
                } else if (vo instanceof ReviewVO) {
                    ((ReviewVO) vo).setCreateTime(createTimeStr);
                }
            }
            if (article.getUpdateTime() != null && vo instanceof ArticleVO) {
                String updateTimeStr = article.getUpdateTime().format(formatter);
                ((ArticleVO) vo).setUpdateTime(updateTimeStr);
            }
        } catch (Exception e) {
            log.error("格式化日期字段失败", e);
        }
    }

    /**
     * 设置点赞数和浏览量
     */
    private void setLoveAndViewCount(Object vo, Article article) {
        Long loveCount = 0L;
        Long viewCount = 0L;

        if (article.getStatus() == 3) { // 已发布的文章
            loveCount = Long.valueOf(loveService.getEntityLoveCount(article.getId(), ARTICLE_LOVE_TYPE));
            viewCount = (long) article.getPageview();
        }

        if (vo instanceof ArticleVO) {
            ((ArticleVO) vo).setLoveCount(loveCount);
            ((ArticleVO) vo).setViewCount(viewCount);
        } else if (vo instanceof ReviewVO) {
            ((ReviewVO) vo).setLoveCount(loveCount);
            ((ReviewVO) vo).setViewCount(viewCount);
        }
    }

    /**
     * 格式化显示时间：优先使用更新时间，如果为null则使用创建时间
     */
    private String formatDisplayTime(Article article) {
        LocalDateTime timeToDisplay = article.getUpdateTime() != null ?
                article.getUpdateTime() :
                article.getCreateTime();

        if (timeToDisplay != null) {
            return timeToDisplay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        return "未知时间";
    }

}
