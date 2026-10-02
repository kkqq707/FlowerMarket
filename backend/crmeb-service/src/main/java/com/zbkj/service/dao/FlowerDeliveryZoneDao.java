package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerDeliveryZone;
import org.apache.ibatis.annotations.Param;

public interface FlowerDeliveryZoneDao extends BaseMapper<FlowerDeliveryZone> {
    FlowerDeliveryZone findEnabledByAreaCode(@Param("areaCode") String areaCode);
}
