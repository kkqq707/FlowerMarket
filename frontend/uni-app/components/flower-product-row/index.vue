<template>
	<view class="flower-product-row">
		<view class="flower-product-row__image-box" role="button" tabindex="0" @click="emitProductClick" @keyup.enter="emitProductClick" @keyup.space.prevent="emitProductClick">
			<image class="flower-product-row__image" :src="imageSource" :alt="view.image.alt" mode="aspectFill" @error="useFallback" />
		</view>
		<view class="flower-product-row__content">
			<view class="flower-product-row__details" role="button" tabindex="0" @click="emitProductClick" @keyup.enter="emitProductClick" @keyup.space.prevent="emitProductClick">
				<text class="flower-product-row__name">{{ view.title }}</text>
				<text v-if="view.materials" class="flower-product-row__materials">{{ view.materials }}</text>
			</view>
			<view class="flower-product-row__bottom">
				<view>
					<view class="flower-product-row__price" :aria-label="view.priceParts.text">
						<text class="flower-product-row__price-symbol">{{ view.priceParts.symbol }}</text>
						<text class="flower-product-row__price-integer">{{ view.priceParts.integer }}</text>
						<text class="flower-product-row__price-decimal">{{ view.priceParts.decimal }}</text>
					</view>
					<text class="flower-product-row__sales">{{ view.salesText }}</text>
				</view>
				<button class="flower-product-row__cart" type="default" aria-label="加入购物车" @click="emitAddCart">加入购物车</button>
			</view>
		</view>
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

.flower-product-row {
	display: flex;
	gap: 24rpx;
	padding: 24rpx 0;
	color: $flower-text;
	border-bottom: 1rpx solid rgba($flower-primary, .12);
	&__image-box { flex: 0 0 214rpx; height: 214rpx; overflow: hidden; background: $flower-background; outline: none; }
	&__image-box:focus-visible, &__details:focus-visible { outline: 2rpx solid $flower-primary; outline-offset: 4rpx; }
	&__image { width: 100%; height: 100%; }
	&__content { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: space-between; outline: none; }
	&__details { outline: none; }
	&__name { display: -webkit-box; overflow: hidden; font-size: 30rpx; font-weight: 500; line-height: 1.4; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
	&__materials, &__english, &__sales { margin-top: 8rpx; font-size: 21rpx; color: rgba($flower-text, .6); }
	&__materials { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
	&__english { font-family: $flower-serif-en; font-size: 18rpx; letter-spacing: 1.5rpx; }
	&__bottom { display: flex; align-items: flex-end; justify-content: space-between; gap: 16rpx; margin-top: 18rpx; }
	&__sales { display: block; }
	&__price { display: inline-flex; align-items: baseline; font-family: Arial, 'Helvetica Neue', sans-serif; font-variant-numeric: tabular-nums; color: $flower-accent; }
	&__price-symbol, &__price-decimal { font-size: 22rpx; font-weight: 600; }
	&__price-integer { font-size: 38rpx; font-weight: 700; line-height: 1; }
	&__cart { margin: 0; padding: 0 18rpx; font-size: 20rpx; line-height: 52rpx; color: $flower-paper; background: $flower-primary; border: 0; border-radius: 0; }
	&__cart::after { border: 0; }
	&__cart:focus-visible { outline: 2rpx solid $flower-accent; outline-offset: 4rpx; }
}
</style>
