import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';
import { parse } from '@babel/parser';
import { parse as parseVue } from '@vue/compiler-sfc';
import { createMemoryHistory, createRouter } from 'vue-router';

const storeSource = readFileSync(new URL('../src/store/modules/menu.js', import.meta.url), 'utf8');
const storeAst = parse(storeSource, { sourceType: 'module' }).program;
const storeSetup = storeAst.body.find((node) => node.type === 'ExportNamedDeclaration')
  .declaration.declarations[0].init.arguments[1].body;
const declarations = [...storeAst.body, ...storeSetup.body]
  .filter((node) => node.type === 'FunctionDeclaration')
  .map((node) => storeSource.slice(node.start, node.end)).join('\n');
const componentSource = parseVue(readFileSync(new URL('../src/layout/navBars/tagsView/tagsView.vue', import.meta.url), 'utf8'))
  .descriptor.scriptSetup.content;
const mountCallback = parse(componentSource, { sourceType: 'module' }).program.body
  .find((node) => node.type === 'ExpressionStatement' && node.expression.callee.name === 'onMounted')
  .expression.arguments[0];

function fixture(list) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', redirect: '/flower/dashboard' },
      { path: '/flower', redirect: '/flower/dashboard', children: [
        { path: 'dashboard', name: 'FlowerDashboard', component: {}, meta: { title: '经营看板', isAffix: true } },
        { path: 'orders', name: 'FlowerOrders', component: {} },
      ] },
    ],
  });
  const state = { value: list };
  let saved;
  const context = {
    router,
    tagNavList: state,
    setTagNavListInLocalstorage: (value) => { saved = JSON.parse(JSON.stringify(value)); },
    getRouteTitleHandled: (route) => ({ ...route, meta: { ...route.meta } }),
    routeHasExist: (tags, route) => tags.some((tag) => tag.name === route.name && JSON.stringify(tag.query || {}) === JSON.stringify(route.query || {})),
  };
  vm.createContext(context);
  vm.runInContext(declarations, context);
  return { state, context, saved: () => saved };
}

const dashboard = (path, extra = {}) => ({ path, name: 'FlowerDashboard', meta: { title: '经营看板', isAffix: true }, ...extra });

test('relative pinned dashboard and visited absolute route become one canonical pinned tab', () => {
  const original = [dashboard('dashboard', { meta: { title: '旧看板', isAffix: false }, fullPath: 'dashboard?period=week', query: { period: 'week' } }), dashboard('/flower/dashboard')];
  const { state, context, saved } = fixture([]);
  context.setTagNavList(original);
  assert.deepEqual(Array.from(state.value, ({ path }) => path), ['/flower/dashboard']);
  assert.equal(state.value[0].meta.isAffix, true);
  assert.equal(state.value[0].meta.title, '经营看板');
  assert.equal(state.value[0].fullPath, '/flower/dashboard?period=week');
  assert.equal(state.value[0].name, 'FlowerDashboard');
  assert.deepEqual(saved().map(({ path }) => path), ['/flower/dashboard']);
  assert.equal(original[0].path, 'dashboard');
});

test('dashboard redirect aliases collapse while unrelated tabs and navigation state remain intact', () => {
  const orders = { path: '/flower/orders', name: 'FlowerOrders', query: { stage: 'packing' }, meta: { title: '花店订单' } };
  const unrelated = { path: '/reports', name: 'Reports', meta: { title: '经营看板', isAffix: true } };
  const { state, context } = fixture([]);
  context.setTagNavList([
    { path: '/', name: 'Dashboard', meta: { title: '经营看板' } },
    orders,
    { path: '/flower', name: 'FlowerShop', meta: { title: '花店经营' } },
    dashboard('/flower/dashboard'),
    unrelated,
  ]);
  assert.deepEqual(Array.from(state.value, ({ path }) => path), ['/flower/dashboard', '/flower/orders', '/reports']);
  assert.equal(state.value[0].meta.isAffix, true);
  assert.equal(state.value[1], orders);
  assert.equal(state.value[2], unrelated);
});

test('mount repairs already populated cached dashboard tabs without clearing other open tabs', () => {
  const orders = { path: '/flower/orders', name: 'FlowerOrders', meta: { title: '花店订单' } };
  const { state, context, saved } = fixture([dashboard('dashboard'), orders, dashboard('/flower/dashboard')]);
  Object.assign(context, {
    menuStore: { get tagNavList() { return state.value; }, setTagNavList: context.setTagNavList },
    tagsViewRef: { value: null },
    scrollbarRef: { value: null },
    window: { addEventListener() {} },
  });
  vm.runInContext(`(${componentSource.slice(mountCallback.start, mountCallback.end)})()`, context);
  assert.deepEqual(Array.from(state.value, ({ path }) => path), ['/flower/dashboard', '/flower/orders']);
  assert.deepEqual(saved().map(({ path }) => path), ['/flower/dashboard', '/flower/orders']);
});

test('adding a dashboard visit with different query values keeps a single dashboard tab', () => {
  const { state, context, saved } = fixture([dashboard('dashboard')]);
  context.addTag({ route: dashboard('/flower/dashboard', { query: { period: 'week' } }), type: 'push' });
  assert.deepEqual(Array.from(state.value, ({ path }) => path), ['/flower/dashboard']);
  assert.deepEqual(saved().map(({ path }) => path), ['/flower/dashboard']);
});
