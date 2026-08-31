package com.example.onlyone.Controller.User;


import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.DynamicService;
import com.example.onlyone.VO.DyArticleVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dynamic")
@Slf4j
public class DynamicController {

    @Resource
    private DynamicService dynamicService;

    /**
     * 获取关注动态 Feed（分页）
     *
     * @param page 页码，从 1 开始，默认第 1 页（老前端不传参数时行为向后兼容）
     * @param size 每页条数，默认 20
     */
    @GetMapping("/newArticles")
    public Result newArticles(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size) {
        List<DyArticleVO> dyArticleVOList = dynamicService.getNewArticles(page, size);
        return Result.success(dyArticleVOList);
    }
}
