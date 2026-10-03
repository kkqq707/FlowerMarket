import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import productView from '../utils/flower-product-view.js';
import * as productViewModule from '../utils/flower-product-view.js';

const {
  FLOWER_IMAGE_FALLBACK,
  buildFlowerProductView,
  formatFlowerPrice,
  formatFlowerSales,
  resolveFlowerImage,
  splitFlowerPrice,
} = productView;

async function loadThemeDataModule() {
  const source = await readFile(new URL('../utils/theme.js', import.meta.url), 'utf8');
  const testableSource = source
    .replace('import { getThemeInfo } from "@/api/api.js";', 'const getThemeInfo = () => Promise.resolve({ data: {} });')
    .replaceAll('export function ', 'function ')
    .concat('\nexport const testExports = { createFlowerTheme: typeof createFlowerTheme === "undefined" ? undefined : createFlowerTheme };');
  return import(`data:text/javascript;base64,${Buffer.from(testableSource).toString('base64')}`);
}

async function loadFlowerStoreData() {
  const source = await readFile(new URL('../api/store.js', import.meta.url), 'utf8');
  const testableSource = source
    .replace(/\/\/ #ifdef MP[\s\S]*?\/\/ #endif/g, '')
    .replace('import request from "@/utils/request.js";', 'const request = {};')
    .replace("import flowerView from '@/utils/flower-product-view.js';", 'const flowerView = ' + JSON.stringify({ FLOWER_IMAGE_FALLBACK }) + ';');
  return import('data:text/javascript;base64,' + Buffer.from(testableSource).toString('base64'));
}

test('normalizes Java camelCase and paginated flower products for reusable cards', async () => {
  const { flowerProductsFromResponse } = await loadFlowerStoreData();
  const item = { id: 42, storeName: '白玫瑰', flowerMaterials: '玫瑰', price: 128 };
  const actual = flowerProductsFromResponse({ data: { list: [item] } });
  assert.equal(actual[0].store_name, '白玫瑰');
  assert.equal(actual[0].flower_materials, '玫瑰');
  assert.deepEqual(flowerProductsFromResponse({ data: [item] }), actual);
  assert.equal(Object.hasOwn(item, 'store_name'), false);
});

test('catalog filters preserve backend tag values rather than substitute frontend labels', async () => {
  const { flowerFiltersFromResponse } = await loadFlowerStoreData();
  const groups = flowerFiltersFromResponse({ data: { occasion: ['生日'], recipient: ['爱人', '客户'], colorSeries: ['白绿色', '奶油色', '白绿色'] } });
  assert.deepEqual(groups.find(group => group.key === 'recipient').values, ['全部', '爱人', '客户']);
  assert.deepEqual(groups.find(group => group.key === 'colorSeries').values, ['全部', '白绿色', '奶油色']);
});

test('demo flower filters combine keyword, occasion, recipient and color', async () => {
  const { filterFlowerDemo } = await loadFlowerStoreData();
  assert.deepEqual(filterFlowerDemo({keyword:'玫瑰',occasion:'生日',recipient:'恋人',colorSeries:'粉色'}).map(item => item.id), ['demo-04']);
  assert.deepEqual(filterFlowerDemo({occasion:'生日',colorSeries:'红色'}), []);
});

test('provides twelve local demo flowers across four occasions', async () => {
  const { flowerDemoProducts } = await loadFlowerStoreData();
  const expectedNames = [
    '晨露 · 白绿洋桔梗', '暖阳 · 向日葵花束', '香颂 · 香槟玫瑰',
    '怦然 · 粉色玫瑰', '欢颜 · 缤纷混搭', '奶油诗 · 郁金香',
    '挚爱 · 红玫瑰', '蝶梦 · 紫色蝴蝶兰', '月光 · 白色百合',
    '橙意 · 暖色花束', '晴空 · 蓝白花篮', '盛放 · 开业桌花',
  ];

  assert.equal(flowerDemoProducts.length, 12);
  assert.deepEqual(flowerDemoProducts.map(item => item.store_name), expectedNames);
  assert.deepEqual(
    flowerDemoProducts.map(item => item.id),
    Array.from({ length: 12 }, (_, index) => `demo-${String(index + 1).padStart(2, '0')}`),
  );
  for (const occasion of ['日常', '生日', '纪念日', '祝贺']) {
    assert.equal(flowerDemoProducts.filter(item => item.occasion === occasion).length, 3);
  }
  assert.ok(flowerDemoProducts.every((item, index) =>
    item.demo && item.sales === 0 &&
    item.image === `/static/images/flower-products/product-${String(index + 1).padStart(2, '0')}.jpg` &&
    item.subtitle && item.flowerMaterials && item.flowerLanguage &&
    item.recipient && item.colorSeries && Number(item.price) > 0
  ));
});

test('cart recommendations never replace empty or unavailable server data with demo products', async () => {
  const { flowerCartRecommendationsFromResponse } = await loadFlowerStoreData();
  const realFlowers = Array.from({ length: 8 }, (_, index) => ({
    id: index + 1,
    storeName: `鲜花 ${index + 1}`,
    flowerMaterials: '玫瑰',
    price: 99 + index,
  }));

  assert.deepEqual(
    flowerCartRecommendationsFromResponse({ data: { list: realFlowers } }).map(item => item.id),
    [1, 2, 3, 4, 5, 6],
  );
  assert.deepEqual(
    flowerCartRecommendationsFromResponse({ data: { list: [] } }).map(item => item.id),
    [],
  );
  assert.deepEqual(flowerCartRecommendationsFromResponse(null), []);
});

test('detail metrics retain zero SKU stock and use the same total sales as product cards', () => {
  assert.equal(typeof productViewModule.flowerDetailMetrics, 'function');
  const product = { stock: 99, sales: 32, ficti: 68 };
  assert.deepEqual(productViewModule.flowerDetailMetrics(product, { stock: 0 }), { stock: 0, sales: 100 });
  assert.deepEqual(productViewModule.flowerDetailMetrics(product, {}), { stock: 99, sales: 100 });
});

test('cart empty state only loads the flower catalogue', async () => {
  const source = await readFile(new URL('../pages/order_addcart/order_addcart.vue', import.meta.url), 'utf8');

  assert.doesNotMatch(source, /@\/components\/recommend\/index\.vue|get_host_product|<recommend\b/);
  assert.match(source, /getFlowerProducts/);
  assert.match(source, /flowerCartRecommendationsFromResponse/);
  assert.match(source, /flower-product-card/);
  assert.match(source, /去挑选鲜花/);
});

test('budget filters preserve inclusive bounds for the expanded local catalogue', async () => {
  const { flowerBudgets, flowerDemoProducts, filterFlowerDemo } = await loadFlowerStoreData();
  assert.deepEqual(filterFlowerDemo(flowerBudgets[1]).map(item => item.id), ['demo-01', 'demo-02']);
  assert.deepEqual(filterFlowerDemo(flowerBudgets[2]).map(item => item.id), ['demo-03', 'demo-04', 'demo-05', 'demo-06', 'demo-10']);
  assert.deepEqual(filterFlowerDemo(flowerBudgets[3]).map(item => item.id), ['demo-07', 'demo-08', 'demo-09', 'demo-11']);
  assert.deepEqual(filterFlowerDemo(flowerBudgets[4]).map(item => item.id), ['demo-12']);
  assert.equal(filterFlowerDemo({}).length, flowerDemoProducts.length);
});

test('formats numeric product prices as two-decimal yuan amounts', () => {
  assert.equal(productViewModule.formatFlowerPrice, formatFlowerPrice);
  assert.equal(formatFlowerPrice(128), '¥128.00');
  assert.equal(formatFlowerPrice('68.5'), '¥68.50');
});

test('splits integer and one-decimal prices into stable display parts', () => {
  assert.deepEqual(splitFlowerPrice(198), {
    symbol: '¥',
    integer: '198',
    decimal: '.00',
    text: '¥198.00',
  });
  assert.deepEqual(splitFlowerPrice('62.5'), {
    symbol: '¥',
    integer: '62',
    decimal: '.50',
    text: '¥62.50',
  });
});

test('adds structured price parts while retaining the compatible price text', () => {
  const view = buildFlowerProductView({ price: -8 });

  assert.deepEqual(view.priceParts, {
    symbol: '¥',
    integer: '0',
    decimal: '.00',
    text: '¥0.00',
  });
  assert.equal(view.priceText, '¥0.00');
});

test('renders structured baseline-aligned prices in card, row, and detail views', async () => {
  const locations = [
    { path: '../components/flower-product-card/index.vue', classPrefix: 'flower-product-card__price-', value: 'view.priceParts' },
    { path: '../components/flower-product-row/index.vue', classPrefix: 'flower-product-row__price-', value: 'view.priceParts' },
    { path: '../pages/goods/goods_details/index.vue', classPrefix: 'flower-price__', value: 'flowerPriceParts' },
  ];
  const sources = await Promise.all(locations.map(({ path }) => readFile(new URL(path, import.meta.url), 'utf8')));

  for (const [index, source] of sources.entries()) {
    const { classPrefix, value } = locations[index];
    assert.ok(source.includes(`class="${classPrefix}symbol">{{ ${value}.symbol }}`));
    assert.ok(source.includes(`class="${classPrefix}integer">{{ ${value}.integer }}`));
    assert.ok(source.includes(`class="${classPrefix}decimal">{{ ${value}.decimal }}`));
    assert.match(source, /font-family:\s*[^;]*sans-serif/);
    assert.match(source, /font-variant-numeric:\s*tabular-nums/);
    assert.match(source, /display:\s*inline-flex/);
    assert.match(source, /align-items:\s*baseline/);
  }
});

test('keeps grid card titles two lines tall and pushes metadata to the bottom', async () => {
  const source = await readFile(new URL('../components/flower-product-card/index.vue', import.meta.url), 'utf8');

  assert.match(source, /&__content\s*\{[^}]*display:\s*flex[^}]*flex-direction:\s*column/s);
  assert.match(source, /&__name\s*\{[^}]*height:\s*82rpx[^}]*line-height:\s*41rpx/s);
  assert.match(source, /&__meta\s*\{[^}]*margin-top:\s*auto/s);
});

