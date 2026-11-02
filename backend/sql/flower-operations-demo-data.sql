-- Local development only. Preserve the twelve workbench orders and add eighteen recent completed orders.
-- Run using deliverables/LocalOperationsData.java (check/apply/verify); it owns the transaction.
-- Existing orders, progressed demo orders and non-demo batches are never reset.
SET @ops_today = CURDATE();
SET @ops_now = NOW();
SET @ops_uid = 950001;
SET @ops_orders = '[
 {"n":1,"rank":1,"stage":"WAIT_ACCEPT","day":1,"slot":1},
 {"n":2,"rank":1,"stage":"WAIT_ACCEPT","day":1,"slot":2},
 {"n":3,"rank":2,"stage":"WAIT_PRODUCTION","day":1,"slot":1},
 {"n":4,"rank":2,"stage":"WAIT_PRODUCTION","day":1,"slot":2},
 {"n":5,"rank":3,"stage":"PRODUCING","day":0,"slot":2},
 {"n":6,"rank":3,"stage":"PRODUCING","day":0,"slot":3},
 {"n":7,"rank":4,"stage":"WAIT_DELIVERY","day":0,"slot":2},
 {"n":8,"rank":4,"stage":"WAIT_DELIVERY","day":0,"slot":3},
 {"n":9,"rank":5,"stage":"DELIVERING","day":0,"slot":2},
 {"n":10,"rank":5,"stage":"DELIVERING","day":0,"slot":3},
 {"n":11,"rank":6,"stage":"COMPLETED","day":0,"slot":1},
 {"n":12,"rank":6,"stage":"COMPLETED","day":0,"slot":1},
 {"n":13,"rank":6,"stage":"COMPLETED","day":-6,"slot":1},
 {"n":14,"rank":6,"stage":"COMPLETED","day":-6,"slot":2},
 {"n":15,"rank":6,"stage":"COMPLETED","day":-6,"slot":3},
 {"n":16,"rank":6,"stage":"COMPLETED","day":-5,"slot":1},
 {"n":17,"rank":6,"stage":"COMPLETED","day":-5,"slot":2},
 {"n":18,"rank":6,"stage":"COMPLETED","day":-5,"slot":3},
 {"n":19,"rank":6,"stage":"COMPLETED","day":-4,"slot":1},
 {"n":20,"rank":6,"stage":"COMPLETED","day":-4,"slot":2},
 {"n":21,"rank":6,"stage":"COMPLETED","day":-4,"slot":3},
 {"n":22,"rank":6,"stage":"COMPLETED","day":-3,"slot":1},
 {"n":23,"rank":6,"stage":"COMPLETED","day":-3,"slot":2},
 {"n":24,"rank":6,"stage":"COMPLETED","day":-3,"slot":3},
 {"n":25,"rank":6,"stage":"COMPLETED","day":-2,"slot":1},
 {"n":26,"rank":6,"stage":"COMPLETED","day":-2,"slot":2},
 {"n":27,"rank":6,"stage":"COMPLETED","day":-2,"slot":3},
 {"n":28,"rank":6,"stage":"COMPLETED","day":-1,"slot":1},
 {"n":29,"rank":6,"stage":"COMPLETED","day":-1,"slot":2},
 {"n":30,"rank":6,"stage":"COMPLETED","day":-1,"slot":3}
]';
SET @ops_new_numbers = (
 SELECT COALESCE(JSON_ARRAYAGG(d.n), JSON_ARRAY())
 FROM JSON_TABLE(@ops_orders, '$[*]' COLUMNS(n INT PATH '$.n')) d
 WHERE NOT EXISTS (SELECT 1 FROM eb_store_order o WHERE o.order_id=CONCAT('DEMO-FLOWER-',LPAD(d.n,4,'0')))
);

-- The audited old slots are unbooked demo slots. Move that unreferenced group together.
SET @ops_slot_shift = (
 SELECT DATEDIFF(DATE_ADD(@ops_today,INTERVAL 1 DAY), MIN(s.delivery_date))
 FROM eb_flower_delivery_slot s
 WHERE s.id BETWEEN 9000000 AND 9999999 AND s.delivery_date<@ops_today AND s.booked_count=0
 AND NOT EXISTS (SELECT 1 FROM eb_flower_order_ext e WHERE e.slot_id=s.id)
);
UPDATE eb_flower_delivery_slot s
SET s.delivery_date=DATE_ADD(s.delivery_date, INTERVAL @ops_slot_shift DAY),
    s.cutoff_time=DATE_ADD(s.cutoff_time, INTERVAL @ops_slot_shift DAY),
    s.version=s.version+1,s.update_time=@ops_now
