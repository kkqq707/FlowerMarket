import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';
import { flowerDetailMetrics, buildFlowerProductView } from '../frontend/uni-app/utils/flower-product-view.js';

const base = process.env.FLOWER_FRONT_URL || 'http://127.0.0.1:8081/api/front/';
let checks = 0;
async function api(path, query = {}) {
  const url = new URL(path, base);
  Object.entries(query).forEach(([key, value]) => url.searchParams.set(key, value));
  const response = await fetch(url, { signal: AbortSignal.timeout(15000) });
  assert.equal(response.status, 200, `${path}: HTTP status`);
  const body = await response.json();
  assert.equal(body.code, 200, `${path}: ${body.message || 'response code'}`);
  return body.data;
}
function same(actual, expected, label) {
  assert.deepEqual(actual, expected, label);
  checks++;
}
async function products(query = {}) {
  const rows = [];
  let page = 1;
  for (;;) {
    const data = await api('flower/product/list', { ...query, page, limit: 100 });
    rows.push(...data.list);
    if (rows.length >= data.total || data.list.length < 100) return rows;
    page++;
  }
}
const list = await products();
const filters = await api('flower/product/filters');
const details = new Map();
const split = value => (value || '').replaceAll('，', ',').split(',').map(v => v.trim()).filter(Boolean);
for (const card of list) {
  const [flower, generic] = await Promise.all([
    api(`flower/product/${card.id}`), api(`product/detail/${card.id}`)
  ]);
  const product = generic.productInfo;
  details.set(card.id, product);
  same(card.storeName, product.storeName, `${card.id}: list/detail name`);
  same(Number(card.price), Number(product.price), `${card.id}: list/detail price`);
  same(Number(card.originalPrice), Number(product.otPrice), `${card.id}: original price`);
  same(card.stock, flowerDetailMetrics(product).stock, `${card.id}: displayed stock`);
  same(card.sales, flowerDetailMetrics(product).sales, `${card.id}: displayed sales`);
  same(buildFlowerProductView(card).title, product.storeName, `${card.id}: card normalization`);
  for (const field of ['storeName', 'price', 'originalPrice', 'sales', 'stock', 'flowerMaterials', 'flowerLanguage', 'occasion', 'recipient', 'colorSeries']) {
    same(flower[field], card[field], `${card.id}: flower list/detail ${field}`);
  }
  const skus = Object.values(generic.productValue || {});
  if (skus.length === 1) {
    same(Number(skus[0].price), Number(card.price), `${card.id}: single SKU price`);
    same(skus[0].stock, card.stock, `${card.id}: single SKU stock`);
  }
}
const ids = rows => rows.map(row => row.id);
for (const field of ['occasion', 'recipient', 'colorSeries']) {
  same([...new Set(list.flatMap(row => split(row[field])))], filters[field], `${field}: metadata comes from stored tags`);
  for (const value of filters[field]) {
    const actual = await products({ [field]: value });
    same(ids(actual), ids(list.filter(row => split(row[field]).includes(value))), `${field}=${value}: exact results`);
  }
}
const categories = await api('category');
for (const category of categories) {
  const matchingIds = categories.filter(c => c.id === category.id || c.path?.includes(`/${category.id}/`)).map(c => String(c.id));
  const expected = list.filter(card => split(details.get(card.id).cateId).some(id => matchingIds.includes(id)));
  same(ids(await products({ cid: category.id })), ids(expected), `category ${category.id}: results`);
}
const material = list.map(row => split((row.flowerMaterials || '').replaceAll('、', ','))).flat().find(value => value === '尤加利叶');
if (material) same(ids(await products({ keyword: material })), ids(list.filter(row => row.storeName.includes(material) || row.flowerMaterials.includes(material))), 'flower material search');
const priceRanges = [{ minPrice: 0, maxPrice: 199.99 }, { minPrice: 200, maxPrice: 299.99 }, { minPrice: 300, maxPrice: 500 }, { minPrice: 500.01 }];
for (const range of priceRanges) {
  same(ids(await products(range)), ids(list.filter(row => Number(row.price) >= range.minPrice && (range.maxPrice === undefined || Number(row.price) <= range.maxPrice))), 'budget results');
}
same(ids(await products({ keyword: '__data_sync_no_match__' })), [], 'empty results stay empty');
const report = { checkedAt: new Date().toISOString(), products: list.length, categories: categories.length, filterValues: Object.values(filters).reduce((n, values) => n + values.length, 0), checks, result: 'passed', coverage: ['list/detail/SKU price and stock', 'displayed sales including configured virtual sales', 'actual filter metadata and exact matches', 'category filters', 'material keyword search', 'budget filters', 'empty results'] };
await writeFile(new URL('./data-sync-verification.json', import.meta.url), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
