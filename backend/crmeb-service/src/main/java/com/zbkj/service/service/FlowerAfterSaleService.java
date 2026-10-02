package com.zbkj.service.service;

import com.zbkj.common.request.OrderRefundApplyRequest;

public interface FlowerAfterSaleService {
    void validateApply(OrderRefundApplyRequest request, Integer currentUid);

    void validateAdminRefund(String orderNo);
}
