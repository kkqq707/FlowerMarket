import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';
import { parse as parseVue } from '@vue/compiler-sfc';
import { parse } from '@babel/parser';

const source = readFileSync(new URL('../src/views/store/index.vue', import.meta.url), 'utf8');
const script = parseVue(source).descriptor.scriptSetup.content;
const handler = parse(script, { sourceType: 'module', plugins: ['jsx'] }).program.body
  .find((node) => node.type === 'FunctionDeclaration' && node.id.name === 'handleDelete');

function fixture(confirm, remove, type = '1') {
  const calls = { removed: [], messages: [], lists: 0, headers: 0 };
  const tableFrom = { type, page: 2 };
  const context = {
    proxy: { $modalSure: confirm },
    tableFrom,
    tableData: { data: [{ id: 900001 }] },
    productDeleteApi: async (...args) => { calls.removed.push(args); return remove(); },
    ElMessage: { success: (message) => calls.messages.push(message) },
    getList: () => calls.lists++,
    goodHeade: () => calls.headers++,
  };
  const handleDelete = vm.runInNewContext(`(${script.slice(handler.start, handler.end)})`, context);
  return { handleDelete, calls, tableFrom };
}

for (const action of ['cancel', 'close']) {
  test(`dismiss recycle confirmation (${action}) without errors or deleting a product`, async () => {
    const { handleDelete, calls } = fixture(() => Promise.reject(action), () => undefined);
    await handleDelete(900001, '1');
    await new Promise(setImmediate);
    assert.deepEqual(calls.removed, []);
    assert.equal(calls.lists, 0);
  });
}

test('a failed recycle request does not show success or refresh the list', async () => {
  const { handleDelete, calls } = fixture(() => Promise.resolve(), () => Promise.reject(new Error('request failed')));
  await handleDelete(900001, '1');
  await new Promise(setImmediate);
  assert.equal(calls.messages.length, 0);
  assert.equal(calls.lists, 0);
});

for (const [type, flag] of [['1', 'recycle'], ['5', 'delete']]) {
  test(`confirmed ${flag} refreshes the product list and counters`, async () => {
    const { handleDelete, calls, tableFrom } = fixture(() => Promise.resolve(), () => undefined, type);
    await handleDelete(900001, type);
    await new Promise(setImmediate);
    assert.deepEqual(calls.removed, [[900001, flag]]);
    assert.equal(calls.messages.length, 1);
    assert.equal(tableFrom.page, 1);
    assert.equal(calls.lists, 1);
    assert.equal(calls.headers, 1);
  });
}
