package com.example.onlyone.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.onlyone.Entity.User;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.VO.RecommendVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface CommunityService extends IService<User> {


    Map<String,Object> getIconAndId();

    boolean isLogin(HttpServletRequest request);

    List<RecommendVO> getReArticles();

    Long getUnreadCount(Long userId);
}
