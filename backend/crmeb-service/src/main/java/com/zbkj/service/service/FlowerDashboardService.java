package com.zbkj.service.service;

import com.zbkj.common.response.FlowerDashboardResponse;
import java.util.List;

public interface FlowerDashboardService {
    FlowerDashboardResponse today();
    List<FlowerDashboardResponse.TrendItem> salesTrend(String startDate, String endDate);
    List<FlowerDashboardResponse.RankingItem> hotProducts(String startDate, String endDate, String sortBy);
}