WHERE @ops_slot_shift>0 AND s.id BETWEEN 9000000 AND 9999999
  AND s.delivery_date<@ops_today AND s.booked_count=0
  AND NOT EXISTS (SELECT 1 FROM eb_flower_order_ext e WHERE e.slot_id=s.id);

-- Today plus the next fourteen days; existing capacity/bookings remain intact.
INSERT INTO eb_flower_delivery_slot
 (id,delivery_date,start_time,end_time,capacity,booked_count,cutoff_time,enabled,version,create_time,update_time)
WITH RECURSIVE days AS (
 SELECT 0 AS day_offset UNION ALL SELECT day_offset+1 FROM days WHERE day_offset<14
), times AS (
 SELECT 1 AS slot_index,CAST('09:00:00' AS TIME) AS start_time,CAST('12:00:00' AS TIME) AS end_time
 UNION ALL SELECT 2,'14:00:00','17:00:00' UNION ALL SELECT 3,'18:00:00','21:00:00'
)
SELECT 9000000+DATEDIFF(DATE_ADD(@ops_today,INTERVAL days.day_offset DAY),'2020-01-01')*10+times.slot_index,
 DATE_ADD(@ops_today,INTERVAL days.day_offset DAY),times.start_time,times.end_time,12,0,
 TIMESTAMP(DATE_ADD(@ops_today,INTERVAL days.day_offset DAY),times.start_time)-INTERVAL 2 HOUR,1,0,@ops_now,@ops_now
FROM days CROSS JOIN times
WHERE NOT EXISTS (SELECT 1 FROM eb_flower_delivery_slot s
 WHERE s.delivery_date=DATE_ADD(@ops_today,INTERVAL days.day_offset DAY)
 AND s.start_time=times.start_time AND s.end_time=times.end_time);

-- Historical delivery windows are disabled so customers cannot book past days.
INSERT INTO eb_flower_delivery_slot
 (id,delivery_date,start_time,end_time,capacity,booked_count,cutoff_time,enabled,version,create_time,update_time)
WITH RECURSIVE days AS (
 SELECT -6 AS day_offset UNION ALL SELECT day_offset+1 FROM days WHERE day_offset<-1
), times AS (
 SELECT 1 AS slot_index,CAST('09:00:00' AS TIME) AS start_time,CAST('12:00:00' AS TIME) AS end_time
 UNION ALL SELECT 2,'14:00:00','17:00:00' UNION ALL SELECT 3,'18:00:00','21:00:00'
)
SELECT 9000000+DATEDIFF(DATE_ADD(@ops_today,INTERVAL days.day_offset DAY),'2020-01-01')*10+times.slot_index,
 DATE_ADD(@ops_today,INTERVAL days.day_offset DAY),times.start_time,times.end_time,12,0,
 TIMESTAMP(DATE_ADD(@ops_today,INTERVAL days.day_offset DAY),times.start_time)-INTERVAL 2 HOUR,0,0,
 TIMESTAMP(DATE_ADD(@ops_today,INTERVAL days.day_offset DAY),'06:00:00'),@ops_now
FROM days CROSS JOIN times
WHERE NOT EXISTS (SELECT 1 FROM eb_flower_delivery_slot s
 WHERE s.delivery_date=DATE_ADD(@ops_today,INTERVAL days.day_offset DAY)
 AND s.start_time=times.start_time AND s.end_time=times.end_time);

-- Only refresh expired demo batches with no allocations; preserve quantities.
UPDATE eb_flower_material_batch b JOIN eb_flower_material m ON m.id=b.material_id
SET b.batch_no=CONCAT('DEMO-',DATE_FORMAT(DATE_SUB(@ops_today,INTERVAL 1 DAY),'%Y%m%d'),'-',m.id),
 b.received_date=DATE_SUB(@ops_today,INTERVAL 1 DAY),
 b.expiry_date=DATE_ADD(DATE_SUB(@ops_today,INTERVAL 1 DAY),INTERVAL m.fresh_days DAY),
 b.update_time=@ops_now
