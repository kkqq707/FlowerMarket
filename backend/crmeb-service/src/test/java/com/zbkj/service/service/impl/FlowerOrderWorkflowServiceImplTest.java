package com.zbkj.service.service.impl;

import com.zbkj.common.enums.FlowerOrderStageEnum;
import com.zbkj.common.enums.ExpiredReservationCancellationResult;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.flower.FlowerOrderProgress;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.FlowerOrderTransitRequest;
import com.zbkj.common.request.StoreOrderSendRequest;
import com.zbkj.common.response.FlowerOrderDetailResponse;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.dao.FlowerOrderProgressDao;
import com.zbkj.service.delete.OrderUtils;
import com.zbkj.service.service.FlowerDeliveryService;
import com.zbkj.service.service.FlowerOrderService;
import com.zbkj.service.service.OrderService;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.UserService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowerOrderWorkflowServiceImplTest {

    @Test
    public void completeSevenStageWorkflowLocksAndPersistsEveryTransition() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_PAY, true);
        fixture.progress.add(progress(FlowerOrderStageEnum.WAIT_PAY, "", "订单创建"));

        fixture.service.onPaySuccess(fixture.orderNo);
        fixture.transition(FlowerOrderStageEnum.WAIT_PRODUCTION, "接单", "", "", "");
        fixture.transition(FlowerOrderStageEnum.PRODUCING, "开始制作", "", "", "");
        fixture.transition(FlowerOrderStageEnum.WAIT_DELIVERY, "制作完成", "product.jpg", "", "");
        fixture.transition(FlowerOrderStageEnum.DELIVERING, "开始配送", "", "王师傅", "13800138000");
        fixture.transition(FlowerOrderStageEnum.COMPLETED, "已送达", "delivery.jpg", "", "");

        Assert.assertEquals(FlowerOrderStageEnum.COMPLETED.name(), fixture.extension.getCurrentStage());
        Assert.assertEquals(Arrays.asList("WAIT_PAY", "WAIT_ACCEPT", "WAIT_PRODUCTION", "PRODUCING",
                "WAIT_DELIVERY", "DELIVERING", "COMPLETED"), fixture.stageNames());
        Assert.assertEquals(5, fixture.lockCount.get());
        Assert.assertEquals(5, fixture.stageUpdateCount.get());
        Assert.assertEquals(1, fixture.sendCount.get());
        Assert.assertEquals(Integer.valueOf(2), fixture.order.getStatus());
        Assert.assertEquals("delivery.jpg", fixture.progress.get(6).getProofImage());
    }

    @Test
    public void rejectsSkippingAStage() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_ACCEPT, true);

        assertCrmebException(() -> fixture.transition(FlowerOrderStageEnum.PRODUCING,
                "越级制作", "", "", ""), "订单阶段不允许跳转");

        Assert.assertEquals(0, fixture.stageUpdateCount.get());
    }

    @Test
    public void repeatedTransitionIsIdempotent() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_PRODUCTION, true);

        Assert.assertTrue(fixture.transition(FlowerOrderStageEnum.WAIT_PRODUCTION,
                "重复接单", "", "", ""));

        Assert.assertEquals(1, fixture.lockCount.get());
        Assert.assertEquals(0, fixture.stageUpdateCount.get());
        Assert.assertTrue(fixture.progress.isEmpty());
        Assert.assertEquals(0, fixture.sendCount.get());
    }

    @Test
    public void nonFlowerOrderIsSkipped() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_ACCEPT, true);
        fixture.extension = null;

        Assert.assertFalse(fixture.transition(FlowerOrderStageEnum.WAIT_PRODUCTION,
                "接单", "", "", ""));
        Assert.assertEquals(0, fixture.stageUpdateCount.get());
    }

    @Test
    public void unpaidOrderCannotBeAccepted() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_ACCEPT, false);

        assertCrmebException(() -> fixture.transition(FlowerOrderStageEnum.WAIT_PRODUCTION,
                "接单", "", "", ""), "未支付订单不能接单");

        Assert.assertEquals(0, fixture.stageUpdateCount.get());
    }

    @Test
    public void unfinishedBouquetCannotBeDelivered() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.PRODUCING, true);

        assertCrmebException(() -> fixture.transition(FlowerOrderStageEnum.DELIVERING,
                "配送", "", "王师傅", "13800138000"), "订单阶段不允许跳转");

        Assert.assertEquals(0, fixture.sendCount.get());
    }

    @Test
    public void detailRequiresOwnerAndContainsProgressPhotosAndDeliveryInformation() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.COMPLETED, true);
        fixture.extension.setSenderName("张同学").setSenderPhone("13800138000")
                .setRecipientName("李同学").setRecipientPhone("13900139000")
                .setDeliveryAddressSnapshot("花城大道1号").setDeliveryDate(new Date(1789488000000L))
                .setSlotId(9).setCourierName("王师傅").setCourierPhone("13700137000")
                .setGreetingCard("愿今天明亮如花").setAnonymous(true).setContactRecipient(false)
                .setSubstitutionPolicy("CONTACT_FIRST").setDeliveryRemark("已放前台");
        fixture.progress.add(progress(FlowerOrderStageEnum.WAIT_DELIVERY, "product.jpg", "制作完成"));
        fixture.progress.add(progress(FlowerOrderStageEnum.COMPLETED, "delivery.jpg", "已送达"));

        FlowerOrderDetailResponse detail = fixture.service.detail(fixture.orderNo, 7);

        Assert.assertEquals("李同学", detail.getRecipientName());
        Assert.assertEquals("花城大道1号", detail.getDeliveryAddress());
        Assert.assertEquals(Integer.valueOf(9), detail.getSlotId());
        Assert.assertEquals(FlowerOrderStageEnum.COMPLETED.name(), detail.getCurrentStage());
        Assert.assertEquals("product.jpg", detail.getFinishedProductImage());
        Assert.assertEquals("delivery.jpg", detail.getDeliveryProofImage());
        Assert.assertEquals("愿今天明亮如花", detail.getGreetingCard());
        Assert.assertTrue(detail.getAnonymous());
        Assert.assertFalse(detail.getContactRecipient());
        Assert.assertEquals("CONTACT_FIRST", detail.getSubstitutionPolicy());
        Assert.assertEquals("已放前台", detail.getDeliveryRemark());
        Assert.assertNotNull(detail.getCompletedTime());
        Assert.assertEquals(2, detail.getProgressList().size());
        assertCrmebException(() -> fixture.service.detail(fixture.orderNo, 8), "无权查看该订单");
    }

    @Test
    public void adminDetailIncludesCommercialSummaryWithoutCustomerOwnershipCheck() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_PRODUCTION, true);
        fixture.order.setPayPrice(new BigDecimal("188.00")).setTotalNum(2);
        fixture.extension.setRecipientName("李同学").setRecipientPhone("13900139000");

        FlowerOrderDetailResponse detail = fixture.service.adminDetail(fixture.orderNo);

        Assert.assertEquals(new BigDecimal("188.00"), detail.getPayPrice());
        Assert.assertEquals(Integer.valueOf(2), detail.getTotalNum());
        Assert.assertEquals("李同学", detail.getRecipientName());
        Assert.assertNotNull(detail.getProducts());
    }

    @Test
    public void customerConfirmationRequiresOwnerAndCompletesIdempotently() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.DELIVERING, true);

        Assert.assertTrue(fixture.service.confirmDelivery(fixture.orderNo, 7));
        Assert.assertTrue(fixture.service.confirmDelivery(fixture.orderNo, 7));

        Assert.assertEquals(1, fixture.stageUpdateCount.get());
        Assert.assertEquals(1, fixture.progress.size());
        Assert.assertEquals(Integer.valueOf(2), fixture.order.getStatus());

        WorkflowFixture another = new WorkflowFixture(FlowerOrderStageEnum.DELIVERING, true);
        assertCrmebException(() -> another.service.confirmDelivery(another.orderNo, 8), "无权操作该订单");
    }

    @Test
    public void customerCancellationIsRejectedAfterProductionStarts() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.PRODUCING, true);

        assertCrmebException(() -> fixture.service.validateCancellation(fixture.orderNo),
                "订单已开始制作，请联系花店取消");

        fixture.extension.setCurrentStage(FlowerOrderStageEnum.WAIT_PRODUCTION.name());
        fixture.service.validateCancellation(fixture.orderNo);
    }

    @Test
    public void cancellationValidationLocksTheExtensionBeforeCheckingItsStage() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_PRODUCTION, true);

        fixture.service.validateCancellation(fixture.orderNo);

        Assert.assertEquals(1, fixture.lockCount.get());
    }

    @Test
    public void productionTransitionRejectsAnOrderCancelledWhileWaitingForTheExtensionLock() {
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.WAIT_PRODUCTION, true);
        fixture.order.setIsDel(true).setIsSystemDel(true);

        assertCrmebException(() -> fixture.transition(FlowerOrderStageEnum.PRODUCING,
                "开始制作", "", "", ""), "订单已取消");

        Assert.assertEquals(0, fixture.stageUpdateCount.get());
    }

    @Test
    public void completingFlowerOrderUsesTheOriginalCompletionContractOnlyOnce() {
        AtomicInteger completions = new AtomicInteger();
        WorkflowFixture fixture = new WorkflowFixture(FlowerOrderStageEnum.DELIVERING, true,
                proxy(OrderService.class, (o, m, a) -> {
                    if ("completeFlowerOrder".equals(m.getName())) {
                        completions.incrementAndGet();
                        return true;
                    }
                    return defaultValue(m.getReturnType());
                }));

        Assert.assertTrue(fixture.transition(FlowerOrderStageEnum.COMPLETED, "送达", "proof.jpg", "", ""));
        Assert.assertTrue(fixture.transition(FlowerOrderStageEnum.COMPLETED, "重复送达", "", "", ""));

        Assert.assertEquals(1, completions.get());
        Assert.assertEquals(0, fixture.originalOrderUpdateCount.get());
    }

    @Test
    public void successfulOriginalOrderCancellationReleasesFlowerReservation() {
        StoreOrder order = new StoreOrder().setId(21).setOrderId("F-CANCEL").setUid(7)
                .setIsDel(false).setIsSystemDel(false);
        StoreOrderService storeOrderService = proxy(StoreOrderService.class, (o, m, a) -> {
            if ("getById".equals(m.getName())) return order;
            if ("updateById".equals(m.getName())) return true;
            return defaultValue(m.getReturnType());
        });
        UserService userService = proxy(UserService.class, (o, m, a) ->
                "getUserIdException".equals(m.getName()) ? 7 : defaultValue(m.getReturnType()));
        OrderUtils orderUtils = new OrderUtils();
        setField(orderUtils, "storeOrderService", storeOrderService);
        setField(orderUtils, "userService", userService);
        AtomicInteger releases = new AtomicInteger();
        FlowerDeliveryService deliveryService = proxy(FlowerDeliveryService.class, (o, m, a) -> {
            if ("release".equals(m.getName())) releases.incrementAndGet();
            return defaultValue(m.getReturnType());
        });
        FlowerOrderService flowerOrderService = proxy(FlowerOrderService.class,
                (o, m, a) -> defaultValue(m.getReturnType()));
        RedisUtil redisUtil = new RedisUtil(null, null) {
            @Override
            public boolean lPush(String key, Object value) {
                return true;
            }
        };
        OrderServiceImpl service = new OrderServiceImpl();
        setField(service, "orderUtils", orderUtils);
        setField(service, "storeOrderService", storeOrderService);
        setField(service, "flowerDeliveryService", deliveryService);
        setField(service, "flowerOrderService", flowerOrderService);
        setField(service, "redisUtil", redisUtil);

        Assert.assertTrue(service.cancel(21));

        Assert.assertEquals(1, releases.get());
    }

    @Test
    public void expiredReservationCancellationDoesNotWinAfterPaymentWinsTheRace() {
        StoreOrder staleOrder = new StoreOrder().setId(21).setOrderId("F-EXPIRE-PAY")
                .setPaid(false).setIsDel(false).setIsSystemDel(false);
        AtomicInteger entityUpdates = new AtomicInteger();
        AtomicInteger conditionalUpdates = new AtomicInteger();
        StoreOrderService storeOrderService = proxy(StoreOrderService.class, (o, m, a) -> {
            if ("getByOderId".equals(m.getName())) return staleOrder;
            if ("updateById".equals(m.getName())) {
                entityUpdates.incrementAndGet();
                staleOrder.setPaid(true);
                return true;
            }
            if ("cancelExpiredReservationAtomically".equals(m.getName())) {
                conditionalUpdates.incrementAndGet();
                staleOrder.setPaid(true);
                return false;
            }
            return defaultValue(m.getReturnType());
        });
        OrderServiceImpl service = expiredCancellationService(storeOrderService);

        Assert.assertFalse(service.cancelExpiredReservation(staleOrder.getOrderId()));

        Assert.assertEquals(1, conditionalUpdates.get());
        Assert.assertEquals(0, entityUpdates.get());
        Assert.assertTrue(staleOrder.getPaid());
    }

    @Test
    public void expiredReservationCancellationReleasesOnlyWhenItsConditionalUpdateWins() {
        StoreOrder order = new StoreOrder().setId(21).setOrderId("F-EXPIRE-CANCEL")
                .setPaid(false).setIsDel(false).setIsSystemDel(false);
        AtomicInteger conditionalUpdates = new AtomicInteger();
        StoreOrderService storeOrderService = proxy(StoreOrderService.class, (o, m, a) -> {
            if ("cancelExpiredReservationAtomically".equals(m.getName())) {
                conditionalUpdates.incrementAndGet();
                order.setIsDel(true).setIsSystemDel(true);
                return true;
            }
            if ("updateById".equals(m.getName())) Assert.fail("expired cancellation must be conditional");
            return defaultValue(m.getReturnType());
        });
        OrderServiceImpl service = expiredCancellationService(storeOrderService);

        Assert.assertTrue(service.cancelExpiredReservation(order.getOrderId()));
        Assert.assertEquals(1, conditionalUpdates.get());
    }

    @Test
    public void alreadyCancelledExpiredReservationIsClassifiedForRecoveryWithoutRepeatingCancellationWork() {
        AtomicInteger atomicCancels = new AtomicInteger();
        AtomicInteger cancellationQueueReads = new AtomicInteger();
        StoreOrderService storeOrderService = proxy(StoreOrderService.class, (o, m, a) -> {
            if ("cancelExpiredReservationAtomically".equals(m.getName())) {
                atomicCancels.incrementAndGet();
                return false;
            }
            if ("hasRecoverableExpiredReservation".equals(m.getName())) return true;
            if ("getByOderId".equals(m.getName())) {
                cancellationQueueReads.incrementAndGet();
                return null;
            }
            return defaultValue(m.getReturnType());
        });
        OrderServiceImpl service = expiredCancellationService(storeOrderService);

        Assert.assertEquals(ExpiredReservationCancellationResult.RECOVERABLE_RELEASE,
                service.classifyExpiredReservationCancellation("F-RECOVER"));
        Assert.assertEquals(1, atomicCancels.get());
        Assert.assertEquals(0, cancellationQueueReads.get());
    }

    @Test
    public void expiredReservationSqlGuardsPaymentDeletionStageExpiryAndReleaseState() throws Exception {
        InputStream stream = FlowerOrderWorkflowServiceImplTest.class.getClassLoader()
                .getResourceAsStream("mapper/store/StoreOrderMapper.xml");
        Assert.assertNotNull(stream);
        String mapper = new String(readAll(stream), StandardCharsets.UTF_8);
        int start = mapper.indexOf("<update id=\"cancelExpiredFlowerReservation\">");
        int end = mapper.indexOf("</update>", start);
        String statement = mapper.substring(start, end);

        Assert.assertTrue(statement.contains("original_order.paid = 0"));
        Assert.assertTrue(statement.contains("original_order.is_del = 0"));
        Assert.assertTrue(statement.contains("original_order.is_system_del = 0"));
        Assert.assertTrue(statement.contains("ext.current_stage = 'WAIT_PAY'"));
        Assert.assertTrue(statement.contains("ext.reservation_expire_time &lt; NOW()"));
        Assert.assertTrue(statement.contains("ext.reservation_released = 0"));

        int recoveryStart = mapper.indexOf("<select id=\"findRecoverableExpiredFlowerReservation\"");
        int recoveryEnd = mapper.indexOf("</select>", recoveryStart);
        String recoveryStatement = mapper.substring(recoveryStart, recoveryEnd);
        Assert.assertTrue(recoveryStatement.contains("original_order.paid = 0"));
        Assert.assertTrue(recoveryStatement.contains("original_order.is_del = 1"));
        Assert.assertTrue(recoveryStatement.contains("original_order.is_system_del = 1"));
        Assert.assertTrue(recoveryStatement.contains("ext.current_stage = 'WAIT_PAY'"));
        Assert.assertTrue(recoveryStatement.contains("ext.reservation_expire_time &lt; NOW()"));
        Assert.assertTrue(recoveryStatement.contains("ext.reservation_released = 0"));
    }

    private static byte[] readAll(InputStream stream) throws java.io.IOException {
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toByteArray();
        } finally {
            stream.close();
        }
    }

    private static OrderServiceImpl expiredCancellationService(StoreOrderService storeOrderService) {
        OrderServiceImpl service = new OrderServiceImpl();
        setField(service, "storeOrderService", storeOrderService);
        setField(service, "redisUtil", new RedisUtil(null, null) {
            @Override
            public boolean lPush(String key, Object value) {
                return true;
            }
        });
        return service;
    }

    private static FlowerOrderProgress progress(FlowerOrderStageEnum stage, String proofImage, String remark) {
        return new FlowerOrderProgress().setOrderId("F20260915001").setStage(stage.name())
                .setProofImage(proofImage).setRemark(remark).setCreateTime(new Date());
    }

    private static void assertCrmebException(Runnable action, String message) {
        try {
            action.run();
            Assert.fail("expected CrmebException");
        } catch (CrmebException e) {
            Assert.assertEquals(message, e.getMessage());
        }
    }

    private static class WorkflowFixture {
        private final String orderNo = "F20260915001";
        private FlowerOrderExt extension;
        private final StoreOrder order;
        private final List<FlowerOrderProgress> progress = new ArrayList<>();
        private final AtomicInteger lockCount = new AtomicInteger();
        private final AtomicInteger stageUpdateCount = new AtomicInteger();
        private final AtomicInteger sendCount = new AtomicInteger();
        private final AtomicInteger originalOrderUpdateCount = new AtomicInteger();
        private final FlowerOrderServiceImpl service;

        private WorkflowFixture(FlowerOrderStageEnum stage, boolean paid) {
            this(stage, paid, null);
        }

        private WorkflowFixture(FlowerOrderStageEnum stage, boolean paid, OrderService originalOrderService) {
            extension = new FlowerOrderExt().setOrderId(orderNo).setCurrentStage(stage.name())
                    .setReservationReleased(false);
            order = new StoreOrder().setId(21).setOrderId(orderNo).setUid(7).setPaid(paid)
                    .setStatus(0).setIsDel(false).setIsSystemDel(false);
            FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (o, m, a) -> {
                if ("selectByOrderIdForUpdate".equals(m.getName())) {
                    lockCount.incrementAndGet();
                    return extension;
                }
                if ("selectByOrderId".equals(m.getName())) return extension;
                if ("markPaidStage".equals(m.getName())) {
                    if (extension != null && FlowerOrderStageEnum.WAIT_PAY.name().equals(extension.getCurrentStage())) {
                        extension.setCurrentStage(FlowerOrderStageEnum.WAIT_ACCEPT.name());
                        return 1;
                    }
                    return 0;
                }
                if ("updateStage".equals(m.getName())) {
                    stageUpdateCount.incrementAndGet();
                    extension.setCurrentStage((String) a[1]);
                    if (a[2] != null) extension.setCourierName((String) a[2]);
                    if (a[3] != null) extension.setCourierPhone((String) a[3]);
                    if (a[4] != null) extension.setDeliveryRemark((String) a[4]);
                    return 1;
                }
                return defaultValue(m.getReturnType());
            });
            FlowerOrderProgressDao progressDao = proxy(FlowerOrderProgressDao.class, (o, m, a) -> {
                if ("insert".equals(m.getName())) {
                    progress.add((FlowerOrderProgress) a[0]);
                    return 1;
                }
                if ("selectByOrderId".equals(m.getName())) return new ArrayList<>(progress);
                return defaultValue(m.getReturnType());
            });
            StoreOrderService storeOrderService = proxy(StoreOrderService.class, (o, m, a) -> {
                if ("getByOderId".equals(m.getName())) return order;
                if ("send".equals(m.getName())) {
                    StoreOrderSendRequest request = (StoreOrderSendRequest) a[0];
                    sendCount.incrementAndGet();
                    order.setStatus(1).setDeliveryType(request.getDeliveryType())
                            .setDeliveryName(request.getDeliveryName()).setDeliveryId(request.getDeliveryTel());
                    return "";
                }
                if ("updateById".equals(m.getName())) {
                    originalOrderUpdateCount.incrementAndGet();
                    return true;
                }
                return defaultValue(m.getReturnType());
            });
            service = new FlowerOrderServiceImpl(extDao, progressDao,
                    proxy(FlowerDeliveryService.class, (o, m, a) -> defaultValue(m.getReturnType())),
                    storeOrderService, originalOrderService);
        }

        private boolean transition(FlowerOrderStageEnum target, String remark, String proofImage,
                                   String courierName, String courierPhone) {
            FlowerOrderTransitRequest request = new FlowerOrderTransitRequest();
            request.setTargetStage(target);
            request.setRemark(remark);
            request.setProofImage(proofImage);
            request.setCourierName(courierName);
            request.setCourierPhone(courierPhone);
            return service.transition(orderNo, request, 99);
        }

        private List<String> stageNames() {
            List<String> names = new ArrayList<>();
            for (FlowerOrderProgress item : progress) names.add(item.getStage());
            return names;
        }
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

    private static void setField(Object target, String name, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("field not found: " + name);
    }
}
