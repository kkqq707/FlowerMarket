import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const sqlUrl = new URL('../../../backend/sql/flower-demo-data.sql', import.meta.url)

test('flower demo data is complete, local and repeatable', async () => {
  const sql = await readFile(sqlUrl, 'utf8')

  const productIds = [...sql.matchAll(/9000(?:0[1-9]|1[0-2])/g)].map((item) => item[0])
  assert.equal(new Set(productIds).size, 12, '应包含 12 个固定的花卉商品编号')

  assert.match(sql, /INSERT INTO `eb_store_product`/)
  assert.match(sql, /INSERT INTO `eb_store_product_attr_value`/)
  assert.match(sql, /INSERT INTO `eb_flower_product_ext`/)
  assert.match(sql, /ON DUPLICATE KEY UPDATE/)
  assert.ok(sql.includes('/static/images/flower-products/product-12.jpg'))
  assert.doesNotMatch(sql, /https?:\/\//, '演示商品不能依赖外部图片地址')

  assert.match(sql, /花间集/)
  assert.match(sql, /flower_store_name/)
  assert.match(sql, /flower_store_phone/)
  assert.match(sql, /flower_store_address/)
  assert.match(sql, /flower_business_hours/)
  assert.match(sql, /flower_mock_pay_enabled[\s\S]*?'1'/)
  assert.match(sql, /花店运营员/)
  assert.match(sql, /DELETE FROM `eb_system_role_menu` WHERE `rid`=9001/)
  assert.match(sql, /INSERT INTO `eb_system_role_menu`/)

  assert.match(sql, /核心配送区/)
  assert.match(sql, /周边配送区/)
  assert.match(sql, /WITH RECURSIVE `demo_days`/)
  assert.match(sql, /INTERVAL 6 DAY/)
  assert.match(sql, /`id` BETWEEN 9000000 AND 9999999/)
  assert.match(sql, /DATEDIFF\(DATE_ADD\(CURDATE\(\), INTERVAL \(`day_offset` \+ 1\) DAY\),'2020-01-01'\)/)
  assert.match(sql, /'09:00:00'/)
  assert.match(sql, /'12:00:00'/)
  assert.match(sql, /'14:00:00','17:00:00'/)
  assert.match(sql, /'18:00:00','21:00:00'/)
})
