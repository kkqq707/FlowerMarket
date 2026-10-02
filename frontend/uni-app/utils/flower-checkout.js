export const FLOWER_SUBSTITUTION_OPTIONS = [
  { value: 'ACCEPT_EQUIVALENT', label: '接受同价替换', description: '缺货时由花艺师选择同价同色系花材' },
  { value: 'CONTACT_FIRST', label: '替换前联系我', description: '花店先电话确认，再调整花材' },
  { value: 'NO_SUBSTITUTION', label: '不接受替换', description: '花材不足时联系改期或取消' },
];

export const FLOWER_STAGE_DEFINITIONS = [
  { key: 'WAIT_PAY', label: '待付款', description: '配送时段将为你保留15分钟' },
  { key: 'WAIT_ACCEPT', label: '待接单', description: '花店正在确认花材与配送安排' },
  { key: 'WAIT_PRODUCTION', label: '待制作', description: '订单已确认，等待花艺师制作' },
  { key: 'PRODUCING', label: '制作中', description: '花艺师正在为你制作花礼' },
  { key: 'WAIT_DELIVERY', label: '待配送', description: '花礼制作完成，等待配送' },
  { key: 'DELIVERING', label: '配送中', description: '花礼正在送往收花地址' },
  { key: 'COMPLETED', label: '已送达', description: '花礼已经送达，请确认花礼状态' },
];

export const FLOWER_QUALITY_REASONS = ['不新鲜', '花材不符', '包装破损', '配送超时'];

const PHONE_PATTERN = /^1[3-9]\d{9}$/;
const SUBSTITUTION_VALUES = new Set(FLOWER_SUBSTITUTION_OPTIONS.map(item => item.value));

function text(value) {
  return value == null ? '' : String(value).trim();
}

export function validateFlowerCheckout(form = {}, options = {}) {
  const shippingType = Number(options.shippingType || 1);
  const errors = {};
  const senderName = text(form.senderName);
  const senderPhone = text(form.senderPhone);

  if (!senderName) errors.senderName = '请填写订花人姓名';
  if (!senderPhone) errors.senderPhone = '请填写订花人手机号';
  else if (!PHONE_PATTERN.test(senderPhone)) errors.senderPhone = '请输入正确的订花人手机号';

  if (shippingType === 1) {
    const recipientName = text(form.recipientName);
    const recipientPhone = text(form.recipientPhone);
    if (!recipientName) errors.recipientName = '请填写收花人姓名';
    if (!recipientPhone) errors.recipientPhone = '请填写收花人手机号';
    else if (!PHONE_PATTERN.test(recipientPhone)) errors.recipientPhone = '请输入正确的收花人手机号';
    if (!form.deliveryZoneId) errors.deliveryZoneId = '请选择配送区域';
  }

  if (!text(form.deliveryDate)) errors.deliveryDate = shippingType === 1 ? '请选择配送日期' : '请选择取花日期';
  if (shippingType === 1 && !form.slotId) errors.slotId = '请选择配送时段';
  if (text(form.greetingCard).length > 200) errors.greetingCard = '贺卡内容不能超过200字';
  if (form.substitutionPolicy && !SUBSTITUTION_VALUES.has(form.substitutionPolicy)) {
    errors.substitutionPolicy = '请选择花材缺货处理方式';
  }

  return errors;
}

export function buildFlowerOrderPayload(form = {}, options = {}) {
  const shippingType = Number(options.shippingType || 1);
  const isDelivery = shippingType === 1;
  return {
    senderName: text(form.senderName),
    senderPhone: text(form.senderPhone),
    recipientName: isDelivery ? text(form.recipientName) : '',
    recipientPhone: isDelivery ? text(form.recipientPhone) : '',
    deliveryZoneId: isDelivery ? (form.deliveryZoneId || null) : null,
    deliveryDate: text(form.deliveryDate),
    slotId: isDelivery ? (form.slotId || null) : null,
    greetingCard: text(form.greetingCard),
    anonymous: Boolean(form.anonymous),
    contactRecipient: form.contactRecipient !== false,
    substitutionPolicy: form.substitutionPolicy || 'CONTACT_FIRST',
  };
}

export function clearUnavailableFlowerSlot(selection = {}, slots = []) {
  const selected = slots.find(slot => String(slot.id) === String(selection.slotId));
  const isAvailable = selected && selected.enabled !== false && Number(selected.bookedCount || 0) < Number(selected.capacity || 0);
  if (isAvailable) return { ...selection };
  return { ...selection, deliveryDate: '', slotId: null };
}

export function flowerStageLabel(stage) {
  return FLOWER_STAGE_DEFINITIONS.find(item => item.key === stage)?.label || '订单处理中';
}

export function flowerStageIndex(stage) {
  return FLOWER_STAGE_DEFINITIONS.findIndex(item => item.key === stage);
}

export function canDirectlyCancelFlowerOrder(stage) {
  return ['WAIT_PAY', 'WAIT_ACCEPT', 'WAIT_PRODUCTION'].includes(stage);
}

export function flowerAfterSaleState({ stage, completedAt, now = Date.now() } = {}) {
  if (stage !== 'COMPLETED' || !completedAt) return { visible: false, contactOnly: true };
  const completedTime = new Date(completedAt).getTime();
  const nowTime = new Date(now).getTime();
  const withinTwoHours = Number.isFinite(completedTime) && Number.isFinite(nowTime)
    && nowTime >= completedTime && nowTime - completedTime <= 2 * 60 * 60 * 1000;
  return { visible: true, contactOnly: !withinTwoHours };
}

export default {
  FLOWER_SUBSTITUTION_OPTIONS,
  FLOWER_STAGE_DEFINITIONS,
  FLOWER_QUALITY_REASONS,
  validateFlowerCheckout,
  buildFlowerOrderPayload,
  clearUnavailableFlowerSlot,
  flowerStageLabel,
  flowerStageIndex,
  canDirectlyCancelFlowerOrder,
  flowerAfterSaleState,
};
