package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.StoreOrderRefundRequest;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.dao.StoreOrderDao;
import com.zbkj.service.dao.FlowerRefundOutboxDao;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.service.AliPayService;
import com.zbkj.service.service.StoreOrderRefundService;
import com.zbkj.service.service.UserBillService;
import com.zbkj.service.service.UserService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreOrderServiceImplTest {

    @Test
    public void refundRejectsOrderWhileRefundIsProcessing() {
        assertRefundRejected(3, "订单退款处理中，请勿重复操作");
    }

    @Test
    public void refundRejectsAlreadyRefundedOrder() {
        assertRefundRejected(2, "订单已退款，请勿重复操作");
    }

    @Test
    public void balanceRefundCompletesImmediately() {
        StoreOrder order = new StoreOrder();
        order.setId(1);
        order.setOrderId("balance-order");
        order.setUid(1);
        order.setPaid(true);
        order.setPayType(Constants.PAY_TYPE_YUE);
        order.setRefundStatus(0);
        order.setRefundPrice(BigDecimal.ZERO);
        order.setPayPrice(BigDecimal.TEN);

        User user = new User();
        user.setUid(order.getUid());
        user.setNowMoney(BigDecimal.TEN);

        StoreOrderDao dao = (StoreOrderDao) Proxy.newProxyInstance(
                StoreOrderDao.class.getClassLoader(),
                new Class<?>[]{StoreOrderDao.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) {
                        return order;
                    }
                    if ("updateById".equals(method.getName())) {
                        return 1;
                    }
                    return null;
                }
        );

        AtomicBoolean balanceUpdated = new AtomicBoolean(false);
        UserService userService = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class<?>[]{UserService.class},
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        return user;
                    }
                    if ("operationNowMoney".equals(method.getName())) {
                        balanceUpdated.set(true);
                        return Boolean.TRUE;
                    }
                    return null;
                }
        );
        UserBillService userBillService = (UserBillService) Proxy.newProxyInstance(
                UserBillService.class.getClassLoader(),
                new Class<?>[]{UserBillService.class},
                (proxy, method, args) -> "saveRefundBill".equals(method.getName()) ? Boolean.TRUE : null
        );

        AtomicBoolean refundTaskQueued = new AtomicBoolean(false);
        RedisUtil redisUtil = new RedisUtil(null, null) {
            @Override
            public boolean lPush(String key, Object value) {
                refundTaskQueued.set(true);
                return true;
            }
        };

        TransactionTemplate transactionTemplate = new TransactionTemplate(new PlatformTransactionManager() {
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

        StoreOrderServiceImpl service = new StoreOrderServiceImpl();
        setField(service, "dao", dao);
        setField(service, "baseMapper", dao);
        setField(service, "userService", userService);
        setField(service, "userBillService", userBillService);
        setField(service, "redisUtil", redisUtil);
        setField(service, "transactionTemplate", transactionTemplate);

        StoreOrderRefundRequest request = new StoreOrderRefundRequest();
        request.setOrderNo(order.getOrderId());
        request.setAmount(BigDecimal.ONE);

        Assert.assertTrue(service.refund(request));
        Assert.assertEquals(Integer.valueOf(2), order.getRefundStatus());
        Assert.assertTrue(balanceUpdated.get());
        Assert.assertTrue(refundTaskQueued.get());
    }

    @Test
    public void mockRefundCompletesWithoutMoneyGatewayAndQueuesPostProcessingOnce() {
        StoreOrder order = new StoreOrder();
        order.setId(2);
        order.setOrderId("mock-order");
        order.setUid(1);
        order.setPaid(true);
        order.setPayType("mock");
        order.setRefundStatus(0);
        order.setRefundPrice(BigDecimal.ZERO);
        order.setPayPrice(BigDecimal.TEN);

        AtomicBoolean mockRefunded = new AtomicBoolean(false);
        StoreOrderDao dao = (StoreOrderDao) Proxy.newProxyInstance(
                StoreOrderDao.class.getClassLoader(), new Class<?>[]{StoreOrderDao.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) return order;
                    if ("completeMockRefund".equals(method.getName())) {
                        if (mockRefunded.compareAndSet(false, true)) {
                            order.setRefundStatus(2);
                            return 1;
                        }
                        return 0;
                    }
                    if ("updateById".equals(method.getName())) return 1;
                    return null;
                });
        User user = new User();
        user.setUid(order.getUid());
        user.setNowMoney(BigDecimal.ZERO);
        UserService userService = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(), new Class<?>[]{UserService.class},
                (proxy, method, args) -> "getById".equals(method.getName()) ? user : null);
        StoreOrderRefundService wechatRefund = (StoreOrderRefundService) Proxy.newProxyInstance(
                StoreOrderRefundService.class.getClassLoader(), new Class<?>[]{StoreOrderRefundService.class},
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) return "wechat-refund-spy";
                    throw new AssertionError("mock refund must not invoke WeChat refund");
                });
        AliPayService aliRefund = (AliPayService) Proxy.newProxyInstance(
                AliPayService.class.getClassLoader(), new Class<?>[]{AliPayService.class},
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) return "alipay-refund-spy";
                    throw new AssertionError("mock refund must not invoke Alipay refund");
                });
        AtomicInteger queued = new AtomicInteger();
        FlowerRefundOutboxDao outboxDao = outboxDao(queued);

        StoreOrderServiceImpl service = new StoreOrderServiceImpl();
        setField(service, "dao", dao);
        setField(service, "baseMapper", dao);
        setField(service, "userService", userService);
        setField(service, "storeOrderRefundService", wechatRefund);
        setField(service, "aliPayService", aliRefund);
        setField(service, "flowerRefundOutboxDao", outboxDao);
        setField(service, "transactionTemplate", transactionTemplate());

        StoreOrderRefundRequest request = new StoreOrderRefundRequest();
        request.setOrderNo(order.getOrderId());
        request.setAmount(BigDecimal.TEN);

        Assert.assertTrue(service.refund(request));
        Assert.assertEquals(Integer.valueOf(2), order.getRefundStatus());
        Assert.assertEquals(1, queued.get());

        try {
            service.refund(request);
            Assert.fail("Expected repeated mock refund to be rejected");
        } catch (CrmebException exception) {
            Assert.assertEquals("订单已退款，请勿重复操作", exception.getMessage());
        }
        Assert.assertEquals(1, queued.get());
    }

    @Test
    public void mockRefundUsesOneConditionalUpdateAndRejectsTheLoserWithoutQueuingAgain() {
        AtomicInteger refundableRows = new AtomicInteger(1);
        AtomicInteger conditionalUpdates = new AtomicInteger();
        AtomicInteger queued = new AtomicInteger();
        StoreOrderDao dao = (StoreOrderDao) Proxy.newProxyInstance(
                StoreOrderDao.class.getClassLoader(), new Class<?>[]{StoreOrderDao.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) return mockOrder();
                    if ("completeMockRefund".equals(method.getName())) {
                        conditionalUpdates.incrementAndGet();
                        return refundableRows.getAndUpdate(rows -> rows > 0 ? rows - 1 : 0) > 0 ? 1 : 0;
                    }
                    if ("updateById".equals(method.getName())) {
                        throw new AssertionError("mock refund must use its conditional update");
                    }
                    return null;
                });
        StoreOrderServiceImpl service = new StoreOrderServiceImpl();
        setField(service, "dao", dao);
        setField(service, "baseMapper", dao);
        setField(service, "flowerRefundOutboxDao", outboxDao(queued));
        setField(service, "transactionTemplate", transactionTemplate());

        Assert.assertTrue(service.refund(mockRefundRequest()));
        try {
            service.refund(mockRefundRequest());
            Assert.fail("Expected losing conditional update to reject the duplicate refund");
        } catch (CrmebException exception) {
            Assert.assertEquals("订单已退款，请勿重复操作", exception.getMessage());
        }
        Assert.assertEquals(2, conditionalUpdates.get());
        Assert.assertEquals(1, queued.get());
    }

    @Test
    public void flowerRefundRefuseCannotReopenCompletedRefund() {
        StoreOrder order = mockOrder();
        order.setRefundStatus(2);
        StoreOrderDao dao = (StoreOrderDao) Proxy.newProxyInstance(
                StoreOrderDao.class.getClassLoader(), new Class<?>[]{StoreOrderDao.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) return order;
                    if ("refuseFlowerRefund".equals(method.getName())) return 0;
                    if ("updateById".equals(method.getName())) throw new AssertionError("flower refusal must use CAS");
                    return null;
                });
        FlowerOrderExtDao extDao = (FlowerOrderExtDao) Proxy.newProxyInstance(
                FlowerOrderExtDao.class.getClassLoader(), new Class<?>[]{FlowerOrderExtDao.class},
                (proxy, method, args) -> "selectByOrderId".equals(method.getName())
                        ? new FlowerOrderExt().setOrderId(order.getOrderId()) : null);
        UserService userService = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(), new Class<?>[]{UserService.class},
                (proxy, method, args) -> "getById".equals(method.getName()) ? new User() : null);
        StoreOrderServiceImpl service = new StoreOrderServiceImpl();
        setField(service, "dao", dao);
        setField(service, "baseMapper", dao);
        setField(service, "flowerOrderExtDao", extDao);
        setField(service, "userService", userService);
        setField(service, "transactionTemplate", transactionTemplate());

        try {
            service.refundRefuse(order.getOrderId(), "不同意");
            Assert.fail("Expected completed refund to stay terminal");
        } catch (CrmebException exception) {
            Assert.assertEquals("订单退款状态已变化，请刷新后重试", exception.getMessage());
        }
    }

    private StoreOrder mockOrder() {
        StoreOrder order = new StoreOrder();
        order.setId(3);
        order.setOrderId("atomic-mock-order");
        order.setUid(1);
        order.setPaid(true);
        order.setPayType(Constants.PAY_TYPE_MOCK);
        order.setRefundStatus(1);
        order.setRefundPrice(BigDecimal.ZERO);
        order.setPayPrice(BigDecimal.TEN);
        return order;
    }

    private StoreOrderRefundRequest mockRefundRequest() {
        StoreOrderRefundRequest request = new StoreOrderRefundRequest();
        request.setOrderNo("atomic-mock-order");
        request.setAmount(BigDecimal.TEN);
        return request;
    }

    private void assertRefundRejected(Integer refundStatus, String expectedMessage) {
        StoreOrder order = new StoreOrder();
        order.setOrderId("order-test");
        order.setPaid(true);
        order.setRefundStatus(refundStatus);
        order.setRefundPrice(BigDecimal.ZERO);
        order.setPayPrice(BigDecimal.TEN);

        StoreOrderDao dao = (StoreOrderDao) Proxy.newProxyInstance(
                StoreOrderDao.class.getClassLoader(),
                new Class<?>[]{StoreOrderDao.class},
                (proxy, method, args) -> "selectOne".equals(method.getName()) ? order : null
        );

        StoreOrderServiceImpl service = new StoreOrderServiceImpl();
        setField(service, "dao", dao);

        StoreOrderRefundRequest request = new StoreOrderRefundRequest();
        request.setOrderNo(order.getOrderId());
        request.setAmount(BigDecimal.ONE);

        try {
            service.refund(request);
            Assert.fail("Expected duplicate refund request to be rejected");
        } catch (CrmebException exception) {
            Assert.assertEquals(expectedMessage, exception.getMessage());
        }
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

    private FlowerRefundOutboxDao outboxDao(AtomicInteger inserted) {
        return (FlowerRefundOutboxDao) Proxy.newProxyInstance(
                FlowerRefundOutboxDao.class.getClassLoader(), new Class<?>[]{FlowerRefundOutboxDao.class},
                (proxy, method, args) -> {
                    if ("insertPending".equals(method.getName())) {
                        inserted.incrementAndGet();
                        return 1;
                    }
                    return null;
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
                throw new AssertionError("Unable to configure test fixture", exception);
            }
        }
        throw new AssertionError("Missing test fixture field: " + name);
    }
}
