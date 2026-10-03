package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.request.flower.FlowerProductExtRequest;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import com.zbkj.common.response.StoreProductTabsHeader;
import com.zbkj.service.dao.FlowerProductExtDao;
import com.zbkj.service.service.StoreProductService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Arrays;
import java.util.Map;
import java.util.List;

/**
 * 鲜花商品扩展服务测试。
 */
public class FlowerProductServiceImplTest {

    @Test
    public void adminTabsPreserveFiltersWithoutChangingTheSelectedStatus() {
        FlowerProductSearchRequest request = new FlowerProductSearchRequest();
        request.setType(2);
        request.setCid(9101);
        request.setRecipient("爱人");
        FlowerProductExtDao dao = proxy(FlowerProductExtDao.class, (instance, method, args) -> {
            if ("countAdmin".equals(method.getName())) {
                FlowerProductSearchRequest criteria = (FlowerProductSearchRequest) args[0];
                Assert.assertEquals(Integer.valueOf(9101), criteria.getCid());
                Assert.assertEquals("爱人", criteria.getRecipient());
                return criteria.getType() * 10;
            }
            return defaultValue(method.getReturnType());
        });
        FlowerProductServiceImpl service = new FlowerProductServiceImpl(dao, proxyReturning(StoreProductService.class, null));
        List<StoreProductTabsHeader> headers = service.adminTabs(request);
        Assert.assertEquals(5, headers.size());
        for (int index = 0; index < headers.size(); index++) {
            Assert.assertEquals(Integer.valueOf(index + 1), headers.get(index).getType());
            Assert.assertEquals(Integer.valueOf((index + 1) * 10), headers.get(index).getCount());
        }
        Assert.assertEquals(Integer.valueOf(2), request.getType());
    }

    @Test
    public void filterOptionsUseActualStoredTagsAndKeepCustomValues() {
        FlowerProductCardResponse first = new FlowerProductCardResponse();
        first.setOccasion("日常,生日");
        first.setRecipient("爱人, 朋友 ");
        first.setColorSeries("白绿色，香槟色");
        FlowerProductCardResponse second = new FlowerProductCardResponse();
        second.setOccasion("生日");
        second.setRecipient("朋友,客户");
        second.setColorSeries("白绿色,自定义色");
        FlowerProductExtDao dao = proxy(FlowerProductExtDao.class, (instance, method, args) ->
                "filterSources".equals(method.getName()) ? Arrays.asList(first, second) : defaultValue(method.getReturnType()));
        FlowerProductServiceImpl service = new FlowerProductServiceImpl(dao, proxyReturning(StoreProductService.class, null));
        Map<String, List<String>> options = service.filterOptions(true);
        Assert.assertEquals(Arrays.asList("日常", "生日"), options.get("occasion"));
        Assert.assertEquals(Arrays.asList("爱人", "朋友", "客户"), options.get("recipient"));
        Assert.assertEquals(Arrays.asList("白绿色", "香槟色", "自定义色"), options.get("colorSeries"));
    }

    @Test(expected = CrmebException.class)
    public void saveOrUpdateRejectsMissingBaseProduct() {
        FlowerProductExtDao flowerDao = proxyReturning(FlowerProductExtDao.class, null);
        StoreProductService productService = proxyReturning(StoreProductService.class, null);
        FlowerProductServiceImpl service = new FlowerProductServiceImpl(flowerDao, productService);

        FlowerProductExtRequest request = new FlowerProductExtRequest();
        request.setFlowerMaterials("香槟玫瑰");
        service.saveOrUpdate(999, request);
    }

    @Test
    public void saveOrUpdateCreatesExtensionAndNormalizesTags() {
        AtomicReference<FlowerProductExt> inserted = new AtomicReference<>();
        FlowerProductExtDao flowerDao = proxy(FlowerProductExtDao.class, (instance, method, args) -> {
            if ("selectOne".equals(method.getName())) {
                return null;
            }
            if ("insert".equals(method.getName())) {
                inserted.set((FlowerProductExt) args[0]);
                return 1;
            }
            return defaultValue(method.getReturnType());
        });
        StoreProduct product = product(12, true);
        StoreProductService productService = proxy(StoreProductService.class, (instance, method, args) ->
                "getById".equals(method.getName()) ? product : defaultValue(method.getReturnType()));
        FlowerProductServiceImpl service = new FlowerProductServiceImpl(flowerDao, productService);

        FlowerProductExtRequest request = new FlowerProductExtRequest();
        request.setFlowerMaterials("  香槟玫瑰  ");
        request.setOccasion("生日， 表白,生日");
        request.setRecipient("恋人, 朋友 ");
        request.setColorSeries("香槟色，暖色");

        FlowerProductExt result = service.saveOrUpdate(12, request);

        Assert.assertSame(result, inserted.get());
        Assert.assertEquals(Integer.valueOf(12), result.getProductId());
        Assert.assertEquals("香槟玫瑰", result.getFlowerMaterials());
        Assert.assertEquals("生日,表白", result.getOccasion());
        Assert.assertEquals("恋人,朋友", result.getRecipient());
        Assert.assertEquals("香槟色,暖色", result.getColorSeries());
        Assert.assertNotNull(result.getCreateTime());
        Assert.assertNotNull(result.getUpdateTime());
    }

    @Test(expected = CrmebException.class)
    public void frontDetailRejectsOffShelfProduct() {
        StoreProductService productService = proxy(StoreProductService.class, (instance, method, args) ->
                "getById".equals(method.getName()) ? product(13, false) : defaultValue(method.getReturnType()));
        FlowerProductServiceImpl service = new FlowerProductServiceImpl(
                proxyReturning(FlowerProductExtDao.class, null), productService);

        service.getFrontDetail(13);
    }

    @Test(expected = CrmebException.class)
    public void searchRejectsReversedBudgetRange() {
        FlowerProductServiceImpl service = new FlowerProductServiceImpl(
                proxyReturning(FlowerProductExtDao.class, null), proxyReturning(StoreProductService.class, null));
        FlowerProductSearchRequest request = new FlowerProductSearchRequest();
        request.setMinPrice(new BigDecimal("200"));
        request.setMaxPrice(new BigDecimal("100"));

        service.search(request);
    }

    @Test
    public void detectsFlowerProductsFromServerSideProductIds() {
        FlowerProductExtDao flowerDao = proxy(FlowerProductExtDao.class, (instance, method, args) ->
                "countByProductIds".equals(method.getName()) ? 1 : defaultValue(method.getReturnType()));
        FlowerProductServiceImpl service = new FlowerProductServiceImpl(flowerDao,
                proxyReturning(StoreProductService.class, null));

        Assert.assertTrue(service.containsFlowerProducts(Arrays.asList(12, 13)));
    }

    private static StoreProduct product(int id, boolean onSale) {
        return new StoreProduct().setId(id).setStoreName("测试鲜花")
                .setIsShow(onSale).setIsDel(false).setIsRecycle(false);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxyReturning(Class<T> type, Object returnValue) {
        return proxy(type, (instance, method, args) -> {
            if ("toString".equals(method.getName())) {
                return type.getSimpleName() + "Proxy";
            }
            Object defaultValue = defaultValue(method.getReturnType());
            return defaultValue == null ? returnValue : defaultValue;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type.equals(boolean.class)) {
            return false;
        }
        if (type.equals(int.class)) {
            return 0;
        }
        if (type.equals(long.class)) {
            return 0L;
        }
        return null;
    }
}
