package com.zbkj.common.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class FlowerDashboardResponse {
    private BigDecimal todaySales;
    private Integer todayOrders;
    private BigDecimal averageOrderValue;
    private Integer waitAcceptCount;
    private Integer waitProductionCount;
    private Integer waitDeliveryCount;
    private Integer afterSaleCount;
    private BigDecimal onTimeRate;
    private List<TrendItem> salesTrend;
    private List<RankingItem> hotProducts;
    private List<SlotItem> popularSlots;

    @Data
    @Accessors(chain = true)
    public static class OrderMetric {
        private BigDecimal payPrice;
        private Integer refundStatus;
        private String currentStage;
        private Date promisedTime;
        private Date completedTime;
    }

    @Data
    public static class TrendItem {
        private String label;
        private BigDecimal amount;
        private Integer orderCount;
    }

    @Data
    public static class RankingItem {
        private Integer productId;
        private String name;
        private String image;
        private Integer quantity;
        private BigDecimal amount;
    }

    @Data
    public static class SlotItem {
        private String label;
        private Integer orderCount;
    }
}
