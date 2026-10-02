package com.zbkj.service.service;

import com.zbkj.common.model.flower.FlowerDeliverySlot;
import com.zbkj.common.model.flower.FlowerDeliveryZone;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.flower.FlowerDeliverySlotRequest;
import com.zbkj.common.request.flower.FlowerDeliveryZoneRequest;
import com.zbkj.common.response.flower.FlowerDeliveryQuoteResponse;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface FlowerDeliveryService {
    FlowerDeliveryQuoteResponse quote(Integer addressId, BigDecimal goodsAmount);
    List<FlowerDeliverySlot> availableSlots(Integer zoneId, Date startDate, Date endDate);
    FlowerDeliverySlot validateSelection(Integer zoneId, Date deliveryDate, Integer slotId);
    void reserve(Integer slotId);
    void release(String orderNo);
    List<FlowerDeliveryZone> zoneList(PageParamRequest pageRequest);
    List<FlowerDeliverySlot> slotList(PageParamRequest pageRequest);
    FlowerDeliveryZone saveZone(FlowerDeliveryZoneRequest request);
    FlowerDeliverySlot saveSlot(FlowerDeliverySlotRequest request);
    void setZoneEnabled(Integer id, Boolean enabled);
    void setSlotEnabled(Integer id, Boolean enabled);
}
