<template>
  <view class="flower-slot-picker">
    <scroll-view v-if="dateGroups.length" class="flower-slot-picker__dates" scroll-x show-scrollbar="false">
      <view class="flower-slot-picker__date-row">
        <button
          v-for="group in dateGroups"
          :key="group.date"
          class="flower-slot-picker__date"
          :class="{ 'is-active': selectedDate === group.date }"
          @click="selectDate(group.date)"
        >
          <text class="flower-slot-picker__weekday">{{ weekday(group.date) }}</text>
          <text class="flower-slot-picker__day">{{ shortDate(group.date) }}</text>
        </button>
      </view>
    </scroll-view>

    <view v-if="visibleSlots.length" class="flower-slot-picker__slots">
      <button
        v-for="slot in visibleSlots"
        :key="slot.id"
        class="flower-slot-picker__slot"
        :class="{ 'is-active': String(modelValue) === String(slot.id), 'is-disabled': !isAvailable(slot) }"
        :disabled="!isAvailable(slot)"
        @click="selectSlot(slot)"
      >
        <text class="flower-slot-picker__time">{{ timeRange(slot) }}</text>
        <text class="flower-slot-picker__remaining">{{ remainingText(slot) }}</text>
      </button>
    </view>

    <view v-else class="flower-slot-picker__empty">当前日期暂无可预约时段</view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue';

const props = defineProps({
  slots: { type: Array, default: () => [] },
  modelValue: { type: [Number, String], default: null },
});
const emit = defineEmits(['update:modelValue', 'change']);
const selectedDate = ref('');

function dateKey(value) {
  if (!value) return '';
  if (typeof value === 'string') return value.slice(0, 10);
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const dateGroups = computed(() => {
  const groups = new Map();
  props.slots.forEach(slot => {
    const date = dateKey(slot.deliveryDate);
    if (!date) return;
    if (!groups.has(date)) groups.set(date, []);
    groups.get(date).push(slot);
  });
  return [...groups.entries()].map(([date, slots]) => ({ date, slots }));
});

const visibleSlots = computed(() => dateGroups.value.find(group => group.date === selectedDate.value)?.slots || []);

watch(dateGroups, groups => {
  const selectedSlot = props.slots.find(slot => String(slot.id) === String(props.modelValue));
  const preferred = selectedSlot ? dateKey(selectedSlot.deliveryDate) : selectedDate.value;
  selectedDate.value = groups.some(group => group.date === preferred) ? preferred : (groups[0]?.date || '');
}, { immediate: true });

function selectDate(date) {
  selectedDate.value = date;
}

function isAvailable(slot) {
  return slot.enabled !== false && Number(slot.bookedCount || 0) < Number(slot.capacity || 0);
}

function selectSlot(slot) {
  if (!isAvailable(slot)) return;
  emit('update:modelValue', slot.id);
  emit('change', { slotId: slot.id, deliveryDate: dateKey(slot.deliveryDate), slot });
}

function parseTime(value) {
  if (!value) return '--:--';
  const stringValue = String(value);
  const match = stringValue.match(/(\d{2}):(\d{2})/);
  return match ? `${match[1]}:${match[2]}` : stringValue.slice(0, 5);
}

function timeRange(slot) {
  return `${parseTime(slot.startTime)}–${parseTime(slot.endTime)}`;
}

function remainingText(slot) {
  const remaining = Math.max(0, Number(slot.capacity || 0) - Number(slot.bookedCount || 0));
  return remaining > 0 ? `剩余 ${remaining} 单` : '已约满';
}

function shortDate(date) {
  return `${Number(date.slice(5, 7))}月${Number(date.slice(8, 10))}日`;
}

function weekday(date) {
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  return labels[new Date(`${date}T00:00:00`).getDay()];
}
</script>

<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';

.flower-slot-picker__dates { width: 100%; }
.flower-slot-picker__date-row { display: inline-flex; gap: 12rpx; padding: 4rpx 0 18rpx; }
.flower-slot-picker__date,
.flower-slot-picker__slot { margin: 0; padding: 0; background: transparent; border: 1rpx solid rgba($flower-primary, .18); border-radius: 10rpx; }
.flower-slot-picker__date::after,
.flower-slot-picker__slot::after { border: 0; }
.flower-slot-picker__date { display: flex; align-items: center; flex-direction: column; min-width: 132rpx; padding: 18rpx 12rpx; color: $flower-text; }
.flower-slot-picker__date.is-active { color: #fff; background: $flower-primary; border-color: $flower-primary; }
.flower-slot-picker__weekday { font-size: 22rpx; }
.flower-slot-picker__day { margin-top: 5rpx; font-family: $flower-serif-cn; font-size: 26rpx; }
.flower-slot-picker__slots { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14rpx; }
.flower-slot-picker__slot { display: flex; align-items: flex-start; flex-direction: column; padding: 20rpx; color: $flower-text; line-height: 1.3; }
.flower-slot-picker__slot.is-active { color: $flower-primary; background: rgba($flower-primary, .08); border-color: $flower-primary; }
.flower-slot-picker__slot.is-disabled { opacity: .45; }
.flower-slot-picker__time { font-family: Georgia, 'Times New Roman', serif; font-size: 26rpx; }
.flower-slot-picker__remaining { margin-top: 8rpx; font-size: 20rpx; color: rgba($flower-text, .58); }
.flower-slot-picker__empty { padding: 28rpx 18rpx; border-radius: 10rpx; background: $flower-background; font-size: 24rpx; text-align: center; color: rgba($flower-text, .56); }
</style>
