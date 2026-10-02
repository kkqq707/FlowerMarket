import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');

test('flower tab icons use one restrained set, with three category tiles and a free leaf', () => {
  const source = read('assets/tabbar-icons/flower-icons.mjs');
  const category = source.match(/category:\s*`([^`]+)`/)?.[1] || '';
  assert.equal((category.match(/<rect\b/g) || []).length, 3);
  assert.match(category, /<path\b[^>]*\/?>/);
  const pages = read('pages.json');
  for (const name of ['home', 'category', 'cart', 'user']) {
    assert.match(pages, new RegExp(`static/tabBar/flower-${name}-idle\\.png`));
    assert.match(pages, new RegExp(`static/tabBar/flower-${name}-active\\.png`));
    for (const state of ['idle', 'active']) {
      const png = readFileSync(new URL(`../static/tabBar/flower-${name}-${state}.png`, import.meta.url));
      assert.equal(png.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
      assert.equal(png.readUInt32BE(16), 81);
      assert.equal(png.readUInt32BE(20), 81);
    }
  }
});

test('scrollable custom pages keep their top navigation visible', () => {
  const customHeaders = [
    ['components/flower-home/hero.vue', 'hero__masthead'],
    ['pages/order_addcart/order_addcart.vue', 'cart_nav'],
    ['pages/user/index.vue', 'account-topbar'],
    ['pages/users/wechat_login/index.vue', 'title-bar'],
    ['pages/users/app_login/index.vue', 'title-bar'],
    ['pages/activity/goods_combination_details/index.vue', 'navbar'],
    ['pages/activity/goods_seckill_details/index.vue', 'navbar'],
    ['pages/goods/goods_details/index.vue', 'flower-detail__nav'],
    ['pages/goods/goods_search/index.vue', 'search'],
    ['pages/order/order_confirm/index.vue', 'mp-nav-bar'],
  ];
  for (const [file, className] of customHeaders) {
    const source = read(file);
    assert.match(source, new RegExp(`\\.${className}[^{}]*\\{[^}]*position:\\s*fixed`, 's'), file);
  }
});

test('launch advertisement remains immersive, without the storefront top bar', () => {
  assert.doesNotMatch(read('pages/guide/index.vue'), /guide-topbar/);
});

test('product header stays above scrolling media without hiding under the WeChat capsule', () => {
  const detail = read('pages/goods/goods_details/index.vue');
  assert.match(detail, /flower-detail__nav-space/);
  assert.match(detail, /flower-detail__nav-title[^>]*>花礼详情</);
  assert.match(detail, /flower-detail__back[^>]*aria-label="返回"/);
  assert.doesNotMatch(detail.match(/<view class="flower-detail__nav"[\s\S]*?<\/view>/)?.[0] || '', /shareFlower/);
  assert.match(detail, /flower-share-action[^>]*@click="shareFlower"/);
  assert.match(detail, /\.flower-detail__back\s*\{[^}]*border:\s*0/);
});

test('cart navigation uses the same warm fixed surface as the account page', () => {
  const cart = read('pages/order_addcart/order_addcart.vue');
  const account = read('pages/user/index.vue');
  assert.match(cart, /class="navbarCon"[^>]*statusBarHeight/);
  assert.match(cart, /\.cart_nav\s*\{[^}]*background:\s*\$flower-background/s);
  assert.match(cart, /\.nav_title\s*\{[^}]*color:\s*\$flower-text/s);
  assert.match(account, /\.account-topbar\s*\{[^}]*background:\s*\$flower-background/s);
});
