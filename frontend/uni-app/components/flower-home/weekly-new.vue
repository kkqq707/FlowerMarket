<template><view class="weekly"><flower-section-title title="每周上新" align="left" /><view class="weekly__grid"><flower-product-card v-for="item in products.slice(0, 2)" :key="item.id" :product="item" @product-click="$emit('select', $event)" @add-cart="$emit('add-cart', $event)" /></view></view></template>
<script setup>
import FlowerSectionTitle from '@/components/flower-section-title/index.vue';
import FlowerProductCard from '@/components/flower-product-card/index.vue';
defineProps({ products: { type: Array, default: () => [] } });
defineEmits(['select', 'add-cart']);
</script>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.weekly { padding:8rpx 0 52rpx; background:$flower-paper; }
.weekly__grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:20rpx; padding:0 32rpx; }
</style>
