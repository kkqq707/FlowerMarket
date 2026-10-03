package com.zbkj.service.service;

import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.request.flower.FlowerProductExtRequest;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import com.zbkj.common.response.flower.FlowerProductDetailResponse;
import com.zbkj.common.response.StoreProductTabsHeader;

import java.util.List;
import java.util.Map;

public interface FlowerProductService {
    FlowerProductExt saveOrUpdate(Integer productId, FlowerProductExtRequest request);
    FlowerProductDetailResponse getAdminDetail(Integer productId);
    FlowerProductDetailResponse getFrontDetail(Integer productId);
    List<FlowerProductCardResponse> search(FlowerProductSearchRequest request);
    List<FlowerProductCardResponse> searchAdmin(FlowerProductSearchRequest request);
    List<StoreProductTabsHeader> adminTabs(FlowerProductSearchRequest request);
    Map<String, List<String>> filterOptions(boolean onSale);
    boolean containsFlowerProducts(List<Integer> productIds);
}
