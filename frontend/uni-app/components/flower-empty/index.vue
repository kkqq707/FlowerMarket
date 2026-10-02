<template>
	<view class="flower-empty">
		<view class="flower-empty__mark" aria-hidden="true">✦</view>
		<text class="flower-empty__title">{{ title }}</text>
		<text v-if="description" class="flower-empty__description">{{ description }}</text>
	</view>
</template>

<script setup>
defineProps({
	title: { type: String, default: '暂无花礼' },
	description: { type: String, default: '换个分类看看吧' },
});
</script>

<style lang="scss" scoped>
@import '@/static/css/flower-tokens.scss';

.flower-empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 14rpx;
	padding: 88rpx 32rpx;
	color: $flower-text;
	text-align: center;
	&__mark { font-family: $flower-serif-en; font-size: 42rpx; color: $flower-primary; opacity: .72; }
	&__title { font-family: $flower-serif-cn; font-size: 30rpx; }
	&__description { font-size: 23rpx; color: rgba($flower-text, .58); }
}
</style>
