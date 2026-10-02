package com.zbkj.front.config;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.lang.reflect.Method;
import java.util.List;

public class WebConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    public void anonymousUsersCanReadOnlyPublicFlowerCatalogAndStoreConfig() throws Exception {
        InterceptorRegistry registry = new InterceptorRegistry();
        new WebConfig().addInterceptors(registry);
        Method method = InterceptorRegistry.class.getDeclaredMethod("getInterceptors");
        method.setAccessible(true);
        List<Object> interceptors = (List<Object>) method.invoke(registry);
        MappedInterceptor token = (MappedInterceptor) interceptors.get(0);
        AntPathMatcher matcher = new AntPathMatcher();

        Assert.assertFalse(token.matches("/api/front/flower/product/list", matcher));
        Assert.assertFalse(token.matches("/api/front/flower/product/900001", matcher));
        Assert.assertFalse(token.matches("/api/front/flower/store/config", matcher));
        Assert.assertTrue(token.matches("/api/front/flower/order/F100", matcher));
        Assert.assertTrue(token.matches("/api/front/flower/pay/mock/F100", matcher));
    }
}
