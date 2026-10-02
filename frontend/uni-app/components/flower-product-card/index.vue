<template>
	<view class="flower-product-card">
		<view class="flower-product-card__details" role="button" tabindex="0" @click="emitProductClick" @keyup.enter="emitProductClick" @keyup.space.prevent="emitProductClick">
			<view class="flower-product-card__image-box">
				<image class="flower-product-card__image" :src="imageSource" :alt="view.image.alt" mode="aspectFill" @error="useFallback" />
			</view>
			<view class="flower-product-card__content">
				<text class="flower-product-card__name">{{ view.title }}</text>
				<view class="flower-product-card__meta">
					<text class="flower-product-card__sales">{{ view.salesText }}</text>
					<view class="flower-product-card__price" :aria-label="view.priceParts.text">
						<text class="flower-product-card__price-symbol">{{ view.priceParts.symbol }}</text>
						<text class="flower-product-card__price-integer">{{ view.priceParts.integer }}</text>
						<text class="flower-product-card__price-decimal">{{ view.priceParts.decimal }}</text>
					</view>
				</view>
			</view>
		</view>
		<button class="flower-product-card__cart" type="default" aria-label="加入购物车" @click="emitAddCart">加入购物车</button>
	</view>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import productView from '@/utils/flower-product-view.js';

const props = defineProps({ product: { type: Object, default: () => ({}) } });
const emit = defineEmits(['product-click', 'add-cart']);
const view = computed(() => productView.buildFlowerProductView(props.product));
const imageSource = ref('');

watch(view, (nextView) => { imageSource.value = nextView.image.src; }, { immediate: true });
function useFallback() { imageSource.value = productView.FLOWER_IMAGE_FALLBACK; }
function emitProductClick() { emit('product-click', props.product); }
function emitAddCart() { emit('add-cart', props.product); }
</script>

<style lang="scss" scoped>
@import '@/static/css/flower-tokens.scss';

.flower-product-card {
	display: flex;
	flex-direction: column;
	height: 100%;
	min-width: 0;
	color: $flower-text;
	outline: none;
	&__details { display: flex; flex: 1; flex-direction: column; outline: none; }
	&__details:focus-visible { outline: 2rpx solid $flower-primary; outline-offset: 6rpx; }
	&__image-box { width: 100%; aspect-ratio: 1 / 1; overflow: hidden; background: $flower-background; }
	&__image { width: 100%; height: 100%; }
	&__content { display: flex; flex: 1; flex-direction: column; padding: 18rpx 2rpx 8rpx; }
	&__name, &__sales { display: block; }
	&__name { display: -webkit-box; height: 82rpx; overflow: hidden; font-size: 29rpx; font-weight: 500; line-height: 41rpx; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
	&__meta { display: flex; align-items: baseline; justify-content: space-between; gap: 8rpx; margin-top: auto; padding-top: 10rpx; }
	&__sales { font-size: 23rpx; color: rgba($flower-text, .72); }
	&__price { display: inline-flex; align-items: baseline; flex: 0 0 auto; font-family: Arial, 'Helvetica Neue', sans-serif; font-variant-numeric: tabular-nums; color: $flower-accent; }
	&__price-symbol, &__price-decimal { font-size: 22rpx; font-weight: 600; }
	&__price-integer { font-size: 38rpx; font-weight: 700; line-height: 1; }
	&__cart { min-height: 80rpx; margin: 14rpx 0 0; padding: 0; font-size: 25rpx; line-height: 80rpx; color: $flower-primary; background: transparent; border: 1rpx solid rgba($flower-primary, .45); border-radius: 0; }
	&__cart::after { border: 0; }
	&__cart:focus-visible { outline: 2rpx solid $flower-accent; outline-offset: 4rpx; }
}
</style>
