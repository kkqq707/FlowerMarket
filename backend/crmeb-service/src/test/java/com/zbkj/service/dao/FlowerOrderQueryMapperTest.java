package com.zbkj.service.dao;

import com.zbkj.common.request.flower.FlowerOrderAdminSearchRequest;
import com.zbkj.common.response.FlowerOrderAdminListResponse;
import com.zbkj.common.response.FlowerOrderSummaryResponse;
import com.zbkj.service.service.impl.FlowerOrderSearch;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.*;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

public class FlowerOrderQueryMapperTest {
    @Test public void rejectsInvalidDateAndStatusFilters() {
        for(String[] pair:new String[][]{{"2026-02-30","2026-03-01"},{"2026-11-03",""},{"2026-11-03","2026-11-02"},{"2025-01-01","2026-11-03"}}) {
            FlowerOrderAdminSearchRequest q=new FlowerOrderAdminSearchRequest();q.setStartDate(pair[0]);q.setEndDate(pair[1]);
            try { FlowerOrderSearch.normalize(q); Assert.fail("Invalid range accepted"); } catch(com.zbkj.common.exception.CrmebException expected) {}
        }
        FlowerOrderAdminSearchRequest q=new FlowerOrderAdminSearchRequest();q.setStage("unsupported");
        try {FlowerOrderSearch.normalize(q);Assert.fail("Invalid stage accepted");} catch(com.zbkj.common.exception.CrmebException expected) {}
    }
    @Test @SuppressWarnings("unchecked") public void queriesReconcileDatesCancellationRefundsAndGroupCountsUsingIsolatedTemporaryTables() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("flower.live.verify"));
        Map<String,Object> config;
        try(InputStream in=Files.newInputStream(Paths.get("../crmeb-admin/src/main/resources/application.yml"))){config=new Yaml().load(in);}
        Map<String,Object> ds=(Map<String,Object>)((Map<String,Object>)config.get("spring")).get("datasource");
        String url=ds.get("url").toString();Assert.assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:"));
        UnpooledDataSource source=new UnpooledDataSource(ds.get("driver-class-name").toString(),url,ds.get("username").toString(),ds.get("password").toString());
        Configuration conf=new Configuration(new Environment("temporary-test",new JdbcTransactionFactory(),source));conf.setMapUnderscoreToCamelCase(true);conf.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        try(InputStream in=getClass().getClassLoader().getResourceAsStream("mapper/store/FlowerOrderExtMapper.xml")){new XMLMapperBuilder(in,conf,"mapper/store/FlowerOrderExtMapper.xml",conf.getSqlFragments()).parse();}
        try(InputStream in=getClass().getClassLoader().getResourceAsStream("mapper/store/FlowerDashboardMapper.xml")){new XMLMapperBuilder(in,conf,"mapper/store/FlowerDashboardMapper.xml",conf.getSqlFragments()).parse();}
        try(SqlSession session=new SqlSessionFactoryBuilder().build(conf).openSession()) {
            // Connection-local tables shadow the real tables; no existing data is changed.
            try(Statement s=session.getConnection().createStatement()) {
                s.execute("CREATE TEMPORARY TABLE eb_store_order (id INT,order_id VARCHAR(80),paid INT,refund_status INT,refund_price DECIMAL(10,2),pay_price DECIMAL(10,2),total_price DECIMAL(10,2),total_num INT,is_del INT,is_system_del INT,create_time DATETIME,pay_time DATETIME,refund_reason_time DATETIME,refund_reason_wap VARCHAR(100),real_name VARCHAR(100))");
                s.execute("CREATE TEMPORARY TABLE eb_flower_order_ext (order_id VARCHAR(80),current_stage VARCHAR(40),reservation_released INT,recipient_name VARCHAR(100),recipient_phone VARCHAR(30),delivery_address_snapshot VARCHAR(100),delivery_date DATE,slot_id INT)");
                s.execute("CREATE TEMPORARY TABLE eb_store_order_status (oid INT,change_type VARCHAR(40),create_time DATETIME)");
                s.execute("CREATE TEMPORARY TABLE eb_store_order_info (id INT,order_id INT,product_name VARCHAR(100),image VARCHAR(100))");
                s.execute("INSERT INTO eb_store_order VALUES (1,'F-1',1,0,0,188,188,1,0,0,'2026-11-03 09:00:00','2026-11-03 09:01:00',NULL,'','A'),(2,'F-2',1,1,50,128,128,1,0,0,'2026-11-03 10:00:00','2026-11-03 10:01:00','2026-11-03 11:00:00','不新鲜','B'),(3,'F-3',1,2,20,100,100,1,0,0,'2026-11-02 10:00:00','2026-11-02 10:01:00','2026-11-02 11:00:00','包装破损','C'),(4,'F-4',0,0,0,88,88,1,0,0,'2026-11-03 12:00:00',NULL,NULL,'','D'),(5,'F-5',0,0,0,99,99,1,1,0,'2026-11-03 13:00:00',NULL,NULL,'','E'),(6,'F-6',1,2,99,99,99,1,0,0,'2026-11-02 14:00:00','2026-11-02 14:01:00','2026-11-02 15:00:00','取消','F')");
                s.execute("INSERT INTO eb_flower_order_ext VALUES ('F-1','WAIT_ACCEPT',0,'A','100','demo','2026-11-03',1),('F-2','WAIT_PRODUCTION',0,'B','101','demo','2026-11-03',1),('F-3','COMPLETED',0,'C','102','demo','2026-11-02',1),('F-4','WAIT_PAY',0,'D','103','demo','2026-11-03',1),('F-5','WAIT_PAY',1,'E','104','demo','2026-11-03',1),('F-6','DELIVERING',0,'F','105','demo','2026-11-02',1)");
                s.execute("INSERT INTO eb_store_order_status VALUES (3,'refund_price','2026-11-03 12:00:00'),(6,'refund_price','2026-11-03 14:00:00'),(6,'refund_price','2026-11-03 14:01:00')");
            }
            FlowerOrderExtDao dao=session.getMapper(FlowerOrderExtDao.class);
            FlowerOrderAdminSearchRequest q=new FlowerOrderAdminSearchRequest();q.setTimeType("created");q.setStartDate("2026-11-03");q.setEndDate("2026-11-03");FlowerOrderSearch.normalize(q);
            Assert.assertEquals(4,dao.selectAdminList(q).size());
            FlowerOrderSummaryResponse summary=dao.selectAdminSummary(q);Assert.assertEquals(Long.valueOf(4),summary.getOrderCount());Assert.assertEquals(0,new BigDecimal("316").compareTo(summary.getPaidAmount()));Assert.assertEquals(0,new BigDecimal("50").compareTo(summary.getPendingRefundAmount()));
            q.setStage("CANCELLED");Assert.assertEquals("F-5",dao.selectAdminList(q).get(0).getOrderNo());
            q.setStage("PRODUCTION");Assert.assertTrue(dao.selectAdminList(q).isEmpty());
            Assert.assertFalse(dao.selectAdminStageCounts(q,true).isEmpty());
            q.setStage("");q.setTimeType("refund");Assert.assertEquals(3,dao.selectAdminList(q).size());
            q.setRefundState("done");Assert.assertEquals(2,dao.selectAdminList(q).size());Assert.assertEquals(0,new BigDecimal("119").compareTo(dao.selectAdminSummary(q).getRefundedAmount()));
            q.setRefundState("partial");List<FlowerOrderAdminListResponse> partial=dao.selectAdminList(q);Assert.assertEquals(1,partial.size());Assert.assertEquals("F-3",partial.get(0).getOrderNo());
            q.setRefundState("");q.setTimeType("paid");q.setSalesOnly(true);Assert.assertEquals("F-1",dao.selectAdminList(q).get(0).getOrderNo());Assert.assertEquals(1,dao.selectAdminList(q).size());
            FlowerDashboardDao dashboard=session.getMapper(FlowerDashboardDao.class);
            java.util.Date start=Timestamp.valueOf("2026-11-03 00:00:00"),end=Timestamp.valueOf("2026-11-04 00:00:00");
            Assert.assertEquals(0,new BigDecimal("119").compareTo(dashboard.selectRefundAmount(start,end)));
            Assert.assertEquals(Integer.valueOf(4),dashboard.countCreatedOrders(start,end));
            Assert.assertEquals(Integer.valueOf(1),dashboard.countAfterSales());
            Assert.assertEquals(1,((Number)dashboard.selectWorkbenchCounts().get("waitAccept")).intValue());
            q.setTimeType("refunded");q.setRefundState("done");q.setSalesOnly(false);Assert.assertEquals(2,dao.selectAdminList(q).size());
            session.rollback();
        }
    }
}
