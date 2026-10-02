package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerDeliverySlot;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface FlowerDeliverySlotDao extends BaseMapper<FlowerDeliverySlot> {
    int reserve(@Param("slotId") Integer slotId);
    int release(@Param("slotId") Integer slotId);
    List<FlowerDeliverySlot> findAvailable(@Param("startDate") Date startDate,
                                           @Param("endDate") Date endDate);
    int updateAdminFields(@Param("slot") FlowerDeliverySlot slot);
    int updateEnabled(@Param("slotId") Integer slotId, @Param("enabled") Boolean enabled);
}
