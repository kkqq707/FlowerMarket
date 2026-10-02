SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `eb_flower_product_ext` (
  `id` int NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL COMMENT '原商城商品编号',
  `flower_materials` varchar(500) NOT NULL DEFAULT '' COMMENT '主要花材',
  `flower_language` varchar(500) NOT NULL DEFAULT '' COMMENT '花语',
  `occasion` varchar(255) NOT NULL DEFAULT '' COMMENT '适用场景',
  `recipient` varchar(255) NOT NULL DEFAULT '' COMMENT '赠送对象',
  `color_series` varchar(255) NOT NULL DEFAULT '' COMMENT '色系',
  `stem_count` int NOT NULL DEFAULT 0 COMMENT '花材枝数',
  `packaging` varchar(500) NOT NULL DEFAULT '' COMMENT '包装方式',
  `care_tips` text NULL COMMENT '养护说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flower_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='花卉商品扩展信息';

CREATE TABLE IF NOT EXISTS `eb_flower_delivery_zone` (
  `id` int NOT NULL AUTO_INCREMENT,
  `zone_name` varchar(100) NOT NULL COMMENT '配送区域名称',
  `area_codes` text NOT NULL COMMENT '覆盖行政区域编码',
  `delivery_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
  `minimum_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '起送金额',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_flower_zone_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='花店同城配送区域';

CREATE TABLE IF NOT EXISTS `eb_flower_delivery_slot` (
  `id` int NOT NULL AUTO_INCREMENT,
  `delivery_date` date NOT NULL COMMENT '配送日期',
  `start_time` time NOT NULL COMMENT '时段开始时间',
  `end_time` time NOT NULL COMMENT '时段结束时间',
  `capacity` int NOT NULL DEFAULT 0 COMMENT '最大接单量',
  `booked_count` int NOT NULL DEFAULT 0 COMMENT '已占用数量',
  `cutoff_time` datetime NOT NULL COMMENT '停止预约时间',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flower_slot_date_time` (`delivery_date`,`start_time`,`end_time`),
  KEY `idx_flower_slot_available` (`delivery_date`,`enabled`,`cutoff_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鲜花预约配送时段';

CREATE TABLE IF NOT EXISTS `eb_flower_order_ext` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` varchar(64) NOT NULL COMMENT '原商城订单号',
  `sender_name` varchar(64) NOT NULL DEFAULT '' COMMENT '订花人姓名',
  `sender_phone` varchar(32) NOT NULL DEFAULT '' COMMENT '订花人电话',
  `recipient_name` varchar(64) NOT NULL DEFAULT '' COMMENT '收花人姓名',
  `recipient_phone` varchar(32) NOT NULL DEFAULT '' COMMENT '收花人电话',
  `delivery_address_snapshot` varchar(500) NOT NULL DEFAULT '' COMMENT '配送地址快照',
  `delivery_zone_id` int NULL COMMENT '配送区域编号',
  `delivery_date` date NULL COMMENT '预约配送日期',
  `slot_id` int NULL COMMENT '预约配送时段编号',
  `greeting_card` varchar(500) NOT NULL DEFAULT '' COMMENT '贺卡祝福语',
  `anonymous` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `contact_recipient` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否提前联系收花人',
  `substitution_policy` varchar(32) NOT NULL DEFAULT 'CONTACT_FIRST' COMMENT '花材替换策略',
  `current_stage` varchar(32) NOT NULL DEFAULT 'WAIT_PAY' COMMENT '当前履约阶段',
  `courier_name` varchar(64) NOT NULL DEFAULT '' COMMENT '配送人员',
  `courier_phone` varchar(32) NOT NULL DEFAULT '' COMMENT '配送联系电话',
  `delivery_remark` varchar(500) NOT NULL DEFAULT '' COMMENT '配送备注',
  `reservation_expire_time` datetime NULL COMMENT '待付款预约失效时间',
  `reservation_released` tinyint(1) NOT NULL DEFAULT 0 COMMENT '预约容量是否已释放',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flower_order_id` (`order_id`),
  KEY `idx_flower_order_slot_expire` (`slot_id`,`reservation_expire_time`,`reservation_released`),
  KEY `idx_flower_order_stage` (`current_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='送花订单扩展信息';

CREATE TABLE IF NOT EXISTS `eb_flower_order_progress` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` varchar(64) NOT NULL COMMENT '原商城订单号',
  `stage` varchar(32) NOT NULL COMMENT '履约阶段',
  `operator_id` int NULL COMMENT '后台操作人编号',
  `proof_image` varchar(1000) NOT NULL DEFAULT '' COMMENT '成品照或送达凭证',
  `remark` varchar(500) NOT NULL DEFAULT '' COMMENT '操作备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flower_order_stage` (`order_id`,`stage`),
  KEY `idx_flower_order_progress_time` (`order_id`,`create_time`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='花店订单履约进度';

CREATE TABLE IF NOT EXISTS `eb_flower_refund_outbox` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL COMMENT '原商城订单主键',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待处理，1已完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flower_refund_outbox_order` (`order_id`),
  KEY `idx_flower_refund_outbox_status` (`status`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='花店模拟退款可靠后置任务';

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'flower_mock_pay_enabled', '花店毕业设计模拟支付开关', 0, '0', 0, NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `eb_system_config` WHERE `name` = 'flower_mock_pay_enabled'
);

INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 578,0,'花店经营','shop',NULL,'','M',190,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=578);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 579,578,'花卉商品',NULL,'admin:flower:product:info','','C',30,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=579);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 580,578,'花店订单',NULL,NULL,'','C',20,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=580);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 581,578,'配送管理',NULL,'admin:flower:delivery:list','','C',10,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=581);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 582,579,'花卉商品详情',NULL,'admin:flower:product:info','','A',20,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=582);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 583,579,'保存花卉商品',NULL,'admin:flower:product:save','','A',10,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=583);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 584,580,'接单',NULL,'admin:flower:order:accept','','A',50,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=584);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 585,580,'开始制作',NULL,'admin:flower:order:produce','','A',40,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=585);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 586,580,'配送',NULL,'admin:flower:order:deliver','','A',30,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=586);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 587,580,'完成送达',NULL,'admin:flower:order:complete','','A',20,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=587);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 588,580,'花店订单退款',NULL,'admin:flower:order:refund','','A',10,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=588);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 589,581,'配送区域列表',NULL,'admin:flower:delivery:list','','A',60,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=589);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 590,581,'保存配送区域',NULL,'admin:flower:delivery:save','','A',50,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=590);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 591,581,'配送区域状态',NULL,'admin:flower:delivery:status','','A',40,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=591);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 592,581,'配送时段列表',NULL,'admin:flower:delivery:list','','A',30,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=592);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 593,581,'保存配送时段',NULL,'admin:flower:delivery:save','','A',20,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=593);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 594,581,'配送时段状态',NULL,'admin:flower:delivery:status','','A',10,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=594);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 595,578,'经营看板',NULL,'admin:flower:dashboard:view','','C',40,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=595);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 596,580,'花店订单列表',NULL,'admin:flower:order:list','','A',70,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=596);
INSERT INTO `eb_system_menu` (`id`,`pid`,`name`,`icon`,`perms`,`component`,`menu_type`,`sort`,`is_show`,`is_delte`,`create_time`,`update_time`)
SELECT 597,580,'花店订单详情',NULL,'admin:flower:order:info','','A',60,1,0,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM `eb_system_menu` WHERE `id`=597);

UPDATE `eb_system_menu` SET `is_show` = 0 WHERE `id` IN (8,85,86,87,93,94,95,96,97,98,104,105,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,207,209,210,211,212,213,214,215,231,325,348,352,353,354,383);

SET FOREIGN_KEY_CHECKS = 1;
