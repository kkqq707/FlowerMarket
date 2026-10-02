package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerMaterialBatch;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface FlowerMaterialBatchDao extends BaseMapper<FlowerMaterialBatch> {
    List<FlowerMaterialBatch> eligibleForUpdate(@Param("materialId") Integer materialId,
                                                @Param("deliveryDate") Date deliveryDate);
    int reserve(@Param("batchId") Long batchId, @Param("quantity") Integer quantity);
    int release(@Param("batchId") Long batchId, @Param("quantity") Integer quantity);
    int consume(@Param("batchId") Long batchId, @Param("quantity") Integer quantity);
    int adjustCurrent(@Param("batchId") Long batchId, @Param("currentQty") Integer currentQty);
}
