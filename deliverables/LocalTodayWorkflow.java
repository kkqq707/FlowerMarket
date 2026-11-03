import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Explicitly requested local mock data: complete today's eight, add two per pending stage. */
public class LocalTodayWorkflow extends LocalTodaySales {
    static final String NEW_PREFIX="DEMO-STAGES-20261103-";
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String mode=args.length==0?"audit":args[0];
        if(!Arrays.asList("audit","check","apply","verify","ontime-check","ontime-apply","ontime-verify").contains(mode))throw new IllegalArgumentException("Invalid mode");
        Map<String,Object> config;
        try(java.io.InputStream in=Files.newInputStream(Paths.get("backend/crmeb-admin/src/main/resources/application.yml"))){config=new Yaml().load(in);}
        Map<String,Object> ds=(Map<String,Object>)((Map<String,Object>)config.get("spring")).get("datasource");
        if(!ds.get("url").toString().startsWith("jdbc:mysql://127.0.0.1:"))throw new IllegalStateException("Local only");
        try(Connection c=DriverManager.getConnection(ds.get("url").toString(),ds.get("username").toString(),ds.get("password").toString())) {
            c.setAutoCommit(false);
            exec(c,"SET time_zone='+08:00'");
            if(!DAY.equals(scalar(c,"SELECT DATE_FORMAT(NOW(),'%Y-%m-%d')")))throw new IllegalStateException("Date mismatch");
            if(mode.startsWith("ontime-")) {
                try {
                    String demo="o.order_id LIKE '"+PREFIX+"%' AND o.uid=950001 AND o.pay_type='mock' AND o.paid=1 AND o.refund_status=0 AND e.current_stage='COMPLETED' AND DATE(o.pay_time)='"+DAY+"'";
                    if(!"8".equals(scalar(c,"SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id WHERE "+demo+" AND s.delivery_date='"+DAY+"' AND TIMESTAMP(s.delivery_date,s.end_time)-INTERVAL 15 MINUTE>=TIMESTAMP(s.delivery_date,s.start_time) AND TIMESTAMP(s.delivery_date,s.end_time)-INTERVAL 15 MINUTE>=o.pay_time FOR UPDATE")))throw new IllegalStateException("Expected eight valid completed mock orders");
                    if(!mode.equals("ontime-verify"))exec(c,"UPDATE eb_flower_order_progress p JOIN eb_store_order o ON o.order_id=p.order_id JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id SET p.create_time=TIMESTAMP(s.delivery_date,s.end_time)-INTERVAL 15 MINUTE,p.remark='演示数据：按用户要求设定预约时段结束前15分钟准时送达，无真实配送' WHERE "+demo+" AND p.stage='COMPLETED'");
                    if(!"8".equals(scalar(c,"SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id WHERE "+demo+" AND (SELECT MAX(p.create_time) FROM eb_flower_order_progress p WHERE p.order_id=o.order_id AND p.stage='COMPLETED') BETWEEN TIMESTAMP(s.delivery_date,s.start_time) AND TIMESTAMP(s.delivery_date,s.end_time)")))throw new IllegalStateException("On-time validation failed");
                    query(c,"SELECT o.order_id,s.start_time,s.end_time,MAX(p.create_time) completed_time FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id JOIN eb_flower_order_progress p ON p.order_id=o.order_id AND p.stage='COMPLETED' WHERE "+demo+" GROUP BY o.order_id,s.start_time,s.end_time ORDER BY o.order_id");
                    query(c,"SELECT COUNT(*) completed_orders,SUM(completed_time<=promised_time) on_time_orders,ROUND(SUM(completed_time<=promised_time)*100.0/COUNT(*),2) on_time_rate FROM (SELECT TIMESTAMP(s.delivery_date,s.end_time) promised_time,(SELECT MAX(p.create_time) FROM eb_flower_order_progress p WHERE p.order_id=o.order_id AND p.stage='COMPLETED') completed_time FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id JOIN eb_flower_delivery_slot s ON s.id=e.slot_id WHERE o.paid=1 AND o.refund_status=0 AND o.is_del=0 AND o.is_system_del=0 AND DATE(o.pay_time)='"+DAY+"' AND e.current_stage='COMPLETED') metrics WHERE completed_time IS NOT NULL AND promised_time IS NOT NULL");
                    if(mode.equals("ontime-apply")){c.commit();System.out.println("Committed on-time mock timestamps");}else{c.rollback();System.out.println("Verified; transaction rolled back");}
                }catch(Exception e){c.rollback();throw e;}
                return;
            }
            if(mode.equals("audit")){report(c);c.rollback();return;}
            if(mode.equals("verify")){verifyWorkflow(c);report(c);c.rollback();return;}
            try {
                String scope="o.order_id LIKE '"+PREFIX+"%'";
                if(!"8".equals(scalar(c,"SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE "+scope+" AND o.uid=950001 AND o.pay_type='mock' AND o.paid=1 AND o.refund_status=0 AND o.is_del=0 AND o.is_system_del=0 AND DATE(o.create_time)='"+DAY+"' AND e.current_stage IN ('WAIT_ACCEPT','COMPLETED') FOR UPDATE")))throw new IllegalStateException("Original mock batch changed");
                for(int n=1;n<=8;n++) {
                    String order=PREFIX+String.format("%02d",n);
                    String stage=scalar(c,"SELECT current_stage FROM eb_flower_order_ext WHERE order_id='"+order+"' FOR UPDATE");
                    if(stage.equals("COMPLETED"))continue;
                    exec(c,"UPDATE eb_store_order SET status=2,delivery_type='send',delivery_name='演示配送员',update_time=NOW() WHERE order_id='"+order+"' AND pay_type='mock'");
                    exec(c,"UPDATE eb_flower_order_ext SET current_stage='COMPLETED',courier_name='演示配送员',delivery_remark='按用户要求将本地演示订单标记为已送达',update_time=NOW() WHERE order_id='"+order+"'");
                    exec(c,"INSERT INTO eb_flower_order_progress(order_id,stage,operator_id,proof_image,remark,create_time) VALUES ('"+order+"','COMPLETED',NULL,'','演示数据调整：按用户要求标记已送达，无真实配送',NOW())");
                }
                int existing=Integer.parseInt(scalar(c,"SELECT COUNT(*) FROM eb_store_order WHERE order_id LIKE '"+NEW_PREFIX+"%'"));
                if(existing!=0 && existing!=6)throw new IllegalStateException("Partial pending batch exists");
                if(existing==0) {
                    String slot=scalar(c,"SELECT s.id FROM eb_flower_delivery_slot s WHERE s.delivery_date='"+DAY+"' AND s.capacity-s.booked_count>=6 AND s.enabled=1 ORDER BY s.start_time DESC LIMIT 1 FOR UPDATE");
                    if(slot==null)throw new IllegalStateException("No capacity for six demo orders");
                    String[] stages={"WAIT_ACCEPT","WAIT_PRODUCTION","WAIT_DELIVERY"};
                    for(int n=1;n<=6;n++) {
                        String source=PREFIX+String.format("%02d",n),target=NEW_PREFIX+String.format("%02d",n),stage=stages[(n-1)/2];
                        String sourceId=scalar(c,"SELECT id FROM eb_store_order WHERE order_id='"+source+"' AND uid=950001 AND pay_type='mock'");
                        if(sourceId==null)throw new IllegalStateException("Missing source");
                        Timestamp now=Timestamp.valueOf(scalar(c,"SELECT DATE_FORMAT(NOW(),'%Y-%m-%d %H:%i:%s')"));
                        cloneRow(c,"eb_store_order","id="+sourceId,overrides("order_id",target,"status",0,"create_time",now,"pay_time",now,"update_time",now,"mark","今日流程演示订单（无真实支付）","remark","按用户要求新增各状态演示订单","out_trade_no","MOCK-"+target,"delivery_type","","delivery_name","","delivery_id",""));
                        String id=scalar(c,"SELECT id FROM eb_store_order WHERE order_id='"+target+"'");
                        cloneRow(c,"eb_store_order_info","order_id="+sourceId,overrides("order_id",Long.valueOf(id),"order_no",target,"unique",target+"-item","create_time",now,"update_time",now));
                        cloneRow(c,"eb_flower_order_ext","order_id='"+source+"'",overrides("order_id",target,"current_stage",stage,"slot_id",Long.valueOf(slot),"courier_name","","courier_phone","","delivery_remark","本地流程演示数据，无真实支付或配送","create_time",now,"update_time",now));
                        String product=scalar(c,"SELECT product_id FROM eb_store_order_info WHERE order_id="+id);
                        if(!"1".equals(scalar(c,"SELECT COUNT(*) FROM eb_flower_product_ext WHERE product_id="+product+" AND batch_managed=0")) || !"0".equals(scalar(c,"SELECT COUNT(*) FROM eb_flower_recipe WHERE product_id="+product)))throw new IllegalStateException("Source requires material allocation");
                        if(exec(c,"UPDATE eb_store_product_attr_value v JOIN eb_store_order_info i ON i.attr_value_id=v.id SET v.stock=v.stock-i.pay_num,v.sales=v.sales+i.pay_num WHERE i.order_id="+id+" AND v.stock>=i.pay_num")!=1)throw new IllegalStateException("SKU stock");
                        if(exec(c,"UPDATE eb_store_product p JOIN eb_store_order_info i ON i.product_id=p.id SET p.stock=p.stock-i.pay_num,p.sales=p.sales+i.pay_num,p.version=p.version+1 WHERE i.order_id="+id+" AND p.stock>=i.pay_num")!=1)throw new IllegalStateException("Product stock");
                        for(String progress:new String[]{"WAIT_ACCEPT","WAIT_PRODUCTION","PRODUCING","WAIT_DELIVERY"}) {
                            exec(c,"INSERT INTO eb_flower_order_progress(order_id,stage,operator_id,proof_image,remark,create_time) VALUES ('"+target+"','"+progress+"',NULL,'','演示：初始化订单流程状态，无真实履约',NOW())");
                            if(progress.equals(stage))break;
                        }
                    }
                    if(exec(c,"UPDATE eb_flower_delivery_slot SET booked_count=booked_count+6,version=version+1,update_time=NOW() WHERE id="+slot+" AND capacity-booked_count>=6")!=1)throw new IllegalStateException("Slot changed");
                }
                verifyWorkflow(c);report(c);
                if(mode.equals("apply")){c.commit();System.out.println("Committed: 8 completed, 6 pending demo orders");}
                else{c.rollback();System.out.println("Dry run verified and rolled back");}
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    static void verifyWorkflow(Connection c)throws Exception {
        if(!"8".equals(scalar(c,"SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE o.order_id LIKE '"+PREFIX+"%' AND o.status=2 AND e.current_stage='COMPLETED' AND EXISTS(SELECT 1 FROM eb_flower_order_progress p WHERE p.order_id=o.order_id AND p.stage='COMPLETED')")))throw new IllegalStateException("Completion mismatch");
        for(String stage:new String[]{"WAIT_ACCEPT","WAIT_PRODUCTION","WAIT_DELIVERY"})if(!"2".equals(scalar(c,"SELECT COUNT(*) FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE o.order_id LIKE '"+NEW_PREFIX+"%' AND e.current_stage='"+stage+"' AND o.paid=1 AND o.pay_type='mock' AND o.status=0 AND o.refund_status=0 AND o.is_del=0 AND o.is_system_del=0")))throw new IllegalStateException("Pending batch mismatch: "+stage);
    }
    static void report(Connection c)throws Exception {
        query(c,"SELECT s.id,s.delivery_date,s.start_time,s.end_time,s.capacity,s.booked_count,s.enabled FROM eb_flower_delivery_slot s WHERE s.delivery_date='"+DAY+"'");
        query(c,"SELECT e.current_stage,COUNT(*) orders,SUM(o.pay_price) amount FROM eb_store_order o JOIN eb_flower_order_ext e ON e.order_id=o.order_id WHERE DATE(o.create_time)='"+DAY+"' GROUP BY e.current_stage");
        query(c,"SELECT COUNT(*) today_orders,SUM(pay_price) today_paid FROM eb_store_order WHERE DATE(create_time)='"+DAY+"'");
    }
}
