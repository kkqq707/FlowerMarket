package com.zbkj.service.service;

import com.zbkj.common.model.flower.FlowerMaterial;
import com.zbkj.common.model.flower.FlowerMaterialBatch;
import com.zbkj.common.model.flower.FlowerRecipe;
import com.zbkj.common.model.order.StoreOrderInfo;

import java.util.Date;
import java.util.List;

public interface FlowerMaterialService {
    void reserve(String orderNo, List<StoreOrderInfo> items, Date deliveryDate);
    void release(String orderNo);
    void consume(String orderNo);
    List<FlowerMaterial> materials();
    FlowerMaterial saveMaterial(FlowerMaterial material);
    List<FlowerMaterialBatch> batches(Integer materialId);
    FlowerMaterialBatch receive(FlowerMaterialBatch batch);
    void adjust(Long batchId, Integer currentQty);
    List<FlowerRecipe> recipe(Integer productId);
    void replaceRecipe(Integer productId, List<FlowerRecipe> recipe);
    Integer suggestedDisplayDays(Integer productId);
}
