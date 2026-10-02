<template><view class="feature" :class="{ 'feature--compact': compact }"><flower-section-title :title="title" :subtitle="subtitle" align="left" /><view class="feature__image-box"><image :src="source" mode="aspectFill" :alt="title" @error="source = fallback" /></view><view class="feature__caption"><text class="feature__name">{{ caption }}</text><text class="feature__note">{{ note }}</text><button @click="$emit('select')">{{ action }} ↗</button></view></view></template>
<script setup>
import { ref, watch } from 'vue';import FlowerSectionTitle from '@/components/flower-section-title/index.vue';import flowerView from '@/utils/flower-product-view.js';
const props=defineProps({title:String,subtitle:String,image:String,caption:String,note:String,compact:{type:Boolean,default:false},action:{type:String,default:'看看这束花'}});defineEmits(['select']);const fallback=flowerView.FLOWER_IMAGE_FALLBACK;const source=ref(fallback);watch(()=>props.image,value=>{source.value=value||fallback;},{immediate:true});
</script>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.feature { padding:0 32rpx 24rpx; }
.feature__image-box { height:460rpx; background:$flower-paper; }
.feature--compact .feature__image-box { height:360rpx; }
.feature__image-box image { width:100%;height:100%; }
.feature__caption { padding:24rpx 0 30rpx; border-bottom:1rpx solid rgba($flower-primary,.18); }
.feature__name { display:block;font:36rpx/1.4 $flower-serif-cn;color:$flower-primary; }
.feature__note { display:block;font-size:26rpx;line-height:1.6;color:$flower-text;margin-top:10rpx; }
button { min-height:64rpx; padding:0; margin:14rpx 0 0; text-align:left; font-size:25rpx;color:$flower-primary;background:transparent;line-height:64rpx;border-radius:0; }
button::after { border:0; }
button:focus-visible { outline:2rpx solid $flower-primary; outline-offset:3rpx; }
</style>
