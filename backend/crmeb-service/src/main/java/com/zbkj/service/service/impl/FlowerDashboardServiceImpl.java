package com.zbkj.service.service.impl;

import com.zbkj.common.response.FlowerDashboardResponse;
import com.zbkj.common.response.FlowerDashboardResponse.OrderMetric;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.service.dao.FlowerDashboardDao;
import com.zbkj.service.service.FlowerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class FlowerDashboardServiceImpl implements FlowerDashboardService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final FlowerDashboardDao dashboardDao;
    private final Clock clock;

    @Autowired
    public FlowerDashboardServiceImpl(FlowerDashboardDao dashboardDao) {
        this(dashboardDao, Clock.system(SHANGHAI));
    }

    public FlowerDashboardServiceImpl(FlowerDashboardDao dashboardDao, Clock clock) {
        this.dashboardDao = dashboardDao;
        this.clock = clock;
    }

    @Override
    public FlowerDashboardResponse today() {
        ZoneId zone = SHANGHAI;
        LocalDate today = LocalDate.now(clock.withZone(zone));
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
        response.setHotProducts(safe(dashboardDao.selectHotProducts(start, end, "quantity")));
        response.setPopularSlots(safe(dashboardDao.selectPopularSlots(start, end)));
        return response;
    }

    @Override
    public List<FlowerDashboardResponse.TrendItem> salesTrend(String startDate, String endDate) {
        Date[] range = dateRange(startDate, endDate);
        return safe(dashboardDao.selectSalesTrend(range[0], range[1]));
    }

    @Override
    public List<FlowerDashboardResponse.RankingItem> hotProducts(String startDate, String endDate, String sortBy) {
        Date[] range = dateRange(startDate, endDate);
        if (!"quantity".equals(sortBy) && !"amount".equals(sortBy)) {
            throw new CrmebException("排序方式仅支持 quantity（销量）或 amount（销售额）");
        }
        return safe(dashboardDao.selectHotProducts(range[0], range[1], sortBy));
    }

    private Date[] dateRange(String startDate, String endDate) {
        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        if (start.isAfter(end)) throw new CrmebException("开始日期不能晚于结束日期");
        if (ChronoUnit.DAYS.between(start, end) >= 31) throw new CrmebException("日期范围最多支持31天（包含开始和结束日期）");
        if (end.isAfter(LocalDate.now(clock.withZone(SHANGHAI)))) throw new CrmebException("结束日期不能晚于今天");
        return new Date[]{Date.from(start.atStartOfDay(SHANGHAI).toInstant()),
                Date.from(end.plusDays(1).atStartOfDay(SHANGHAI).toInstant())};
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null || value.isEmpty()) throw new CrmebException(field + "不能为空");
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new CrmebException(field + "格式必须为 YYYY-MM-DD");
        }
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.getYear() < 1) throw new DateTimeParseException("invalid year", value, 0);
            return date;
        } catch (DateTimeParseException exception) {
            throw new CrmebException(field + "不是有效的日历日期");
        }
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private int zero(Integer value) { return value == null ? 0 : value; }
    private <T> List<T> safe(List<T> value) { return value == null ? Collections.emptyList() : value; }
}
