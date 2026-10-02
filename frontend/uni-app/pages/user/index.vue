<template>
  <view class="flower-account">
    <view class="account-topbar" :style="{ paddingTop: statusBarHeight + 'px' }"><view class="page-title">我的</view></view>
    <view class="account-topbar-space" :style="{ height: 'calc(' + statusBarHeight + 'px + 88rpx)' }"></view>

    <view class="account-hero" @click="openAccountHero">
      <view class="account-hero__copy">
        <text class="account-hero__eyebrow">花间集 · 鲜花与心意</text>
        <text class="account-hero__title">{{ isLogin ? displayName : '把花的故事，留在这里' }}</text>
        <text class="account-hero__hint">{{ isLogin ? '花礼订单与收花信息都在这里' : '登录后查看订单与收花信息' }}</text>
        <view class="account-hero__actions"><button v-if="isLogin" class="account-hero__logout" @click.stop="confirmLogout">退出登录</button><text v-else class="account-hero__action">点击登录 ↗</text></view>
      </view>
      <image class="account-hero__image" :src="avatar" mode="aspectFill" alt="账户头像" />
    </view>

    <view class="section-head">
      <view><text class="section-head__eyebrow">我的订单</text><text class="section-head__title">订单中心</text></view>
      <button class="section-head__link" @click="openProtected('/pages/users/order_list/index')">查看全部 <text aria-hidden="true">›</text></button>
    </view>
    <view class="order-card">
      <button v-for="item in orderEntries" :key="item.status" class="order-entry" @click="openProtected('/pages/users/order_list/index?status=' + item.status)">
        <text class="order-entry__icon iconfont" :class="item.icon" aria-hidden="true"></text>
        <text class="order-entry__label">{{ item.label }}</text>
      </button>
    </view>

    <view class="section-head section-head--services">
      <view><text class="section-head__eyebrow">贴心服务</text><text class="section-head__title">花礼服务</text></view>
    </view>
    <view class="service-card">
      <button v-for="item in serviceEntries" :key="item.label" class="service-entry" @click="openProtected(item.url)">
        <text class="service-entry__icon iconfont" :class="item.icon" aria-hidden="true"></text>
        <view class="service-entry__copy"><text class="service-entry__title">{{ item.label }}</text><text class="service-entry__hint">{{ item.hint }}</text></view>
        <text class="service-entry__arrow" aria-hidden="true">›</text>
      </button>
    </view>
    <button class="account-legal" @click="openAgreements">协议与资质 <text aria-hidden="true">›</text></button>
    <view class="account-ending">花开有期 · 心意常在</view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { storeToRefs } from 'pinia';
import { getUserInfo } from '@/api/user.js';
import { useAppStore } from '@/store/app.js';
import { FLOWER_BRAND } from '@/utils/flower-brand.js';
import { confirmFlowerLogout } from '@/utils/flower-logout.js';

const appStore = useAppStore();
const { isLogin } = storeToRefs(appStore);
const userInfo = ref({});
const statusBarHeight = typeof uni.getWindowInfo === 'function' ? Number(uni.getWindowInfo().statusBarHeight || 0) : 0;
const displayName = computed(() => userInfo.value.nickname || userInfo.value.nickName || '爱花的人');
const avatar = computed(() => isLogin.value && userInfo.value.avatar ? userInfo.value.avatar : FLOWER_BRAND.logo);
const orderEntries = [
  { label: '待付款', status: 0, icon: 'icon-daifukuan1' },
  { label: '待发货', status: 1, icon: 'icon-daifahuo1' },
  { label: '待收货', status: 2, icon: 'icon-daishouhuo1' },
  { label: '待评价', status: 3, icon: 'icon-daipingjia1' },
];
const serviceEntries = [
  { label: '售后服务', hint: '查看退款与售后进度', url: '/pages/users/user_return_list/index', icon: 'icon-ic_shouhou' },
  { label: '地址管理', hint: '管理常用收花地址', url: '/pages/users/user_address_list/index', icon: 'icon-dizhi1' },
  { label: '我的收藏', hint: '留住喜欢的花礼', url: '/pages/users/user_goods_collection/index', icon: 'icon-shoucang3' },
  { label: '个人资料', hint: '完善你的账户信息', url: '/pages/infos/user_info/index', icon: 'icon-gerenzhongxin1' },
];

function openProtected(url) {
  if (!isLogin.value) {
    uni.navigateTo({ url: '/pages/users/wechat_login/index?back_url=' + encodeURIComponent('/pages/user/index') });
    return;
  }
  uni.navigateTo({ url });
}

function openAccountHero() {
  if (!isLogin.value) openProtected('/pages/infos/user_info/index');
}

function openAgreements() {
  uni.navigateTo({ url: '/pages/goods/agreement_rules/index' });
}

