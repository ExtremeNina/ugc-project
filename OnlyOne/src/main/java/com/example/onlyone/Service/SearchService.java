package com.example.onlyone.Service;

import com.example.onlyone.VO.SearchArticleVO;

import java.util.List;

public interface SearchService{


    List<SearchArticleVO> searchArticle(String keyword);

}
