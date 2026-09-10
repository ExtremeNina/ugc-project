package com.example.onlyone.Controller.User;


import com.example.onlyone.Common.Result;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.RecommendService;
import com.example.onlyone.VO.RmArticleVO;
import com.example.onlyone.Utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/Recommend")
@Slf4j
public class RecommendController {

    @Resource
    private RecommendService recommendService;
    @Resource
    private UserMapper userMapper;



    @GetMapping("/feed")
    public Result<Map<String, Object>> recommend(@RequestParam(required = false) Long userId,
                                                @RequestParam(defaultValue = "0") int offset,
                                               @RequestParam(defaultValue = "20") int limit) {
        Long authenticatedUserId = null;
        try { authenticatedUserId = SecurityUtils.getCurrentUserId(); } catch (Exception ignored) { }
        boolean loggedIn = authenticatedUserId != null;
        List<Article> articles;
        if (loggedIn) {
            articles = recommendService.getFeedList(authenticatedUserId, offset, limit);
        } else {
            articles = recommendService.getHotFeed(offset, limit); // 未登录只走热门
        }

        // 转换为前端 VO 并填充作者信息
        List<RmArticleVO> voList = articles.stream().map(article -> {
            RmArticleVO vo = new RmArticleVO();
            vo.setArticleId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setCoverImageUrl(article.getCoverImageUrl());
            vo.setLoveCount(article.getLove());
            // 查询作者（可优化为批量查询）
            User author = userMapper.selectById(article.getAuthorId());
            vo.setAuthor(author != null ? author.getUsername() : "未知");
            return vo;
        }).toList();

        boolean hasMore = voList.size() == limit;

        Map<String, Object> data = Map.of(
                "hasMore", hasMore,
                "notes", voList
        );
        return Result.success(data);
    }

}
