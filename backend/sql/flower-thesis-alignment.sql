-- 花间集：论文目标对齐版
-- 作用：收敛后台菜单、清理非花卉演示数据、重建花卉分类与装修素材。
-- 安全约束：仅软删除未被订单明细引用的非花卉商品；执行前必须备份相关表。
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- 一、后台只显示论文要求的功能。按钮权限保留，用于接口鉴权但不会显示为菜单。
UPDATE `eb_system_menu` SET `is_show`=0 WHERE `menu_type` IN ('M','C');

UPDATE `eb_system_menu` SET `name`='花卉商品', `component`='/store', `sort`=180, `is_show`=1 WHERE `id`=2;
UPDATE `eb_system_menu` SET `name`='花卉商品', `component`='/store/index', `is_show`=1 WHERE `id`=14;
UPDATE `eb_system_menu` SET `name`='花卉分类', `component`='/store/sort', `is_show`=1 WHERE `id`=15;
UPDATE `eb_system_menu` SET `name`='商品规格与库存管理', `component`='/store/attr', `is_show`=1 WHERE `id`=16;
UPDATE `eb_system_menu` SET `name`='商品评价', `component`='/store/comment', `is_show`=1 WHERE `id`=17;

UPDATE `eb_system_menu` SET `name`='顾客与员工', `component`='/user', `is_show`=1 WHERE `id`=4;
UPDATE `eb_system_menu` SET `name`='顾客管理', `component`='/user/index', `is_show`=1 WHERE `id`=39;

UPDATE `eb_system_menu` SET `name`='花店经营', `component`='/flower', `sort`=190, `is_show`=1 WHERE `id`=578;
UPDATE `eb_system_menu` SET `name`='经营看板', `component`='/flower/dashboard', `is_show`=1 WHERE `id`=595;
UPDATE `eb_system_menu` SET `name`='花店订单', `component`='/flower/orders', `is_show`=1 WHERE `id`=580;
UPDATE `eb_system_menu` SET `name`='配送管理', `component`='/flower/delivery', `is_show`=1 WHERE `id`=581;

UPDATE `eb_system_menu` SET `name`='商城装饰', `component`='/design', `is_show`=1 WHERE `id`=439;
UPDATE `eb_system_menu` SET `name`='商城主题', `component`='/design/mall_theme', `is_show`=1 WHERE `id`=506;
UPDATE `eb_system_menu` SET `name`='花卉素材', `component`='/design/picture', `is_show`=1 WHERE `id`=158;
UPDATE `eb_system_menu` SET `name`='开屏广告', `component`='/design/advertisement', `is_show`=1 WHERE `id`=492;

UPDATE `eb_system_menu` SET `name`='系统设置', `component`='/operation', `is_show`=1 WHERE `id`=12;
UPDATE `eb_system_menu` SET `name`='花店基础设置', `component`='/operation/setting', `is_show`=1 WHERE `id`=121;
UPDATE `eb_system_menu` SET `name`='消息通知', `component`='/operation/notification', `is_show`=1 WHERE `id`=269;
UPDATE `eb_system_menu` SET `name`='协议与隐私', `component`='/operation/agreement', `is_show`=1 WHERE `id`=452;
UPDATE `eb_system_menu` SET `name`='员工与权限', `component`='/operation/roleManager', `is_show`=1 WHERE `id`=123;
UPDATE `eb_system_menu` SET `name`='员工账号', `component`='/operation/roleManager/adminList', `is_show`=1 WHERE `id`=137;
UPDATE `eb_system_menu` SET `name`='角色管理', `component`='/operation/roleManager/identityManager', `is_show`=1 WHERE `id`=136;
UPDATE `eb_system_menu` SET `name`='权限规则', `component`='/operation/roleManager/promiseRules', `is_show`=1 WHERE `id`=138;

-- 花店运营员拥有日常经营功能；系统配置仍只对管理员开放。
UPDATE `eb_system_role`
SET `role_name`='花店运营员',
    `rules`='2,14,15,16,17,4,39,439,506,158,492,578,580,581,595,582,583,584,585,586,587,588,589,590,591,592,593,594,596,597',
    `status`=1,
    `update_time`=NOW()
