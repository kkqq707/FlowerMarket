package com.zbkj.service.dao;

import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Assume;
import org.yaml.snakeyaml.Yaml;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class FlowerProductMapperTest {
    /** Opt-in, read-only verification against the configured local development database. */
    @Test
    @SuppressWarnings("unchecked")
    public void liveAdminCountsAndFrontCatalogAgree() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("flower.live.verify"));
        Map<String, Object> config;
        try (InputStream input = Files.newInputStream(Paths.get("../crmeb-admin/src/main/resources/application.yml"))) {
            config = new Yaml().load(input);
        }
        Map<String, Object> spring = (Map<String, Object>) config.get("spring");
        Map<String, Object> database = (Map<String, Object>) spring.get("datasource");
        String url = String.valueOf(database.get("url"));
        Assert.assertTrue("Verification only connects to the local development database", url.startsWith("jdbc:mysql://127.0.0.1:"));
        UnpooledDataSource datasource = new UnpooledDataSource(String.valueOf(database.get("driver-class-name")), url,
                String.valueOf(database.get("username")), String.valueOf(database.get("password")));
        Configuration configuration = new Configuration(new Environment("read-only", new JdbcTransactionFactory(), datasource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        String resource = "mapper/store/FlowerProductExtMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        try (Connection connection = datasource.getConnection()) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            int warningStock = 0;
            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT value FROM eb_system_config WHERE name='store_stock' LIMIT 1")) {
                if (result.next()) warningStock = Integer.parseInt(result.getString(1));
            }
            try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession(connection)) {
                FlowerProductExtDao mapper = session.getMapper(FlowerProductExtDao.class);
                FlowerProductSearchRequest request = new FlowerProductSearchRequest();
                request.setType(1);
                List<FlowerProductCardResponse> admin = mapper.searchAdmin(request, warningStock);
                List<FlowerProductCardResponse> front = mapper.searchOnSale(request);
                Assert.assertEquals(front, admin);
                int comparisons = 1;
                for (String recipient : Arrays.asList(null, "爱人", "朋友", "客户")) {
                    request.setRecipient(recipient);
                    for (int type = 1; type <= 5; type++) {
                        request.setType(type);
                        List<FlowerProductCardResponse> rows = mapper.searchAdmin(request, warningStock);
                        Assert.assertEquals(rows.size(), mapper.countAdmin(request, warningStock));
                        if (type == 1) Assert.assertTrue(rows.stream().allMatch(row -> Boolean.TRUE.equals(row.getIsShow())));
                        if (type == 2) Assert.assertTrue(rows.stream().allMatch(row -> Boolean.FALSE.equals(row.getIsShow())));
                        if (type == 3) Assert.assertTrue(rows.stream().allMatch(row -> row.getStock() <= 0));
                        if (type == 4) for (FlowerProductCardResponse row : rows) Assert.assertTrue(row.getStock() <= warningStock);
                        comparisons++;
                    }
                }
                System.out.println("Live read-only mapper verification passed: " + admin.size() + " products, " + comparisons + " admin/front comparisons.");
            }
        }
    }

    private BoundSql sql(String statement, int type) throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/store/FlowerProductExtMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        FlowerProductSearchRequest request = new FlowerProductSearchRequest();
        request.setType(type);
        request.setCid(9101);
        request.setRecipient("爱人");
        Map<String, Object> params = new HashMap<>();
        params.put("request", request);
        params.put("warningStock", 10);
        return configuration.getMappedStatement("com.zbkj.service.dao.FlowerProductExtDao." + statement).getBoundSql(params);
    }

    @Test
    public void adminStatusAndCountsUseTheSameCategoryAndTagFilters() throws Exception {
        for (int type = 1; type <= 5; type++) {
            String list = sql("searchAdmin", type).getSql().replaceAll("\\s+", " ");
            String count = sql("countAdmin", type).getSql().replaceAll("\\s+", " ");
            Assert.assertTrue(list.contains("FIND_IN_SET(c.id, p.cate_id)"));
            Assert.assertTrue(count.contains("FIND_IN_SET(c.id, p.cate_id)"));
            Assert.assertTrue(list.contains("FIND_IN_SET(?, e.recipient)"));
            Assert.assertTrue(count.contains("FIND_IN_SET(?, e.recipient)"));
            String status = type == 5 ? "p.is_recycle = 1" : "p.is_recycle = 0";
            Assert.assertTrue(list.contains(status));
            Assert.assertTrue(count.contains(status));
            if (type == 1 || type == 2) {
                String onSale = "p.is_show = " + (type == 1 ? "1" : "0");
                Assert.assertTrue(list.contains(onSale));
                Assert.assertTrue(count.contains(onSale));
            }
            if (type == 3) {
                Assert.assertTrue(list.contains("p.stock <= 0"));
                Assert.assertTrue(count.contains("p.stock <= 0"));
            }
            if (type == 4) {
                Assert.assertTrue(list.contains("p.stock <= ?"));
                Assert.assertTrue(count.contains("p.stock <= ?"));
            }
        }
    }
}