test('keeps the grid image wrapper pure and places price markup after it', async () => {
  const source = await readFile(new URL('../components/flower-product-card/index.vue', import.meta.url), 'utf8');
  const imageBox = source.match(/<view class="flower-product-card__image-box">([\s\S]*?)<\/view>/);

  assert.ok(imageBox);
  assert.match(imageBox[1].trim(), /^<image\b[^>]*\/>$/);
  assert.ok(source.indexOf('flower-product-card__price') > source.indexOf('</view>', source.indexOf('flower-product-card__image-box')));
});

test('keeps the detail gallery free from navigation and share controls', async () => {
  const source = await readFile(new URL('../pages/goods/goods_details/index.vue', import.meta.url), 'utf8');
  const gallery = source.match(/<view class="flower-gallery">([\s\S]*?)<\/view>\s*<!-- End pure-image gallery/);

  assert.ok(gallery, 'detail gallery should retain an explicit pure-image boundary');
  assert.doesNotMatch(gallery[1], /<button|flower-gallery__(?:back|share)/);
  assert.match(gallery[1], /<swiper\b[\s\S]*<image\b/);
});

test('groups flower meaning and purchase guidance without losing detail actions', async () => {
  const source = await readFile(new URL('../pages/goods/goods_details/index.vue', import.meta.url), 'utf8');
  const body = source.match(/<view v-else class="flower-detail__body">([\s\S]*?)<flower-section-title title="也许，你还喜欢"/);

  assert.ok(body);
  assert.match(body[1], /class="flower-poem"[\s\S]*flowerInfo\.flowerLanguage/);
  assert.match(body[1], /class="flower-info-list"[\s\S]*@click="selecAttr"/);
  assert.match(body[1], /支持同城配送 \/ 到店自提/);
  assert.match(body[1], /flowerInfo\.careTips/);
  assert.match(body[1], /@click="openCareGuide"/);
  assert.match(body[1], /goods_comment_list\/index\?productId=/);
  assert.match(body[1], /<mp-html :content="description"/);
});

