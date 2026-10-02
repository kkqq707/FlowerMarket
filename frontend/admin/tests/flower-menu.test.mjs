import assert from 'node:assert/strict';
import test from 'node:test';

import { arrangeFlowerMenus } from '../src/utils/flower-menu.js';

const menu = (id, name, component, childList = []) => ({ id, name, component, childList });

test('员工账号和角色权限显示在顾客与员工下，技术权限项保留在系统设置', () => {
  const original = [
    menu(4, '顾客与员工', '/user', [menu(39, '顾客管理', '/user/index')]),
    menu(12, '系统设置', '/operation', [
      menu(121, '花店基础设置', '/operation/setting'),
      menu(123, '员工与权限', '/operation/roleManager', [
        menu(137, '员工账号', '/operation/roleManager/adminList'),
        menu(136, '角色管理', '/operation/roleManager/identityManager'),
        menu(138, '权限规则', '/operation/roleManager/promiseRules'),
      ]),
    ]),
  ];

  const arranged = arrangeFlowerMenus(original);
  assert.deepEqual(arranged[0].childList.map(({ id }) => id), [39, 137, 136]);
  assert.equal(arranged[0].childList[2].name, '角色与权限');
  assert.equal(arranged[0].childList[1].component, '/operation/roleManager/adminList');
  assert.deepEqual(arranged[1].childList.map(({ id }) => id), [121, 138]);
  assert.equal(arranged[1].childList[1].name, '菜单与权限项');
  assert.equal(original[1].childList[1].childList[1].name, '角色管理');
});

test('缺少顾客与员工入口时保留员工菜单的原位置，避免权限入口丢失', () => {
  const original = [menu(12, '系统设置', '/operation', [
    menu(123, '员工与权限', '/operation/roleManager', [menu(137, '员工账号', '/operation/roleManager/adminList')]),
  ])];

  const arranged = arrangeFlowerMenus(original);
  assert.equal(arranged[0].childList[0].childList[0].id, 137);
});

test('格式化后的菜单树沿用相同分组，并避免重复入口', () => {
  const original = [
    { id: 4, name: '顾客与员工', child: [{ id: 39, name: '顾客管理', child: [] }] },
    { id: 12, name: '系统设置', child: [{ id: 123, name: '员工与权限', child: [
      { id: 137, name: '员工账号', child: [] },
      { id: 136, name: '角色管理', child: [] },
      { id: 138, name: '权限规则', child: [] },
    ] }] },
  ];

  const arranged = arrangeFlowerMenus(original);
  assert.deepEqual(arranged[0].child.map(({ id }) => id), [39, 137, 136]);
  assert.deepEqual(arranged[1].child.map(({ id }) => id), [138]);
});
