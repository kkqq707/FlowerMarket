-- 花材批次库存：仅追加数据结构；执行前请备份数据库。可重复执行。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `eb_flower_material` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `unit` varchar(20) NOT NULL DEFAULT '枝',
  `fresh_days` int NOT NULL DEFAULT 7,
  `display_days` int NULL,
  `low_stock_threshold` int NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_flower_material_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `eb_flower_material_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` int NOT NULL,
  `batch_no` varchar(80) NOT NULL,
  `received_date` date NOT NULL,
  `expiry_date` date NOT NULL,
  `received_qty` int NOT NULL,
  `current_qty` int NOT NULL,
  `reserved_qty` int NOT NULL DEFAULT 0,
  `remark` varchar(500) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_flower_material_batch_no` (`material_id`,`batch_no`),
  KEY `idx_flower_batch_fefo` (`material_id`,`expiry_date`,`received_date`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `eb_flower_recipe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `material_id` int NOT NULL,
  `qty_per_item` int NOT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_flower_recipe_item` (`product_id`,`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `eb_flower_material_allocation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` varchar(64) NOT NULL,
  `material_id` int NOT NULL,
  `batch_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'RESERVED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_flower_allocation_order_batch` (`order_id`,`batch_id`),
  KEY `idx_flower_allocation_order_status` (`order_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- MySQL 8.0 不支持通用的 ADD COLUMN IF NOT EXISTS，使用信息表保证重跑安全。
SET @flower_batch_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_flower_product_ext' AND COLUMN_NAME = 'batch_managed');
SET @flower_batch_sql = IF(@flower_batch_column = 0,
  'ALTER TABLE eb_flower_product_ext ADD COLUMN batch_managed tinyint(1) NOT NULL DEFAULT 0 COMMENT ''启用花材批次管理''',
  'SELECT 1');
PREPARE flower_batch_stmt FROM @flower_batch_sql;
EXECUTE flower_batch_stmt;
DEALLOCATE PREPARE flower_batch_stmt;

-- 演示花材和配方仅使用专用 ID，不覆盖店员实际库存。
INSERT INTO eb_flower_material (id,name,unit,fresh_days,display_days,low_stock_threshold,enabled)
VALUES (9001,'玫瑰','枝',7,5,30,1),(9002,'洋桔梗','枝',7,6,30,1),
 (9003,'向日葵','枝',6,5,15,1),(9004,'百合','枝',7,6,15,1),
 (9005,'郁金香','枝',5,4,15,1),(9006,'蝴蝶兰','枝',10,8,10,1),
 (9007,'绣球','枝',5,4,10,1),(9008,'红掌','枝',10,8,10,1),
 (9009,'菊花','枝',7,6,10,1),(9010,'尤加利叶','枝',10,8,20,1),
 (9011,'配叶','枝',7,5,20,1)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO eb_flower_recipe (product_id,material_id,qty_per_item) VALUES
 (900001,9002,12),(900001,9011,6),(900002,9003,8),(900002,9010,2),
 (900003,9001,12),(900003,9002,5),(900003,9010,2),
 (900004,9001,12),(900004,9002,6),(900004,9011,3),
 (900005,9001,10),(900005,9009,5),(900005,9002,5),(900005,9011,5),
 (900006,9005,12),(900006,9011,6),(900007,9001,28),(900007,9010,5),
 (900008,9006,12),(900008,9001,4),(900009,9004,8),(900009,9001,5),(900009,9010,2),
 (900010,9001,12),(900010,9003,6),(900010,9011,4),
 (900011,9007,8),(900011,9001,8),(900011,9002,12),
 (900012,9008,12),(900012,9001,12),(900012,9004,8),(900012,9011,4)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO eb_flower_material_batch
 (material_id,batch_no,received_date,expiry_date,received_qty,current_qty,reserved_qty,remark)
SELECT m.id, CONCAT('DEMO-',DATE_FORMAT(CURDATE(),'%Y%m%d'),'-',m.id), CURDATE(),
 DATE_ADD(CURDATE(), INTERVAL m.fresh_days DAY), 600, 600, 0, '演示入库批次；正式经营请按实际进货登记'
FROM eb_flower_material m WHERE m.id BETWEEN 9001 AND 9011
ON DUPLICATE KEY UPDATE remark=remark;

-- 仅为存在的演示商品启用；历史花卉商品保持旧 SKU 流程。
UPDATE eb_flower_product_ext SET batch_managed=1
WHERE product_id BETWEEN 900001 AND 900012
  AND EXISTS (SELECT 1 FROM eb_flower_recipe r WHERE r.product_id=eb_flower_product_ext.product_id);

INSERT INTO eb_system_menu (id,pid,name,icon,perms,component,menu_type,sort,is_show,is_delte,create_time,update_time)
VALUES
 (650,578,'花材库存',NULL,'admin:flower:material:list','/flower/material','C',15,1,0,NOW(),NOW()),
 (651,650,'查看花材与批次',NULL,'admin:flower:material:list','','A',50,1,0,NOW(),NOW()),
 (652,650,'保存花材',NULL,'admin:flower:material:save','','A',40,1,0,NOW(),NOW()),
 (653,650,'批次入库',NULL,'admin:flower:material:receive','','A',30,1,0,NOW(),NOW()),
 (654,650,'批次盘点',NULL,'admin:flower:material:adjust','','A',20,1,0,NOW(),NOW())
ON DUPLICATE KEY UPDATE id=id;
INSERT INTO eb_system_role_menu (rid,menu_id)
SELECT 9001,m.id FROM eb_system_menu m WHERE m.id BETWEEN 650 AND 654
AND NOT EXISTS (SELECT 1 FROM eb_system_role_menu rm WHERE rm.rid=9001 AND rm.menu_id=m.id);
UPDATE eb_system_role SET rules=CONCAT_WS(',',NULLIF(rules,''),'650','651','652','653','654')
WHERE id=9001 AND NOT FIND_IN_SET('650',rules);
