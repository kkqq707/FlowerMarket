import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { createSSRApp } from 'vue';
import { renderToString } from '@vue/server-renderer';

async function renderDeliveryMetric(onTimeRate) {
  const source = await readFile(new URL('../src/views/flower/dashboard/index.vue', import.meta.url), 'utf8');
  const template = source.match(/<section class="metrics">[\s\S]*?<\/section>/)[0];
  const html = await renderToString(createSSRApp({
    template,
    setup: () => ({ data: { onTimeRate }, money: value => Number(value || 0).toFixed(2) }),
  }));
  return html.match(/<article>[^]*?准时送达率[^]*?<\/article>/)[0].split('准时送达率')[1].replace(/<[^>]*>/g, '');
}

test('missing delivery metrics show an empty state without a percent sign', async () => {
  for (const value of [null, undefined]) {
    const text = await renderDeliveryMetric(value);
    assert.ok(text.includes('暂无数据'));
    assert.ok(text.includes('暂无可统计的送达记录'));
    assert.ok(!text.includes('%'));
  }
});

test('actual zero and positive delivery rates retain their percentages', async () => {
  for (const value of [0, 50, 100]) {
    const text = await renderDeliveryMetric(value);
    assert.ok(text.includes(`${value.toFixed(2)}%`));
    assert.ok(!text.includes('暂无数据'));
  }
});
