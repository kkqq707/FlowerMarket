package com.zbkj.service.service.impl;

import com.zbkj.common.response.FlowerDashboardResponse;
import com.zbkj.common.response.FlowerDashboardResponse.OrderMetric;
import com.zbkj.service.dao.FlowerDashboardDao;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FlowerDashboardServiceImplTest {

    @Test
    public void dashboardExcludesRefundsAndCalculatesOperationalCounts() {
        Date promise = Date.from(Instant.parse("2026-09-22T10:00:00Z"));
        OrderMetric paid = metric("88.00", 0, "WAIT_ACCEPT", null, null);
        OrderMetric refunded = metric("199.00", 2, "WAIT_PRODUCTION", null, null);
        OrderMetric onTime = metric("168.00", 0, "COMPLETED", promise,
                Date.from(Instant.parse("2026-09-22T09:50:00Z")));
        OrderMetric late = metric("176.00", 0, "COMPLETED", promise,
                Date.from(Instant.parse("2026-09-22T10:20:00Z")));
        OrderMetric producing = metric("128.00", 0, "PRODUCING", null, null);
        FlowerDashboardDao dao = daoWith(Arrays.asList(paid, refunded, onTime, late, producing), 3);
        Clock clock = Clock.fixed(Instant.parse("2026-09-22T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

        FlowerDashboardResponse result = new FlowerDashboardServiceImpl(dao, clock).today();

        Assert.assertEquals(new BigDecimal("560.00"), result.getTodaySales());
        Assert.assertEquals(Integer.valueOf(4), result.getTodayOrders());
        Assert.assertEquals(new BigDecimal("140.00"), result.getAverageOrderValue());
        Assert.assertEquals(Integer.valueOf(1), result.getWaitAcceptCount());
        Assert.assertEquals(Integer.valueOf(1), result.getWaitProductionCount());
        Assert.assertEquals(Integer.valueOf(0), result.getWaitDeliveryCount());
        Assert.assertEquals(Integer.valueOf(3), result.getAfterSaleCount());
        Assert.assertEquals(new BigDecimal("50.00"), result.getOnTimeRate());
    }

    @Test
    public void emptyDashboardReturnsZerosAndEmptyRankings() {
        FlowerDashboardResponse result = new FlowerDashboardServiceImpl(
                daoWith(Collections.emptyList(), 0),
                Clock.fixed(Instant.parse("2026-09-22T04:00:00Z"), ZoneId.of("Asia/Shanghai")))
                .today();

        Assert.assertEquals(BigDecimal.ZERO.setScale(2), result.getTodaySales());
        Assert.assertEquals(BigDecimal.ZERO.setScale(2), result.getAverageOrderValue());
        Assert.assertEquals(BigDecimal.ZERO.setScale(2), result.getOnTimeRate());
        Assert.assertTrue(result.getSalesTrend().isEmpty());
        Assert.assertTrue(result.getHotProducts().isEmpty());
        Assert.assertTrue(result.getPopularSlots().isEmpty());
    }

    @Test
    public void promisedDeliveryTimeCombinesDeliveryDateAndSlotEndTime() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("mapper/store/FlowerDashboardMapper.xml");
        Assert.assertNotNull(stream);
        byte[] bytes = new byte[stream.available()];
        int read = stream.read(bytes);
        stream.close();
        Assert.assertTrue(read > 0);
        String mapper = new String(bytes, 0, read, StandardCharsets.UTF_8);
        Assert.assertTrue(mapper.contains("TIMESTAMP(s.delivery_date, s.end_time) AS promised_time"));
    }

    @Test
    public void salesTrendGroupingIsCompatibleWithOnlyFullGroupBy() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("mapper/store/FlowerDashboardMapper.xml");
        Assert.assertNotNull(stream);
        byte[] bytes = new byte[stream.available()];
        int read = stream.read(bytes);
        stream.close();
        Assert.assertTrue(read > 0);
        String mapper = new String(bytes, 0, read, StandardCharsets.UTF_8);
        Assert.assertTrue(mapper.contains("GROUP BY DATE_FORMAT(o.pay_time, '%m-%d')"));
        Assert.assertTrue(mapper.contains("ORDER BY MIN(o.pay_time)"));
    }

    private static OrderMetric metric(String price, int refund, String stage, Date promised, Date completed) {
        return new OrderMetric().setPayPrice(new BigDecimal(price)).setRefundStatus(refund)
                .setCurrentStage(stage).setPromisedTime(promised).setCompletedTime(completed);
    }

    private static FlowerDashboardDao daoWith(java.util.List<OrderMetric> orders, int afterSales) {
        return (FlowerDashboardDao) Proxy.newProxyInstance(FlowerDashboardDao.class.getClassLoader(),
                new Class[]{FlowerDashboardDao.class}, (proxy, method, args) -> {
                    if ("selectOrderMetrics".equals(method.getName())) return orders;
                    if ("countAfterSales".equals(method.getName())) return afterSales;
                    if (java.util.List.class.equals(method.getReturnType())) return Collections.emptyList();
                    if (Integer.class.equals(method.getReturnType()) || int.class.equals(method.getReturnType())) return 0;
                    return null;
                });
    }
}
