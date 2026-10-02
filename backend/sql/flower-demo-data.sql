-- 花间集：可重复导入的本地演示数据
-- 前置条件：已执行 Crmeb_v3.1.sql 与 v3.1-to-flower-mall-upgrade.sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- 门店基础配置。模拟支付只在演示库显式开启，升级脚本和生产默认值仍为 0。
UPDATE `eb_system_config` SET `value`='花间集', `title`='花店名称', `update_time`=NOW() WHERE `name`='flower_store_name';
INSERT INTO `eb_system_config` (`name`,`title`,`form_id`,`value`,`status`,`create_time`,`update_time`)
SELECT 'flower_store_name','花店名称',0,'花间集',0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='flower_store_name');
UPDATE `eb_system_config` SET `value`='13800000000', `title`='花店联系电话', `update_time`=NOW() WHERE `name`='flower_store_phone';
INSERT INTO `eb_system_config` (`name`,`title`,`form_id`,`value`,`status`,`create_time`,`update_time`)
SELECT 'flower_store_phone','花店联系电话',0,'13800000000',0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='flower_store_phone');
UPDATE `eb_system_config` SET `value`='示范市花园区春风路 88 号', `title`='花店地址', `update_time`=NOW() WHERE `name`='flower_store_address';
INSERT INTO `eb_system_config` (`name`,`title`,`form_id`,`value`,`status`,`create_time`,`update_time`)
SELECT 'flower_store_address','花店地址',0,'示范市花园区春风路 88 号',0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='flower_store_address');
UPDATE `eb_system_config` SET `value`='09:00-21:00', `title`='花店营业时间', `update_time`=NOW() WHERE `name`='flower_business_hours';
INSERT INTO `eb_system_config` (`name`,`title`,`form_id`,`value`,`status`,`create_time`,`update_time`)
SELECT 'flower_business_hours','花店营业时间',0,'09:00-21:00',0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='flower_business_hours');
UPDATE `eb_system_config` SET `value`='让每一束花，都有故事', `title`='花店品牌文案', `update_time`=NOW() WHERE `name`='flower_brand_slogan';
INSERT INTO `eb_system_config` (`name`,`title`,`form_id`,`value`,`status`,`create_time`,`update_time`)
SELECT 'flower_brand_slogan','花店品牌文案',0,'让每一束花，都有故事',0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='flower_brand_slogan');
UPDATE `eb_system_config` SET `value`='1', `title`='花店演示模拟支付开关', `update_time`=NOW() WHERE `name`='flower_mock_pay_enabled';
INSERT INTO `eb_system_config` (`name`,`title`,`form_id`,`value`,`status`,`create_time`,`update_time`)
SELECT 'flower_mock_pay_enabled','花店演示模拟支付开关',0,'1',0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='flower_mock_pay_enabled');

