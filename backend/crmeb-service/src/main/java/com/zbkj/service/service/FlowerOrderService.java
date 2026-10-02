package com.zbkj.service.service;

import com.zbkj.common.enums.FlowerOrderStageEnum;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.UserAddress;
import com.zbkj.common.request.flower.FlowerOrderRequest;
import com.zbkj.common.request.FlowerOrderTransitRequest;
import com.zbkj.common.response.FlowerOrderDetailResponse;
import com.zbkj.common.response.FlowerOrderAdminListResponse;
import com.zbkj.common.request.flower.FlowerOrderAdminSearchRequest;
import com.zbkj.common.response.flower.FlowerDeliveryQuoteResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FlowerOrderService {
    void requireFlowerRequest(boolean containsFlowerProducts, FlowerOrderRequest request);
    FlowerDeliveryQuoteResponse validateForCreate(FlowerOrderRequest request, UserAddress address,
                                                   Integer currentUid, Integer shippingType, BigDecimal goodsAmount);
    FlowerOrderExt createExtension(StoreOrder order, FlowerOrderRequest request, UserAddress address);
    void appendProgress(String orderNo, FlowerOrderStageEnum stage, Integer operatorId,
                        String proofImage, String remark);
    void onPaySuccess(String orderNo);
    boolean transition(String orderNo, FlowerOrderTransitRequest request, Integer operatorId);
    FlowerOrderDetailResponse detail(String orderNo, Integer currentUid);
    boolean confirmDelivery(String orderNo, Integer currentUid);
    void validateCancellation(String orderNo);
    List<FlowerOrderAdminListResponse> adminList(FlowerOrderAdminSearchRequest request);
    FlowerOrderDetailResponse adminDetail(String orderNo);
}
