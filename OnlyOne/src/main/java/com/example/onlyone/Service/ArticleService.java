package com.example.onlyone.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.VO.ArticleDetailVO;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.Entity.Article;
import com.github.pagehelper.PageInfo;

public interface ArticleService extends IService<Article> {


    ArticleDetailVO getArticleDetail(Long articleId);

    PageInfo<ArticleVO> getAllArticle();

    void updateArticleStatus(Long articleId, Integer status);
}