WHERE b.material_id BETWEEN 9001 AND 9011 AND b.batch_no LIKE 'DEMO-%'
 AND b.remark LIKE '演示入库批次%' AND b.expiry_date<@ops_today AND b.reserved_qty=0
 AND NOT EXISTS (SELECT 1 FROM eb_flower_material_allocation a WHERE a.batch_id=b.id);

-- Dedicated demo customer: no telephone, external login identity or account balance.
INSERT INTO eb_user (uid,account,nickname,real_name,phone,user_type,login_type,mark,status)
SELECT @ops_uid,'flower_ops_demo','花店订单演示顾客','演示顾客','','h5','h5','本地工作台演示数据；无真实支付',1
WHERE NOT EXISTS (SELECT 1 FROM eb_user WHERE uid=@ops_uid OR account='flower_ops_demo');

INSERT INTO eb_store_order
 (id,order_id,uid,real_name,user_phone,user_address,total_num,total_price,pay_price,cost,paid,pay_time,
 pay_type,create_time,status,mark,remark,is_del,is_system_del,shipping_type,is_channel,
 delivery_type,delivery_name,delivery_id,pro_total_price,before_pay_price,out_trade_no,update_time)
SELECT 950000+d.n,CONCAT('DEMO-FLOWER-',LPAD(d.n,4,'0')),@ops_uid,
 CONCAT('演示收花人',LPAD(d.n,2,'0')),CONCAT('13000001',LPAD(d.n,3,'0')),
 CONCAT('北京市朝阳区演示花园',d.n,'号（演示地址）'),
 IF(MOD(d.n,4)=0,2,1),v.price*IF(MOD(d.n,4)=0,2,1),v.price*IF(MOD(d.n,4)=0,2,1),
 v.cost*IF(MOD(d.n,4)=0,2,1),1,
 IF(d.day_offset<0,TIMESTAMP(DATE_ADD(@ops_today,INTERVAL d.day_offset DAY),CASE d.slot_index WHEN 1 THEN '09:00:00' WHEN 2 THEN '14:00:00' ELSE '18:00:00' END)-INTERVAL 155 MINUTE,DATE_SUB(@ops_now,INTERVAL 235 MINUTE)),
 'mock',IF(d.day_offset<0,TIMESTAMP(DATE_ADD(@ops_today,INTERVAL d.day_offset DAY),CASE d.slot_index WHEN 1 THEN '09:00:00' WHEN 2 THEN '14:00:00' ELSE '18:00:00' END)-INTERVAL 160 MINUTE,DATE_SUB(@ops_now,INTERVAL 4 HOUR)),CASE WHEN d.stage_rank=6 THEN 2 WHEN d.stage_rank=5 THEN 1 ELSE 0 END,
 '订单工作台演示数据（无真实支付）','演示订单，可用于查看制作与配送流程',0,0,1,2,
 IF(d.stage_rank>=5,'send',''),IF(d.stage_rank>=5,'演示配送员',''),IF(d.stage_rank>=5,'13000009999',''),
 v.price*IF(MOD(d.n,4)=0,2,1),v.price*IF(MOD(d.n,4)=0,2,1),
 CONCAT('MOCK-DEMO-FLOWER-',LPAD(d.n,4,'0')),
 IF(d.day_offset<0,TIMESTAMP(DATE_ADD(@ops_today,INTERVAL d.day_offset DAY),CASE d.slot_index WHEN 1 THEN '09:00:00' WHEN 2 THEN '14:00:00' ELSE '18:00:00' END)+INTERVAL 20 MINUTE,@ops_now)
FROM JSON_TABLE(@ops_orders,'$[*]' COLUMNS(n INT PATH '$.n',stage_rank INT PATH '$.rank',day_offset INT PATH '$.day',slot_index INT PATH '$.slot')) d
JOIN eb_store_product p ON p.id=900000+d.n AND p.is_del=0
JOIN eb_store_product_attr_value v ON v.id=(SELECT MIN(v2.id) FROM eb_store_product_attr_value v2 WHERE v2.product_id=p.id AND v2.type=0 AND v2.is_del=0)
WHERE d.n IN (SELECT n FROM JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh);

