import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

async function loadBrandModule() {
  try {
    const source = await readFile(new URL('../utils/flower-brand.js', import.meta.url), 'utf8');
    return import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
  } catch (error) {
    if (error.code === 'ENOENT') return null;
    throw error;
  }
}

async function read(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8');
}

function visibleTemplateText(source) {
  const template = source.match(/<template>([\s\S]*?)<\/template>/)?.[1] || '';
  return template
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\{\{[\s\S]*?\}\}/g, ' ');
}

async function loadAdminPageTitle(storedTitle) {
  const source = await read('../../admin/src/utils/get-page-title.js');
  const previousStorage = globalThis.localStorage;
  globalThis.localStorage = {
    getItem: (key) => key === 'singleAdminSiteName' ? storedTitle : null,
  };
  try {
    return await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}#${encodeURIComponent(String(storedTitle))}`);
  } finally {
    globalThis.localStorage = previousStorage;
  }
}

test('publishes the exact local 花间集 brand contract', async () => {
  const brandModule = await loadBrandModule();
  assert.ok(brandModule, 'flower brand module should exist');
  assert.deepEqual(brandModule.FLOWER_BRAND, {
    name: '花间集',
    logo: '/static/images/huajianji/logo.png',
    wordmark: '/static/images/huajianji/wordmark.png',
  });
  assert.equal(Object.isFrozen(brandModule.FLOWER_BRAND), true);
});

test('returns to the previous page when flower login has navigation history', async () => {
  const brandModule = await loadBrandModule();
  assert.ok(brandModule, 'flower brand module should exist');
  const calls = [];

  brandModule.goBackFromFlowerLogin({
    pageCount: () => 2,
    back: () => calls.push('back'),
    home: () => calls.push('home'),
  });

  assert.deepEqual(calls, ['back']);
});

test('returns to the flower home when login has no previous page', async () => {
  const brandModule = await loadBrandModule();
  assert.ok(brandModule, 'flower brand module should exist');
  const calls = [];

  brandModule.goBackFromFlowerLogin({
    pageCount: () => 1,
    back: () => calls.push('back'),
    home: () => calls.push('home'),
  });

  assert.deepEqual(calls, ['home']);
});

