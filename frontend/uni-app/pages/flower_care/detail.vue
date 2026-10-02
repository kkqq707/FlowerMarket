<template>
  <view class="care-detail"><view class="care-nav" :style="{paddingTop:statusHeight+'px',height:navHeight+'px'}"><button class="back" aria-label="返回" @click="back">‹</button><text>养花指南</text></view><view :style="{height:(statusHeight+navHeight)+'px'}"></view>
    <view v-if="loading" class="state">正在打开养花指南…</view><view v-else-if="error" class="state"><text>{{ error }}</text><button @click="load">重新加载</button></view>
    <article v-else-if="article.id"><image class="hero" :src="cover(article.imageInput)" :alt="article.title" mode="aspectFill"/><view class="body"><text class="title">{{ article.title }}</text><text class="meta">{{ article.author || '花间集花艺师' }} · {{ article.createTime || '' }}</text><text class="summary">{{ article.synopsis }}</text><mp-html :content="article.content"/><view class="ending">愿这一束花，陪你更久一点。</view></view></article>
  </view>
</template>

<script setup>
import { ref } from 'vue';import { onLoad } from '@dcloudio/uni-app';
import { HTTP_REQUEST_URL } from '@/config/app.js';import { getFlowerCareArticle } from '@/api/flower.js';
const info=uni.getSystemInfoSync();const statusHeight=info.statusBarHeight||0,navHeight=44;
const article=ref({}),loading=ref(true),error=ref(''),id=ref(null);
function cover(path){if(!path)return '';return /^https?:\/\//.test(path)?path:HTTP_REQUEST_URL+'/'+String(path).replace(/^\//,'');}
function back(){uni.navigateBack();}
async function load(){loading.value=true;error.value='';try{const result=await getFlowerCareArticle(id.value);article.value=result?.data||{};if(!article.value.id)error.value='这篇指南暂时无法查看。';}catch{error.value='这篇指南暂时无法查看，请稍后重试。';}finally{loading.value=false;}}
onLoad(options=>{id.value=options.id;load();});
</script>

<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.care-detail{min-height:100vh;background:$flower-background;color:$flower-text}.care-nav{position:fixed;z-index:20;top:0;left:0;right:0;box-sizing:content-box;display:flex;align-items:center;justify-content:center;background:$flower-background;border-bottom:1rpx solid rgba($flower-primary,.12);font-size:32rpx;font-weight:600}.back{position:absolute;left:24rpx;bottom:13rpx;width:64rpx;height:64rpx;padding:0;background:transparent;color:$flower-primary;font-size:52rpx;line-height:58rpx}.back::after{border:0}.hero{display:block;width:100%;height:480rpx;background:#e4e7dc}.body{padding:45rpx 38rpx 100rpx;background:$flower-paper}.title{display:block;font:44rpx/1.35 $flower-serif-cn}.meta{display:block;margin-top:18rpx;font-size:22rpx;color:#69766a}.summary{display:block;margin:42rpx 0;padding:26rpx 0;border-top:1rpx solid #dee3d8;border-bottom:1rpx solid #dee3d8;line-height:1.7;font-size:25rpx;color:$flower-primary}.ending{margin-top:60rpx;padding-top:26rpx;border-top:1rpx solid #dee3d8;font:25rpx $flower-serif-cn;color:#69766a;text-align:center}.state{min-height:360rpx;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:20rpx;color:#69766a}.state button{background:transparent;color:$flower-primary;font-size:24rpx}
</style>
