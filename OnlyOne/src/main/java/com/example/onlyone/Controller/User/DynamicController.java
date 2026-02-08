package com.example.onlyone.Controller.User;


import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.DynamicService;
import com.example.onlyone.VO.DyArticleVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/dynamic")
@Slf4j
public class DynamicController {

    @Resource
    private DynamicService dynamicService;

    //获取最新文章
    @GetMapping("/newArticles")
    public Result newArticles() {
        List<DyArticleVO> dyArticleVOList = dynamicService.getNewArticles();
        return Result.success(dyArticleVOList);

    }
}
