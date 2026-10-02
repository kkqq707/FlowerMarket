package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerOrderProgress;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowerOrderProgressDao extends BaseMapper<FlowerOrderProgress> {
    List<FlowerOrderProgress> selectByOrderId(@Param("orderId") String orderId);
}
