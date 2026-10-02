<template><view class="service"><text class="service__title">用心，抵达每一份心意</text><view class="service__grid"><view><text>当季花材</text><text class="service__note">随季节，自然生长</text></view><view><text>手作花礼</text><text class="service__note">留住花朵的姿态</text></view><view><text>同城配送</text><text class="service__note">可选时段以下单为准</text></view></view></view></template>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.service { padding:50rpx 32rpx 56rpx; background:$flower-deep; color:$flower-paper; }
.service__title { display:block; font:36rpx/1.4 $flower-serif-cn; }
.service__grid { display:grid; grid-template-columns:1fr; margin-top:24rpx; }
.service__grid>view { display:flex; align-items:baseline; justify-content:space-between; gap:20rpx; padding:18rpx 0; border-top:1rpx solid rgba($flower-paper,.24); font-size:27rpx; }
.service__note { font-size:24rpx; color:#e5e9e2; line-height:1.4; text-align:right; }
</style>
