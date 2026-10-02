import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'

const root = resolve(import.meta.dirname, '..')
const read = (relativePath) => readFileSync(resolve(root, relativePath), 'utf8')

test('登录页严格保留目标图内容且不出现公司版权和欢迎回来区块', () => {
  const source = read('src/views/login/index.vue')
  assert.match(source, /花开有期/)
  assert.match(source, /生意常青/)
  assert.match(source, /把日常 · 过成花的样子/)
  assert.match(source, /花店管理后台/)
  assert.match(source, /用花连接生活 · 让美好常在/)
  assert.match(source, /安全登录 · 保护您的账号安全/)
  assert.doesNotMatch(source, /Copyright|西安众邦网络科技有限公司/)
  assert.doesNotMatch(source, /欢迎回来|WELCOME BACK|登录后继续管理您的花店/)
})

test('后台只注册论文目标所需的业务路由模块', () => {
  const source = read('src/router/index.js')
  for (const moduleName of ['flowerRouter', 'storeRouter', 'userRouter', 'designRouter', 'operationRouter']) {
    assert.match(source, new RegExp(`\\b${moduleName}\\b`))
  }
  for (const moduleName of [
    'orderRouter',
    'distributionRouter',
    'marketingRouter',
    'financialRouter',
    'contentRouter',
    'appSettingRouter',
    'maintainRouter',
  ]) {
    assert.doesNotMatch(source, new RegExp(`\\b${moduleName}\\b`))
  }
})

test('商品、顾客、装修和设置路由均按论文范围收敛', () => {
  const store = read('src/router/modules/store.js')
  assert.match(store, /花卉商品/)
  assert.match(store, /花卉分类/)
  assert.match(store, /库存管理/)
  assert.doesNotMatch(store, /保障服务|评论设置/)

  const user = read('src/router/modules/user.js')
  assert.match(user, /顾客与员工/)
  assert.doesNotMatch(user, /用户标签|用户分组/)

  const design = read('src/router/modules/design.js')
  assert.match(design, /商城装饰/)
  assert.match(design, /花卉素材/)
  assert.doesNotMatch(design, /我的主题|专题页面|一键换色/)

  const operation = read('src/router/modules/operation.js')
  assert.match(operation, /系统设置/)
  assert.doesNotMatch(operation, /一号通|短信购买|物流公司|城市数据/)
})

test('经营菜单位于商品菜单之前且数据库排序一致', () => {
  const router = read('src/router/index.js')
  const sql = read('../../backend/sql/flower-thesis-alignment.sql')
  assert.ok(router.indexOf('  flowerRouter,') < router.indexOf('  storeRouter,'))
  assert.match(sql, /`sort`=190[^\n]*`id`=578/)
  assert.match(sql, /商城装饰[^\n]*`id`=439/)
})

test('数据库升级脚本包含花卉分类、商品清理、素材和菜单白名单', () => {
  const sql = read('../../backend/sql/flower-thesis-alignment.sql')
  for (const category of ['玫瑰花束', '百合花束', '郁金香', '向日葵', '混合花束', '开业花篮']) {
    assert.match(sql, new RegExp(category))
  }
  assert.match(sql, /UPDATE `eb_store_product` SET `is_del`=1/)
  assert.match(sql, /eb_system_attachment/)
  assert.match(sql, /花卉素材/)
  assert.match(sql, /UPDATE `eb_system_menu` SET `is_show`=0/)
  assert.match(sql, /START TRANSACTION/)
  assert.match(sql, /COMMIT/)
})

test('花卉商品编辑页不再请求论文范围外的商品采集服务', () => {
  const listSource = read('src/views/store/index.vue')
  const source = read('src/views/store/creatStore/index.vue')
  assert.doesNotMatch(listSource, /商品采集|onCopy/)
  assert.doesNotMatch(source, /copyConfigApi|copyProductApi|一号通|99Api/)
})

test('登录后的默认落点固定为花店经营首页并忽略已下线路由', () => {
  const source = read('src/views/login/index.vue')
  assert.match(source, /FLOWER_HOME\s*=\s*['"]\/flower\/dashboard['"]/)
  assert.match(source, /ALLOWED_REDIRECT_PREFIXES/)
  assert.doesNotMatch(source, /redirect\.value \|\| ['"]\/dashboard['"]/) 
})

test('开屏广告只展示花卉图片而不泄露调试对象文本', () => {
  const source = read('src/views/design/advertisement/index.vue')
  assert.match(source, /item\.imageUrl/)
  assert.doesNotMatch(source, /\{\{\s*item\s*\}\}/)
})
