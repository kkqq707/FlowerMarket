import test from 'node:test'
import assert from 'node:assert/strict'
import { arrangeFlowerMenus } from '../src/utils/flower-menu.js'

test('existing database menus show operations before products with the updated decoration name', () => {
  const menus = [
    { id: 2, name: '花卉商品' },
    { id: 4, name: '顾客与员工' },
    { id: 578, name: '花店经营' },
    { id: 439, name: '店铺装修' },
  ]
  const arranged = arrangeFlowerMenus(menus)
  assert.deepEqual(arranged.map(item => item.id), [578, 2, 4, 439])
  assert.equal(arranged[0].name, '花卉经营')
  assert.equal(arranged[3].name, '商城装饰')
})

test('coupon entries are hidden from nested backend menus', () => {
  const menus = [
    { id: 1, name: '顾客与员工', child: [
      { id: 2, name: '顾客管理', url: '/user/index' },
      { id: 3, name: '发送优惠券', url: '/marketing/coupon/send' },
    ] },
    { id: 4, name: '优惠券', url: '/marketing/coupon/list' },
  ]
  const result = arrangeFlowerMenus(menus)
  assert.deepEqual(result.map(item => item.id), [1])
  assert.deepEqual(result[0].child.map(item => item.id), [2])
})

test('raw backend menu and cached sidebar menu both use the requested order and title', () => {
  const raw = [
    { id: 2, name: '花卉商品', component: '/store', childList: [] },
    { id: 439, name: '店铺装修', component: '/design', childList: [] },
    { id: 578, name: '花店经营', component: '/flower', childList: [] },
  ]
  const arranged = arrangeFlowerMenus(raw)
  assert.deepEqual(arranged.map(item => item.id), [578, 2, 439])
  assert.equal(arranged[0].name, '花卉经营')
  assert.equal(arranged[2].name, '商城装饰')

  const cached = raw.map(({ id, name, component }) => ({ id, title: name, path: component, children: [] }))
  const restored = arrangeFlowerMenus(cached)
  assert.deepEqual(restored.map(item => item.id), [578, 2, 439])
  assert.equal(restored[0].title, '花卉经营')
  assert.equal(restored[2].title, '商城装饰')
})
