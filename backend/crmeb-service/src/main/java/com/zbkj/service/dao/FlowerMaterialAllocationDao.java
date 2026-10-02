package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerMaterialAllocation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowerMaterialAllocationDao extends BaseMapper<FlowerMaterialAllocation> {
    List<FlowerMaterialAllocation> selectByOrderForUpdate(@Param("orderNo") String orderNo);
    int changeStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);
}
