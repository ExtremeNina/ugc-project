package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.DTO.ArticleDTO;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.VO.ReviewVO;
import com.example.onlyone.Entity.*;
import com.example.onlyone.Mapper.ArticleAndLabelMapper;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.LabelMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.CreateCenterService;
import com.example.onlyone.Utils.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreateCenterServiceImpl extends ServiceImpl<ArticleMapper, Article> implements CreateCenterService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private HtmlSanitizer htmlSanitizer;
    @Resource
    private ArticleAndLabelMapper articleAndLabelMapper;
    @Resource
    private MinioUtils minioUtils;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private LabelMapper labelMapper;
    @Resource
    private ArticleDataConverterUtils articleDataConverterUtils;

    // 定义文章点赞类型ID（根据您的实际情况调整）
    //private static final Long ARTICLE_LOVE_TYPE = 1L; // 假设文章点赞类型ID为1


    // 发表文章
    @Override
    @Transactional
    public void publishArticle(@RequestBody ArticleDTO articleDTO) {
        SecurityUtils.validateAuthentication();
        ValidationUtils.validateArticleTitle(articleDTO.getTitle());

        try {
            Article article = createOrUpdateArticle(articleDTO, 2L); // 状态2: 待审核
            saveArticleLabels(article.getId(), articleDTO.getLabel());

            String username = SecurityUtils.getCurrentUsername();
            log.info("用户 {} 成功发布文章: {}", username, articleDTO.getTitle());
        } catch (Exception e) {
            log.error("发布文章失败: {}", e.getMessage());
            throw new RuntimeException("发布文章失败，请稍后重试");
        }
    }

    // 保存草稿
    @Override
    @Transactional
    public void draftArticle(ArticleDTO articleDTO) {
        SecurityUtils.validateAuthentication();
        ValidationUtils.validateArticleTitle(articleDTO.getTitle());

        try {
            Article article = createNewArticle(articleDTO, 1L); // 状态1: 草稿
            saveArticleLabels(article.getId(), articleDTO.getLabel());

            String username = SecurityUtils.getCurrentUsername();
            log.info("用户 {} 成功保存草稿: {}", username, articleDTO.getTitle());
        } catch (Exception e) {
            log.error("保存草稿失败: {}", e.getMessage());
            throw new RuntimeException("保存草稿失败，请稍后重试");
        }
    }

    // 查询草稿文章
    @Override
    public List<ArticleVO> selectDraft() {
        Long userId = SecurityUtils.getCurrentUserId();
        return selectKindOfArticle(userId, 1);
    }


    //编辑文章的回显
    @Override
    public Article modifyDraft(Long id) {
        SecurityUtils.validateAuthentication();
        log.info("编辑草稿回显，文章ID: {}", id);

        // 查询草稿文章详情
        Article article = articleMapper.selectDraftByUserId(id);
        if (article == null) {
            throw new RuntimeException("草稿文章不存在或已被删除");
        }

        // 验证当前用户是否有权限编辑此草稿
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new SecurityException("无权编辑此草稿");
        }

        return article;
    }


    //删除草稿
    @Override
    @Transactional
    public void deleteDraft(Long draftId) {
        SecurityUtils.validateAuthentication();
        log.info("删除草稿文章，文章ID: {}", draftId);

        // 验证文章是否存在且属于当前用户
        Article article = articleMapper.selectById(draftId);
        if (article == null) {
            throw new RuntimeException("草稿文章不存在");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new SecurityException("无权删除此草稿");
        }

        // 验证文章状态为草稿
        if (article.getStatus() != 1) {
            throw new RuntimeException("只能删除草稿状态的文章");
        }

        deleteArticleAndLabels(draftId);
        log.info("草稿文章删除成功，文章ID: {}", draftId);
    }



    // 查询已发布的文章
    @Override
    public List<ArticleVO> queryPublish() {
        Long userId = SecurityUtils.getCurrentUserId();
        return selectKindOfArticle(userId, 3);
    }

    // 查询待审核文章
    @Override
    public List<ArticleVO> queryReview() {
        Long userId = SecurityUtils.getCurrentUserId();
        return selectKindOfArticle(userId, 2);
    }


    //删除已发布的文章
    @Override
    @Transactional
    public void deletePublishArticle(Long articleId) {
        SecurityUtils.validateAuthentication();
        log.info("删除已发布文章，文章ID: {}", articleId);

        // 验证文章是否存在且属于当前用户
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new SecurityException("无权删除此文章");
        }

        // 验证文章状态为已发布
        if (article.getStatus() != 3) {
            throw new RuntimeException("只能删除已发布状态的文章");
        }

        deleteArticleAndLabels(articleId);
        log.info("已发布文章删除成功，文章ID: {}", articleId);
    }




    // 查询一篇待审核的文章
    @Override
    public Map<ReviewVO, List<String>> queryOneReviewArticle(Long articleId) {
        Article article = articleMapper.selectByArticleId(articleId);
        User user = userMapper.selectById(article.getAuthorId());

        ReviewVO reviewVO = articleDataConverterUtils.convertToReviewVO(article, user);
        List<String> labelList = selectArticleOfLabel(articleId);

        log.info("查询文章详情 - 作者: {}, 创建时间: {}, 点赞数: {}, 浏览数: {}, 标签: {}",
                reviewVO.getAuthor(), reviewVO.getCreateTime(),
                reviewVO.getLoveCount(), reviewVO.getViewCount(), labelList);

        return Map.of(reviewVO, labelList);
    }

    // 根据标题查询文章
    @Override
    public List<ArticleVO> selectByTitle(String title, int status) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (title == null || title.trim().isEmpty()) {
            return selectKindOfArticle(userId, status);
        }

        List<Article> articleList = articleMapper.selectByTitle(userId, title, status);
        return articleDataConverterUtils.convertToArticleVOList(articleList);
    }

    // 根据日期搜索文章
    @Override
    public List<ArticleVO> selectByDate(String date, int status) {
        Long userId = SecurityUtils.getCurrentUserId();
        String searchDate = ValidationUtils.validateAndProcessDate(date);

        List<Article> articleList = articleMapper.selectBySmartDate(userId, searchDate, status);
        return articleDataConverterUtils.convertToArticleVOList(articleList);
    }

    // 上传图片到minio
    @Override
    public Map<String, String> uploadImage(MultipartFile file) {
        try {
            String bucketName = "update-images";
            String imageUrl = uploadCoverImage(file, bucketName);

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("message", "图片上传成功");
            log.info("图片上传成功，URL: {}", imageUrl);
            return result;
        } catch (Exception e) {
            log.error("图片上传失败: {}", e.getMessage());
            return Map.of("error", "图片上传失败");
        }
    }

    // ============ 私有方法 ============


    /**
     * 创建或更新文章
     *
     * @param articleDTO 文章数据传输对象
     * @param status 文章状态 (1:草稿, 2:待审核, 3:已发布)
     * @return 创建或更新后的文章实体
     */
    private Article createOrUpdateArticle(ArticleDTO articleDTO, Long status) {
        boolean isUpdate = articleDTO.getId() != null;
        Article article;

        if (isUpdate) {
            article = articleMapper.selectById(articleDTO.getId());
            log.info("编辑旧文章");
        } else {
            article = new Article();
            article.setPageview(0L);
            article.setCreateTime(LocalDateTime.now());
            log.info("创建新文章");
        }

        BeanUtils.copyProperties(articleDTO, article);

        String username = SecurityUtils.getCurrentUsername();
        User user = userMapper.selectByUsername(username);

        article.setAuthorId(user.getId());
        article.setStatus(status);
        article.setUpdateTime(LocalDateTime.now());
        article.setCoverImageUrl(articleDTO.getCoverImageUrl());
        article.setLove(0L);
        article.setIsCommented(articleDTO.getIsCommented());

        if (isUpdate) {
            articleMapper.updateById(article);
            articleAndLabelMapper.deleteById(articleDTO.getId());
        } else {
            articleMapper.insert(article);
        }

        return article;
    }



    /**
     * 创建新文章（仅用于草稿保存）
     *
     * @param articleDTO 文章数据传输对象
     * @param status 文章状态 (1:草稿)
     * @return 新创建的文章实体
     */
    private Article createNewArticle(ArticleDTO articleDTO, Long status) {

        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);

        String username = SecurityUtils.getCurrentUsername();
        User user = userMapper.selectByUsername(username);

        article.setAuthorId(user.getId());
        article.setStatus(status);
        article.setCreateTime(LocalDateTime.now());
        article.setCoverImageUrl(articleDTO.getCoverImageUrl());

        articleMapper.insert(article);
        return article;
    }


    /**
     * 保存文章标签关联关系
     *
     * @param articleId 文章ID
     * @param labelList 标签ID列表
     */
    private void saveArticleLabels(Long articleId, List<Long> labelList) {
        if (labelList == null || labelList.isEmpty()) {
            return;
        }

        for (Long labelId : labelList) {
            ArticleAndLabel articleAndLabel = new ArticleAndLabel();
            articleAndLabel.setArticleId(articleId);
            articleAndLabel.setLabelId(labelId);
            articleAndLabel.setCreateTime(LocalDateTime.now());
            articleAndLabelMapper.insert(articleAndLabel);
        }
    }


    /**
     * 删除文章及其标签关联关系
     *
     * @param articleId 要删除的文章ID
     */
    private void deleteArticleAndLabels(Long articleId) {
        articleMapper.deleteById(articleId);
        articleAndLabelMapper.deleteById(articleId);
    }


    /**
     * 查询指定用户和状态的文章列表
     *
     * @param userId 用户ID
     * @param status 文章状态 (1:草稿, 2:待审核, 3:已发布)
     * @return 文章VO列表
     */
    private List<ArticleVO> selectKindOfArticle(Long userId, int status) {
        List<Article> articleList = articleMapper.selectKindOfArticle(userId, status);
        return articleDataConverterUtils.convertToArticleVOList(articleList);
    }


    /**
     * 查询文章对应的标签名称列表
     *
     * @param articleId 文章ID
     * @return 标签名称列表
     */
    private List<String> selectArticleOfLabel(Long articleId) {
        List<ArticleAndLabel> list = articleAndLabelMapper.selectAll(articleId);
        List<Long> labelIdList = list.stream()
                .map(ArticleAndLabel::getLabelId)
                .collect(Collectors.toList());

        if (labelIdList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Label> labelList = labelMapper.selectLabelList(labelIdList);
        return labelList.stream()
                .map(Label::getName)
                .collect(Collectors.toList());
    }


    /**
     * 上传封面图片到Minio对象存储
     *
     * @param coverImage 封面图片文件
     * @param bucketName Minio存储桶名称
     * @return 图片访问URL
     */
    private String uploadCoverImage(MultipartFile coverImage, String bucketName) {
        if (coverImage == null || coverImage.isEmpty()) {
            log.info("不上传图片，则默认无封面");
            return null;
        }

        try {
            ValidationUtils.validateImage(coverImage);
            Map<String, String> resultMap = minioUtils.uploadFile(coverImage, bucketName);

            if (resultMap == null || resultMap.get("url") == null) {
                throw new RuntimeException("封面图片上传失败");
            }

            log.info("图片上传成功");
            return resultMap.get("url");
        } catch (Exception e) {
            log.error("封面图片上传失败: {}", e.getMessage());
            throw new RuntimeException("封面图片上传失败: " + e.getMessage());
        }
    }

}
