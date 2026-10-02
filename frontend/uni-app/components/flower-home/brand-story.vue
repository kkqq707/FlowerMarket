<template><view class="story"><text class="story__title">把日常，交给花来点亮</text><view class="story__rule"/><text class="story__text">{{ story || '顺着季节的脚步，挑选当下的花材。让每一次相见、每一句祝福，都有一束恰好的花。' }}</text><text class="story__sign">{{ brand || '鲜花与生活' }} · 花艺手记</text></view></template>
<script setup>defineProps({ brand: String, story: String });</script>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.story { padding:52rpx 44rpx 64rpx; text-align:center; color:$flower-primary; }
.story__title { display:block; font:38rpx/1.45 $flower-serif-cn; }
.story__rule { width:52rpx; height:1rpx; background:rgba($flower-primary,.55); margin:24rpx auto; }
.story__text { display:block; font-size:27rpx; line-height:1.75; }
.story__sign { display:block; margin-top:24rpx; font:24rpx/1.4 $flower-serif-cn; }
</style>
