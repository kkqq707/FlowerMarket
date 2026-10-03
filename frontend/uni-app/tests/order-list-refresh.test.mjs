import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

// Run the page's real handlers with controlled requests to reproduce reversed response order.
const source = readFileSync(new URL('../pages/users/order_list/index.vue', import.meta.url), 'utf8');
function handler(name) {
  const start = source.indexOf(`\tfunction ${name}(`);
  assert.ok(start >= 0, `missing page handler: ${name}`);
  const end = source.indexOf('\n\t}', start) + 3;
  return source.slice(start, end);
}
function page() {
  const pending = [];
  const request = query => new Promise((resolve, reject) => pending.push({ query, resolve, reject }));
  const state = Object.fromEntries(Object.entries({ orderStatus: 0, loading: false, loadend: false, page: 1, limit: 10, keywords: '', orderList: [], loadTitle: '', isShow: false }).map(([key, value]) => [key, { value }]));
  const create = new Function('state', 'getOrderListApi', `
    const { orderStatus, loading, loadend, page, limit, keywords, orderList, loadTitle, isShow } = state;
    const proxy = { $util: { SplitArray: (list, previous) => previous.concat(list) } };
    let orderRequestVersion = 0;
    const hydrateFlowerStages = () => {};
    ${handler('statusClick')}
    ${handler('getOrderList')}
    return { statusClick, getOrderList };
  `);
  return { ...create(state, request), pending, state };
}
const flush = () => new Promise(resolve => setImmediate(resolve));

test('switching order tabs starts a new request and discards the previous tab response', async () => {
  const view = page();
  view.statusClick(0);
  view.statusClick(1);
  assert.equal(view.pending.length, 2);
  assert.equal(view.pending[1].query.type, 1);
  view.pending[1].resolve({ data: { list: [{ orderId: 'current-tab' }] } });
  await flush();
  view.pending[0].resolve({ data: { list: [{ orderId: 'previous-tab' }] } });
  await flush();
  assert.deepEqual(view.state.orderList.value, [{ orderId: 'current-tab' }]);
  assert.equal(view.state.page.value, 2);
});

test('an obsolete request failure does not release the current request loading state', async () => {
  const view = page();
  view.statusClick(0);
  view.statusClick(2);
  view.pending[0].reject(new Error('old request failed'));
  await flush();
  assert.equal(view.state.loading.value, true);
  view.pending[1].resolve({ data: { list: [] } });
  await flush();
  assert.equal(view.state.loading.value, false);
  assert.equal(view.state.loadend.value, true);
});
