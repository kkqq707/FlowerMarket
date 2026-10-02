package com.zbkj.service.service.impl;

import com.zbkj.common.enums.FlowerOrderStageEnum;
import com.zbkj.common.enums.FlowerSubstitutionPolicyEnum;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.flower.FlowerOrderProgress;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.UserAddress;
import com.zbkj.common.request.flower.FlowerOrderRequest;
import com.zbkj.common.response.flower.FlowerDeliveryQuoteResponse;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.dao.FlowerOrderProgressDao;
import com.zbkj.service.service.FlowerDeliveryService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

public class FlowerOrderCreateIntegrationTest {

    @Test(expected = CrmebException.class)
    public void deliveryOrderRejectsMissingRecipient() {
        FlowerOrderServiceImpl service = service(null, null);
        FlowerOrderRequest request = validRequest();
        request.setRecipientPhone(null);

        service.validateForCreate(request, address(), 1, 1, new BigDecimal("188"));
    }

    @Test(expected = CrmebException.class)
    public void deliveryOrderRejectsZoneThatDoesNotMatchServerAddressQuote() {
        FlowerDeliveryQuoteResponse quote = new FlowerDeliveryQuoteResponse();
        quote.setZoneId(2);
        quote.setDeliveryFee(new BigDecimal("12"));
        FlowerOrderServiceImpl service = service(quote, null);
        FlowerOrderRequest request = validRequest();
        request.setDeliveryZoneId(1);

        service.validateForCreate(request, address(), 1, 1, new BigDecimal("188"));
    }

    @Test(expected = CrmebException.class)
    public void flowerProductsCannotOmitFlowerOrderInformation() {
        service(null, null).requireFlowerRequest(true, null);
    }

    @Test(expected = CrmebException.class)
    public void deliveryOrderRejectsAddressOwnedByAnotherUser() {
        UserAddress anotherUsersAddress = address().setUid(99);
        service(null, null).validateForCreate(validRequest(), anotherUsersAddress, 1, 1,
                new BigDecimal("188"));
    }

    @Test(expected = CrmebException.class)
    public void pickupOrderRejectsPastCollectionDate() {
        FlowerOrderRequest request = validRequest();
        request.setDeliveryDate(new Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000));
        service(null, null).validateForCreate(request, null, 1, 2, new BigDecimal("188"));
    }

    @Test
    public void extensionUsesServerAddressSnapshotAndCreatesWaitPayProgress() {
        AtomicReference<FlowerOrderExt> extensionRef = new AtomicReference<>();
        AtomicReference<FlowerOrderProgress> progressRef = new AtomicReference<>();
        FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (o, m, a) -> {
            if ("insert".equals(m.getName())) {
                extensionRef.set((FlowerOrderExt) a[0]);
                return 1;
            }
            return defaultValue(m.getReturnType());
        });
        FlowerOrderProgressDao progressDao = proxy(FlowerOrderProgressDao.class, (o, m, a) -> {
            if ("insert".equals(m.getName())) {
                progressRef.set((FlowerOrderProgress) a[0]);
                return 1;
            }
            return defaultValue(m.getReturnType());
        });
        FlowerOrderServiceImpl service = new FlowerOrderServiceImpl(extDao, progressDao,
                proxy(FlowerDeliveryService.class, (o, m, a) -> defaultValue(m.getReturnType())));
        StoreOrder order = new StoreOrder().setOrderId("F20260915001");
        Date before = new Date();

        FlowerOrderExt result = service.createExtension(order, validRequest(), address());

        Assert.assertSame(result, extensionRef.get());
        Assert.assertEquals("广东省广州市天河区花城大道1号", result.getDeliveryAddressSnapshot());
        Assert.assertEquals(FlowerOrderStageEnum.WAIT_PAY.name(), result.getCurrentStage());
        Assert.assertFalse(result.getReservationReleased());
        Assert.assertTrue(result.getReservationExpireTime().getTime() >= before.getTime() + 14 * 60 * 1000L);
        Assert.assertEquals(FlowerOrderStageEnum.WAIT_PAY.name(), progressRef.get().getStage());
        Assert.assertEquals("F20260915001", progressRef.get().getOrderId());
    }

    @Test
    public void paySuccessMovesFlowerOrderToWaitAcceptAndAppendsProgress() {
        AtomicReference<FlowerOrderProgress> progressRef = new AtomicReference<>();
        FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (o, m, a) ->
                "markPaidStage".equals(m.getName()) ? 1 : defaultValue(m.getReturnType()));
        FlowerOrderProgressDao progressDao = proxy(FlowerOrderProgressDao.class, (o, m, a) -> {
            if ("insert".equals(m.getName())) {
                progressRef.set((FlowerOrderProgress) a[0]);
                return 1;
            }
            return defaultValue(m.getReturnType());
        });
        FlowerOrderServiceImpl service = new FlowerOrderServiceImpl(extDao, progressDao,
                proxy(FlowerDeliveryService.class, (o, m, a) -> defaultValue(m.getReturnType())));

        service.onPaySuccess("F20260915001");

        Assert.assertEquals(FlowerOrderStageEnum.WAIT_ACCEPT.name(), progressRef.get().getStage());
        Assert.assertEquals("支付成功，等待花店接单", progressRef.get().getRemark());
    }

    private static FlowerOrderServiceImpl service(FlowerDeliveryQuoteResponse quote, AtomicReference<Integer> slotRef) {
        FlowerDeliveryService deliveryService = proxy(FlowerDeliveryService.class, (o, m, a) -> {
            if ("quote".equals(m.getName())) return quote;
            if ("validateSelection".equals(m.getName()) && slotRef != null) slotRef.set((Integer) a[2]);
            return defaultValue(m.getReturnType());
        });
        return new FlowerOrderServiceImpl(
                proxy(FlowerOrderExtDao.class, (o, m, a) -> defaultValue(m.getReturnType())),
                proxy(FlowerOrderProgressDao.class, (o, m, a) -> defaultValue(m.getReturnType())),
                deliveryService);
    }

    private static FlowerOrderRequest validRequest() {
        return new FlowerOrderRequest()
                .setSenderName("张同学")
                .setSenderPhone("13800138000")
                .setRecipientName("李同学")
                .setRecipientPhone("13900139000")
                .setDeliveryZoneId(2)
                .setDeliveryDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L))
                .setSlotId(9)
                .setGreetingCard("毕业快乐")
                .setAnonymous(false)
                .setContactRecipient(true)
                .setSubstitutionPolicy(FlowerSubstitutionPolicyEnum.CONTACT_FIRST);
    }

    private static UserAddress address() {
        return new UserAddress().setId(7).setUid(1).setProvince("广东省").setCity("广州市")
                .setDistrict("天河区").setDetail("花城大道1号").setCityId(440106).setIsDel(false);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type.equals(boolean.class)) return false;
        if (type.equals(int.class)) return 0;
        if (type.equals(long.class)) return 0L;
        return null;
    }
}
