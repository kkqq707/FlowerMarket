import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const pages = ['goods_cate/goods_cate.vue', 'goods/goods_list/index.vue'];
function pageQuery(page, cid = '') {
  const source = readFileSync(new URL(`../pages/${page}`, import.meta.url), 'utf8');
  const start = source.indexOf('function query()');
  const end = source.indexOf('\n}', start) + 2;
  assert.ok(start >= 0 && end > start);
  const state = {
    keyword: { value: '' }, cid: { value: cid }, page: { value: 1 },
    flowerFilters: { value: [{ key: 'occasion', values: ['全部', '生日'] }] },
    selected: { occasion: 0, budget: 0 },
  };
  // Execute the page's query builder, then check what reaches the request serializer.
  return new Function('state', 'flowerBudgets', `
    const { keyword, cid, page, flowerFilters, selected } = state;
    ${source.slice(start, end)}
    return query();
  `)(state, [{}]);
}

for (const page of pages) {
  test(`${page}: all products omits the optional category parameter`, () => {
    const query = pageQuery(page);
    assert.equal(Object.hasOwn(query, 'cid'), false);
    const serialized = new URLSearchParams(query).toString();
    assert.equal(serialized.includes('undefined'), false);
    assert.equal(serialized.includes('cid='), false);
  });

  test(`${page}: an explicit category stays in the product request`, () => {
    const query = pageQuery(page, '9101');
    assert.equal(query.cid, '9101');
    assert.equal(new URLSearchParams(query).get('cid'), '9101');
  });

  test(`${page}: actual page parameters load the live catalog`, { skip: process.env.FLOWER_CATALOG_LIVE !== '1' }, async () => {
    const base = 'http://127.0.0.1:8081/api/front/flower/product/list';
    const body = await (await fetch(`${base}?${new URLSearchParams(pageQuery(page))}`)).json();
    assert.equal(body.code, 200, body.message);
    assert.ok(body.data.list.length > 0, 'local catalog has available flowers');
    const expected = await (await fetch(`${base}?page=1&limit=12`)).json();
    assert.deepEqual(body.data.list.map(item => item.id), expected.data.list.map(item => item.id));
  });
}