function confirmLogout() {
  confirmFlowerLogout(appStore);
}

function refreshProfile() {
  if (!isLogin.value) {
    userInfo.value = {};
    uni.stopPullDownRefresh();
    return;
  }
  getUserInfo().then((res) => {
    userInfo.value = res.data || {};
    appStore.UPDATE_USERINFO(userInfo.value);
  }).catch(() => {}).finally(() => uni.stopPullDownRefresh());
}

onShow(() => {
  uni.showTabBar();
  refreshProfile();
});
onPullDownRefresh(refreshProfile);
</script>

<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.flower-account { min-height:100vh; box-sizing:border-box; padding:0 30rpx 54rpx; background:$flower-background; color:$flower-text; }
.account-topbar { position:fixed; z-index:100; top:0; left:0; right:0; background:$flower-background; border-bottom:1rpx solid rgba($flower-primary,.12); }
.page-title { height:88rpx; display:flex; align-items:center; justify-content:center; font-size:32rpx; font-weight:600; }
.account-hero { display:flex; min-height:300rpx; padding:44rpx 34rpx; box-sizing:border-box; align-items:center; justify-content:space-between; gap:24rpx; background:$flower-deep; color:$flower-paper; }
.account-hero__copy { display:flex; flex:1; min-width:0; flex-direction:column; align-items:flex-start; }
.account-hero__eyebrow { font:17rpx $flower-serif-en; letter-spacing:2rpx; opacity:.72; }
.account-hero__title { margin-top:24rpx; font:38rpx/1.4 $flower-serif-cn; letter-spacing:1rpx; }
.account-hero__hint { margin-top:12rpx; font-size:22rpx; opacity:.76; }
.account-hero__actions { display:flex; align-items:center; gap:30rpx; margin-top:25rpx; }
.account-hero__action { padding-bottom:7rpx; border-bottom:1rpx solid rgba(255,255,255,.7); font-size:23rpx; }
.account-hero__logout { flex:none; height:auto; margin:0; padding:0 0 7rpx; border-bottom:1rpx solid rgba(255,255,255,.5); border-radius:0; background:transparent; color:$flower-paper; font-size:23rpx; line-height:1.4; }
.account-hero__logout::after { border:0; }
.account-hero__image { width:132rpx; height:132rpx; flex:none; border-radius:50%; background:$flower-paper; }
.section-head { display:flex; align-items:end; justify-content:space-between; margin:58rpx 4rpx 22rpx; }
.section-head__eyebrow { display:block; font:17rpx $flower-serif-en; letter-spacing:3rpx; color:#7B8377; }
.section-head__title { display:block; margin-top:8rpx; font:34rpx $flower-serif-cn; }
.section-head__link { margin:0; padding:0; background:transparent; color:$flower-primary; font-size:22rpx; line-height:1.8; }
.section-head__link::after,.order-entry::after,.service-entry::after { border:0; }
.section-head__link text { margin-left:7rpx; font-size:30rpx; }
.order-card { display:flex; padding:30rpx 8rpx; background:$flower-paper; border:1rpx solid #E5E5D9; }
.order-entry { flex:1; min-width:0; margin:0; padding:0; display:flex; align-items:center; flex-direction:column; background:transparent; color:$flower-text; line-height:1.5; }
.order-entry__icon { font-size:46rpx; color:$flower-primary; }
.order-entry__label { margin-top:18rpx; font-size:22rpx; }
.section-head--services { margin-top:64rpx; }
.service-card { padding:0 26rpx; background:$flower-paper; border:1rpx solid #E5E5D9; }
.service-entry { display:flex; align-items:center; width:100%; min-height:120rpx; margin:0; padding:0; border-bottom:1rpx solid #EAE9DF; border-radius:0; background:transparent; color:$flower-text; text-align:left; }
.service-entry:last-child { border-bottom:0; }
.service-entry__icon { width:58rpx; flex:none; margin-right:25rpx; color:$flower-primary; font-size:37rpx; text-align:center; }
.service-entry__copy { display:flex; flex:1; flex-direction:column; }
.service-entry__title { font-size:27rpx; font-weight:500; }
.service-entry__hint { margin-top:4rpx; color:#858981; font-size:20rpx; }
.service-entry__arrow { color:#858981; font-size:40rpx; }
.account-legal { display:flex; align-items:center; justify-content:center; gap:8rpx; width:100%; height:76rpx; margin:26rpx 0 0; padding:0; background:transparent; color:#687365; font-size:23rpx; }
.account-legal::after { border:0; }
.account-legal text { font-size:31rpx; }
.account-ending { padding:54rpx 0 30rpx; color:#81887D; font:22rpx $flower-serif-cn; letter-spacing:4rpx; text-align:center; }
</style>
