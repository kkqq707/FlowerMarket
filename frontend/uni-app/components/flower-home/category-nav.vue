<template>
  <view class="categories">
    <flower-section-title title="为此刻，选一束花" align="left" />
    <view class="categories__grid">
      <button v-for="item in items" :key="item.id || item.name" @click="$emit('select', item)">
        <text class="categories__name">{{ item.name }}</text>
        <text class="categories__action">去选花</text>
      </button>
    </view>
  </view>
</template>
<script setup>
import FlowerSectionTitle from '@/components/flower-section-title/index.vue';
defineProps({ items: { type: Array, default: () => [] } });
defineEmits(['select']);
</script>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.categories { padding-bottom:48rpx; }
.categories__grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12rpx; padding:0 32rpx; }
button { display:flex; flex-direction:column; justify-content:center; width:100%; min-height:136rpx; margin:0; padding:20rpx 24rpx; text-align:left; color:$flower-primary; background:$flower-paper; border:1rpx solid rgba($flower-primary,.14); border-radius:0; line-height:1.4; }
button::after { border:0; }
.categories__name { display:block; width:100%; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; font-family:$flower-serif-cn; font-size:30rpx; }
.categories__action { display:block; margin-top:10rpx; font-size:24rpx; color:rgba($flower-primary,.75); }
button:focus-visible { outline:2rpx solid $flower-primary; outline-offset:3rpx; }
</style>
