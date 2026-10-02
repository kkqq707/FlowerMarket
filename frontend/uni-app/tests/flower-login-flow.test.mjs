import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('the branded login entry opens the full phone login page', async () => {
  const entry = await read('../pages/users/wechat_login/index.vue');
  const phone = await read('../pages/users/login/index.vue');
  const pages = await read('../pages.json');
  assert.match(entry, /@click="openPhoneLogin"[^>]*>\s*立即登录/);
  assert.match(entry, /uni\.navigateTo\(\{\s*url:\s*['"]\/pages\/users\/login\/index['"]/);
  assert.doesNotMatch(entry, /open-type="getPhoneNumber"/);
  assert.match(phone, /FLOWER_BRAND\.logo/);
  assert.match(phone, /placeholder="输入手机号码"/);
  assert.match(phone, /placeholder="填写验证码"/);
  assert.match(phone, /我已阅读并同意/);
  assert.match(phone, /账号登录/);
  assert.match(pages, /"path": "login\/index"[\s\S]*?"navigationStyle": "custom"/);
});
