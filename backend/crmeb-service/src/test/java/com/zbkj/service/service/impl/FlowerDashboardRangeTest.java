package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.response.FlowerDashboardResponse.RankingItem;
import com.zbkj.common.response.FlowerDashboardResponse.TrendItem;
import com.zbkj.service.dao.FlowerDashboardDao;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FlowerDashboardRangeTest {
    private final List<Object[]> calls = new ArrayList<>();
    private final TrendItem trend = new TrendItem();
    private final RankingItem ranking = new RankingItem();
    private final FlowerDashboardDao dao = (FlowerDashboardDao) Proxy.newProxyInstance(
            FlowerDashboardDao.class.getClassLoader(), new Class[]{FlowerDashboardDao.class}, (proxy, method, args) -> {
                calls.add(new Object[]{method.getName(), args});
                if ("selectSalesTrend".equals(method.getName())) return Collections.singletonList(trend);
                if ("selectHotProducts".equals(method.getName())) return Collections.singletonList(ranking);
                if ("countAfterSales".equals(method.getName())) return 0;
                return Collections.emptyList();
            });
    // UTC still says Jan 31; Shanghai has entered Feb 1.
    private final FlowerDashboardServiceImpl service = new FlowerDashboardServiceImpl(dao,
            Clock.fixed(Instant.parse("2026-01-31T16:30:00Z"), ZoneOffset.UTC));

    @Test
    public void singleDayIncludesWholeShanghaiDayAndReturnsDaoResults() {
        trend.setLabel("2026-02-01");
        trend.setAmount(new BigDecimal("88.00"));
        Assert.assertSame(trend, service.salesTrend("2026-02-01", "2026-02-01").get(0));
        assertBounds(0, "selectSalesTrend", "2026-01-31T16:00:00Z", "2026-02-01T16:00:00Z");
    }

    @Test
    public void inclusive31DayRangeIsAllowedBut32DaysNeverQueriesDao() {
        service.salesTrend("2026-01-02", "2026-02-01");
        assertBounds(0, "selectSalesTrend", "2026-01-01T16:00:00Z", "2026-02-01T16:00:00Z");
        calls.clear();
        reject(() -> service.salesTrend("2026-01-01", "2026-02-01"), "31");
    }

    @Test
    public void leapDayAndYearBoundaryAreValidCalendarRanges() {
        service.salesTrend("2024-02-29", "2024-03-01");
        assertBounds(0, "selectSalesTrend", "2024-02-28T16:00:00Z", "2024-03-01T16:00:00Z");
        service.salesTrend("2025-12-31", "2026-01-01");
        assertBounds(1, "selectSalesTrend", "2025-12-30T16:00:00Z", "2026-01-01T16:00:00Z");
    }

    @Test
    public void missingMalformedAndImpossibleDatesNeverQueryDao() {
        for (String invalid : new String[]{null, "", " ", "2026-2-01", "2026/02/01", "2026-02-29",
                "2024-04-31", "2026-01-01T00:00:00", "+2026-01-01", "0000-01-01"}) {
            reject(() -> service.salesTrend(invalid, "2026-02-01"), "日期");
            reject(() -> service.hotProducts("2026-01-01", invalid, "quantity"), "日期");
        }
    }

    @Test
    public void reversedAndFutureRangesNeverQueryDao() {
        reject(() -> service.salesTrend("2026-02-01", "2026-01-31"), "开始");
        reject(() -> service.hotProducts("2026-02-01", "2026-02-02", "quantity"), "今天");
    }

    @Test
    public void supportedRankingSortsAreBoundAndReturnDaoResults() {
        for (String sort : new String[]{"quantity", "amount"}) {
            Assert.assertSame(ranking, service.hotProducts("2026-01-31", "2026-02-01", sort).get(0));
            int index = calls.size() - 1;
            assertBounds(index, "selectHotProducts", "2026-01-30T16:00:00Z", "2026-02-01T16:00:00Z");
            Assert.assertEquals(sort, ((Object[]) calls.get(index)[1])[2]);
        }
    }

    @Test
    public void unsupportedSortIncludingSqlFragmentNeverQueriesDao() {
        for (String invalid : new String[]{null, "", "AMOUNT", "price", "quantity DESC; DROP TABLE eb_store_order"}) {
            reject(() -> service.hotProducts("2026-01-31", "2026-02-01", invalid), "排序");
        }
    }

    @Test
    public void defaultDashboardKeepsTodayMetricsSevenDayTrendAndQuantityRanking() {
        service.today();
        assertBounds(0, "selectOrderMetrics", "2026-01-31T16:00:00Z", "2026-02-01T16:00:00Z");
        assertBounds(2, "selectSalesTrend", "2026-01-25T16:00:00Z", "2026-02-01T16:00:00Z");
        assertBounds(3, "selectHotProducts", "2026-01-31T16:00:00Z", "2026-02-01T16:00:00Z");
        Assert.assertEquals("quantity", ((Object[]) calls.get(3)[1])[2]);
    }

    private void reject(Runnable operation, String message) {
        int before = calls.size();
        try {
            operation.run();
            Assert.fail("Invalid filter must be rejected before querying");
        } catch (CrmebException exception) {
            Assert.assertTrue(exception.getMessage(), exception.getMessage().contains(message));
        }
        Assert.assertEquals(before, calls.size());
    }

    private void assertBounds(int index, String method, String start, String end) {
        Assert.assertEquals(method, calls.get(index)[0]);
        Object[] args = (Object[]) calls.get(index)[1];
        Assert.assertEquals(Date.from(Instant.parse(start)), args[0]);
        Assert.assertEquals(Date.from(Instant.parse(end)), args[1]);
    }
}
