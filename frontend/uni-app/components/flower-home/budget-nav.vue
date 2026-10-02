<template><view class="budget"><text class="budget__title">心意，不止一种选择</text><view class="budget__grid"><button v-for="(item, index) in labels" :key="item" @click="$emit('select', index + 1)"><text class="budget__range">{{ item }}</text><text class="budget__action">选花</text></button></view></view></template>
<script setup>defineEmits(['select']);const labels = ['200元以内','200–300元','300–500元','500元以上'];</script>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.budget { background:$flower-primary; color:$flower-paper; padding:42rpx 32rpx 48rpx; }
.budget__title { display:block; margin-bottom:24rpx; font:38rpx/1.35 $flower-serif-cn; }
.budget__grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12rpx; }
button { display:flex; align-items:center; justify-content:space-between; width:100%; min-height:102rpx; margin:0; padding:14rpx 18rpx; text-align:left; border:1rpx solid rgba($flower-paper,.55); border-radius:0; background:transparent; color:$flower-paper; font-size:26rpx; line-height:1.35; }
.budget__range { min-width:0; font-size:26rpx; }
.budget__action { margin-left:8rpx; flex:0 0 auto; font-size:24rpx; color:#e5e9e2; }
button::after { border:0; }
button:focus-visible { outline:2rpx solid $flower-paper; outline-offset:3rpx; }
</style>