INSERT INTO eb_store_order_info
 (order_id,product_id,info,`unique`,order_no,product_name,attr_value_id,image,sku,price,pay_num,
 weight,volume,give_integral,is_reply,is_sub,vip_price,product_type,create_time,update_time)
SELECT o.id,p.id,
 JSON_OBJECT('productId',p.id,'productName',p.store_name,'attrValueId',v.id,
 'image',CONCAT('http://localhost:8080/',TRIM(LEADING '/' FROM p.image)),
 'sku',v.suk,'price',v.price,'payNum',o.total_num,'weight',v.weight,'volume',v.volume,
 'tempId',p.temp_id,'giveIntegral',0,'isReply',0,'isSub',FALSE,'vipPrice',v.price,'productType',0),
 MD5(CONCAT(o.order_id,'-item')),o.order_id,p.store_name,v.id,
 CONCAT('http://localhost:8080/',TRIM(LEADING '/' FROM p.image)),v.suk,v.price,o.total_num,
 v.weight,v.volume,0,0,0,v.price,0,o.create_time,o.update_time
FROM JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh
JOIN eb_store_order o ON o.order_id=CONCAT('DEMO-FLOWER-',LPAD(fresh.n,4,'0')) AND o.uid=@ops_uid
JOIN eb_store_product p ON p.id=900000+fresh.n
JOIN eb_store_product_attr_value v ON v.id=(SELECT MIN(v2.id) FROM eb_store_product_attr_value v2 WHERE v2.product_id=p.id AND v2.type=0 AND v2.is_del=0);

INSERT INTO eb_flower_order_ext
 (order_id,sender_name,sender_phone,recipient_name,recipient_phone,delivery_address_snapshot,
 delivery_zone_id,delivery_date,slot_id,greeting_card,anonymous,contact_recipient,substitution_policy,
 current_stage,courier_name,courier_phone,delivery_remark,reservation_expire_time,reservation_released,create_time,update_time)
SELECT o.order_id,CONCAT('演示订花人',LPAD(d.n,2,'0')),CONCAT('13000002',LPAD(d.n,3,'0')),
 o.real_name,o.user_phone,o.user_address,9001,s.delivery_date,s.id,
 CASE MOD(d.n,3) WHEN 0 THEN '愿每一天都有鲜花相伴。' WHEN 1 THEN '生日快乐，愿你常有欢喜。' ELSE '把这一束温柔送给你。' END,
 MOD(d.n,3)=0,1,'CONTACT_FIRST',d.stage_name,IF(d.stage_rank>=5,'演示配送员',''),
 IF(d.stage_rank>=5,'13000009999',''),'本地演示订单，无真实支付；配送信息为演示内容',NULL,0,o.create_time,o.update_time
FROM JSON_TABLE(@ops_orders,'$[*]' COLUMNS(n INT PATH '$.n',stage_rank INT PATH '$.rank',stage_name VARCHAR(32) PATH '$.stage',day_offset INT PATH '$.day',slot_index INT PATH '$.slot')) d
JOIN eb_store_order o ON o.order_id=CONCAT('DEMO-FLOWER-',LPAD(d.n,4,'0')) AND o.uid=@ops_uid
JOIN eb_flower_delivery_slot s ON s.delivery_date=DATE_ADD(@ops_today,INTERVAL d.day_offset DAY)
 AND s.start_time=CASE d.slot_index WHEN 1 THEN '09:00:00' WHEN 2 THEN '14:00:00' ELSE '18:00:00' END
WHERE d.n IN (SELECT n FROM JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh);

INSERT INTO eb_flower_order_progress (order_id,stage,operator_id,proof_image,remark,create_time)
SELECT o.order_id,steps.stage_name,NULL,
 IF(steps.stage_rank IN (4,6),i.image,''),
 CASE steps.stage_rank WHEN 1 THEN '演示：模拟支付完成，等待花店接单' WHEN 2 THEN '演示：花店已接单，等待制作'
 WHEN 3 THEN '演示：花艺师开始制作' WHEN 4 THEN '演示：制作完成；图片为示例商品图'
 WHEN 5 THEN '演示：配送员已取花出发' ELSE '演示：已送达；图片为示例商品图' END,
 DATE_ADD(o.create_time,INTERVAL (steps.stage_rank*IF(d.n>=13,30,20)) MINUTE)
