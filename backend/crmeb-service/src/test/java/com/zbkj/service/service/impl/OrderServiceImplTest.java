package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.OrderRefundApplyRequest;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.dao.StoreOrderDao;
import com.zbkj.service.service.FlowerAfterSaleService;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.StoreOrderStatusService;
import com.zbkj.service.service.SystemAttachmentService;
import com.zbkj.service.service.UserService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderServiceImplTest {

    @Test
    public void flowerRefundApplyCannotOverwriteConcurrentCompletedRefund() {
        StoreOrder order = new StoreOrder();
        order.setId(11);
        order.setOrderId("flower-race");
        order.setUid(7);
        order.setPaid(true);
        order.setRefundStatus(0);
        order.setRefundPrice(BigDecimal.ZERO);
        AtomicInteger ordinaryUpdates = new AtomicInteger();
        AtomicInteger logs = new AtomicInteger();

        StoreOrderService orderService = proxy(StoreOrderService.class, (method, args) -> {
            if ("getByEntityOne".equals(method)) return order;
            if ("updateById".equals(method)) {
                ordinaryUpdates.incrementAndGet();
                return Boolean.TRUE;
            }
            return null;
        });
        StoreOrderDao orderDao = proxy(StoreOrderDao.class, (method, args) -> {
            if ("applyFlowerRefund".equals(method)) return 0;
            return null;
        });
        FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (method, args) ->
                "selectByOrderId".equals(method) ? new FlowerOrderExt().setOrderId(order.getOrderId()) : null);
        UserService userService = proxy(UserService.class, (method, args) ->
                "getUserIdException".equals(method) ? order.getUid() : null);
        FlowerAfterSaleService afterSale = proxy(FlowerAfterSaleService.class, (method, args) -> null);
        SystemAttachmentService attachment = proxy(SystemAttachmentService.class, (method, args) ->
                "clearPrefix".equals(method) ? args[0] : null);
        StoreOrderStatusService statusService = proxy(StoreOrderStatusService.class, (method, args) -> {
            if ("createLog".equals(method)) logs.incrementAndGet();
            return null;
        });

        OrderServiceImpl service = new OrderServiceImpl();
        setField(service, "storeOrderService", orderService);
        setField(service, "storeOrderDao", orderDao);
        setField(service, "flowerOrderExtDao", extDao);
        setField(service, "userService", userService);
        setField(service, "flowerAfterSaleService", afterSale);
        setField(service, "systemAttachmentService", attachment);
        setField(service, "storeOrderStatusService", statusService);
        setField(service, "transactionTemplate", transactionTemplate());

        OrderRefundApplyRequest request = new OrderRefundApplyRequest();
        request.setUni(order.getOrderId());
        request.setText("不新鲜");
        request.setExplain("并发测试");
        request.setReasonImage("");
        try {
            service.refundApply(request);
            Assert.fail("Expected the stale customer request to lose the refund race");
        } catch (CrmebException exception) {
            Assert.assertEquals("订单退款状态已变化，请刷新后重试", exception.getMessage());
        }
        Assert.assertEquals(0, ordinaryUpdates.get());
        Assert.assertEquals(0, logs.get());
    }

    private interface Invocation {
        Object call(String method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> invocation.call(method.getName(), args));
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override public TransactionStatus getTransaction(TransactionDefinition definition) { return new SimpleTransactionStatus(); }
            @Override public void commit(TransactionStatus status) { }
            @Override public void rollback(TransactionStatus status) { }
        });
    }

    private void setField(Object target, String name, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError("Missing test fixture field: " + name);
    }
}
