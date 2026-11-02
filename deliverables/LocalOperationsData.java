import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Runs the explicitly scoped local operations demo-data refresh in one transaction. */
public class LocalOperationsData {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Map<String,Object> config;
        try (java.io.InputStream input = Files.newInputStream(Paths.get("backend/crmeb-admin/src/main/resources/application.yml"))) {
            config = new Yaml().load(input);
        }
        Map<String,Object> spring = (Map<String,Object>) config.get("spring");
        Map<String,Object> datasource = (Map<String,Object>) spring.get("datasource");
        String url = String.valueOf(datasource.get("url"));
        if (!url.startsWith("jdbc:mysql://127.0.0.1:")) throw new IllegalStateException("Local development database required");
        Class.forName(String.valueOf(datasource.get("driver-class-name")));
        try (Connection connection = DriverManager.getConnection(url, String.valueOf(datasource.get("username")), String.valueOf(datasource.get("password")))) {
            connection.setAutoCommit(false);
            String mode = args.length == 0 ? "audit" : args[0];
            if (mode.equals("audit")) {
                connection.setReadOnly(true);
                try (Statement statement = connection.createStatement()) { statement.execute("SET SESSION group_concat_max_len=65535"); }
                query(connection, "SELECT CURDATE() AS today,@@session.time_zone AS time_zone");
                for (String table : Arrays.asList("eb_flower_delivery_slot", "eb_flower_material", "eb_flower_material_batch", "eb_flower_material_allocation", "eb_flower_order_ext", "eb_flower_order_progress", "eb_store_order", "eb_store_order_info", "eb_user")) {
                    query(connection, "SELECT '"+table+"' AS table_name,COUNT(*) AS rows_count FROM "+table);
                    query(connection, "SELECT GROUP_CONCAT(CONCAT(column_name,':',column_type,':',is_nullable,':',IFNULL(column_default,'NULL')) ORDER BY ordinal_position SEPARATOR ' | ') AS fields FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='"+table+"'");
                }
                query(connection, "SELECT id,delivery_date,start_time,end_time,capacity,booked_count,enabled FROM eb_flower_delivery_slot ORDER BY delivery_date,id LIMIT 30");
                query(connection, "SELECT id,material_id,batch_no,received_date,expiry_date,received_qty,current_qty,reserved_qty,remark FROM eb_flower_material_batch ORDER BY id");
                query(connection, "SELECT id,name,unit,fresh_days FROM eb_flower_material ORDER BY id");
                query(connection, "SELECT current_stage,COUNT(*) AS cnt FROM eb_flower_order_ext GROUP BY current_stage");
            } else if (mode.equals("apply") || mode.equals("check")) {
                try {
                    require(connection, "SELECT COUNT(*) FROM eb_user WHERE uid=950001 AND account<>'flower_ops_demo'", 0);
                    require(connection, "SELECT COUNT(*) FROM eb_store_order WHERE order_id LIKE 'DEMO-FLOWER-%' AND (uid<>950001 OR pay_type<>'mock')", 0);
                    require(connection, "SELECT COUNT(*) FROM eb_flower_product_ext WHERE product_id BETWEEN 900013 AND 900030 AND batch_managed=0", 18);
                    require(connection, "SELECT COUNT(*) FROM eb_flower_recipe WHERE product_id BETWEEN 900013 AND 900030", 0);
                    // Preserve every existing customer/order row, including prices and any progressed demo orders.
                    Map<String,List<String>> originalRows = snapshots(connection, Arrays.asList("eb_user", "eb_store_order", "eb_store_order_info", "eb_flower_order_ext", "eb_flower_order_progress", "eb_flower_material_allocation"));
                    String source = new String(Files.readAllBytes(Paths.get("backend/sql/flower-operations-demo-data.sql")), StandardCharsets.UTF_8);
                    int affected = executeSeed(connection, source);
                    verify(connection);
                    Map<String,List<String>> currentRows = snapshots(connection, originalRows.keySet());
                    for (String table : originalRows.keySet()) {
                        if (!new HashSet<>(currentRows.get(table)).containsAll(originalRows.get(table))) throw new IllegalStateException("Existing rows changed in "+table);
                    }
                    // Executing again must not change stock, sales, bookings, timestamps or any order data.
                    Map<String,List<String>> firstRun = snapshots(connection, Arrays.asList("eb_user", "eb_store_order", "eb_store_order_info", "eb_flower_order_ext", "eb_flower_order_progress", "eb_flower_material_allocation", "eb_flower_material_batch", "eb_flower_delivery_slot", "eb_store_product", "eb_store_product_attr_value"));
                    int repeatedAffected = executeSeed(connection, source);
                    if (repeatedAffected!=0 || !firstRun.equals(snapshots(connection, firstRun.keySet()))) throw new IllegalStateException("Repeated seed changed data");
                    System.out.println("Existing customer/order snapshots unchanged; repeated seed affected rows=0");
                    if (mode.equals("check")) {
                        connection.rollback();
                        System.out.println("Dry-run verified and rolled back; prospective affected rows="+affected);
                    } else {
                        connection.commit();
                        System.out.println("Operations demo refresh committed; affected rows="+affected);
                    }
                } catch (Exception error) { connection.rollback(); throw error; }
            } else if (mode.equals("verify")) {
                connection.setReadOnly(true);
                verify(connection);
            } else throw new IllegalArgumentException("Use audit, check, apply, or verify");
        }
    }
    static int executeSeed(Connection connection, String source) throws SQLException {
        int affected = 0;
        for (String sql : LocalFlowerData.statements(source)) {
            String upper = sql.trim().toUpperCase(Locale.ROOT);
            if (!(upper.startsWith("SET ") || upper.startsWith("INSERT INTO ") || upper.startsWith("UPDATE "))) throw new IllegalArgumentException("Unexpected statement");
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                affected += Math.max(0, statement.getUpdateCount());
            }
        }
        return affected;
    }
    static Map<String,List<String>> snapshots(Connection connection, Collection<String> tables) throws SQLException {
        Map<String,List<String>> rows = new LinkedHashMap<>();
        for (String table : tables) {
            List<String> tableRows = LocalFlowerData.snapshot(connection, "SELECT * FROM "+table);
            Collections.sort(tableRows);
            rows.put(table, tableRows);
        }
        return rows;
    }
    static void verify(Connection connection) throws SQLException {
        require(connection, "SELECT COUNT(*) FROM eb_store_order WHERE order_id REGEXP '^DEMO-FLOWER-00(0[1-9]|[12][0-9]|30)$' AND uid=950001 AND paid=1 AND pay_type='mock' AND is_system_del=0", 30);
        require(connection, "SELECT COUNT(*) FROM eb_store_order_info WHERE order_no REGEXP '^DEMO-FLOWER-00(0[1-9]|[12][0-9]|30)$' AND JSON_VALID(info)=1", 30);
        require(connection, "SELECT COUNT(*) FROM eb_flower_order_ext e JOIN eb_store_order o ON o.order_id=e.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id AND s.delivery_date=e.delivery_date WHERE o.uid=950001 AND o.order_id LIKE 'DEMO-FLOWER-%'", 30);
        require(connection, "SELECT COUNT(DISTINCT DATE(pay_time)) FROM eb_store_order WHERE uid=950001 AND order_id LIKE 'DEMO-FLOWER-%' AND pay_time BETWEEN DATE_SUB(CURDATE(),INTERVAL 6 DAY) AND NOW()", 7);
        require(connection, "SELECT COUNT(*) FROM (SELECT DATE(pay_time) AS paid_day FROM eb_store_order WHERE order_id REGEXP '^DEMO-FLOWER-00(1[3-9]|2[0-9]|30)$' GROUP BY DATE(pay_time) HAVING COUNT(*)=3) days", 6);
        require(connection, "SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id WHERE o.order_id REGEXP '^DEMO-FLOWER-00(1[3-9]|2[0-9]|30)$' AND (e.current_stage<>'COMPLETED' OR o.status<>2 OR DATE(o.create_time)<>e.delivery_date OR DATE(o.pay_time)<>e.delivery_date OR e.delivery_date NOT BETWEEN DATE_SUB(CURDATE(),INTERVAL 6 DAY) AND DATE_SUB(CURDATE(),INTERVAL 1 DAY) OR s.enabled<>0)", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_order_progress p JOIN eb_flower_order_ext e ON e.order_id=p.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id WHERE p.order_id REGEXP '^DEMO-FLOWER-00(1[3-9]|2[0-9]|30)$' AND p.stage='COMPLETED' AND p.create_time BETWEEN TIMESTAMP(e.delivery_date,s.start_time) AND TIMESTAMP(e.delivery_date,s.end_time)", 18);
        require(connection, "SELECT COUNT(*) FROM eb_flower_order_progress p JOIN eb_store_order o ON o.order_id=p.order_id WHERE o.uid=950001 AND o.order_id LIKE 'DEMO-FLOWER-%' AND (p.create_time>NOW() OR p.create_time<o.pay_time)", 0);
        require(connection, "SELECT COUNT(*) FROM eb_store_order WHERE uid=950001 AND order_id LIKE 'DEMO-FLOWER-%' AND (create_time>pay_time OR pay_time>NOW())", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_order_progress WHERE order_id REGEXP '^DEMO-FLOWER-00(1[3-9]|2[0-9]|30)$'", 108);
        require(connection, "SELECT COUNT(*) FROM eb_flower_material_allocation WHERE order_id REGEXP '^DEMO-FLOWER-00(1[3-9]|2[0-9]|30)$'", 0);
        require(connection, "SELECT COUNT(*) FROM eb_store_order o JOIN eb_store_order_info i ON i.order_no=o.order_id WHERE o.uid=950001 AND o.order_id LIKE 'DEMO-FLOWER-%' AND (o.total_num<>i.pay_num OR o.pay_price<>i.price*i.pay_num OR o.total_price<>o.pay_price)", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_order_ext e WHERE e.order_id LIKE 'DEMO-FLOWER-%' AND NOT EXISTS(SELECT 1 FROM eb_flower_order_progress p WHERE p.order_id=e.order_id AND p.stage=e.current_stage)", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_material m WHERE m.id BETWEEN 9001 AND 9011 AND EXISTS(SELECT 1 FROM eb_flower_material_batch b WHERE b.material_id=m.id AND b.batch_no LIKE 'DEMO-%' AND b.received_date<=CURDATE() AND b.expiry_date>CURDATE() AND b.current_qty>b.reserved_qty)", 11);
        require(connection, "SELECT COUNT(*) FROM eb_flower_material_batch WHERE current_qty<0 OR reserved_qty<0 OR reserved_qty>current_qty OR current_qty>received_qty", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_material_batch b WHERE b.material_id BETWEEN 9001 AND 9011 AND b.reserved_qty<>(SELECT COALESCE(SUM(a.quantity),0) FROM eb_flower_material_allocation a WHERE a.batch_id=b.id AND a.status='RESERVED')", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_recipe r JOIN eb_store_order_info i ON i.product_id=r.product_id WHERE i.order_no LIKE 'DEMO-FLOWER-%' AND NOT EXISTS(SELECT 1 FROM eb_flower_material_allocation a WHERE a.order_id=i.order_no AND a.material_id=r.material_id AND a.quantity=r.qty_per_item*i.pay_num)", 0);
        require(connection, "SELECT COUNT(*) FROM eb_store_product p WHERE p.id BETWEEN 900001 AND 900036 AND (p.stock<0 OR p.stock<>(SELECT COALESCE(SUM(v.stock),0) FROM eb_store_product_attr_value v WHERE v.product_id=p.id AND v.type=0 AND v.is_del=0))", 0);
        require(connection, "SELECT COUNT(*) FROM eb_store_product_attr_value WHERE product_id BETWEEN 900001 AND 900036 AND type=0 AND is_del=0 AND stock<0", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_delivery_slot WHERE booked_count<0 OR booked_count>capacity", 0);
        require(connection, "SELECT COUNT(*) FROM eb_flower_delivery_slot s WHERE s.booked_count<(SELECT COUNT(*) FROM eb_flower_order_ext e WHERE e.slot_id=s.id AND e.order_id LIKE 'DEMO-FLOWER-%')", 0);
        require(connection, "SELECT COUNT(DISTINCT delivery_date) FROM eb_flower_delivery_slot WHERE delivery_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(),INTERVAL 14 DAY) AND enabled=1", 15);
        query(connection, "SELECT MIN(delivery_date) AS delivery_from,MAX(delivery_date) AS delivery_to,COUNT(*) AS slots,SUM(booked_count) AS bookings FROM eb_flower_delivery_slot WHERE delivery_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(),INTERVAL 14 DAY)");
        query(connection, "SELECT MIN(received_date) AS received_from,MIN(expiry_date) AS first_expiry,MAX(expiry_date) AS last_expiry,COUNT(*) AS batches FROM eb_flower_material_batch WHERE batch_no LIKE 'DEMO-%' AND expiry_date>CURDATE()");
        query(connection, "SELECT e.current_stage,COUNT(*) AS orders FROM eb_flower_order_ext e JOIN eb_store_order o ON o.order_id=e.order_id WHERE o.uid=950001 GROUP BY e.current_stage ORDER BY e.current_stage");
        query(connection, "SELECT DATE(pay_time) AS paid_day,COUNT(*) AS orders,SUM(pay_price) AS paid_total FROM eb_store_order WHERE uid=950001 AND order_id LIKE 'DEMO-FLOWER-%' GROUP BY DATE(pay_time) ORDER BY paid_day");
    }
    static void require(Connection connection, String sql, int expected) throws SQLException {
        try (Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(sql)) {
            result.next(); int actual=result.getInt(1);
            if (actual!=expected) throw new IllegalStateException("Consistency check failed; expected="+expected+", actual="+actual+"; query="+sql);
        }
    }
    static void query(Connection connection,String sql) throws SQLException {
        try (Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(sql)) {
            ResultSetMetaData columns=result.getMetaData();
            while(result.next()) { StringJoiner row=new StringJoiner(" | "); for(int i=1;i<=columns.getColumnCount();i++) row.add(columns.getColumnLabel(i)+"="+result.getString(i)); System.out.println(row); }
        }
    }
}
