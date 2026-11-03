import test from 'node:test';
import assert from 'node:assert/strict';
import { access } from 'node:fs/promises';
import { normalizeFlowerImages } from '../utils/flower-images.js';

test('local backend flower images use the matching bundled photos across response fields', async () => {
  const response = { list: [{ image: 'http://localhost:8080/crmebimage/public/flower/product-01.jpg',
    sliderImage: '["http://127.0.0.1:8080/crmebimage/public/flower/product-12.jpg"]',
    sku: { image: 'crmebimage/public/flower/product-04.jpg' } }],
    logo: 'http://localhost:8080/crmebimage/public/flower/logo.png' };
  const result = normalizeFlowerImages(response);
  assert.equal(result.list[0].image, '/static/images/flower-products/product-01.jpg');
  assert.deepEqual(JSON.parse(result.list[0].sliderImage), ['/static/images/flower-products/product-12.jpg']);
  assert.equal(result.list[0].sku.image, '/static/images/flower-products/product-04.jpg');
  assert.equal(result.logo, '/static/images/huajianji/logo.png');
  for (const path of [result.list[0].image, ...JSON.parse(result.list[0].sliderImage), result.list[0].sku.image, result.logo]) {
    await access(new URL(`..${path}`, import.meta.url));
  }
});

test('remote uploads, unknown files and other response data retain their original values', () => {
  for (const value of ['https://cdn.example.com/crmebimage/public/flower/product-01.jpg',
    'http://images.example.com/rose.jpg', '/static/images/flower-products/product-02.jpg',
    'http://localhost:8080/crmebimage/public/flower/product-99.jpg',
    'http://localhost:8080/uploads/custom.jpg', '[invalid', '', null, 0, false]) {
    assert.equal(normalizeFlowerImages(value), value);
  }
});
