package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.article.Article;
import com.zbkj.common.model.category.Category;
import com.zbkj.service.dao.ArticleDao;
import com.zbkj.service.dao.CategoryDao;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;

public class FlowerCareServiceImplTest {
    @Test
    public void publishedVisibleCareArticleIsReadable() {
        FlowerCareServiceImpl service = service(new Article().setId(7).setCid("90001")
                .setStatus(false).setHide(false).setTitle("收到鲜花后如何醒花"));
        Assert.assertEquals("收到鲜花后如何醒花", service.detail(7).getTitle());
    }

    @Test(expected = CrmebException.class)
    public void hiddenCareArticleCannotBeOpenedByDirectUrl() {
        service(new Article().setId(7).setCid("90001").setStatus(false).setHide(true)).detail(7);
    }

    @Test(expected = CrmebException.class)
    public void unrelatedArticleCannotBeOpenedByDirectUrl() {
        service(new Article().setId(7).setCid("601").setStatus(false).setHide(false)).detail(7);
    }

    @Test(expected = CrmebException.class)
    public void draftCareArticleCannotBeOpenedByDirectUrl() {
        service(new Article().setId(7).setCid("90001").setStatus(true).setHide(false)).detail(7);
    }

    @SuppressWarnings("unchecked")
    private FlowerCareServiceImpl service(Article article) {
        CategoryDao categories = (CategoryDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{CategoryDao.class}, (p, m, a) -> "selectOne".equals(m.getName())
                        ? new Category().setId(90001).setStatus(true) : null);
        ArticleDao articles = (ArticleDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{ArticleDao.class}, (p, m, a) -> "selectById".equals(m.getName()) ? article : null);
        return new FlowerCareServiceImpl(categories, articles, null);
    }
}
