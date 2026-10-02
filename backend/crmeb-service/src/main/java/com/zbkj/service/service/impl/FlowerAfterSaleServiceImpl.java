package com.zbkj.service.service.impl;

import com.zbkj.common.enums.FlowerOrderStageEnum;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.flower.FlowerOrderProgress;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.OrderRefundApplyRequest;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.dao.FlowerOrderProgressDao;
import com.zbkj.service.service.FlowerAfterSaleService;
import com.zbkj.service.service.StoreOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Clock;

@Service
public class FlowerAfterSaleServiceImpl implements FlowerAfterSaleService {

    private static final long QUALITY_AFTER_SALE_WINDOW_MILLIS = 2L * 60L * 60L * 1000L;
    private static final Set<String> QUALITY_REASONS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("不新鲜", "花材不符", "包装破损", "配送超时")));

    private final FlowerOrderExtDao orderExtDao;
    private final FlowerOrderProgressDao progressDao;
    private final StoreOrderService storeOrderService;
    private final Clock clock;

    @Autowired
    public FlowerAfterSaleServiceImpl(FlowerOrderExtDao orderExtDao,
                                      FlowerOrderProgressDao progressDao,
                                      StoreOrderService storeOrderService) {
        this(orderExtDao, progressDao, storeOrderService, Clock.systemDefaultZone());
    }

    public FlowerAfterSaleServiceImpl(FlowerOrderExtDao orderExtDao,
                                      FlowerOrderProgressDao progressDao,
                                      StoreOrderService storeOrderService,
                                      Clock clock) {
        this.orderExtDao = orderExtDao;
        this.progressDao = progressDao;
        this.storeOrderService = storeOrderService;
        this.clock = clock;
    }

    @Override
    public void validateApply(OrderRefundApplyRequest request, Integer currentUid) {
        if (request == null || !StringUtils.hasText(request.getUni())) {
            throw new CrmebException("待退款订单不能为空");
        }
        FlowerOrderExt extension = orderExtDao.selectByOrderId(request.getUni());
        if (extension == null) {
            return;
        }
        StoreOrder order = storeOrderService.getByOderId(request.getUni());
        if (order == null) {
            throw new CrmebException("原订单不存在");
        }
        if (currentUid == null || !currentUid.equals(order.getUid())) {
            throw new CrmebException("无权操作该订单");
        }
        rejectDuplicateRefund(order.getRefundStatus());

        FlowerOrderStageEnum stage = parseStage(extension.getCurrentStage());
        if (stage.ordinal() < FlowerOrderStageEnum.PRODUCING.ordinal()) {
            return;
        }
        if (stage != FlowerOrderStageEnum.COMPLETED) {
            throw new CrmebException("订单已开始制作，请联系花店处理退款");
        }
        String reason = request.getText() == null ? "" : request.getText().trim();
        if (!QUALITY_REASONS.contains(reason)) {
            throw new CrmebException("请选择品质售后原因：不新鲜、花材不符、包装破损、配送超时");
        }
        Date completedAt = findCompletionTime(progressDao.selectByOrderId(request.getUni()));
        if (completedAt == null) {
            throw new CrmebException("未找到可靠的送达时间，请联系花店客服");
        }
        if (completedAt.after(Date.from(clock.instant()))) {
            throw new CrmebException("送达时间异常，请联系花店客服");
        }
        Date afterSaleDeadline = new Date(clock.millis() - QUALITY_AFTER_SALE_WINDOW_MILLIS);
        if (completedAt.before(afterSaleDeadline)) {
            throw new CrmebException("已超过送达后两小时售后时限，请联系花店客服");
        }
    }

    @Override
    public void validateAdminRefund(String orderNo) {
        if (!StringUtils.hasText(orderNo) || orderExtDao.selectByOrderId(orderNo) == null) {
            throw new CrmebException("花店退款权限不能处理普通订单");
        }
    }

    private void rejectDuplicateRefund(Integer refundStatus) {
        if (Integer.valueOf(1).equals(refundStatus)) {
            throw new CrmebException("正在申请退款中");
        }
        if (Integer.valueOf(2).equals(refundStatus)) {
            throw new CrmebException("订单已退款");
        }
        if (Integer.valueOf(3).equals(refundStatus)) {
            throw new CrmebException("订单退款中");
        }
    }

    private FlowerOrderStageEnum parseStage(String stage) {
        try {
            return FlowerOrderStageEnum.valueOf(stage);
        } catch (Exception exception) {
            throw new CrmebException("花店订单阶段无效");
        }
    }

    private Date findCompletionTime(List<FlowerOrderProgress> progressList) {
        if (progressList == null) {
            return null;
        }
        Date completedAt = null;
        for (FlowerOrderProgress progress : progressList) {
            if (progress != null && FlowerOrderStageEnum.COMPLETED.name().equals(progress.getStage())
                    && progress.getCreateTime() != null
                    && (completedAt == null || progress.getCreateTime().before(completedAt))) {
                completedAt = progress.getCreateTime();
            }
        }
        return completedAt;
    }
}
