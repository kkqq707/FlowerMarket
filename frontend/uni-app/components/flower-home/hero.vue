<template>
  <view class="hero">
    <view class="hero__masthead" :style="{ paddingTop: statusBarHeight + 'px' }"><text>{{ brand || '花间集' }}</text></view>
    <view class="hero__masthead-space" :style="{ height: 'calc(' + statusBarHeight + 'px + 88rpx)' }"></view>
    <image class="hero__image" :src="source" mode="aspectFill" alt="当季花艺" @error="source = fallback" />
    <view class="hero__copy"><text class="hero__title">让心意，自然盛放</text><text class="hero__note">一束鲜花，一段值得珍藏的日常。</text><button @click="$emit('browse')">开启今日花事 ↗</button></view>
  </view>
</template>
<script setup>
import { ref, watch } from 'vue';
import flowerView from '@/utils/flower-product-view.js';
const statusBarHeight = typeof uni.getWindowInfo === 'function' ? Number(uni.getWindowInfo().statusBarHeight || 0) : 0;
const props = defineProps({ brand: String, image: String });
defineEmits(['browse']);
const fallback = flowerView.FLOWER_IMAGE_FALLBACK;
const source = ref(fallback);
watch(() => props.image, value => { source.value = value || fallback; }, { immediate: true });
</script>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.hero { background:$flower-deep; color:$flower-paper; }
.hero__masthead { position:fixed; z-index:100; top:0; left:0; right:0; display:flex; justify-content:space-between; align-items:center; height:88rpx; box-sizing:content-box; padding-left:32rpx; padding-right:32rpx; background:$flower-deep; font-family:$flower-serif-cn; font-size:32rpx; letter-spacing:2rpx; }
.hero__masthead-space { background:$flower-deep; }
.hero__edition { font:16rpx $flower-serif-en; letter-spacing:2rpx; }
/* The WeChat capsule owns the right side of the fixed title row. */
/* #ifdef MP-WEIXIN */
.hero__edition { display:none; }
/* #endif */
.hero__image { display:block; width:100%; height:520rpx; }
.hero__copy { padding:34rpx 32rpx 44rpx; }
.hero__title { display:block; font:46rpx/1.35 $flower-serif-cn; letter-spacing:1rpx; }
.hero__note { display:block; margin-top:12rpx; font-size:26rpx; line-height:1.5; color:#e5e9e2; }
button { min-width:300rpx; min-height:76rpx; margin:28rpx 0 0; padding:0 24rpx; color:$flower-paper; background:transparent; border:1rpx solid rgba($flower-paper,.7); border-radius:0; line-height:76rpx; font-size:26rpx; }
button::after { border:0; } button:focus-visible { outline:2px solid $flower-paper; outline-offset:4px; }
</style>
