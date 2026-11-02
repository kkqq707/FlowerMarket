import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';
import { parse as parseVue } from '@vue/compiler-sfc';
import { parse } from '@babel/parser';

const source = readFileSync(new URL('../src/views/store/index.vue', import.meta.url), 'utf8');
const script = parseVue(source).descriptor.scriptSetup.content;
const functions = parse(script, { sourceType: 'module', plugins: ['jsx'] }).program.body
  .filter((node) => node.type === 'FunctionDeclaration')
  .map((node) => script.slice(node.start, node.end)).join('\n');

function fixture() {
  const requests = [];
  const errors = [];
  const tableData = { data: [{ id: 900001 }], total: 36 };
  const tableFrom = { page: 1, limit: 20, keywords: '花束', cateId: 9101,
    type: '1', occasion: '生日', recipient: '朋友', colorSeries: '粉色' };
  const context = vm.createContext({
    tableData, tableFrom, listLoading: { value: false },
    occasionOptions: { value: [] }, recipientOptions: { value: [] }, colorOptions: { value: [] },
    getFlowerFilterOptions: async () => ({}),
    getFlowerProductList: (query) => new Promise((resolve, reject) => {
      requests.push({ query: JSON.parse(JSON.stringify(query)), resolve, reject });
    }),
    ElMessage: { error: (message) => errors.push(message) },
  });
  vm.runInContext(`let productRequestVersion = 0; ${functions}`, context);
  return { context, requests, errors, tableData, tableFrom };
}

test('next-page loading preserves total so pagination does not reset the selected page', async () => {
  const { context, requests, tableData, tableFrom } = fixture();
  context.pageChange(2);
  assert.equal(tableData.total, 36);
  assert.equal(tableFrom.page, 2);
  assert.deepEqual(requests[0].query, { page: 2, limit: 20, keyword: '花束', cid: 9101,
    type: 1, occasion: '生日', recipient: '朋友', colorSeries: '粉色' });
  const products = [{ id: 900021 }, { id: 900036 }];
  requests[0].resolve({ list: products, total: 36 });
  await new Promise(setImmediate);
  assert.deepEqual(tableData.data, products);
  assert.equal(tableData.total, 36);
  assert.equal(tableFrom.page, 2);
  assert.equal(context.listLoading.value, false);
});

test('changing page size requests the first page with the new size', async () => {
  const { context, requests, tableFrom } = fixture();
  tableFrom.page = 2;
  context.handleSizeChange(40);
  assert.equal(requests[0].query.page, 1);
  assert.equal(requests[0].query.limit, 40);
  assert.equal(tableFrom.page, 1);
  requests[0].resolve({ list: [], total: 36 });
  await new Promise(setImmediate);
});

test('a late earlier page response cannot overwrite the latest page', async () => {
  const { context, requests, tableData, tableFrom } = fixture();
  context.pageChange(2);
  context.pageChange(1);
  requests[1].resolve({ list: [{ id: 900001 }], total: 36 });
  await new Promise(setImmediate);
  requests[0].resolve({ list: [{ id: 900021 }], total: 36 });
  await new Promise(setImmediate);
  assert.equal(tableData.data[0].id, 900001);
  assert.equal(tableFrom.page, 1);
  assert.equal(tableData.total, 36);
});

test('failed pagination retains the page controls and reports the error', async () => {
  const { context, requests, tableData, errors } = fixture();
  context.pageChange(2);
  requests[0].reject(new Error('connection failed'));
  await new Promise(setImmediate);
  assert.equal(tableData.total, 36);
  assert.equal(context.listLoading.value, false);
  assert.deepEqual(errors, ['connection failed']);
});