test('login begins with an accessible return control wired to the navigation helper', async () => {
  const source = await read('../pages/users/login/index.vue');
  const rootContents = source.match(/<div class="login-wrapper"[^>]*>([\s\S]*)/)?.[1] || '';

  assert.match(rootContents, /^\s*<button[^>]*class="login-back"[^>]*aria-label="返回"[^>]*@click="handleBack"/);
  assert.match(source, /min-width:\s*88rpx/);
  assert.match(source, /min-height:\s*88rpx/);
  assert.match(source, /\.login-back\s*\{[\s\S]*?width:\s*88rpx;[\s\S]*?height:\s*88rpx;[\s\S]*?border-radius:\s*50%;/);
  assert.match(source, /\.login-back:focus-visible\s*\{/);
  assert.match(source, /pageCount:\s*\(\)\s*=>\s*getCurrentPages\(\)\.length/);
  assert.match(source, /back:\s*\(\)\s*=>\s*uni\.navigateBack\(\{\s*delta:\s*1,\s*fail:\s*home\s*\}\)/);
  assert.match(source, /home:\s*\(\)\s*=>\s*uni\.switchTab\(\{\s*url:\s*['"]\/pages\/index\/index['"]\s*\}\)/);
});

test('login uses local brand visuals and retains a visible 花间集 fallback', async () => {
  const source = await read('../pages/users/login/index.vue');

  assert.match(source, /FLOWER_BRAND\.logo/);
  assert.match(source, /FLOWER_BRAND\.wordmark/);
  assert.match(visibleTemplateText(source), /花间集/);
  assert.doesNotMatch(source, /:src="mobileLoginLogo"/);
});

test('homepage uses configured flower brand with 花间集 as the safe fallback', async () => {
  const source = await read('../pages/index/index.vue');

  assert.match(source, /import\s+\{\s*FLOWER_BRAND\s*\}\s+from\s+['"]@\/utils\/flower-brand\.js['"]/);
  assert.match(source, /brand\s*=\s*ref\(FLOWER_BRAND\.name\)/);
  assert.match(source, /getFlowerStoreConfig/);
  assert.match(source, /brand\.value\s*=\s*config\.storeName\s*\|\|\s*FLOWER_BRAND\.name/);
  assert.doesNotMatch(source, /brand\.value\s*=\s*config\.(?:brandName|siteName)/);
  assert.doesNotMatch(source, /brand\.value\s*=\s*uni\.getStorageSync\(['"]companyName['"]\)/);
  assert.match(source, /onShareAppMessage\(\(\)\s*=>\s*\(\{\s*title:\s*brand\.value/);
});

test('customer-visible brand defaults do not expose CRMEB', async () => {
  const [login, home, manifest] = await Promise.all([
    read('../pages/users/login/index.vue'),
    read('../pages/index/index.vue'),
    read('../manifest.json'),
  ]);
  const manifestName = manifest.match(/"name"\s*:\s*"([^"]*)"/)?.[1] || '';
  const manifestDescription = manifest.match(/"description"\s*:\s*"([^"]*)"/)?.[1] || '';

  assert.doesNotMatch(visibleTemplateText(login), /CRMEB/i);
  assert.doesNotMatch(visibleTemplateText(home), /CRMEB/i);
  assert.doesNotMatch(`${manifestName} ${manifestDescription}`, /CRMEB/i);
});

test('manifest identifies the app as 花间集同城鲜花商城', async () => {
  const manifest = await read('../manifest.json');

  assert.match(manifest, /"name"\s*:\s*"花间集"/);
  assert.match(manifest, /"description"\s*:\s*"花间集同城鲜花商城"/);
});

test('admin page titles use 花间集 and reject cached legacy branding', async () => {
  const empty = await loadAdminPageTitle(null);
  const legacy = await loadAdminPageTitle('CRMEB Java');
  const custom = await loadAdminPageTitle('鲜花门店');

  assert.equal(empty.default(), '花间集管理后台');
  assert.equal(legacy.default('商品管理'), '商品管理 - 花间集管理后台');
  assert.equal(custom.default('订单'), '订单 - 鲜花门店');
});

test('admin shell and login use only local 花间集 brand assets', async () => {
  const [logo, login] = await Promise.all([
    read('../../admin/src/layout/logo/index.vue'),
    read('../../admin/src/views/login/index.vue'),
  ]);

  assert.match(logo, /['"]\/huajianji\/wordmark\.png['"]/);
  assert.match(logo, /['"]\/huajianji\/logo\.png['"]/);
  assert.doesNotMatch(logo, /getSiteLogoApi|Cookies\.get\(['"]logoInfo/);
  assert.match(login, /src=['"]\/huajianji\/wordmark-login\.png['"]/);
  assert.match(login, /src=['"]\/huajianji\/login-bouquet-v2\.png['"]/);
  assert.doesNotMatch(login, /loginLogo\.value\s*=\s*res\.loginLogo|version\s*=\s*ref\(['"]CRMEB/i);
});

test('admin visible defaults identify 花间集 without exposing legacy copyright metadata', async () => {
  const [dashboard, theme, html] = await Promise.all([
    read('../../admin/src/views/dashboard/index.vue'),
    read('../../admin/src/store/modules/themeConfig.js'),
    read('../../admin/index.html'),
  ]);

  assert.match(visibleTemplateText(dashboard), /欢迎使用花间集花店管理后台/);
  assert.doesNotMatch(visibleTemplateText(dashboard), /CRMEB/i);
  assert.match(theme, /globalTitle:\s*['"]花间集管理后台['"]/);
  assert.match(html, /<link\s+rel="icon"\s+href="\/huajianji\/logo\.png"\s*\/?>/);
  assert.match(html, /<title>花间集管理后台<\/title>/);
  assert.doesNotMatch(html, /CRMEB|copyright/i);
});

test('secondary customer pages do not expose the legacy storefront brand', async () => {
  const [pages, updatePage] = await Promise.all([
    read('../pages.json'),
    read('../pages/users/app_update/app_update.vue'),
  ]);

  const authBlock = pages.match(/"root"\s*:\s*"pages\/auth"[\s\S]*?\/\/\s*#endif/)?.[0] || '';
  assert.match(authBlock, /"navigationBarTitleText"\s*:\s*"花间集"/);
  assert.doesNotMatch(authBlock, /CRMEB/i);
  assert.match(updatePage, /FLOWER_BRAND\.logo/);
  assert.match(updatePage, /\{\{\s*FLOWER_BRAND\.name\s*\}\}/);
  assert.doesNotMatch(visibleTemplateText(updatePage), /CRMEB/i);
});