FROM JSON_TABLE(@ops_orders,'$[*]' COLUMNS(n INT PATH '$.n',stage_rank INT PATH '$.rank')) d
JOIN eb_store_order o ON o.order_id=CONCAT('DEMO-FLOWER-',LPAD(d.n,4,'0')) AND o.uid=@ops_uid
JOIN eb_store_order_info i ON i.order_no=o.order_id
JOIN (SELECT 1 AS stage_rank,'WAIT_ACCEPT' AS stage_name UNION ALL SELECT 2,'WAIT_PRODUCTION'
 UNION ALL SELECT 3,'PRODUCING' UNION ALL SELECT 4,'WAIT_DELIVERY' UNION ALL SELECT 5,'DELIVERING' UNION ALL SELECT 6,'COMPLETED') steps
 ON steps.stage_rank<=d.stage_rank
WHERE d.n IN (SELECT n FROM JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh);

-- Reserve/consume the actual recipe quantities, once for newly inserted orders only.
INSERT INTO eb_flower_material_allocation (order_id,material_id,batch_id,quantity,status,create_time,update_time)
SELECT e.order_id,r.material_id,b.id,r.qty_per_item*i.pay_num,
 IF(e.current_stage IN ('WAIT_ACCEPT','WAIT_PRODUCTION'),'RESERVED','CONSUMED'),@ops_now,@ops_now
FROM JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh
JOIN eb_flower_order_ext e ON e.order_id=CONCAT('DEMO-FLOWER-',LPAD(fresh.n,4,'0'))
JOIN eb_store_order_info i ON i.order_no=e.order_id
JOIN eb_flower_recipe r ON r.product_id=i.product_id
JOIN eb_flower_material_batch b ON b.id=(SELECT MIN(b2.id) FROM eb_flower_material_batch b2
 WHERE b2.material_id=r.material_id AND b2.batch_no LIKE 'DEMO-%' AND b2.expiry_date>=e.delivery_date AND b2.received_date<=@ops_today);

UPDATE eb_flower_material_batch b JOIN (
 SELECT a.batch_id,SUM(IF(a.status='RESERVED',a.quantity,0)) AS reserved_qty,
 SUM(IF(a.status='CONSUMED',a.quantity,0)) AS consumed_qty
 FROM eb_flower_material_allocation a JOIN JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh
 ON a.order_id=CONCAT('DEMO-FLOWER-',LPAD(fresh.n,4,'0')) GROUP BY a.batch_id
) delta ON delta.batch_id=b.id
SET b.reserved_qty=b.reserved_qty+delta.reserved_qty,b.current_qty=b.current_qty-delta.consumed_qty,b.update_time=@ops_now;

UPDATE eb_flower_delivery_slot s JOIN (
 SELECT e.slot_id,COUNT(*) AS quantity FROM eb_flower_order_ext e
 JOIN JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh
 ON e.order_id=CONCAT('DEMO-FLOWER-',LPAD(fresh.n,4,'0')) GROUP BY e.slot_id
) delta ON delta.slot_id=s.id
SET s.booked_count=s.booked_count+delta.quantity,s.version=s.version+1,s.update_time=@ops_now;

UPDATE eb_store_product_attr_value v JOIN eb_store_order_info i ON i.attr_value_id=v.id
JOIN JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh
 ON i.order_no=CONCAT('DEMO-FLOWER-',LPAD(fresh.n,4,'0'))
SET v.stock=v.stock-i.pay_num,v.sales=v.sales+i.pay_num;
UPDATE eb_store_product p JOIN eb_store_order_info i ON i.product_id=p.id
JOIN JSON_TABLE(@ops_new_numbers,'$[*]' COLUMNS(n INT PATH '$')) fresh
 ON i.order_no=CONCAT('DEMO-FLOWER-',LPAD(fresh.n,4,'0'))
SET p.stock=p.stock-i.pay_num,p.sales=p.sales+i.pay_num,p.version=p.version+1;
