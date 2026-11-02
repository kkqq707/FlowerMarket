package com.zbkj.service.service.impl;

import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.response.StoreProductInfoResponse;
import com.zbkj.service.dao.StoreProductDao;
import com.zbkj.service.service.StoreProductAttrService;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductCouponService;
import com.zbkj.service.service.StoreProductDescriptionService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StoreProductInfoTest {
    private static final List<String> DEFAULT_ACTIVITIES = Arrays.asList("默认", "秒杀", "砍价", "拼团");

    @Test
    public void emptyActivityLoadsProductAndStockWithDefaultActivities() {
        StoreProductInfoResponse info = info("");
        Assert.assertEquals(DEFAULT_ACTIVITIES, info.getActivity());
        Assert.assertEquals(Integer.valueOf(900001), info.getId());
        Assert.assertEquals("晨露 · 白绿洋桔梗", info.getStoreName());
        Assert.assertEquals(1, info.getAttrValue().size());
        Assert.assertEquals(Integer.valueOf(99), info.getAttrValue().get(0).getStock());
        Assert.assertEquals(new BigDecimal("188.00"), info.getAttrValue().get(0).getPrice());
    }

    @Test
    public void nullActivityUsesDefaultActivities() {
        Assert.assertEquals(DEFAULT_ACTIVITIES, info(null).getActivity());
    }

    @Test
    public void whitespaceActivityUsesDefaultActivities() {
        Assert.assertEquals(DEFAULT_ACTIVITIES, info(" \t ").getActivity());
    }

    @Test
    public void activityIdsAllowSpacesAndEmptySeparators() {
        Assert.assertEquals(Arrays.asList("砍价", "默认", "秒杀"), info(" 2, ,0,, 1, ").getActivity());
    }

    @Test
    public void existingActivityIdsPreserveTheirOrder() {
        Assert.assertEquals(DEFAULT_ACTIVITIES, info("0, 1, 2, 3").getActivity());
        Assert.assertEquals(Arrays.asList("拼团", "秒杀", "默认"), info("3,1,0").getActivity());
    }

    private StoreProductInfoResponse info(String activity) {
        StoreProduct product = new StoreProduct();
        product.setId(900001);
        product.setStoreName("晨露 · 白绿洋桔梗");
        product.setStock(99);
        product.setActivity(activity);
        product.setGuaranteeIds("");
        StoreProductAttrValue sku = new StoreProductAttrValue();
        sku.setSuk("标准款");
        sku.setStock(99);
        sku.setPrice(new BigDecimal("188.00"));
        StoreProductServiceImpl service = new StoreProductServiceImpl();
        ReflectionTestUtils.setField(service, "dao", stub(StoreProductDao.class, "selectById", product));
        ReflectionTestUtils.setField(service, "attrService", stub(StoreProductAttrService.class,
                "getListByProductIdAndType", Collections.emptyList()));
        ReflectionTestUtils.setField(service, "storeProductAttrValueService", stub(StoreProductAttrValueService.class,
                "getListByProductIdAndType", Collections.singletonList(sku)));
        ReflectionTestUtils.setField(service, "storeProductDescriptionService", stub(StoreProductDescriptionService.class,
                "getByProductIdAndType", null));
        ReflectionTestUtils.setField(service, "storeProductCouponService", stub(StoreProductCouponService.class,
                "getListByProductId", Collections.emptyList()));
        return service.getInfo(900001);
    }

    private static <T> T stub(Class<T> type, String methodName, Object result) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (method.getName().equals(methodName)) {
                return result;
            }
            if (method.getName().equals("toString")) {
                return type.getSimpleName() + "Stub";
            }
            throw new AssertionError("Unexpected dependency call: " + method.getName());
        }));
    }
}
