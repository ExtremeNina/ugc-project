package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.CategoryMapper;
import com.example.onlyone.Mapper.PrivateMessageMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.CommunityService;
import com.example.onlyone.Utils.ArticleDataConverterUtils;
import com.example.onlyone.VO.ArticleVO;
import com.example.onlyone.VO.RecommendVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CommunityServiceImpl extends ServiceImpl<UserMapper,User> implements CommunityService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ArticleDataConverterUtils articleDataConverterUtils;
    @Resource
    private PrivateMessageMapper privateMessageMapper;



    @Override
    public Map<String,Object> getIconAndId() {

        //获取用户名
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        //获取用户头像路径
        String icon = user.getIcon();
        Long id = user.getId();
        Map<String,Object> map = new HashMap<>();
        map.put("id",id);
        map.put("icon",icon);
        return map;
    }

    @Override
    public boolean isLogin(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);
        if ( jwt == null || StringUtils.isEmpty(jwt) ) {
            return false;
        }
        return true;
    }


    //按照时间发布倒序获取全部文章
    @Override
    public List<RecommendVO> getReArticles() {
        List<Article> articleList = articleMapper.getAllArticlesByTime();
        List<RecommendVO> recommendVOList = articleDataConverterUtils.convertToReCommendVOList(articleList);
        log.info("查找到的文章队列:{}",recommendVOList);
        return recommendVOList;
    }


    //获取该用户未读消息数
    @Override
    public Long getUnreadCount(Long userId) {
        Long count = privateMessageMapper.selectUnreadCount(userId);
        if (count == null) {
            return null;
        }
        return count;
    }


    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
