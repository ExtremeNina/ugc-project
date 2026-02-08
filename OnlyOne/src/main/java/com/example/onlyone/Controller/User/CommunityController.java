package com.example.onlyone.Controller.User;

import com.example.onlyone.Common.Result;
import com.example.onlyone.Service.CommunityService;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.VO.RecommendVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Resource
    private CommunityService communityService;

    //登录后首页展示头像
    //@GetMapping
    @GetMapping("/user/avatar")
    public Result getIcon() {
        Map<String,Object> urlAndId = communityService.getIconAndId();
        log.info("用户的头像:"+urlAndId.get("icon"));
        return Result.success(urlAndId);
    }

    //首页判断是否登录
    @GetMapping("/isLogin")
    public Result IsLogin(HttpServletRequest request) {
        boolean isLogin = communityService.isLogin(request);
        return Result.success(isLogin);
    }

    //首页推荐文章模块
    @GetMapping("/Recommend")
    public Result getReArticles() {
        List<RecommendVO> list = communityService.getReArticles();
        return Result.success(list);
    }


    @GetMapping("/unreadMessage")
    public Result getUnreadMessage(Long userId) {
        Long unreadCount = communityService.getUnreadCount(userId);
        if (unreadCount == null) {
            return Result.success(null);
        }
        log.info("当前用户：{}的未读消息数为：{}",userId,unreadCount);
        return Result.success(unreadCount);
    }







}
