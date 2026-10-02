package com.zbkj.service.dao;

import com.zbkj.common.response.FlowerDashboardResponse.OrderMetric;
import com.zbkj.common.response.FlowerDashboardResponse.RankingItem;
import com.zbkj.common.response.FlowerDashboardResponse.SlotItem;
import com.zbkj.common.response.FlowerDashboardResponse.TrendItem;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface FlowerDashboardDao {
    List<OrderMetric> selectOrderMetrics(@Param("start") Date start, @Param("end") Date end);
    Integer countAfterSales();
    List<TrendItem> selectSalesTrend(@Param("start") Date start, @Param("end") Date end);
    List<RankingItem> selectHotProducts(@Param("start") Date start, @Param("end") Date end);
    List<SlotItem> selectPopularSlots(@Param("start") Date start, @Param("end") Date end);
}
