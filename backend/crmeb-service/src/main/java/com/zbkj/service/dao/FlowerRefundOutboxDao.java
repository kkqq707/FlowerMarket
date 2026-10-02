package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerRefundOutbox;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowerRefundOutboxDao extends BaseMapper<FlowerRefundOutbox> {
    int insertPending(@Param("orderId") Integer orderId);
    List<Integer> findPendingIds(@Param("limit") Integer limit);
    FlowerRefundOutbox selectForUpdate(@Param("id") Integer id);
    int markDone(@Param("id") Integer id);
}