WHERE `id`=9001;
DELETE FROM `eb_system_role_menu` WHERE `rid`=9001;
INSERT INTO `eb_system_role_menu` (`rid`,`menu_id`) VALUES
(9001,2),(9001,14),(9001,15),(9001,16),(9001,17),(9001,4),(9001,39),
(9001,439),(9001,506),(9001,158),(9001,492),(9001,578),(9001,580),(9001,581),(9001,595),
(9001,582),(9001,583),(9001,584),(9001,585),(9001,586),(9001,587),(9001,588),
(9001,589),(9001,590),(9001,591),(9001,592),(9001,593),(9001,594),(9001,596),(9001,597);

-- 二、商品只保留花卉。历史订单引用的商品不会被清理。
UPDATE `eb_store_product` SET `is_del`=1, `is_show`=0
WHERE `id` NOT BETWEEN 900001 AND 900012
  AND NOT EXISTS (SELECT 1 FROM `eb_flower_product_ext` `flower` WHERE `flower`.`product_id`=`eb_store_product`.`id`)
  AND `is_del`=0
  AND NOT EXISTS (
    SELECT 1 FROM `eb_store_order_info` `oi` WHERE `oi`.`product_id`=`eb_store_product`.`id`
  );

DELETE FROM `eb_category` WHERE `type`=1;
INSERT INTO `eb_category`
(`id`,`pid`,`path`,`name`,`type`,`url`,`extra`,`status`,`sort`,`create_time`,`update_time`)
VALUES
(9101,0,'/0/','玫瑰花束',1,'','http://localhost:8080/crmebimage/public/flower/product-03.jpg',1,10,NOW(),NOW()),
(9102,0,'/0/','百合花束',1,'','http://localhost:8080/crmebimage/public/flower/product-09.jpg',1,20,NOW(),NOW()),
(9103,0,'/0/','郁金香',1,'','http://localhost:8080/crmebimage/public/flower/product-06.jpg',1,30,NOW(),NOW()),
(9104,0,'/0/','向日葵',1,'','http://localhost:8080/crmebimage/public/flower/product-02.jpg',1,40,NOW(),NOW()),
(9105,0,'/0/','洋桔梗',1,'','http://localhost:8080/crmebimage/public/flower/product-01.jpg',1,50,NOW(),NOW()),
(9106,0,'/0/','混合花束',1,'','http://localhost:8080/crmebimage/public/flower/product-05.jpg',1,60,NOW(),NOW()),
(9107,0,'/0/','鲜花礼盒',1,'','http://localhost:8080/crmebimage/public/flower/product-08.jpg',1,70,NOW(),NOW()),
(9108,0,'/0/','开业花篮',1,'','http://localhost:8080/crmebimage/public/flower/product-11.jpg',1,80,NOW(),NOW()),
(9109,0,'/0/','桌花与瓶插花',1,'','http://localhost:8080/crmebimage/public/flower/product-12.jpg',1,90,NOW(),NOW());

UPDATE `eb_store_product` SET `cate_id`='9106' WHERE `id` IN (900001,900005,900008,900010);
UPDATE `eb_store_product` SET `cate_id`='9104' WHERE `id`=900002;
UPDATE `eb_store_product` SET `cate_id`='9101' WHERE `id` IN (900003,900004,900007);
UPDATE `eb_store_product` SET `cate_id`='9103' WHERE `id`=900006;
UPDATE `eb_store_product` SET `cate_id`='9102' WHERE `id`=900009;
UPDATE `eb_store_product` SET `cate_id`='9108' WHERE `id`=900011;
UPDATE `eb_store_product` SET `cate_id`='9109' WHERE `id`=900012;
UPDATE `eb_store_product`
SET `image`=CONCAT('crmebimage/public/flower/product-', LPAD(`id`-900000, 2, '0'), '.jpg'),
    `slider_image`=CONCAT('["crmebimage/public/flower/product-', LPAD(`id`-900000, 2, '0'), '.jpg"]')
WHERE `id` BETWEEN 900001 AND 900012;
UPDATE `eb_store_product_attr_value`
SET `image`=CONCAT('crmebimage/public/flower/product-', LPAD(`product_id`-900000, 2, '0'), '.jpg')
WHERE `product_id` BETWEEN 900001 AND 900012;

-- 三、素材库只保留花卉素材。
DELETE FROM `eb_system_attachment`;
DELETE FROM `eb_category` WHERE `type`=2;
INSERT INTO `eb_category`
(`id`,`pid`,`path`,`name`,`type`,`url`,`extra`,`status`,`sort`,`create_time`,`update_time`)
VALUES (9500,0,'/0/','花卉素材',2,'','登录主视觉、商品图、主题图和广告图',1,1,NOW(),NOW());

