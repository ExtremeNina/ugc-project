package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ArticleMapper extends BaseMapper<Article> {

    //@Select("select * from article where author_id = #{userId} and status = #{status} order by create_time desc")
    //List<Article> selectAllDraft(Long userId, int status);

    @Select("select * from article where id = #{userId}")
    Article selectDraftByUserId(Long userId);

    //@Select("select * from article where author_id = #{userId} and status = #{status} order by create_time desc")
    //List<Article> selectAllPublish(Long userId, int status);

    @Select("select * from article where author_id = #{userId} and status = #{status} order by create_time desc")
    List<Article> selectKindOfArticle(Long userId, int status);

    @Select("select * from article where id = #{articleId}")
    Article selectByArticleId(Long articleId);

    @Select("select * from article where author_id = #{userId} and title like concat('%',#{title},'%') and status = #{status}")
    List<Article> selectByTitle(Long userId,String title,int status);



    @Select("SELECT * FROM article WHERE author_id = #{userId} AND status = #{status} " +
            "AND (DATE(update_time) = #{date} OR (update_time IS NULL AND DATE(create_time) = #{date}))")
    List<Article> selectBySmartDate(@Param("userId") Long userId,
                                    @Param("date") String date,
                                    @Param("status") int status);

    @Select("select * from article where status = #{status} and title like concat('%',#{keyword},'%') ")
    List<Article> searchArticle(String keyword,int status);


    @Update("UPDATE article SET pageview = pageview + 1 WHERE id = #{articleId}")
    void incrementViewCount(Long articleId);



    @Select("select * from article where author_id = #{userId}")
    List<Article> selectByAuthorId(Long userId);


    List<Article> selectArticlesByStatuses(List<Integer> statuses);

    @Update("UPDATE article SET status = #{status} WHERE id = #{articleId}")
    void updateArticleByStatus(Long articleId, Integer status);

    //批量查询文章
    List<Article> BatchArticles(List<Long> articleIds);

    //获取全部已发布文章按照最新发布进行排序
    @Select("select * from article where status = 3 order by create_time desc ")
    List<Article> getAllArticlesByTime();
}
