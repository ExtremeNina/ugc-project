package com.example.onlyone.Service.ServiceImpl;

import com.example.onlyone.VO.SearchArticleVO;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.Category;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.CategoryMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.LoveService;
import com.example.onlyone.Service.SearchService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private LoveService loveService;


    @Override
    public List<SearchArticleVO> searchArticle(String keyword) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userMapper.selectByUsername(username);

        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        //查询已发布的文章
        List<Article> articleList = articleMapper.searchArticle(keyword,3);
        List<SearchArticleVO> searchList = new ArrayList<>();
        //封装对应信息
        for (Article article : articleList) {
            SearchArticleVO vo = new SearchArticleVO();
            //封装用户头像
            //封装发布时间
            if (article.getUpdateTime() == null) {
                String dateTime = article.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                vo.setPublishTime(dateTime);
            }else {
                vo.setPublishTime(article.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            //查询文章作者信息
            Long authorId = article.getAuthorId();
            User author = userMapper.selectById(authorId);
            vo.setAuthor(author.getUsername());
            vo.setUserIcon(author.getIcon());
            //封装分类信息
            Long categoryId = article.getCategoryId();
            Category category = categoryMapper.selectById(categoryId);
            if (category != null) {
                vo.setCategory(category.getCategoryName());
            }


            String loveCount = loveService.getEntityLoveCount(article.getId(),1L);
            vo.setLoveCount(loveCount);

            //获取点赞显示状态
            Boolean isLove = false;

            //判断当前用户是否在点赞过该文章
            Boolean result = loveService.isEntityLovedByUser(article.getId(),1L);
            if (result) {
                isLove = true;
            }

            vo.setIsLove(isLove);

            //封装浏览量信息
            vo.setViewCount(article.getPageview());

            BeanUtils.copyProperties(article, vo);
            log.info("文章id:"+vo.getId());
            log.info("文章的点赞数量:{}", loveCount);
            log.info("文章的点赞状态:{}",vo.getIsLove());
            log.info("文章的浏览量:{}",vo.getViewCount());
            searchList.add(vo);
        }

        return searchList;
    }


    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
