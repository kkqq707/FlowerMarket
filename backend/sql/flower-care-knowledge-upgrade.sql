-- 养花指南：复用现有文章表，不修改普通商城文章。可重复执行。
SET NAMES utf8mb4;

INSERT INTO eb_category (pid,path,name,type,url,extra,status,sort,create_time,update_time)
SELECT 0,'/0/','鲜花养护',3,'flower-care','',1,10,NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_category WHERE type=3 AND url='flower-care');

INSERT INTO eb_article
 (id,cid,title,author,image_input,synopsis,share_title,share_synopsis,visit,sort,status,hide,is_hot,is_banner,content,create_time,update_time)
SELECT 990001,CAST(c.id AS CHAR),'收到鲜花后的第一件事：醒花','花间集花艺师',
 'crmebimage/public/flower/product-01.jpg','斜剪花枝、清理水下叶片，让花束重新喝饱水。',
 '鲜花醒花指南','花间集鲜花养护',0,30,0,0,0,0,
 '<p>拆开包装后，先清理会浸入水中的叶片。用干净的剪刀斜剪花枝约 2 厘米，再放进清洁的花瓶中补水。不同花材醒花时长不同，请以花店随花提示为准。</p>',NOW(),NOW()
FROM eb_category c WHERE c.type=3 AND c.url='flower-care' ORDER BY c.id LIMIT 1
ON DUPLICATE KEY UPDATE title=title;

INSERT INTO eb_article
 (id,cid,title,author,image_input,synopsis,share_title,share_synopsis,visit,sort,status,hide,is_hot,is_banner,content,create_time,update_time)
SELECT 990002,CAST(c.id AS CHAR),'花瓶每天换水，花期更安心','花间集花艺师',
 'crmebimage/public/flower/product-04.jpg','干净花瓶、适量水位和阴凉通风，是鲜切花养护的基础。',
 '鲜花换水指南','花间集鲜花养护',0,20,0,0,0,0,
 '<p>建议每天换用洁净的水，并冲洗花瓶内壁。换水时检查枝条切口，及时修剪发软的部分。花束放在阴凉、通风处，避免空调直吹、强烈日晒以及成熟水果附近。</p>',NOW(),NOW()
FROM eb_category c WHERE c.type=3 AND c.url='flower-care' ORDER BY c.id LIMIT 1
ON DUPLICATE KEY UPDATE title=title;

INSERT INTO eb_article
 (id,cid,title,author,image_input,synopsis,share_title,share_synopsis,visit,sort,status,hide,is_hot,is_banner,content,create_time,update_time)
SELECT 990003,CAST(c.id AS CHAR),'常见花材的日常照看','花间集花艺师',
 'crmebimage/public/flower/product-09.jpg','玫瑰、百合与向日葵各有特点，按花材调整照看方法。',
 '常见花材养护','花间集鲜花养护',0,10,0,0,0,0,
 '<p>玫瑰需要及时去除水下叶片；百合花粉成熟后可小心摘除花药，避免沾染花瓣；向日葵宜放在通风处并勤换水。观赏期受环境温度、运输和养护方式影响，展示天数只作参考。</p>',NOW(),NOW()
FROM eb_category c WHERE c.type=3 AND c.url='flower-care' ORDER BY c.id LIMIT 1
ON DUPLICATE KEY UPDATE title=title;

INSERT INTO eb_system_menu (id,pid,name,icon,perms,component,menu_type,sort,is_show,is_delte,create_time,update_time)
VALUES (655,578,'养花指南',NULL,'admin:flower:care:list','/flower/care','C',12,1,0,NOW(),NOW()),
 (656,655,'查看养花文章',NULL,'admin:flower:care:list','','A',20,1,0,NOW(),NOW()),
 (657,655,'编辑养花文章',NULL,'admin:flower:care:save','','A',10,1,0,NOW(),NOW())
ON DUPLICATE KEY UPDATE id=id;
INSERT INTO eb_system_role_menu (rid,menu_id)
SELECT 9001,m.id FROM eb_system_menu m WHERE m.id BETWEEN 655 AND 657
AND NOT EXISTS (SELECT 1 FROM eb_system_role_menu rm WHERE rm.rid=9001 AND rm.menu_id=m.id);
UPDATE eb_system_role SET rules=CONCAT_WS(',',NULLIF(rules,''),'655','656','657')
WHERE id=9001 AND NOT FIND_IN_SET('655',rules);
