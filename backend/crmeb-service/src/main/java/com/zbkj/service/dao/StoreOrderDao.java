package com.zbkj.service.dao;

import com.zbkj.common.model.order.StoreOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.request.StoreDateRangeSqlPram;
import com.zbkj.common.response.OrderBrokerageData;
import com.zbkj.common.response.StoreOrderStatisticsChartItemResponse;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单表 Mapper 接口
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2024 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
public interface StoreOrderDao extends BaseMapper<StoreOrder> {

    /**
     * 订单总金额
     */
    BigDecimal getTotalPrice(String where);

    /**
     * 退款总金额
     */
    BigDecimal getRefundPrice(String where);

    /**
     * 获取退款总单数
     */
    Integer getRefundTotal(String where);

    List<StoreOrder> findFrontList(Map<String, Object> searchMap);

    /**
     * 订单统计详情 price
     * @param pram 时间区间参数
     * @return 月数据
     */
    List<StoreOrderStatisticsChartItemResponse> getOrderStatisticsPriceDetail(StoreDateRangeSqlPram pram);

    /**
     * 订单统计详情 订单量
     * @param pram 时间区间参数
     * @return 月数据
     */
    List<StoreOrderStatisticsChartItemResponse> getOrderStatisticsOrderCountDetail(StoreDateRangeSqlPram pram);

    /**
     * 获取佣金相关数据
     * @param uid 用户uid
     * @param spreadId 推广人uid
     */
    OrderBrokerageData getBrokerageData(@Param("uid") Integer uid, @Param("spreadId") Integer spreadId);

    Integer getAdminOrderCount(HashMap<String, Object> map);

    List<StoreOrder> getAdminOrderList(Map<String, Object> map);

    /** Atomically cancel an expired, unpaid flower reservation. */
    int cancelExpiredFlowerReservation(@Param("orderNo") String orderNo);

    /** True only after an unpaid flower order has already been safely cancelled but not released. */
    int findRecoverableExpiredFlowerReservation(@Param("orderNo") String orderNo);

    /** Atomically move an active delivered order to awaiting-review. */
    int completeFlowerOrder(@Param("orderNo") String orderNo);

    /** Atomically complete a mock-payment refund only once. */
    int completeMockRefund(@Param("orderId") Integer orderId,
                           @Param("refundPrice") BigDecimal refundPrice);

    int refuseFlowerRefund(@Param("orderId") Integer orderId, @Param("reason") String reason);

    int applyFlowerRefund(@Param("order") StoreOrder order);
}
