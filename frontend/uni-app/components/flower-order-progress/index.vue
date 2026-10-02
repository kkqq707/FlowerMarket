<template>
  <view class="flower-progress">
    <view
      v-for="(stage, index) in FLOWER_STAGE_DEFINITIONS"
      :key="stage.key"
      class="flower-progress__item"
      :class="{ 'is-done': index <= currentIndex, 'is-current': index === currentIndex }"
    >
      <view class="flower-progress__rail">
        <view class="flower-progress__bud"></view>
        <view v-if="index < FLOWER_STAGE_DEFINITIONS.length - 1" class="flower-progress__stem"></view>
      </view>
      <view class="flower-progress__content">
        <view class="flower-progress__head">
          <text class="flower-progress__label">{{ stage.label }}</text>
          <text v-if="progressFor(stage.key)?.createTime" class="flower-progress__time">
            {{ formatTime(progressFor(stage.key).createTime) }}
          </text>
        </view>
        <text class="flower-progress__description">
          {{ progressFor(stage.key)?.remark || stage.description }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue';
import { FLOWER_STAGE_DEFINITIONS, flowerStageIndex } from '@/utils/flower-checkout.js';

const props = defineProps({
  currentStage: { type: String, default: 'WAIT_PAY' },
  progressList: { type: Array, default: () => [] },
});

const currentIndex = computed(() => Math.max(0, flowerStageIndex(props.currentStage)));
function progressFor(stage) { return props.progressList.find(item => item.stage === stage); }
function formatTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = number => String(number).padStart(2, '0');
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
</script>

<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';

.flower-progress__item { display: grid; grid-template-columns: 34rpx minmax(0, 1fr); gap: 20rpx; min-height: 112rpx; }
.flower-progress__rail { display: flex; align-items: center; flex-direction: column; }
.flower-progress__bud { width: 18rpx; height: 18rpx; margin-top: 7rpx; background: #d9ddd5; border: 5rpx solid $flower-paper; border-radius: 50%; box-shadow: 0 0 0 1rpx rgba($flower-primary, .18); }
.flower-progress__stem { flex: 1; width: 2rpx; margin: 6rpx 0 -7rpx; background: #d9ddd5; }
.flower-progress__item.is-done .flower-progress__bud,
.flower-progress__item.is-done .flower-progress__stem { background: $flower-primary; }
.flower-progress__item.is-current .flower-progress__bud { background: $flower-accent; box-shadow: 0 0 0 6rpx rgba($flower-accent, .12); }
.flower-progress__content { padding-bottom: 30rpx; }
.flower-progress__head { display: flex; align-items: baseline; justify-content: space-between; gap: 20rpx; }
.flower-progress__label { font-family: $flower-serif-cn; font-size: 30rpx; color: rgba($flower-text, .42); }
.flower-progress__item.is-done .flower-progress__label { color: $flower-text; }
.flower-progress__time { flex: none; font-family: Georgia, 'Times New Roman', serif; font-size: 20rpx; color: rgba($flower-text, .46); }
.flower-progress__description { display: block; margin-top: 9rpx; font-size: 23rpx; line-height: 1.55; color: rgba($flower-text, .58); }
</style>