test('flower detail sends guests to login before opening the care guide', async () => {
  const source = await readFile(new URL('../pages/goods/goods_details/index.vue', import.meta.url), 'utf8');
  assert.match(source, /const openCareGuide = \(\) => \{\s*if \(!isLogin\.value\) return toLogin\(\);\s*uni\.navigateTo\(\{ url: '\/pages\/flower_care\/index' \}\);\s*\}/);
});

test('uses the local flower placeholder when a product image is empty', () => {
  assert.equal(resolveFlowerImage(''), FLOWER_IMAGE_FALLBACK);
  assert.equal(resolveFlowerImage(null), FLOWER_IMAGE_FALLBACK);
  assert.doesNotMatch(FLOWER_IMAGE_FALLBACK, /["\s]/);
  assert.match(decodeURIComponent(FLOWER_IMAGE_FALLBACK), /viewBox="0 0 400 400"/);
});

test('keeps an English subtitle optional in the product view', () => {
  const withSubtitle = buildFlowerProductView({
    id: 1,
    store_name: '晨雾玫瑰',
    store_name_en: 'MORNING ROSE',
    image: '/static/rose.jpg',
  });
  const withoutSubtitle = buildFlowerProductView({
    id: 2,
    store_name: '白色郁金香',
    image: '/static/tulip.jpg',
  });

  assert.equal(withSubtitle.englishTitle, 'MORNING ROSE');
  assert.equal(withoutSubtitle.englishTitle, '');
});

test('creates a readable sales label for missing and numeric sales counts', () => {
  assert.equal(formatFlowerSales(36), '已售 36 件');
  assert.equal(formatFlowerSales(), '已售 0 件');
});

test('keeps the product image view free from price-overlay fields', () => {
  const view = buildFlowerProductView({
    id: 3,
    store_name: '日落花园',
    image: '/static/sunset.jpg',
    price: 168,
    sales: 8,
  });

  assert.deepEqual(view.image, {
    src: '/static/sunset.jpg',
    alt: '日落花园',
  });
  assert.equal(Object.hasOwn(view.image, 'price'), false);
  assert.equal(Object.hasOwn(view.image, 'priceText'), false);
  assert.equal(Object.hasOwn(view.image, 'salesText'), false);
});

test('creates fixed flower theme data instead of reusing historical theme colours', async () => {
  const themeModule = await loadThemeDataModule();
  assert.equal(typeof themeModule.testExports.createFlowerTheme, 'function');

  const theme = themeModule.testExports.createFlowerTheme();
  assert.equal(theme.data.theme_color, '#35402F');
  assert.equal(theme.data.price_color, '#ED3C2D');
  assert.match(theme.style, /--view-theme: rgba\(53,64,47,1\)/);
  assert.match(theme.style, /--view-priceColor: #ED3C2D/);
});

test('home hero does not inherit unrelated legacy decoration carousels', async () => {
  const source = await readFile(new URL('../pages/index/index.vue', import.meta.url), 'utf8');
  assert.doesNotMatch(source, /swiperConfig\.list\[0\]\.img/);
  assert.match(source, /heroImage\.value\s*=\s*config\.flowerHeroImage\s*\|\|\s*''/);
});
