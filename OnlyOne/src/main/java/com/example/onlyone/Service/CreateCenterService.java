package com.example.onlyone.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.DTO.ArticleDTO;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.VO.ReviewVO;
import com.example.onlyone.Entity.Article;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CreateCenterService extends IService<Article> {

    void publishArticle(ArticleDTO articleDTO);

    Map<String,String> uploadImage(MultipartFile file);

    void draftArticle(ArticleDTO articleDTO);

    List<ArticleVO> selectDraft();

    Article modifyDraft(Long draftId);

    void deleteDraft(Long draftId);

    List<ArticleVO> queryPublish();

    List<ArticleVO> queryReview();

    void deletePublishArticle(Long articleId);

    Map<ReviewVO,List<String>> queryOneReviewArticle(Long articleId);

    List<ArticleVO> selectByTitle(String title,int status);

    List<ArticleVO> selectByDate(String date, int status);
}
