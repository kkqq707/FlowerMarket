import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/** Local-only affordable catalog and explicitly requested mock order repricing. */
public class LocalAffordablePricing extends LocalTodaySales {
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        Map<String,Object> cfg;
        try(java.io.InputStream in=Files.newInputStream(Paths.get("backend/crmeb-admin/src/main/resources/application.yml"))){cfg=new Yaml().load(in);}
        Map<String,Object> ds=(Map<String,Object>)((Map<String,Object>)cfg.get("spring")).get("datasource");
        if(!ds.get("url").toString().startsWith("jdbc:mysql://127.0.0.1:"))throw new IllegalStateException("Local only");
        try(Connection c=DriverManager.getConnection(ds.get("url").toString(),ds.get("username").toString(),ds.get("password").toString())){
            c.setAutoCommit(false);exec(c,"SET time_zone='+08:00'");
            String mode=args.length==0?"audit":args[0];
            if(mode.equals("verify")){verifyAffordable(c);report(c);c.rollback();return;}
            if(mode.equals("check") || mode.equals("apply")) {
                try {
                    require(c,"SELECT COUNT(*) FROM eb_flower_product_ext WHERE product_id NOT BETWEEN 900001 AND 900036",0);
                    require(c,"SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE o.pay_type<>'mock' OR o.uid<>950001 OR o.order_id NOT LIKE 'DEMO-%' OR o.refund_status<>0 OR o.refund_price<>0 OR o.coupon_price<>0 OR o.deduction_price<>0 OR o.total_postage<>0 OR o.pay_postage<>0 OR o.use_integral<>0",0);
                    require(c,"SELECT COUNT(*) FROM eb_store_order_info i JOIN eb_flower_order_ext e ON e.order_id=i.order_no LEFT JOIN eb_store_product_attr_value v ON v.id=i.attr_value_id AND v.product_id=i.product_id WHERE v.id IS NULL OR i.product_id NOT BETWEEN 900001 AND 900036 OR JSON_VALID(i.info)=0",0);
                    Map<String,List<String>> protectedRows=protectedRows(c);
                    if(mode.equals("apply"))backup(c);
                    LocalMarketPricing.updatePrices(c);
                    exec(c,"UPDATE eb_store_order_info i JOIN eb_store_order o ON o.id=i.order_id AND o.order_id=i.order_no JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_store_product_attr_value v ON v.id=i.attr_value_id AND v.product_id=i.product_id SET i.update_time=i.update_time,i.price=v.price,i.vip_price=v.price,i.info=JSON_SET(i.info,'$.price',v.price,'$.vipPrice',v.price) WHERE o.pay_type='mock' AND o.uid=950001 AND o.order_id LIKE 'DEMO-%'");
                    exec(c,"UPDATE eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN (SELECT order_id,SUM(price*pay_num) amount FROM eb_store_order_info GROUP BY order_id) i ON i.order_id=o.id SET o.update_time=o.update_time,o.total_price=i.amount,o.pay_price=i.amount,o.pro_total_price=i.amount,o.before_pay_price=i.amount WHERE o.pay_type='mock' AND o.uid=950001 AND o.order_id LIKE 'DEMO-%'");
                    verifyAffordable(c);
                    Map<String,List<String>> after=protectedRows(c);
                    for(String key:protectedRows.keySet())if(!protectedRows.get(key).equals(after.get(key)))throw new IllegalStateException("Protected data changed: "+key);
                    // A second pass must produce identical financial data.
                    List<String> first=snapshotFinance(c);
                    LocalMarketPricing.updatePrices(c);
                    if(!first.equals(snapshotFinance(c)))throw new IllegalStateException("Catalog price update is not idempotent");
                    report(c);
                    if(mode.equals("apply")){c.commit();System.out.println("Affordable catalog and all mock order prices committed");}else{c.rollback();System.out.println("Dry-run validation passed and rolled back");}
                }catch(Exception error){c.rollback();throw error;}
                return;
            }
            if(!mode.equals("audit"))throw new IllegalArgumentException("audit/check/apply/verify");
            query(c,"SELECT p.id,p.store_name,p.price,p.cost,e.packaging,GROUP_CONCAT(CONCAT(v.id,':',v.price)) skus FROM eb_store_product p JOIN eb_flower_product_ext e ON e.product_id=p.id LEFT JOIN eb_store_product_attr_value v ON v.product_id=p.id AND v.type=0 AND v.is_del=0 GROUP BY p.id,p.store_name,p.price,p.cost,e.packaging ORDER BY p.id");
            query(c,"SELECT o.pay_type,o.refund_status,o.paid,o.status,e.current_stage,COUNT(*) orders_count FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id GROUP BY o.pay_type,o.refund_status,o.paid,o.status,e.current_stage");
            query(c,"SHOW COLUMNS FROM eb_store_order_info");
            query(c,"SELECT i.order_no,i.price,i.pay_num,i.vip_price,i.info FROM eb_store_order_info i JOIN eb_flower_order_ext e ON e.order_id=i.order_no ORDER BY i.id LIMIT 2");
            query(c,"SELECT o.order_id,o.total_price,o.pay_price,o.pro_total_price,o.before_pay_price,o.refund_price,o.total_postage,o.pay_postage,o.coupon_price,o.deduction_price,o.use_integral FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id ORDER BY o.id");
            c.rollback();
        }
    }
    static void require(Connection c,String sql,int n)throws Exception {if(!String.valueOf(n).equals(scalar(c,sql)))throw new IllegalStateException("Data precondition/consistency failure: "+sql);}
    static Map<String,List<String>> protectedRows(Connection c)throws Exception {
        Map<String,List<String>> r=new LinkedHashMap<>();
        String[][] checks={
            {"inventory","SELECT id,stock,sales,cost FROM eb_store_product ORDER BY id"},
            {"skuInventory","SELECT id,stock,sales,cost FROM eb_store_product_attr_value ORDER BY id"},
            {"stages","SELECT * FROM eb_flower_order_ext ORDER BY id"},
            {"progress","SELECT * FROM eb_flower_order_progress ORDER BY id"},
            {"orders","SELECT id,order_id,paid,status,refund_status,refund_price,total_num,create_time,pay_time,update_time FROM eb_store_order ORDER BY id"},
            {"otherOrders","SELECT o.* FROM eb_store_order o LEFT JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE e.order_id IS NULL ORDER BY o.id"},
            {"otherItems","SELECT i.* FROM eb_store_order_info i LEFT JOIN eb_flower_order_ext e ON e.order_id=i.order_no WHERE e.order_id IS NULL ORDER BY i.id"},
            {"otherProducts","SELECT * FROM eb_store_product WHERE id NOT BETWEEN 900001 AND 900036 ORDER BY id"},
            {"slots","SELECT * FROM eb_flower_delivery_slot ORDER BY id"},
            {"materials","SELECT * FROM eb_flower_material_batch ORDER BY id"}
        };
        for(String[] check:checks)r.put(check[0],LocalFlowerData.snapshot(c,check[1]));return r;
    }
    static List<String> snapshotFinance(Connection c)throws Exception {
        List<String> r=new ArrayList<>();
        for(String t:new String[]{"eb_store_product","eb_store_product_attr_value","eb_store_product_attr_result","eb_store_order","eb_store_order_info"})r.addAll(LocalFlowerData.snapshot(c,"SELECT * FROM "+t+" ORDER BY id"));
        return r;
    }
    static void verifyAffordable(Connection c)throws Exception {
        LocalMarketPricing.verify(c);
        require(c,"SELECT COUNT(*) FROM eb_store_product_attr_value WHERE product_id BETWEEN 900001 AND 900036 AND product_id NOT IN (900029,900030,900031) AND (price>99 OR ot_price>99 OR price<=0)",0);
        require(c,"SELECT COUNT(*) FROM eb_store_product_attr_value WHERE product_id IN (900029,900030,900031) AND (price<119 OR price>169)",0);
        require(c,"SELECT COUNT(*) FROM eb_store_order_info i JOIN eb_flower_order_ext e ON e.order_id=i.order_no JOIN eb_store_product_attr_value v ON v.id=i.attr_value_id WHERE i.price<>v.price OR i.vip_price<>v.price OR CAST(JSON_EXTRACT(i.info,'$.price') AS DECIMAL(10,2))<>v.price OR CAST(JSON_EXTRACT(i.info,'$.vipPrice') AS DECIMAL(10,2))<>v.price",0);
        require(c,"SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN (SELECT order_id,SUM(price*pay_num) amount FROM eb_store_order_info GROUP BY order_id) i ON i.order_id=o.id WHERE o.total_price<>i.amount OR o.pay_price<>i.amount OR o.pro_total_price<>i.amount OR o.before_pay_price<>i.amount",0);
    }
    static void report(Connection c)throws Exception {
        query(c,"SELECT IF(id IN (900029,900030,900031),'premium_box','affordable') price_group,COUNT(*) products,MIN(price) min_price,MAX(price) max_price FROM eb_store_product WHERE id BETWEEN 900001 AND 900036 GROUP BY price_group");
        query(c,"SELECT e.current_stage,COUNT(*) orders,SUM(o.pay_price) sales FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id GROUP BY e.current_stage");
        query(c,"SELECT COUNT(*) today_orders,SUM(pay_price) today_sales FROM eb_store_order WHERE DATE(pay_time)='2026-11-03' AND paid=1 AND refund_status=0 AND is_del=0 AND is_system_del=0");
    }
    static void backup(Connection c)throws Exception {
        StringBuilder out=new StringBuilder("-- Before affordable repricing: local demo price fields only.\nSTART TRANSACTION;\n");
        String[][] tables={
            {"eb_store_product","id BETWEEN 900001 AND 900036","price,vip_price,ot_price,version"},
            {"eb_store_product_attr_value","product_id BETWEEN 900001 AND 900036","price,ot_price"},
            {"eb_store_product_attr_result","product_id BETWEEN 900001 AND 900036","result"},
            {"eb_store_order","order_id IN (SELECT order_id FROM eb_flower_order_ext)","total_price,pay_price,pro_total_price,before_pay_price"},
            {"eb_store_order_info","order_no IN (SELECT order_id FROM eb_flower_order_ext)","price,vip_price,info"}
        };
        for(String[] t:tables)try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT id,"+t[2]+" FROM "+t[0]+" WHERE "+t[1]+" ORDER BY id")) {
            while(rs.next()) {
                List<String> assignments=new ArrayList<>();
                for(int n=2;n<=rs.getMetaData().getColumnCount();n++) {
                    String value=rs.getString(n),literal="NULL";
                    if(value!=null){StringBuilder hex=new StringBuilder();for(byte b:value.getBytes(StandardCharsets.UTF_8))hex.append(String.format("%02x",b&255));literal=value.isEmpty()?"''":"CONVERT(0x"+hex+" USING utf8mb4)";}
                    assignments.add("`"+rs.getMetaData().getColumnLabel(n)+"`="+literal);
                }
                out.append("UPDATE ").append(t[0]).append(" SET ").append(String.join(",",assignments)).append(" WHERE id=").append(rs.getLong(1)).append(";\n");
            }
        }
        out.append("COMMIT;\n");
        Path file=Paths.get("deliverables/affordable-pricing-before-"+System.currentTimeMillis()+".sql");
        Files.write(file,out.toString().getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW);
        System.out.println("Price backup saved: "+file);
    }
}
