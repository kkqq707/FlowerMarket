package com.zbkj.service.service.impl;

import com.zbkj.common.model.flower.FlowerRefundOutbox;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.service.dao.FlowerRefundOutboxDao;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.StoreOrderTaskService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowerRefundOutboxServiceImplTest {

    @Test
    public void duplicateDispatchProcessesRefundPostActionsOnce() {
        FlowerRefundOutbox task = pendingTask();
        AtomicInteger processed = new AtomicInteger();
        AtomicInteger markedDone = new AtomicInteger();
        FlowerRefundOutboxServiceImpl service = fixture(task, processed, markedDone, false);

        service.dispatchPending(100);
        service.dispatchPending(100);

        Assert.assertEquals(1, processed.get());
        Assert.assertEquals(1, markedDone.get());
        Assert.assertEquals(Integer.valueOf(1), task.getStatus());
    }

    @Test
    public void failedPostActionLeavesTaskPendingForRetry() {
        FlowerRefundOutbox task = pendingTask();
        AtomicInteger processed = new AtomicInteger();
        AtomicInteger markedDone = new AtomicInteger();
        FlowerRefundOutboxServiceImpl service = fixture(task, processed, markedDone, true);

        service.dispatchPending(100);
        Assert.assertEquals(Integer.valueOf(0), task.getStatus());
        Assert.assertEquals(0, markedDone.get());

        service.dispatchPending(100);
        Assert.assertEquals(2, processed.get());
        Assert.assertEquals(1, markedDone.get());
        Assert.assertEquals(Integer.valueOf(1), task.getStatus());
    }

    private FlowerRefundOutboxServiceImpl fixture(FlowerRefundOutbox task,
                                                   AtomicInteger processed,
                                                   AtomicInteger markedDone,
                                                   boolean failFirst) {
        FlowerRefundOutboxDao dao = (FlowerRefundOutboxDao) Proxy.newProxyInstance(
                FlowerRefundOutboxDao.class.getClassLoader(), new Class<?>[]{FlowerRefundOutboxDao.class},
                (proxy, method, args) -> {
                    if ("findPendingIds".equals(method.getName())) {
                        return Collections.singletonList(task.getId());
                    }
                    if ("selectForUpdate".equals(method.getName())) {
                        return task;
                    }
                    if ("markDone".equals(method.getName())) {
                        task.setStatus(1);
                        markedDone.incrementAndGet();
                        return 1;
                    }
                    return null;
                });
        StoreOrder order = new StoreOrder();
        order.setId(task.getOrderId());
        StoreOrderService orderService = (StoreOrderService) Proxy.newProxyInstance(
                StoreOrderService.class.getClassLoader(), new Class<?>[]{StoreOrderService.class},
                (proxy, method, args) -> "getById".equals(method.getName()) ? order : null);
        StoreOrderTaskService taskService = (StoreOrderTaskService) Proxy.newProxyInstance(
                StoreOrderTaskService.class.getClassLoader(), new Class<?>[]{StoreOrderTaskService.class},
                (proxy, method, args) -> {
                    if ("refundOrder".equals(method.getName())) {
                        int call = processed.incrementAndGet();
                        return !(failFirst && call == 1);
                    }
                    return null;
                });
        FlowerRefundOutboxServiceImpl service = new FlowerRefundOutboxServiceImpl();
        setField(service, "dao", dao);
        setField(service, "storeOrderService", orderService);
        setField(service, "storeOrderTaskService", taskService);
        setField(service, "transactionTemplate", transactionTemplate());
        return service;
    }

    private FlowerRefundOutbox pendingTask() {
        return new FlowerRefundOutbox().setId(1).setOrderId(9).setStatus(0);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
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
