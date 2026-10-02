package com.zbkj.service.service;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.article.Article;
import com.zbkj.common.request.ArticleRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.ArticleResponse;
import com.zbkj.common.vo.ArticleVo;

public interface FlowerCareService {
    PageInfo<ArticleResponse> list(String keyword, PageParamRequest page);
    ArticleResponse detail(Integer id);
    PageInfo<ArticleVo> adminList(String keyword, PageParamRequest page);
    Article adminDetail(Integer id);
    void save(ArticleRequest request);
    void update(Integer id, ArticleRequest request);
}
