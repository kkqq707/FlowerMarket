<template>
  <view class="care-page">
    <view class="care-nav" :style="{paddingTop:statusHeight+'px',height:navHeight+'px'}"><button class="back" aria-label="返回" @click="back">‹</button><text>养花指南</text></view>
    <view :style="{height:(statusHeight+navHeight)+'px'}"></view>
    <view class="intro"><text class="intro-title">把花养得更久一点</text><text class="intro-copy">收到一束花之后，从醒花、换水到日常照看，都有章可循。</text></view>
    <view class="search"><input v-model="keyword" confirm-type="search" placeholder="搜索醒花、换水或花材" @confirm="reload"/><button @click="reload">查找</button></view>
    <view v-if="loading&&page===1" class="state">正在整理养花指南…</view>
    <view v-else-if="error" class="state"><text>{{ error }}</text><button @click="reload">重新加载</button></view>
    <view v-else-if="!articles.length" class="state"><text>养花指南正在整理中，稍后再来看看。</text></view>
    <view v-else class="article-list"><view v-for="item in articles" :key="item.id" class="article" @click="open(item.id)"><image :src="cover(item.imageInput)" mode="aspectFill" :alt="item.title" lazy-load/><view class="article-copy"><text class="article-title">{{ item.title }}</text><text class="article-summary">{{ item.synopsis }}</text><text class="read">阅读养护方法  ›</text></view></view><view v-if="loading" class="list-note">正在加载更多…</view><view v-else-if="!hasMore" class="list-note">花事读到这里，去照看你的花吧。</view></view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad,onReachBottom,onPullDownRefresh } from '@dcloudio/uni-app';
import { HTTP_REQUEST_URL } from '@/config/app.js';
import { getFlowerCareArticles } from '@/api/flower.js';
const info=uni.getSystemInfoSync();const statusHeight=info.statusBarHeight||0;const navHeight=44;
const articles=ref([]),keyword=ref(''),page=ref(1),loading=ref(false),error=ref(''),hasMore=ref(true);
function cover(path){if(!path)return '';return /^https?:\/\//.test(path)?path:HTTP_REQUEST_URL+'/'+String(path).replace(/^\//,'');}
function back(){const pages=getCurrentPages();pages.length>1?uni.navigateBack():uni.switchTab({url:'/pages/index/index'});}
function open(id){uni.navigateTo({url:'/pages/flower_care/detail?id='+id});}
async function load(){if(loading.value||!hasMore.value)return;loading.value=true;error.value='';try{const response=await getFlowerCareArticles({page:page.value,limit:10,keyword:keyword.value});const data=response?.data||{};const rows=Array.isArray(data.list)?data.list:[];articles.value=page.value===1?rows:articles.value.concat(rows);hasMore.value=articles.value.length<Number(data.total||0);page.value+=1;}catch{error.value='指南暂时无法加载，请检查网络后重试。';}finally{loading.value=false;uni.stopPullDownRefresh();}}
function reload(){page.value=1;hasMore.value=true;articles.value=[];load();}
onLoad(reload);onReachBottom(load);onPullDownRefresh(reload);
</script>

<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.care-page{min-height:100vh;background:$flower-background;color:$flower-text}.care-nav{position:fixed;z-index:20;top:0;left:0;right:0;box-sizing:content-box;display:flex;align-items:center;justify-content:center;background:$flower-background;border-bottom:1rpx solid rgba($flower-primary,.12);font-size:32rpx;font-weight:600}.back{position:absolute;left:24rpx;bottom:13rpx;width:64rpx;height:64rpx;padding:0;background:transparent;color:$flower-primary;font-size:52rpx;line-height:58rpx}.back::after{border:0}.intro{display:flex;flex-direction:column;gap:14rpx;padding:54rpx 36rpx 40rpx;background:$flower-deep;color:$flower-paper}.intro-title{font:48rpx/1.35 $flower-serif-cn}.intro-copy{font-size:25rpx;line-height:1.7;color:#d7dfd3}.search{display:flex;align-items:center;gap:16rpx;padding:28rpx 30rpx}.search input{flex:1;height:74rpx;padding:0 22rpx;background:$flower-paper;border:1rpx solid #dce2d6;font-size:26rpx}.search button{margin:0;padding:0 24rpx;height:74rpx;line-height:74rpx;border-radius:0;background:$flower-primary;color:$flower-paper;font-size:25rpx}.search button::after{border:0}.article-list{padding:0 30rpx 45rpx}.article{display:flex;gap:22rpx;min-height:180rpx;padding:24rpx 0;border-bottom:1rpx solid #e1e3d9}.article image{width:190rpx;height:180rpx;flex:none;background:#e6e8dd}.article-copy{display:flex;flex:1;flex-direction:column;min-width:0}.article-title{font:31rpx/1.35 $flower-serif-cn}.article-summary{margin-top:12rpx;font-size:23rpx;line-height:1.5;color:#637061;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.read{margin-top:auto;padding-top:12rpx;font-size:22rpx;color:$flower-primary}.state{min-height:350rpx;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:20rpx;padding:30rpx;color:#637061;text-align:center}.state button{font-size:24rpx;color:$flower-primary;background:transparent}.list-note{text-align:center;color:#778075;font-size:22rpx;padding:34rpx}
</style>
