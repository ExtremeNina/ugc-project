package com.example.onlyone.Controller.User;

import com.example.onlyone.VO.ArticleDetailVO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.ArticleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("UserArticleController")
@RequestMapping("/api/articles")
@Slf4j
public class ArticleController {

    @Resource
    private ArticleService articleService;

    // 通用文章详情接口 - 用于搜索页面、主页等
    @GetMapping("/{articleId}")
    public Result getArticleDetail(@PathVariable Long articleId) {
        log.info("articleId:{}", articleId);
        ArticleDetailVO article = articleService.getArticleDetail(articleId);
        return Result.success(article);
    }


}
