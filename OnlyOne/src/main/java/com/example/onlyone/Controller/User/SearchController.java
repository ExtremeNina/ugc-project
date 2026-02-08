package com.example.onlyone.Controller.User;

import com.example.onlyone.VO.SearchArticleVO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.SearchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Resource
    private SearchService searchService;

    //首页搜索文章
    @GetMapping("/articles")
    public Result search(String keyword) {
        List<SearchArticleVO> list = searchService.searchArticle(keyword);
        return Result.success(list);
    }


}
