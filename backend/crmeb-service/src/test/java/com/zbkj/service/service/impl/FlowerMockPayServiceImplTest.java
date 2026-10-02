package com.zbkj.service.service.impl;

import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.service.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowerMockPayServiceImplTest {

    @Test(expected = CrmebException.class)
    public void mockPayRejectsWhenFeatureSwitchIsOff() {
        FlowerMockPayServiceImpl service = service("0", new MockEnvironment(), order(1), validExtension(), 1,
                proxy(OrderPayService.class, (o, m, a) -> null));
        service.pay("F100");
    }

    @Test(expected = CrmebException.class)
    public void mockPayIsAlwaysBlockedInProductionProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        FlowerMockPayServiceImpl service = service("1", environment, order(1), validExtension(), 1,
                proxy(OrderPayService.class, (o, m, a) -> null));
        service.pay("F100");
    }

    @Test(expected = CrmebException.class)
    public void mockPayRejectsAnotherUsersOrder() {
        FlowerMockPayServiceImpl service = service("1", new MockEnvironment(), order(2), validExtension(), 1,
                proxy(OrderPayService.class, (o, m, a) -> null));
        service.pay("F100");
    }

    @Test(expected = CrmebException.class)
    public void mockPayRejectsExpiredReservation() {
        FlowerOrderExt expired = validExtension();
        expired.setReservationExpireTime(new Date(System.currentTimeMillis() - 1000));
        FlowerMockPayServiceImpl service = service("1", new MockEnvironment(), order(1), expired, 1,
                proxy(OrderPayService.class, (o, m, a) -> null));
        service.pay("F100");
    }

    @Test
    public void mockPayReturnsSuccessAndUsesSharedPayTransition() {
        AtomicInteger transitions = new AtomicInteger();
        OrderPayService payService = proxy(OrderPayService.class, (o, m, a) -> {
            if ("markPaidAndEnqueueSuccess".equals(m.getName())) {
                transitions.incrementAndGet();
                return true;
            }
            return null;
        });
        FlowerMockPayServiceImpl service = service("1", new MockEnvironment(), order(1), validExtension(), 1, payService);

        OrderPayResultResponse result = service.pay("F100");

        Assert.assertTrue(result.getStatus());
        Assert.assertEquals("F100", result.getOrderNo());
        Assert.assertEquals("mock", result.getPayType());
        Assert.assertEquals(1, transitions.get());
    }

    @Test
    public void sharedPayTransitionEnqueuesOnlyAfterFirstAtomicUpdate() {
        AtomicInteger updates = new AtomicInteger();
        AtomicInteger queues = new AtomicInteger();
        StoreOrderService orderService = proxy(StoreOrderService.class, (o, m, a) -> {
            if ("markPaidAtomically".equals(m.getName())) return updates.getAndIncrement() == 0;
            return null;
        });
        OrderPaySuccessQueueService queueService = orderNo -> {
            queues.incrementAndGet();
            return true;
        };
        OrderPayServiceImpl payService = new OrderPayServiceImpl();
        ReflectionTestUtils.setField(payService, "storeOrderService", orderService);
        ReflectionTestUtils.setField(payService, "orderPaySuccessQueueService", queueService);

        Assert.assertTrue(payService.markPaidAndEnqueueSuccess("F100", "mock", "MOCK-F100"));
        Assert.assertFalse(payService.markPaidAndEnqueueSuccess("F100", "mock", "MOCK-F100"));
        Assert.assertEquals(1, queues.get());
    }

    private static FlowerMockPayServiceImpl service(String switchValue, Environment environment,
                                                     StoreOrder order, FlowerOrderExt extension,
                                                     Integer currentUid, OrderPayService payService) {
        SystemConfigService configService = proxy(SystemConfigService.class, (o, m, a) ->
                "getValueByKey".equals(m.getName()) && SysConfigConstants.CONFIG_FLOWER_MOCK_PAY_ENABLED.equals(a[0])
                        ? switchValue : null);
        StoreOrderService orderService = proxy(StoreOrderService.class, (o, m, a) ->
                "getByOderId".equals(m.getName()) ? order : null);
        User currentUser = new User().setUid(currentUid);
        UserService userService = proxy(UserService.class, (o, m, a) ->
                "getInfoException".equals(m.getName()) ? currentUser : null);
        FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (o, m, a) ->
                "selectByOrderId".equals(m.getName()) ? extension : null);
        return new FlowerMockPayServiceImpl(configService, environment, orderService, userService, extDao, payService);
    }

    private static StoreOrder order(Integer uid) {
        return new StoreOrder().setId(10).setOrderId("F100").setUid(uid).setPaid(false)
                .setIsDel(false).setIsSystemDel(false).setRefundStatus(0).setStatus(0);
    }

    private static FlowerOrderExt validExtension() {
        return new FlowerOrderExt().setOrderId("F100")
                .setReservationExpireTime(new Date(System.currentTimeMillis() + 10 * 60 * 1000L))
                .setReservationReleased(false);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler);
    }
}
