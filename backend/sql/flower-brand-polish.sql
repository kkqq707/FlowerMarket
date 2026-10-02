-- Apply to an existing 花间集 database after placing crmebimage/public/flower/logo.png on the backend host.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `eb_system_menu` SET `sort`=190, `update_time`=NOW() WHERE `id`=578;
UPDATE `eb_system_menu` SET `sort`=180, `update_time`=NOW() WHERE `id`=2;
UPDATE `eb_system_menu` SET `name`='商城装饰', `update_time`=NOW() WHERE `id`=439;

UPDATE `eb_system_config`
SET `value`='crmebimage/public/flower/logo.png', `update_time`=NOW()
WHERE `name` IN ('mobile_login_logo','site_logo_login','site_logo_lefttop','site_logo_square');

UPDATE `eb_system_form_temp`
SET `content`=REPLACE(
  REPLACE(
    REPLACE(
      REPLACE(`content`,
        'crmebimage/public/product/2025/05/30/31919ceca46d409f9a3d4af3c0ff0337y2jhe3less.png',
        'crmebimage/public/flower/logo.png'),
      'crmebimage/public/product/2025/05/30/081a002d92894a74ad498c825c29aa26or4ycyd3cy.png',
      'crmebimage/public/flower/logo.png'),
    'crmebimage/public/product/2025/05/30/80463f2c08f942c7919ffdb1508aaefcadw8s5mfey.jpeg',
    'crmebimage/public/flower/logo.png'),
  'crmebimage/public/product/2025/05/30/d9668baa15d04179aff2ccddf985e92e83dqxh6y9x.png',
  'crmebimage/public/flower/logo.png')
WHERE `id`=151;

COMMIT;
