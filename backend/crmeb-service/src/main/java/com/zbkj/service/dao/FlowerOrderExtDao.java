package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.response.FlowerOrderAdminListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface FlowerOrderExtDao extends BaseMapper<FlowerOrderExt> {
    FlowerOrderExt selectByOrderId(@Param("orderId") String orderId);
    FlowerOrderExt selectByOrderIdForUpdate(@Param("orderId") String orderId);
    int updateStage(@Param("orderId") String orderId,
                    @Param("targetStage") String targetStage,
                    @Param("courierName") String courierName,
                    @Param("courierPhone") String courierPhone,
                    @Param("deliveryRemark") String deliveryRemark);
    List<FlowerOrderExt> findExpiredReservations(@Param("now") Date now);
    int markReservationReleased(@Param("orderId") String orderId);
    int markPaidStage(@Param("orderId") String orderId);
    List<FlowerOrderAdminListResponse> selectAdminList(@Param("stage") String stage,
                                                       @Param("keyword") String keyword);
}
