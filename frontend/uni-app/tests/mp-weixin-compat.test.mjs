import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (relativePath) => readFile(new URL(relativePath, import.meta.url), 'utf8');

test('scoped styles never compile a universal selector into invalid WXSS', async () => {
  const files = [
    '../components/flower-home/hero.vue',
    '../pages/index/index.vue',
    '../pages/goods_cate/goods_cate.vue',
    '../pages/goods/goods_list/index.vue',
    '../pages/goods/goods_details/index.vue',
  ];

  for (const file of files) {
    const source = await read(file);
    assert.doesNotMatch(
      source,
      /@media\s*\(?\s*prefers-reduced-motion[\s\S]*?\{\s*\*\s*\{/,
      `${file} must not use a scoped universal selector inside reduced-motion media queries`,
    );
  }
});

test('WeChat output configuration contains only supported window and timeout keys', async () => {
  const [pages, manifest] = await Promise.all([
    read('../pages.json'),
    read('../manifest.json'),
  ]);
  const globalStyle = pages.match(/"globalStyle"\s*:\s*\{([\s\S]*?)\n\s*\},\n\s*"tabBar"/)?.[1] || '';
  assert.doesNotMatch(globalStyle, /"(?:titleNView|rpxCalcMaxDeviceWidth|rpxCalcBaseDeviceWidth|rpxCalcIncludeWidth)"/);
  assert.doesNotMatch(manifest, /"networkTimeout"\s*:\s*\{[\s\S]*?"socket"\s*:/);
  assert.match(manifest, /"networkTimeout"\s*:\s*\{[\s\S]*?"connectSocket"\s*:/);
});

test('local WeChat builds use the developer-tools tourist AppID', async () => {
  const manifest = await read('../manifest.json');
  const weixinBlock = manifest.match(/"mp-weixin"\s*:\s*\{([\s\S]*?)\n\s*\},\n\s*"mp-alipay"/)?.[1] || '';
  assert.match(weixinBlock, /"appid"\s*:\s*"touristappid"/);
});
