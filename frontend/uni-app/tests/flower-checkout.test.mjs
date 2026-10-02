import test from 'node:test';
import assert from 'node:assert/strict';

import {
  FLOWER_QUALITY_REASONS,
  FLOWER_STAGE_DEFINITIONS,
  buildFlowerOrderPayload,
  clearUnavailableFlowerSlot,
  flowerAfterSaleState,
  flowerStageLabel,
  validateFlowerCheckout,
} from '../utils/flower-checkout.js';

const validDelivery = {
  senderName: '林夏',
  senderPhone: '13800138000',
  recipientName: '苏禾',
  recipientPhone: '13900139000',
  deliveryZoneId: 2,
  deliveryDate: '2026-09-23',
  slotId: 18,
  greetingCard: '愿花香陪你度过明亮的一天。',
  anonymous: false,
  contactRecipient: true,
  substitutionPolicy: 'CONTACT_FIRST',
};

test('delivery checkout requires sender, recipient, address zone, date and slot', () => {
  const errors = validateFlowerCheckout({}, { shippingType: 1 });

  assert.deepEqual(errors, {
    senderName: '请填写订花人姓名',
    senderPhone: '请填写订花人手机号',
    recipientName: '请填写收花人姓名',
    recipientPhone: '请填写收花人手机号',
    deliveryZoneId: '请选择配送区域',
    deliveryDate: '请选择配送日期',
    slotId: '请选择配送时段',
  });
});

test('delivery checkout rejects malformed phones, oversized cards and unknown substitution policies', () => {
  const errors = validateFlowerCheckout({
    ...validDelivery,
    senderPhone: '123',
    recipientPhone: '1880000',
    greetingCard: '花'.repeat(201),
    substitutionPolicy: 'RANDOM_REPLACEMENT',
  }, { shippingType: 1 });

  assert.equal(errors.senderPhone, '请输入正确的订花人手机号');
  assert.equal(errors.recipientPhone, '请输入正确的收花人手机号');
  assert.equal(errors.greetingCard, '贺卡内容不能超过200字');
  assert.equal(errors.substitutionPolicy, '请选择花材缺货处理方式');
});

test('store pickup only requires the ordering contact and pickup date', () => {
  const pickup = {
    senderName: '林夏',
    senderPhone: '13800138000',
    deliveryDate: '2026-09-23',
    substitutionPolicy: 'NO_SUBSTITUTION',
  };

  assert.deepEqual(validateFlowerCheckout(pickup, { shippingType: 2 }), {});
  assert.equal(buildFlowerOrderPayload(pickup, { shippingType: 2 }).slotId, null);
  assert.equal(buildFlowerOrderPayload(pickup, { shippingType: 2 }).recipientName, '');
});

test('a selected slot is cleared when it disappears, closes, or reaches capacity', () => {
  const base = { deliveryDate: '2026-09-23', slotId: 18 };
  const available = [{ id: 18, deliveryDate: '2026-09-23', enabled: true, capacity: 8, bookedCount: 3 }];

  assert.deepEqual(clearUnavailableFlowerSlot(base, available), base);
  assert.deepEqual(clearUnavailableFlowerSlot(base, []), { deliveryDate: '', slotId: null });
  assert.deepEqual(clearUnavailableFlowerSlot(base, [{ ...available[0], bookedCount: 8 }]), { deliveryDate: '', slotId: null });
  assert.deepEqual(clearUnavailableFlowerSlot(base, [{ ...available[0], enabled: false }]), { deliveryDate: '', slotId: null });
});

test('flower progress exposes the seven user-facing stages in fixed order', () => {
  assert.equal(FLOWER_STAGE_DEFINITIONS.length, 7);
  assert.deepEqual(FLOWER_STAGE_DEFINITIONS.map(item => item.key), [
    'WAIT_PAY', 'WAIT_ACCEPT', 'WAIT_PRODUCTION', 'PRODUCING',
    'WAIT_DELIVERY', 'DELIVERING', 'COMPLETED',
  ]);
  assert.equal(flowerStageLabel('PRODUCING'), '制作中');
  assert.equal(flowerStageLabel('UNKNOWN'), '订单处理中');
});

test('quality after-sale stays open for two hours after delivery and uses four fixed reasons', () => {
  const completedAt = '2026-09-21T10:00:00+08:00';

  assert.deepEqual(FLOWER_QUALITY_REASONS, ['不新鲜', '花材不符', '包装破损', '配送超时']);
  assert.deepEqual(flowerAfterSaleState({
    stage: 'COMPLETED',
    completedAt,
    now: '2026-09-21T11:59:59+08:00',
  }), { visible: true, contactOnly: false });
  assert.deepEqual(flowerAfterSaleState({
    stage: 'COMPLETED',
    completedAt,
    now: '2026-09-21T12:00:01+08:00',
  }), { visible: true, contactOnly: true });
  assert.deepEqual(flowerAfterSaleState({ stage: 'PRODUCING', completedAt: null }), {
    visible: false,
    contactOnly: true,
  });
});

