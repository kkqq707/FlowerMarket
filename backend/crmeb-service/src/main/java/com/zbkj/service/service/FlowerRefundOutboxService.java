package com.zbkj.service.service;

public interface FlowerRefundOutboxService {
    void dispatchPending(int limit);
}
