package com.zbkj.service.service;

import com.zbkj.common.response.OrderPayResultResponse;

public interface FlowerMockPayService {
    OrderPayResultResponse pay(String orderNo);
}
