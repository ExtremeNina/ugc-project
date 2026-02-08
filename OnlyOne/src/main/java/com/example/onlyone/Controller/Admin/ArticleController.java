package com.example.onlyone.Controller.Admin;

import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.ArticleService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController("AdminArticleController")
@RequestMapping("/api/admin/articles")
@Slf4j
public class ArticleController {

    @Resource
    private ArticleService articleService;

    //查看待审核和已发布文章
    @GetMapping("/review-and-publish")
    public Result getAllArticle() {
        PageInfo<ArticleVO> articleVOList = articleService.getAllArticle();
        if (articleVOList == null || articleVOList.getList() == null) {
            return Result.success("查找不到相关的文章");
        }
        return Result.success(articleVOList);

    }

    @PutMapping("{articleId}")
    public Result updateArticleStatus(@PathVariable  Long articleId, @RequestParam Integer status) {
        articleService.updateArticleStatus(articleId,status);
        return Result.success();
    }




}
