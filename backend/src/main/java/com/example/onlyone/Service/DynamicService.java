package com.example.onlyone.Service;

import com.example.onlyone.VO.DyArticleVO;

import java.util.List;

public interface DynamicService {

    /**
     * 获取关注动态 Feed（分页 + 推拉结合）
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     */
    List<DyArticleVO> getNewArticles(int page, int size);
}
