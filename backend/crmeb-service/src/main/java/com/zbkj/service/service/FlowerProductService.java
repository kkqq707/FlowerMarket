package com.zbkj.service.service;

import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.request.flower.FlowerProductExtRequest;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import com.zbkj.common.response.flower.FlowerProductDetailResponse;

import java.util.List;

public interface FlowerProductService {
    FlowerProductExt saveOrUpdate(Integer productId, FlowerProductExtRequest request);
    FlowerProductDetailResponse getAdminDetail(Integer productId);
    FlowerProductDetailResponse getFrontDetail(Integer productId);
    List<FlowerProductCardResponse> search(FlowerProductSearchRequest request);
    List<FlowerProductCardResponse> searchAdmin(FlowerProductSearchRequest request);
    boolean containsFlowerProducts(List<Integer> productIds);
}
