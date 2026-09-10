package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

public interface ArticleMapper extends BaseMapper<Article> {

    //@Select("select * from article where author_id = #{userId} and status = #{status} order by create_time desc")
    //List<Article> selectAllDraft(Long userId, int status);

    @Select("select * from article where id = #{userId} and is_draft = 1")
    Article selectDraftByUserId(Long userId);

    @Select("select * from article where author_id = #{userId} and is_draft = 1 order by create_time desc")
    List<Article> selectDraftsByUserId(Long userId);

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



    @Select("select * from article where author_id = #{userId} order by create_time desc")
    List<Article> selectByAuthorId(Long userId);

    @Select("select * from article where author_id = #{userId} and status in (0, 1, 2, 3) and is_draft = 0 order by create_time desc")
    List<Article> selectAllByUserId(Long userId);


    List<Article> selectArticlesByStatuses(List<Integer> statuses);

    @Update("UPDATE article SET status = #{status} WHERE id = #{articleId}")
    void updateArticleByStatus(Long articleId, Integer status);

    /**
     * 拉模式查询：一次 SQL 查出一批作者（大V）在 since 之后发布的最新文章
     * 用于 Feed 流推拉结合——大V的文章不推入收件箱，用户读 Feed 时按关注的大V现查
     */
    @Select("<script>" +
            "select * from article where status = 1 and is_draft = 0 and create_time &gt;= #{since} " +
            "and author_id in " +
            "<foreach collection='authorIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "order by create_time desc limit #{limit}" +
            "</script>")
    List<Article> selectRecentByAuthors(@Param("authorIds") List<Long> authorIds,
                                        @Param("since") Date since,
                                        @Param("limit") int limit);


    //获取全部已发布文章按照最新发布进行排序
    @Select("select * from article where status = 1 and is_draft = 0 order by create_time desc ")
    List<Article> getAllArticlesByTime();
}
