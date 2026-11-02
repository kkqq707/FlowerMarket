package com.zbkj.service.dao;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Assume;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.yaml.snakeyaml.Yaml;
import com.zbkj.common.response.FlowerDashboardResponse.OrderMetric;
import com.zbkj.common.response.FlowerDashboardResponse.RankingItem;
import com.zbkj.common.response.FlowerDashboardResponse.TrendItem;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;

public class FlowerDashboardMapperTest {
    /** Optional verification against existing local data, using a read-only connection. */
    @Test
    @SuppressWarnings("unchecked")
    public void liveReadOnlyQueriesReconcileTrendTotalsAndBothRankings() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("flower.live.verify"));
        Map<String, Object> config;
        try (InputStream input = Files.newInputStream(Paths.get("../crmeb-admin/src/main/resources/application.yml"))) {
            config = new Yaml().load(input);
        }
        Map<String, Object> database = (Map<String, Object>) ((Map<String, Object>) config.get("spring")).get("datasource");
        String url = String.valueOf(database.get("url"));
        Assert.assertTrue("Only the configured local development database is allowed", url.startsWith("jdbc:mysql://127.0.0.1:"));
        UnpooledDataSource datasource = new UnpooledDataSource(String.valueOf(database.get("driver-class-name")), url,
                String.valueOf(database.get("username")), String.valueOf(database.get("password")));
        Configuration configuration = new Configuration(new Environment("read-only", new JdbcTransactionFactory(), datasource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        String resource = "mapper/store/FlowerDashboardMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        Date start = Date.from(today.minusDays(30).atStartOfDay(zone).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(zone).toInstant());
        try (Connection connection = datasource.getConnection()) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession(connection)) {
                FlowerDashboardDao mapper = session.getMapper(FlowerDashboardDao.class);
                BigDecimal expectedAmount = BigDecimal.ZERO;
                int expectedCount = 0;
                for (OrderMetric record : mapper.selectOrderMetrics(start, end)) {
                    if (record.getRefundStatus() != null && record.getRefundStatus() == 0) {
                        expectedAmount = expectedAmount.add(record.getPayPrice());
                        expectedCount++;
                    }
                }
                BigDecimal actualAmount = BigDecimal.ZERO;
                int actualCount = 0;
                List<TrendItem> trend = mapper.selectSalesTrend(start, end);
                String previous = "";
                for (TrendItem item : trend) {
                    Assert.assertTrue(item.getLabel().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"));
                    Assert.assertTrue(previous.compareTo(item.getLabel()) < 0);
                    previous = item.getLabel();
                    actualAmount = actualAmount.add(item.getAmount());
                    actualCount += item.getOrderCount();
                }
                Assert.assertEquals(0, expectedAmount.compareTo(actualAmount));
                Assert.assertEquals(expectedCount, actualCount);
                for (String sort : new String[]{"quantity", "amount"}) {
                    List<RankingItem> ranking = mapper.selectHotProducts(start, end, sort);
                    Assert.assertTrue(ranking.size() <= 5);
                    for (int i = 1; i < ranking.size(); i++) {
                        RankingItem before = ranking.get(i - 1), after = ranking.get(i);
                        int primary = "amount".equals(sort) ? before.getAmount().compareTo(after.getAmount())
                                : before.getQuantity().compareTo(after.getQuantity());
                        int secondary = "amount".equals(sort) ? before.getQuantity().compareTo(after.getQuantity())
                                : before.getAmount().compareTo(after.getAmount());
                        Assert.assertTrue(primary > 0 || (primary == 0 && secondary > 0)
                                || (primary == 0 && secondary == 0 && before.getProductId() <= after.getProductId()));
                    }
                }
                System.out.println("Dashboard read-only SQL verification: " + expectedCount + " paid orders, " + trend.size() + " trend dates, both ranking sorts.");
            }
        }
    }

    @Test
    public void rankingSortsUseWhitelistedSqlAndDeterministicTies() throws Exception {
        for (String sort : new String[]{"quantity", "amount", "amount DESC; DELETE FROM eb_store_order"}) {
            BoundSql bound = sql("selectHotProducts", sort);
            String query = bound.getSql().replaceAll("\\s+", " ");
            String ordering = "amount".equals(sort)
                    ? "ORDER BY amount DESC, quantity DESC, i.product_id ASC"
                    : "ORDER BY quantity DESC, amount DESC, i.product_id ASC";
            Assert.assertTrue(query, query.contains(ordering));
            Assert.assertFalse(query, query.contains("DELETE"));
            assertDateBounds(bound);
            Assert.assertTrue(query.contains("LIMIT 5"));
        }
    }

    @Test
    public void trendUsesFullCalendarDatesAndBoundExclusiveEnd() throws Exception {
        BoundSql bound = sql("selectSalesTrend", null);
        String query = bound.getSql().replaceAll("\\s+", " ");
        Assert.assertTrue(query, query.contains("DATE_FORMAT(o.pay_time, '%Y-%m-%d') AS label"));
        Assert.assertTrue(query, query.contains("GROUP BY DATE_FORMAT(o.pay_time, '%Y-%m-%d') ORDER BY MIN(o.pay_time)"));
        assertDateBounds(bound);
    }

    private void assertDateBounds(BoundSql bound) {
        String query = bound.getSql().replaceAll("\\s+", " ");
        Assert.assertTrue(query, query.contains("o.pay_time >= ? AND o.pay_time < ?"));
        Assert.assertEquals(2, bound.getParameterMappings().size());
        Assert.assertEquals("start", bound.getParameterMappings().get(0).getProperty());
        Assert.assertEquals("end", bound.getParameterMappings().get(1).getProperty());
        Assert.assertTrue(query.contains("o.paid = 1"));
        Assert.assertTrue(query.contains("o.refund_status = 0"));
        Assert.assertTrue(query.contains("o.is_del = 0"));
        Assert.assertTrue(query.contains("o.is_system_del = 0"));
    }

    private BoundSql sql(String name, String sort) throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/store/FlowerDashboardMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", new Date(0));
        parameters.put("end", new Date(86400000L));
        parameters.put("sortBy", sort);
        return configuration.getMappedStatement("com.zbkj.service.dao.FlowerDashboardDao." + name).getBoundSql(parameters);
    }
}
