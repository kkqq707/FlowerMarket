package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.article.Article;
import com.zbkj.common.model.category.Category;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.ArticleRequest;
import com.zbkj.common.request.ArticleSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.ArticleResponse;
import com.zbkj.common.vo.ArticleVo;
import com.zbkj.service.dao.ArticleDao;
import com.zbkj.service.dao.CategoryDao;
import com.zbkj.service.service.ArticleService;
import com.zbkj.service.service.FlowerCareService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlowerCareServiceImpl implements FlowerCareService {
    private final CategoryDao categoryDao;
    private final ArticleDao articleDao;
    private final ArticleService articleService;

    public FlowerCareServiceImpl(CategoryDao categoryDao, ArticleDao articleDao, ArticleService articleService) {
        this.categoryDao = categoryDao;
        this.articleDao = articleDao;
        this.articleService = articleService;
    }

    @Override
    public PageInfo<ArticleResponse> list(String keyword, PageParamRequest page) {
        String cid = categoryId();
        Page<Article> paging = PageHelper.startPage(Math.max(1, page.getPage()), Math.min(50, Math.max(1, page.getLimit())));
        QueryWrapper<Article> query = new QueryWrapper<Article>().eq("cid", cid).eq("status", 0).eq("hide", 0);
        if (StringUtils.hasText(keyword)) query.like("title", keyword.trim());
        List<ArticleResponse> rows = articleDao.selectList(query.orderByDesc("sort", "create_time", "id"))
                .stream().map(this::toResponse).collect(Collectors.toList());
        return CommonPage.copyPageInfo(paging, rows);
    }

    @Override
    public ArticleResponse detail(Integer id) {
        Article article = articleDao.selectById(id);
        if (article == null || !categoryId().equals(article.getCid())
                || !Boolean.FALSE.equals(article.getStatus()) || !Boolean.FALSE.equals(article.getHide()))
            throw new CrmebException("养花内容不存在");
        return toResponse(article);
    }

    @Override
    public PageInfo<ArticleVo> adminList(String keyword, PageParamRequest page) {
        return articleService.getAdminList(new ArticleSearchRequest().setCid(categoryId()).setKeywords(keyword), page);
    }

    @Override
    public Article adminDetail(Integer id) {
        Article article = articleService.getDetail(id);
        if (!categoryId().equals(article.getCid())) throw new CrmebException("养花内容不存在");
        return article;
    }

    @Override
    public void save(ArticleRequest request) {
        request.setCid(categoryId());
        if (request.getStatus() == null) request.setStatus(true);
        if (request.getHide() == null) request.setHide(false);
        if (!articleService.create(request)) throw new CrmebException("保存养花文章失败");
    }

    @Override
    public void update(Integer id, ArticleRequest request) {
        adminDetail(id);
        request.setCid(categoryId());
        if (!articleService.updateArticle(id, request)) throw new CrmebException("更新养花文章失败");
    }

    private String categoryId() {
        Category category = categoryDao.selectOne(new QueryWrapper<Category>()
                .eq("type", 3).eq("url", "flower-care").eq("status", 1).last("LIMIT 1"));
        if (category == null) throw new CrmebException("养花指南分类尚未初始化，请先执行升级脚本");
        return String.valueOf(category.getId());
    }

    private ArticleResponse toResponse(Article article) {
        ArticleResponse response = new ArticleResponse();
        BeanUtils.copyProperties(article, response);
        return response;
    }
}
