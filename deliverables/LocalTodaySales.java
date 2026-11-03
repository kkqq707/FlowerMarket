import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/** Append one explicitly dated batch of eight local mock sales; check rolls back. */
public class LocalTodaySales {
    static final String DAY = "2026-11-03";
    static final String PREFIX = "DEMO-SALES-20261103-";
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "audit" : args[0];
        if (!Arrays.asList("audit", "check", "apply").contains(mode)) throw new IllegalArgumentException("audit/check/apply");
        Map<String,Object> config;
        try (java.io.InputStream in = Files.newInputStream(Paths.get("backend/crmeb-admin/src/main/resources/application.yml"))) {
            config = new Yaml().load(in);
        }
        Map<String,Object> ds = (Map<String,Object>)((Map<String,Object>)config.get("spring")).get("datasource");
        String url = ds.get("url").toString();
        if (!url.startsWith("jdbc:mysql://127.0.0.1:")) throw new IllegalStateException("Local database only");
        try (Connection c = DriverManager.getConnection(url, ds.get("username").toString(), ds.get("password").toString())) {
            c.setAutoCommit(false);
            exec(c, "SET time_zone='+08:00'");
            if (!DAY.equals(scalar(c,"SELECT DATE_FORMAT(NOW(),'%Y-%m-%d')"))) throw new IllegalStateException("Expected Shanghai date "+DAY);
            query(c, "SELECT COUNT(*) today_orders,COALESCE(SUM(o.pay_price),0) today_sales FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE o.paid=1 AND o.refund_status=0 AND o.is_del=0 AND o.is_system_del=0 AND o.pay_time>='"+DAY+"' AND o.pay_time<DATE_ADD('"+DAY+"',INTERVAL 1 DAY)");
            query(c, "SELECT o.order_id,o.pay_price,o.total_num,i.product_id,i.attr_value_id,e.current_stage FROM eb_store_order o JOIN eb_store_order_info i ON i.order_id=o.id JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE o.order_id BETWEEN 'DEMO-FLOWER-0013' AND 'DEMO-FLOWER-0020' ORDER BY o.order_id");
            if (mode.equals("audit")) { c.rollback(); return; }
            try {
                int present = Integer.parseInt(scalar(c,"SELECT COUNT(*) FROM eb_store_order WHERE order_id LIKE '"+PREFIX+"%'"));
                if (present == 8) { verify(c); c.rollback(); System.out.println("Batch already exists; no changes"); return; }
                if (present != 0) throw new IllegalStateException("Partial batch exists");
                long before = Long.parseLong(scalar(c,"SELECT COUNT(*) FROM eb_store_order"));
                BigDecimal amount = BigDecimal.ZERO;
                String slot = scalar(c,"SELECT id FROM eb_flower_delivery_slot WHERE delivery_date='"+DAY+"' AND enabled=1 AND capacity-booked_count>=8 AND cutoff_time>NOW() ORDER BY start_time LIMIT 1 FOR UPDATE");
                if (slot == null) throw new IllegalStateException("No available delivery slot for eight orders");
                Timestamp now = Timestamp.valueOf(scalar(c,"SELECT DATE_FORMAT(NOW(),'%Y-%m-%d %H:%i:%s')"));
                for (int n=1;n<=8;n++) {
                    String source=String.format("DEMO-FLOWER-%04d",n+12), target=PREFIX+String.format("%02d",n);
                    String sourceId=scalar(c,"SELECT id FROM eb_store_order WHERE order_id='"+source+"' AND uid=950001 AND paid=1 AND pay_type='mock' AND refund_status=0 FOR UPDATE");
                    if(sourceId==null) throw new IllegalStateException("Missing mock source "+source);
                    Timestamp created=new Timestamp(now.getTime()-(9-n)*60000L);
                    Map<String,Object> order=overrides("order_id",target,"create_time",created,"pay_time",created,"update_time",created,"status",0,"paid",1,"refund_status",0,
                        "mark","今日销售演示订单（无真实支付）","remark","2026-11-03新增演示数据","out_trade_no","MOCK-"+target,
                        "delivery_type","","delivery_name","","delivery_id","");
                    cloneRow(c,"eb_store_order","id="+sourceId,order);
                    String newId=scalar(c,"SELECT id FROM eb_store_order WHERE order_id='"+target+"'");
                    cloneRow(c,"eb_store_order_info","order_id="+sourceId,overrides("order_id",Long.valueOf(newId),"order_no",target,"unique",target+"-item","create_time",created,"update_time",created));
                    cloneRow(c,"eb_flower_order_ext","order_id='"+source+"'",overrides("order_id",target,"delivery_date",java.sql.Date.valueOf(DAY),"slot_id",Long.valueOf(slot),"current_stage","WAIT_ACCEPT","courier_name","","courier_phone","","delivery_remark","今日销售演示订单，无真实支付","create_time",created,"update_time",created));
                    exec(c,"INSERT INTO eb_flower_order_progress(order_id,stage,operator_id,proof_image,remark,create_time) VALUES ('"+target+"','WAIT_ACCEPT',NULL,'','演示：模拟支付完成，等待花店接单','"+created+"')");
                    String itemCount=scalar(c,"SELECT COUNT(*) FROM eb_store_order_info WHERE order_id="+newId);
                    if(!"1".equals(itemCount)) throw new IllegalStateException("Expected one line per order");
                    String product=scalar(c,"SELECT product_id FROM eb_store_order_info WHERE order_id="+newId);
                    if(!"1".equals(scalar(c,"SELECT COUNT(*) FROM eb_flower_product_ext WHERE product_id="+product+" AND batch_managed=0"))) throw new IllegalStateException("Batch-managed source requires material allocation");
                    if(!"0".equals(scalar(c,"SELECT COUNT(*) FROM eb_flower_recipe WHERE product_id="+product))) throw new IllegalStateException("Unexpected material recipe");
                    int sku=exec(c,"UPDATE eb_store_product_attr_value v JOIN eb_store_order_info i ON i.attr_value_id=v.id SET v.stock=v.stock-i.pay_num,v.sales=v.sales+i.pay_num WHERE i.order_id="+newId+" AND v.stock>=i.pay_num");
                    int productUpdated=exec(c,"UPDATE eb_store_product p JOIN eb_store_order_info i ON i.product_id=p.id SET p.stock=p.stock-i.pay_num,p.sales=p.sales+i.pay_num,p.version=p.version+1 WHERE i.order_id="+newId+" AND p.stock>=i.pay_num");
                    if(sku!=1 || productUpdated!=1) throw new IllegalStateException("Stock update failed");
                    amount=amount.add(new BigDecimal(scalar(c,"SELECT pay_price FROM eb_store_order WHERE id="+newId)));
                }
                if(exec(c,"UPDATE eb_flower_delivery_slot SET booked_count=booked_count+8,version=version+1,update_time=NOW() WHERE id="+slot+" AND capacity-booked_count>=8")!=1) throw new IllegalStateException("Slot capacity changed");
                if(Long.parseLong(scalar(c,"SELECT COUNT(*) FROM eb_store_order"))!=before+8) throw new IllegalStateException("Unexpected order delta");
                verify(c);
                if(mode.equals("check")) { c.rollback(); System.out.println("Dry run rolled back: 8 orders, amount="+amount); }
                else { c.commit(); System.out.println("Committed: 8 new mock sales for "+DAY+", amount="+amount); }
            } catch(Exception ex) { c.rollback(); throw ex; }
        }
    }
    static void verify(Connection c)throws Exception {
        String valid="SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_store_order_info i ON i.order_id=o.id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id WHERE o.order_id LIKE '"+PREFIX+"%' AND o.paid=1 AND o.pay_type='mock' AND o.refund_status=0 AND o.is_del=0 AND o.is_system_del=0 AND DATE(o.pay_time)='"+DAY+"' AND e.delivery_date=s.delivery_date AND e.current_stage='WAIT_ACCEPT' AND i.order_no=o.order_id AND JSON_VALID(i.info)=1";
        if(!"8".equals(scalar(c,valid))) throw new IllegalStateException("Batch association validation failed");
        query(c,"SELECT COUNT(*) batch_orders,SUM(pay_price) batch_sales FROM eb_store_order WHERE order_id LIKE '"+PREFIX+"%'");
    }
    static Map<String,Object> overrides(Object... args) { Map<String,Object> m=new HashMap<>(); for(int i=0;i<args.length;i+=2)m.put(args[i].toString(),args[i+1]);return m; }
    static void cloneRow(Connection c,String table,String where,Map<String,Object> overrides)throws Exception {
        List<String> cols=new ArrayList<>(); List<Object> vals=new ArrayList<>();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM "+table+" WHERE "+where)) {
            if(!r.next())throw new IllegalStateException("Missing source in "+table);
            ResultSetMetaData md=r.getMetaData();
            for(int i=1;i<=md.getColumnCount();i++){String key=md.getColumnName(i);if(md.isAutoIncrement(i))continue;cols.add("`"+key+"`");vals.add(overrides.containsKey(key)?overrides.get(key):r.getObject(i));}
            if(r.next())throw new IllegalStateException("Ambiguous source in "+table);
        }
        try(PreparedStatement p=c.prepareStatement("INSERT INTO "+table+" ("+String.join(",",cols)+") VALUES ("+String.join(",",Collections.nCopies(vals.size(),"?"))+")")) {
            for(int i=0;i<vals.size();i++)p.setObject(i+1,vals.get(i));
            if(p.executeUpdate()!=1)throw new IllegalStateException("Insert failed");
        }
    }
    static String scalar(Connection c,String sql)throws Exception {try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){return r.next()?r.getString(1):null;}}
    static int exec(Connection c,String sql)throws Exception {try(Statement s=c.createStatement()){return s.executeUpdate(sql);}}
    static void query(Connection c,String sql)throws Exception {try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){while(r.next()){StringJoiner row=new StringJoiner(" | ");for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.add(r.getMetaData().getColumnLabel(i)+"="+r.getString(i));System.out.println(row);}}}
}
