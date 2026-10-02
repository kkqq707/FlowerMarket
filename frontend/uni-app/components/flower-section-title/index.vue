<template>
	<view class="flower-section-title" :class="{ 'flower-section-title--left': align === 'left' }">
		<view class="flower-section-title__line" />
		<view class="flower-section-title__content">
			<text class="flower-section-title__cn">{{ title }}</text>
			<text v-if="subtitle" class="flower-section-title__en">{{ subtitle }}</text>
		</view>
		<view class="flower-section-title__line" />
	</view>
</template>

<script setup>
defineProps({
	title: { type: String, default: '花店推荐' },
	subtitle: { type: String, default: '' },
	align: { type: String, default: 'center' },
});
</script>

<style lang="scss" scoped>
@import '@/static/css/flower-tokens.scss';

.flower-section-title {
	display: flex;
	align-items: center;
	gap: 18rpx;
	padding: 50rpx 32rpx 26rpx;
	color: $flower-primary;

	&__line { width: 48rpx; height: 1rpx; background: rgba($flower-primary, .36); }
	&__content { flex: 1; min-width: 0; text-align: center; }
	&__cn, &__en { display: block; }
	&__cn { font-family: $flower-serif-cn; font-size: 38rpx; line-height: 1.35; letter-spacing: 1rpx; }
	&__en { margin-top: 8rpx; font-family: $flower-serif-en; font-size: 22rpx; letter-spacing: 2rpx; color: rgba($flower-text, .7); }
	&--left &__line { display: none; }
	&--left &__content { text-align: left; }
}
</style>
