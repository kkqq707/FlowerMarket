package com.zbkj.service.service.impl;

import com.zbkj.common.response.FlowerDashboardResponse;
import com.zbkj.common.response.FlowerDashboardResponse.OrderMetric;
import com.zbkj.service.dao.FlowerDashboardDao;
import com.zbkj.service.service.FlowerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class FlowerDashboardServiceImpl implements FlowerDashboardService {
    private final FlowerDashboardDao dashboardDao;
    private final Clock clock;

    @Autowired
    public FlowerDashboardServiceImpl(FlowerDashboardDao dashboardDao) {
        this(dashboardDao, Clock.system(ZoneId.of("Asia/Shanghai")));
    }

    public FlowerDashboardServiceImpl(FlowerDashboardDao dashboardDao, Clock clock) {
        this.dashboardDao = dashboardDao;
        this.clock = clock;
    }

    @Override
    public FlowerDashboardResponse today() {
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        Date start = Date.from(today.atStartOfDay(zone).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(zone).toInstant());
        Date trendStart = Date.from(today.minusDays(6).atStartOfDay(zone).toInstant());
        List<OrderMetric> records = safe(dashboardDao.selectOrderMetrics(start, end));

        BigDecimal sales = BigDecimal.ZERO;
        int orders = 0;
        int waitAccept = 0;
        int waitProduction = 0;
        int waitDelivery = 0;
        int completed = 0;
        int onTime = 0;
        for (OrderMetric record : records) {
            if (record == null || (record.getRefundStatus() != null && record.getRefundStatus() > 0)) continue;
            orders++;
            sales = sales.add(record.getPayPrice() == null ? BigDecimal.ZERO : record.getPayPrice());
            String stage = record.getCurrentStage();
            if ("WAIT_ACCEPT".equals(stage)) waitAccept++;
            if ("WAIT_PRODUCTION".equals(stage) || "PRODUCING".equals(stage)) waitProduction++;
            if ("WAIT_DELIVERY".equals(stage) || "DELIVERING".equals(stage)) waitDelivery++;
            if ("COMPLETED".equals(stage) && record.getCompletedTime() != null && record.getPromisedTime() != null) {
                completed++;
                if (!record.getCompletedTime().after(record.getPromisedTime())) onTime++;
            }
        }

        FlowerDashboardResponse response = new FlowerDashboardResponse();
        response.setTodaySales(money(sales));
        response.setTodayOrders(orders);
        response.setAverageOrderValue(orders == 0 ? money(BigDecimal.ZERO)
                : money(sales.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP)));
        response.setWaitAcceptCount(waitAccept);
        response.setWaitProductionCount(waitProduction);
        response.setWaitDeliveryCount(waitDelivery);
        response.setAfterSaleCount(zero(dashboardDao.countAfterSales()));
        response.setOnTimeRate(completed == 0 ? money(BigDecimal.ZERO)
                : money(BigDecimal.valueOf(onTime * 100L).divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP)));
        response.setSalesTrend(safe(dashboardDao.selectSalesTrend(trendStart, end)));
        response.setHotProducts(safe(dashboardDao.selectHotProducts(start, end)));
        response.setPopularSlots(safe(dashboardDao.selectPopularSlots(start, end)));
        return response;
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private int zero(Integer value) { return value == null ? 0 : value; }
    private <T> List<T> safe(List<T> value) { return value == null ? Collections.emptyList() : value; }
}
