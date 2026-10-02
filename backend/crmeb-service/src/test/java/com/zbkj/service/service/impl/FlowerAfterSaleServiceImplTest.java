package com.zbkj.service.service.impl;

import com.zbkj.common.enums.FlowerOrderStageEnum;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.flower.FlowerOrderProgress;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.OrderRefundApplyRequest;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.dao.FlowerOrderProgressDao;
import com.zbkj.service.service.StoreOrderService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FlowerAfterSaleServiceImplTest {

    @Test
    public void nonFlowerOrderKeepsOriginalRefundFlow() {
        FlowerAfterSaleServiceImpl service = new FlowerAfterSaleServiceImpl(
                proxy(FlowerOrderExtDao.class, null),
                proxy(FlowerOrderProgressDao.class, null),
                proxy(StoreOrderService.class, null));

        service.validateApply(request("ordinary-order", "任意原退款原因"), 9);
    }

    @Test
    public void flowerRefundRejectsAnotherUsersOrder() {
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.WAIT_ACCEPT, 0, Collections.emptyList());

        assertRejected(() -> fixture.service.validateApply(request("flower-order", "临时有事"), 10),
                "无权操作该订单");
    }

    @Test
    public void flowerRefundRejectsExistingRefundStates() {
        assertRefundStateRejected(1, "正在申请退款中");
        assertRefundStateRejected(2, "订单已退款");
        assertRefundStateRejected(3, "订单退款中");
    }

    @Test
    public void flowerCancellationBeforeProductionUsesOriginalApprovalFlow() {
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.WAIT_PRODUCTION, 0, Collections.emptyList());

        fixture.service.validateApply(request("flower-order", "临时有事"), 9);
    }

    @Test
    public void customerCannotCancelAfterProductionStarts() {
        for (FlowerOrderStageEnum stage : Arrays.asList(
                FlowerOrderStageEnum.PRODUCING,
                FlowerOrderStageEnum.WAIT_DELIVERY,
                FlowerOrderStageEnum.DELIVERING)) {
            Fixture fixture = flowerFixture(stage, 0, Collections.emptyList());
            assertRejected(() -> fixture.service.validateApply(request("flower-order", "临时有事"), 9),
                    "订单已开始制作，请联系花店处理退款");
        }
    }

    @Test
    public void completedOrderAcceptsFixedQualityReasonsAndKeepsImages() {
        List<String> reasons = Arrays.asList("不新鲜", "花材不符", "包装破损", "配送超时");
        Instant now = Instant.parse("2026-09-15T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Date completedAt = Date.from(now.minusSeconds(60L * 60L));
        for (String reason : reasons) {
            Fixture fixture = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0,
                    Collections.singletonList(progress(FlowerOrderStageEnum.COMPLETED, completedAt)), clock);
            OrderRefundApplyRequest request = request("flower-order", reason)
                    .setReasonImage("/uploads/refund/one.jpg,/uploads/refund/two.jpg");

            fixture.service.validateApply(request, 9);

            Assert.assertEquals("/uploads/refund/one.jpg,/uploads/refund/two.jpg", request.getReasonImage());
        }
    }

    @Test
    public void completedOrderRejectsNonQualityReason() {
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0,
                Collections.singletonList(progress(FlowerOrderStageEnum.COMPLETED, new Date())));

        assertRejected(() -> fixture.service.validateApply(request("flower-order", "不想要了"), 9),
                "请选择品质售后原因：不新鲜、花材不符、包装破损、配送超时");
    }

    @Test
    public void qualityAfterSaleUsesServerCompletionTimeAndRejectsAfterTwoHours() {
        Instant now = Instant.parse("2026-09-15T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Date overBoundary = Date.from(now.minusMillis(2L * 60L * 60L * 1000L + 2000L));
        Fixture expired = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0,
                Collections.singletonList(progress(FlowerOrderStageEnum.COMPLETED, overBoundary)), clock);
        assertRejected(() -> expired.service.validateApply(request("flower-order", "不新鲜"), 9),
                "已超过送达后两小时售后时限，请联系花店客服");

        Date insideBoundary = Date.from(now.minusMillis(2L * 60L * 60L * 1000L - 2000L));
        Fixture allowed = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0,
                Collections.singletonList(progress(FlowerOrderStageEnum.COMPLETED, insideBoundary)), clock);
        allowed.service.validateApply(request("flower-order", "不新鲜"), 9);
    }

    @Test
    public void qualityAfterSaleAcceptsExactlyTwoHoursFromFirstCompletedProgress() {
        Instant now = Instant.parse("2026-09-15T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0, Arrays.asList(
                progress(FlowerOrderStageEnum.COMPLETED, Date.from(now.minusSeconds(2 * 60L * 60L))),
                progress(FlowerOrderStageEnum.COMPLETED, Date.from(now.minusSeconds(60L * 60L)))), clock);

        fixture.service.validateApply(request("flower-order", "不新鲜"), 9);
    }

    @Test
    public void qualityAfterSaleRejectsOneMillisecondPastTwoHours() {
        Instant now = Instant.parse("2026-09-15T10:00:00Z");
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0,
                Collections.singletonList(progress(FlowerOrderStageEnum.COMPLETED,
                        Date.from(now.minusMillis(2L * 60L * 60L * 1000L + 1L)))),
                Clock.fixed(now, ZoneOffset.UTC));

        assertRejected(() -> fixture.service.validateApply(request("flower-order", "不新鲜"), 9),
                "已超过送达后两小时售后时限，请联系花店客服");
    }

    @Test
    public void qualityAfterSaleUsesFirstCompletionAndRejectsFutureCompletion() {
        Instant now = Instant.parse("2026-09-15T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Fixture expiredFirstCompletion = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0, Arrays.asList(
                progress(FlowerOrderStageEnum.COMPLETED, Date.from(now.minusSeconds(2 * 60L * 60L + 1))),
                progress(FlowerOrderStageEnum.COMPLETED, Date.from(now.minusSeconds(60)))), clock);
        assertRejected(() -> expiredFirstCompletion.service.validateApply(request("flower-order", "不新鲜"), 9),
                "已超过送达后两小时售后时限，请联系花店客服");

        Fixture futureCompletion = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0,
                Collections.singletonList(progress(FlowerOrderStageEnum.COMPLETED, Date.from(now.plusSeconds(1)))), clock);
        assertRejected(() -> futureCompletion.service.validateApply(request("flower-order", "不新鲜"), 9),
                "送达时间异常，请联系花店客服");
    }

    @Test
    public void completedOrderWithoutServerCompletionProgressIsRejected() {
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.COMPLETED, 0, Collections.emptyList());

        assertRejected(() -> fixture.service.validateApply(request("flower-order", "不新鲜"), 9),
                "未找到可靠的送达时间，请联系花店客服");
    }

    @Test
    public void flowerAdminRefundPermissionCannotRefundOrdinaryOrder() {
        FlowerAfterSaleServiceImpl service = new FlowerAfterSaleServiceImpl(
                proxy(FlowerOrderExtDao.class, null),
                proxy(FlowerOrderProgressDao.class, null),
                proxy(StoreOrderService.class, null));

        assertRejected(() -> service.validateAdminRefund("ordinary-order"),
                "花店退款权限不能处理普通订单");
    }

    @Test
    public void flowerAdminRefundAllowsManualDecisionAfterProduction() {
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.PRODUCING, 1, Collections.emptyList());

        fixture.service.validateAdminRefund("flower-order");
    }

    private void assertRefundStateRejected(int refundStatus, String message) {
        Fixture fixture = flowerFixture(FlowerOrderStageEnum.WAIT_ACCEPT, refundStatus, Collections.emptyList());
        assertRejected(() -> fixture.service.validateApply(request("flower-order", "临时有事"), 9), message);
    }

    private Fixture flowerFixture(FlowerOrderStageEnum stage, int refundStatus,
                                  List<FlowerOrderProgress> progressList) {
        return flowerFixture(stage, refundStatus, progressList, Clock.systemUTC());
    }

    private Fixture flowerFixture(FlowerOrderStageEnum stage, int refundStatus,
                                  List<FlowerOrderProgress> progressList, Clock clock) {
        FlowerOrderExt extension = new FlowerOrderExt()
                .setOrderId("flower-order")
                .setCurrentStage(stage.name());
        StoreOrder order = new StoreOrder();
        order.setOrderId("flower-order");
        order.setUid(9);
        order.setRefundStatus(refundStatus);

        FlowerOrderExtDao extDao = (FlowerOrderExtDao) Proxy.newProxyInstance(
                FlowerOrderExtDao.class.getClassLoader(), new Class<?>[]{FlowerOrderExtDao.class},
                (proxy, method, args) -> method.getName().startsWith("selectByOrderId") ? extension : null);
        FlowerOrderProgressDao progressDao = (FlowerOrderProgressDao) Proxy.newProxyInstance(
                FlowerOrderProgressDao.class.getClassLoader(), new Class<?>[]{FlowerOrderProgressDao.class},
                (proxy, method, args) -> "selectByOrderId".equals(method.getName()) ? progressList : null);
        StoreOrderService orderService = (StoreOrderService) Proxy.newProxyInstance(
                StoreOrderService.class.getClassLoader(), new Class<?>[]{StoreOrderService.class},
                (proxy, method, args) -> "getByOderId".equals(method.getName()) ? order : null);
        return new Fixture(new FlowerAfterSaleServiceImpl(extDao, progressDao, orderService, clock));
    }

    private FlowerOrderProgress progress(FlowerOrderStageEnum stage, Date createTime) {
        return new FlowerOrderProgress().setOrderId("flower-order").setStage(stage.name()).setCreateTime(createTime);
    }

    private void assertRejected(Runnable action, String expectedMessage) {
        try {
            action.run();
            Assert.fail("Expected flower after-sale request to be rejected");
        } catch (CrmebException exception) {
            Assert.assertEquals(expectedMessage, exception.getMessage());
        }
    }

    private OrderRefundApplyRequest request(String orderNo, String reason) {
        return new OrderRefundApplyRequest().setUni(orderNo).setText(reason);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Object returnValue) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> returnValue);
    }

    private static class Fixture {
        private final FlowerAfterSaleServiceImpl service;

        private Fixture(FlowerAfterSaleServiceImpl service) {
            this.service = service;
        }
    }
}
