import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';

const base = 'http://127.0.0.1:8080/api/admin/';
const password = process.env.FLOWER_STAFF_PASSWORD;
assert.ok(password, 'Set FLOWER_STAFF_PASSWORD to the initial demo password');
const profiles = [
  ['flower_manager', ['flower/dashboard', 'flower/order/list', 'flower/material/list', 'flower/product/list'], ['admin:flower:order:refund'], []],
  ['flower_goods', ['flower/product/list', 'flower/material/list'], ['admin:flower:product:save'], ['admin:flower:order:refund', 'admin:flower:order:deliver']],
  ['flower_florist', ['flower/order/list', 'flower/material/list'], ['admin:flower:order:produce'], ['admin:flower:order:refund', 'admin:flower:order:deliver', 'admin:flower:material:adjust']],
  ['flower_delivery', ['flower/order/list', 'flower/delivery/zone/list'], ['admin:flower:order:deliver', 'admin:flower:order:complete'], ['admin:flower:order:refund', 'admin:flower:order:produce']],
  ['flower_service', ['flower/order/list', 'user/list'], ['admin:flower:order:accept'], ['admin:flower:order:refund', 'admin:user:operate:founds']],
];
async function request(path, token, data) {
  const response = await fetch(new URL(path, base), {
    method: data ? 'POST' : 'GET',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(data ? { body: JSON.stringify(data) } : {}),
    signal: AbortSignal.timeout(15000),
  });
  return response.json();
}
const results = [];
function assertDenied(response, label) {
  // The running local backend uses code 500 with a localized access-denied message.
  assert.ok(response.code === 403 || (response.code === 500 && ['不允许访问', 'Access is denied'].includes(response.message)), label);
  assert.ok(response.data == null, `${label}: no protected data returned`);
}
for (const [account, allowedPaths, requiredPermissions, forbiddenPermissions] of profiles) {
  const login = await request('login', null, { account, pwd: password });
  assert.equal(login.code, 200, `${account}: login`);
  const token = login.data.token;
  try {
    const info = await request('getAdminInfoByToken', token);
    assert.equal(info.code, 200, `${account}: identity`);
    assert.equal(info.data.account, account);
    const permissions = info.data.permissionsList;
    for (const permission of requiredPermissions) assert.ok(permissions.includes(permission), `${account}: missing ${permission}`);
    for (const permission of forbiddenPermissions) assert.ok(!permissions.includes(permission), `${account}: unexpected ${permission}`);
    assert.ok(!permissions.some(p => p === '*:*:*' || p.startsWith('admin:system:admin:') || p.startsWith('admin:system:role:')));
    const menus = await request('getMenus', token);
    assert.equal(menus.code, 200, `${account}: menus`);
    assert.ok(menus.data.length > 0, `${account}: usable navigation`);
    for (const path of allowedPaths) assert.equal((await request(path, token)).code, 200, `${account}: ${path}`);
    for (const path of ['system/admin/list', 'system/role/list']) assertDenied(await request(path, token), `${account}: block ${path}`);
    if (account !== 'flower_manager') assertDenied(await request('flower/dashboard', token), `${account}: block dashboard`);
    results.push({ account, login: 'passed', allowedPaths, permissionChecks: 'passed', privilegedAccess: 'blocked' });
    console.log(`${account}: login, menus, permitted APIs and access restrictions passed`);
  } finally {
    assert.equal((await request('logout', token)).code, 200, `${account}: logout`);
  }
}
await writeFile(new URL('./staff-data-verification.json', import.meta.url), JSON.stringify({ checkedAt: new Date().toISOString(), result: 'passed', accounts: results }, null, 2) + '\n');