-- 演示角色只拥有花店经营权限，不授予系统配置权限。
INSERT INTO `eb_system_role` (`id`,`role_name`,`rules`,`level`,`status`,`create_time`,`update_time`)
VALUES (9001,'花店运营员','578,579,580,581,582,583,584,585,586,587,588,589,590,591,592,593,594,595,596,597',1,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`),`rules`=VALUES(`rules`),`status`=1,`update_time`=NOW();
DELETE FROM `eb_system_role_menu` WHERE `rid`=9001;
INSERT INTO `eb_system_role_menu` (`rid`,`menu_id`) VALUES
(9001,578),(9001,579),(9001,580),(9001,581),(9001,582),(9001,583),(9001,584),(9001,585),(9001,586),(9001,587),
(9001,588),(9001,589),(9001,590),(9001,591),(9001,592),(9001,593),(9001,594),(9001,595),(9001,596),(9001,597);

-- 12 个花卉商品，覆盖日常、生日、纪念日、祝贺及四档价格。
INSERT INTO `eb_store_product`
(`id`,`mer_id`,`image`,`slider_image`,`store_name`,`store_info`,`keyword`,`bar_code`,`cate_id`,`price`,`vip_price`,`ot_price`,`postage`,`unit_name`,`sort`,`sales`,`stock`,`is_show`,`is_hot`,`is_benefit`,`is_best`,`is_new`,`add_time`,`is_postage`,`is_del`,`cost`,`is_good`,`ficti`,`browse`,`temp_id`,`spec_type`,`activity`,`flat_pattern`,`is_recycle`,`version`,`guarantee_ids`)
VALUES
(900001,0,'/static/images/flower-products/product-01.jpg','["/static/images/flower-products/product-01.jpg"]','晨露 · 白绿洋桔梗','清新白绿系手工花束','洋桔梗,日常,白绿色','HJ000001','9106',188.00,188.00,218.00,0.00,'束',120,32,99,1,1,0,1,1,UNIX_TIMESTAMP(),1,0,96.00,1,68,126,1,0,'','','0',0,'0'),
(900002,0,'/static/images/flower-products/product-02.jpg','["/static/images/flower-products/product-02.jpg"]','暖阳 · 向日葵花束','明亮向日葵元气花束','向日葵,日常,黄色','HJ000002','9104',168.00,168.00,198.00,0.00,'束',115,28,99,1,1,1,1,1,UNIX_TIMESTAMP(),1,0,82.00,1,60,118,1,0,'','','0',0,'0'),
(900003,0,'/static/images/flower-products/product-03.jpg','["/static/images/flower-products/product-03.jpg"]','香颂 · 香槟玫瑰','温柔香槟色玫瑰花束','香槟玫瑰,日常,香槟色','HJ000003','9101',228.00,228.00,268.00,0.00,'束',110,24,88,1,1,0,1,1,UNIX_TIMESTAMP(),1,0,118.00,1,52,108,1,0,'','','0',0,'0'),
(900004,0,'/static/images/flower-products/product-04.jpg','["/static/images/flower-products/product-04.jpg"]','怦然 · 粉色玫瑰','浪漫粉色玫瑰生日花束','粉玫瑰,生日,粉色','HJ000004','9101',268.00,268.00,318.00,0.00,'束',105,22,88,1,1,1,1,1,UNIX_TIMESTAMP(),1,0,138.00,1,48,102,1,0,'','','0',0,'0'),
(900005,0,'/static/images/flower-products/product-05.jpg','["/static/images/flower-products/product-05.jpg"]','欢颜 · 缤纷混搭','多色鲜花混搭生日花束','混搭,生日,缤纷','HJ000005','9106',298.00,298.00,358.00,0.00,'束',100,20,76,1,1,0,1,1,UNIX_TIMESTAMP(),1,0,152.00,1,46,98,1,0,'','','0',0,'0'),
(900006,0,'/static/images/flower-products/product-06.jpg','["/static/images/flower-products/product-06.jpg"]','奶油诗 · 郁金香','奶油色郁金香生日花束','郁金香,生日,奶油色','HJ000006','9103',238.00,238.00,288.00,0.00,'束',95,18,66,1,0,1,1,1,UNIX_TIMESTAMP(),1,0,126.00,1,40,86,1,0,'','','0',0,'0'),
(900007,0,'/static/images/flower-products/product-07.jpg','["/static/images/flower-products/product-07.jpg"]','挚爱 · 红玫瑰','经典红玫瑰纪念日花束','红玫瑰,纪念日,红色','HJ000007','9101',398.00,398.00,468.00,0.00,'束',90,26,66,1,1,0,1,0,UNIX_TIMESTAMP(),1,0,208.00,1,56,132,1,0,'','','0',0,'0'),
(900008,0,'/static/images/flower-products/product-08.jpg','["/static/images/flower-products/product-08.jpg"]','蝶梦 · 紫色蝴蝶兰','紫色蝴蝶兰轻奢花礼','蝴蝶兰,纪念日,紫色','HJ000008','9106',328.00,328.00,388.00,0.00,'束',85,16,56,1,0,0,1,1,UNIX_TIMESTAMP(),1,0,176.00,1,36,78,1,0,'','','0',0,'0'),
(900009,0,'/static/images/flower-products/product-09.jpg','["/static/images/flower-products/product-09.jpg"]','月光 · 白色百合','白色百合纪念日花束','百合,纪念日,白色','HJ000009','9102',358.00,358.00,428.00,0.00,'束',80,14,56,1,0,1,1,0,UNIX_TIMESTAMP(),1,0,188.00,1,34,72,1,0,'','','0',0,'0'),
(900010,0,'/static/images/flower-products/product-10.jpg','["/static/images/flower-products/product-10.jpg"]','橙意 · 暖色花束','暖橙色祝贺花束','玫瑰,祝贺,橙色','HJ000010','9106',288.00,288.00,338.00,0.00,'束',75,18,60,1,1,0,1,1,UNIX_TIMESTAMP(),1,0,148.00,1,38,82,1,0,'','','0',0,'0'),
(900011,0,'/static/images/flower-products/product-11.jpg','["/static/images/flower-products/product-11.jpg"]','晴空 · 蓝白花篮','蓝白色系祝贺花篮','花篮,祝贺,蓝白色','HJ000011','9108',468.00,468.00,548.00,0.00,'篮',70,12,45,1,1,0,1,0,UNIX_TIMESTAMP(),1,0,248.00,1,28,68,1,0,'','','0',0,'0'),
(900012,0,'/static/images/flower-products/product-12.jpg','["/static/images/flower-products/product-12.jpg"]','盛放 · 开业桌花','大气开业祝贺桌花','桌花,祝贺,红粉色','HJ000012','9109',598.00,598.00,698.00,0.00,'盆',65,10,36,1,1,0,1,0,UNIX_TIMESTAMP(),1,0,328.00,1,22,58,1,0,'','','0',0,'0')
ON DUPLICATE KEY UPDATE `image`=VALUES(`image`),`slider_image`=VALUES(`slider_image`),`store_name`=VALUES(`store_name`),`store_info`=VALUES(`store_info`),`keyword`=VALUES(`keyword`),`price`=VALUES(`price`),`vip_price`=VALUES(`vip_price`),`ot_price`=VALUES(`ot_price`),`stock`=VALUES(`stock`),`is_show`=1,`is_del`=0,`version`=`version`+1;

INSERT INTO `eb_store_product_attr` (`id`,`product_id`,`attr_name`,`attr_values`,`type`,`is_del`,`is_show_image`)
VALUES
(900001,900001,'规格','默认',0,0,0),(900002,900002,'规格','默认',0,0,0),(900003,900003,'规格','默认',0,0,0),
(900004,900004,'规格','默认',0,0,0),(900005,900005,'规格','默认',0,0,0),(900006,900006,'规格','默认',0,0,0),
(900007,900007,'规格','默认',0,0,0),(900008,900008,'规格','默认',0,0,0),(900009,900009,'规格','默认',0,0,0),
(900010,900010,'规格','默认',0,0,0),(900011,900011,'规格','默认',0,0,0),(900012,900012,'规格','默认',0,0,0)
ON DUPLICATE KEY UPDATE `attr_values`='默认',`is_del`=0;

INSERT INTO `eb_store_product_attr_option` (`id`,`product_id`,`attr_id`,`option_name`,`image`,`sort`,`is_del`)
VALUES
(900001,900001,900001,'默认','',0,0),(900002,900002,900002,'默认','',0,0),(900003,900003,900003,'默认','',0,0),
(900004,900004,900004,'默认','',0,0),(900005,900005,900005,'默认','',0,0),(900006,900006,900006,'默认','',0,0),
(900007,900007,900007,'默认','',0,0),(900008,900008,900008,'默认','',0,0),(900009,900009,900009,'默认','',0,0),
(900010,900010,900010,'默认','',0,0),(900011,900011,900011,'默认','',0,0),(900012,900012,900012,'默认','',0,0)
ON DUPLICATE KEY UPDATE `option_name`='默认',`is_del`=0;

INSERT INTO `eb_store_product_attr_value`
(`id`,`product_id`,`suk`,`stock`,`sales`,`price`,`image`,`unique`,`cost`,`bar_code`,`ot_price`,`weight`,`volume`,`brokerage`,`brokerage_two`,`type`,`quota`,`quota_show`,`attr_value`,`is_del`,`version`,`is_default`,`is_show`)
VALUES
(900001,900001,'默认',99,32,188.00,'/static/images/flower-products/product-01.jpg','HJ000001',96.00,'HJ000001',218.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900002,900002,'默认',99,28,168.00,'/static/images/flower-products/product-02.jpg','HJ000002',82.00,'HJ000002',198.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900003,900003,'默认',88,24,228.00,'/static/images/flower-products/product-03.jpg','HJ000003',118.00,'HJ000003',268.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900004,900004,'默认',88,22,268.00,'/static/images/flower-products/product-04.jpg','HJ000004',138.00,'HJ000004',318.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900005,900005,'默认',76,20,298.00,'/static/images/flower-products/product-05.jpg','HJ000005',152.00,'HJ000005',358.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900006,900006,'默认',66,18,238.00,'/static/images/flower-products/product-06.jpg','HJ000006',126.00,'HJ000006',288.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900007,900007,'默认',66,26,398.00,'/static/images/flower-products/product-07.jpg','HJ000007',208.00,'HJ000007',468.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900008,900008,'默认',56,16,328.00,'/static/images/flower-products/product-08.jpg','HJ000008',176.00,'HJ000008',388.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900009,900009,'默认',56,14,358.00,'/static/images/flower-products/product-09.jpg','HJ000009',188.00,'HJ000009',428.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900010,900010,'默认',60,18,288.00,'/static/images/flower-products/product-10.jpg','HJ000010',148.00,'HJ000010',338.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900011,900011,'默认',45,12,468.00,'/static/images/flower-products/product-11.jpg','HJ000011',248.00,'HJ000011',548.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1),
(900012,900012,'默认',36,10,598.00,'/static/images/flower-products/product-12.jpg','HJ000012',328.00,'HJ000012',698.00,0,0,0,0,0,0,0,'{"规格":"默认"}',0,0,1,1)
ON DUPLICATE KEY UPDATE `stock`=VALUES(`stock`),`price`=VALUES(`price`),`image`=VALUES(`image`),`cost`=VALUES(`cost`),`ot_price`=VALUES(`ot_price`),`attr_value`=VALUES(`attr_value`),`is_del`=0,`is_default`=1,`is_show`=1;

INSERT INTO `eb_flower_product_ext`
(`product_id`,`flower_materials`,`flower_language`,`occasion`,`recipient`,`color_series`,`stem_count`,`packaging`,`care_tips`,`create_time`,`update_time`)
VALUES
(900001,'白色洋桔梗、绿色配叶','纯净陪伴，温柔如初','日常','朋友,自己','白绿色',18,'米白雾面纸手工包扎','收到后斜剪花枝，使用洁净花瓶并每日换水。',NOW(),NOW()),
(900002,'向日葵、香槟配花、尤加利叶','向阳而生，未来明亮','日常','朋友,同事','黄色',10,'奶油黄双层包装','避免阳光直射，每日换水并及时去除水下叶片。',NOW(),NOW()),
(900003,'香槟玫瑰、洋桔梗、尤加利叶','爱是细水长流的温柔','日常','爱人,朋友','香槟色',19,'浅咖韩式包装','斜剪根部两厘米，花瓶水位保持三分之一。',NOW(),NOW()),
(900004,'粉玫瑰、粉色洋桔梗、满天星','愿你的每一天都甜美闪亮','生日','爱人,朋友','粉色',21,'粉白渐变包装','放置阴凉通风处，每日清洗花瓶并换水。',NOW(),NOW()),
(900005,'玫瑰、乒乓菊、洋桔梗、配叶','缤纷欢喜，如约而至','生日','朋友,家人','缤纷',25,'彩色艺术纸包装','不同花材分层修剪，保持水质清洁。',NOW(),NOW()),
(900006,'郁金香、洋甘菊、配叶','把春日温柔送给你','生日','爱人,朋友','奶油色',18,'奶油色柔光包装','郁金香有向光性，请每日转动花瓶并少量加水。',NOW(),NOW()),
(900007,'红玫瑰、尤加利叶','热烈真挚，始终如一','纪念日','爱人','红色',33,'黑红高级感包装','斜剪花枝后深水醒花，避免放在水果旁。',NOW(),NOW()),
(900008,'紫色蝴蝶兰、紫玫瑰、配花','浪漫长久，幸福向你飞来','纪念日','爱人,长辈','紫色',16,'紫灰轻奢包装','保持环境通风，避免花瓣沾水和阳光暴晒。',NOW(),NOW()),
(900009,'白百合、白玫瑰、尤加利叶','纯粹的爱与长久祝福','纪念日','爱人,家人','白色',15,'白绿自然风包装','百合花粉成熟后可摘除花药，延长观赏期。',NOW(),NOW()),
(900010,'橙玫瑰、太阳花、配叶','心想事成，前程明亮','祝贺','朋友,同事','橙色',22,'暖橙撞色包装','斜剪根部并每日换水，远离空调直吹。',NOW(),NOW()),
(900011,'蓝色绣球、白玫瑰、洋桔梗','晴空万里，好事发生','祝贺','朋友,同事','蓝白色',28,'蓝白手提花篮','花泥每日少量补水，避免积水溢出。',NOW(),NOW()),
(900012,'红掌、粉玫瑰、百合、配叶','生意兴隆，盛放启程','祝贺','客户,同事','红粉色',36,'桌面花器造型','每日向花泥缓慢补水，保持花器平稳。',NOW(),NOW())
ON DUPLICATE KEY UPDATE `flower_materials`=VALUES(`flower_materials`),`flower_language`=VALUES(`flower_language`),`occasion`=VALUES(`occasion`),`recipient`=VALUES(`recipient`),`color_series`=VALUES(`color_series`),`stem_count`=VALUES(`stem_count`),`packaging`=VALUES(`packaging`),`care_tips`=VALUES(`care_tips`),`update_time`=NOW();

INSERT INTO `eb_store_product_description` (`id`,`product_id`,`description`,`type`)
SELECT 900001,900001,'<p>花间集花艺师当日手工制作；鲜花为自然生长，开放度与枝形存在合理差异。</p>',0
WHERE NOT EXISTS (SELECT 1 FROM `eb_store_product_description` WHERE `id`=900001);
UPDATE `eb_store_product_description` SET `description`='<p>花间集花艺师当日手工制作；鲜花为自然生长，开放度与枝形存在合理差异。</p>' WHERE `product_id` BETWEEN 900001 AND 900012;
INSERT INTO `eb_store_product_description` (`id`,`product_id`,`description`,`type`)
SELECT 900000+`n`,`product_id`,'<p>花间集花艺师当日手工制作；鲜花为自然生长，开放度与枝形存在合理差异。</p>',0
FROM (
  SELECT 2 AS `n`,900002 AS `product_id` UNION ALL SELECT 3,900003 UNION ALL SELECT 4,900004 UNION ALL SELECT 5,900005 UNION ALL
  SELECT 6,900006 UNION ALL SELECT 7,900007 UNION ALL SELECT 8,900008 UNION ALL SELECT 9,900009 UNION ALL
  SELECT 10,900010 UNION ALL SELECT 11,900011 UNION ALL SELECT 12,900012
) `demo_description`
WHERE NOT EXISTS (SELECT 1 FROM `eb_store_product_description` `d` WHERE `d`.`product_id`=`demo_description`.`product_id` AND `d`.`type`=0);

-- 两级同城配送区域。
INSERT INTO `eb_flower_delivery_zone` (`id`,`zone_name`,`area_codes`,`delivery_fee`,`minimum_amount`,`enabled`,`create_time`,`update_time`)
VALUES
(9001,'核心配送区','110101,110102,110105',0.00,99.00,1,NOW(),NOW()),
(9002,'周边配送区','110106,110107,110108',12.00,168.00,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE `zone_name`=VALUES(`zone_name`),`area_codes`=VALUES(`area_codes`),`delivery_fee`=VALUES(`delivery_fee`),`minimum_amount`=VALUES(`minimum_amount`),`enabled`=1,`update_time`=NOW();

-- 清理演示专用 ID 段内、目标日期范围外且无人预约的旧时段，不影响真实时段和已有订单。
DELETE FROM `eb_flower_delivery_slot`
WHERE `id` BETWEEN 9000000 AND 9999999
  AND (`delivery_date` < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
       OR `delivery_date` > DATE_ADD(CURDATE(), INTERVAL 7 DAY))
  AND `booked_count`=0;

-- 从明天开始生成未来 7 天、每天 3 个可预约配送时段；稳定 ID 用于重复导入时精准归属。
INSERT INTO `eb_flower_delivery_slot`
(`id`,`delivery_date`,`start_time`,`end_time`,`capacity`,`booked_count`,`cutoff_time`,`enabled`,`version`,`create_time`,`update_time`)
WITH RECURSIVE `demo_days` AS (
  SELECT 0 AS `day_offset`
  UNION ALL SELECT `day_offset` + 1 FROM `demo_days` WHERE `day_offset` < 6 /* INTERVAL 6 DAY */
), `demo_times` AS (
  SELECT 1 AS `slot_index`,CAST('09:00:00' AS TIME) `start_time`, CAST('12:00:00' AS TIME) `end_time`
  UNION ALL SELECT 2,'14:00:00','17:00:00'
  UNION ALL SELECT 3,'18:00:00','21:00:00'
)
SELECT 9000000 + DATEDIFF(DATE_ADD(CURDATE(), INTERVAL (`day_offset` + 1) DAY),'2020-01-01') * 10 + `slot_index`,
       DATE_ADD(CURDATE(), INTERVAL (`day_offset` + 1) DAY),`start_time`,`end_time`,12,0,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL (`day_offset` + 1) DAY),`start_time`) - INTERVAL 2 HOUR,
       1,0,NOW(),NOW()
FROM `demo_days` CROSS JOIN `demo_times`
WHERE 1=1
ON DUPLICATE KEY UPDATE `capacity`=VALUES(`capacity`),`enabled`=1,`cutoff_time`=VALUES(`cutoff_time`),`update_time`=NOW();

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