INSERT INTO `eb_system_attachment`
(`name`,`att_dir`,`satt_dir`,`att_size`,`att_type`,`pid`,`image_type`,`create_time`,`update_time`)
VALUES
('花间集登录花束','','crmebimage/public/flower/login-bouquet-v2.png','2602 KB','png',9500,1,NOW(),NOW()),
('晨露白绿洋桔梗','','crmebimage/public/flower/product-01.jpg','127 KB','jpg',9500,1,NOW(),NOW()),
('暖阳向日葵','','crmebimage/public/flower/product-02.jpg','143 KB','jpg',9500,1,NOW(),NOW()),
('香颂香槟玫瑰','','crmebimage/public/flower/product-03.jpg','211 KB','jpg',9500,1,NOW(),NOW()),
('怦然粉色玫瑰','','crmebimage/public/flower/product-04.jpg','192 KB','jpg',9500,1,NOW(),NOW()),
('欢颜缤纷混搭','','crmebimage/public/flower/product-05.jpg','129 KB','jpg',9500,1,NOW(),NOW()),
('奶油诗郁金香','','crmebimage/public/flower/product-06.jpg','193 KB','jpg',9500,1,NOW(),NOW()),
('挚爱红玫瑰','','crmebimage/public/flower/product-07.jpg','164 KB','jpg',9500,1,NOW(),NOW()),
('蝶梦紫色花礼','','crmebimage/public/flower/product-08.jpg','223 KB','jpg',9500,1,NOW(),NOW()),
('月光白色百合','','crmebimage/public/flower/product-09.jpg','160 KB','jpg',9500,1,NOW(),NOW()),
('橙意暖色花束','','crmebimage/public/flower/product-10.jpg','124 KB','jpg',9500,1,NOW(),NOW()),
('晴空蓝白花篮','','crmebimage/public/flower/product-11.jpg','184 KB','jpg',9500,1,NOW(),NOW()),
('盛放开业桌花','','crmebimage/public/flower/product-12.jpg','187 KB','jpg',9500,1,NOW(),NOW());

-- 四、主题封面和开屏广告全部改为花卉资源。
UPDATE `eb_theme`
SET `title`='花间集 · 自然绿',
    `info`='以鼠尾草绿和暖米白呈现花卉商城',
    `home_image`='crmebimage/public/flower/login-bouquet-v2.png',
    `category_image`='crmebimage/public/flower/product-05.jpg',
    `detail_image`='crmebimage/public/flower/product-03.jpg',
    `user_image`='crmebimage/public/flower/product-01.jpg',
    `home_default_image`='crmebimage/public/flower/login-bouquet-v2.png',
    `category_default_image`='crmebimage/public/flower/product-05.jpg',
    `detail_default_image`='crmebimage/public/flower/product-03.jpg',
    `user_default_image`='crmebimage/public/flower/product-01.jpg',
    `is_use`=1,
    `is_del`=0,
    `up_time`=UNIX_TIMESTAMP()
WHERE `id`=1;

DELETE FROM `eb_group_config` WHERE `tag`=13;
INSERT INTO `eb_group_config`
(`tag`,`name`,`link_url`,`image_url`,`value`,`message`,`status`,`sort`,`is_del`,`create_time`,`update_time`,`expand`)
VALUES
(13,'生日花礼','/pages/goods/goods_details/index?id=900004','crmebimage/public/flower/product-04.jpg','','',1,1,0,NOW(),NOW(),''),
(13,'纪念日玫瑰','/pages/goods/goods_details/index?id=900007','crmebimage/public/flower/product-07.jpg','','',1,2,0,NOW(),NOW(),''),
(13,'开业祝贺','/pages/goods/goods_details/index?id=900012','crmebimage/public/flower/product-12.jpg','','',1,3,0,NOW(),NOW(),'');

UPDATE `eb_system_config` SET `value`='1', `update_time`=NOW() WHERE `name`='splash_ad_switch';
UPDATE `eb_system_config` SET `value`='5', `update_time`=NOW() WHERE `name`='splash_ad_show_time';
UPDATE `eb_system_config` SET `value`='24', `update_time`=NOW() WHERE `name`='splash_ad_show_interval';

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
