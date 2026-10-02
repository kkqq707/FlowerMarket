<template>
  <view
    class="page"
    :data-theme="theme"
    :style="[colorStyle, { 'background-image': `url(${backBg})` }]"
  >
    <view class="system-height" :style="{ height: statusBarHeight }"></view>
    <!-- #ifdef MP -->
    <view class="title-bar" :style="{ height: '43px', paddingTop: statusBarHeight }">
      <view class="icon" @click="back" v-if="!isHome">
        <image :src="urlDomain + 'crmebimage/perset/usersImg/left.png'"></image>
      </view>
      <view class="icon" @click="home" v-else>
        <image :src="urlDomain + 'crmebimage/perset/usersImg/home.png'"></image>
      </view>
      账户登录
    </view>
    <!-- #endif -->
    <!-- #ifdef MP -->
    <view class="title-bar-space" style="height: 43px"></view>
    <!-- #endif -->
    <view class="wechat_login">
      <view class="img acea-row row-center">
        <image :src="FLOWER_BRAND.logo" mode="aspectFit" class="image" alt="花间集标志"></image>
      </view>
      <view class="company"><image :src="FLOWER_BRAND.wordmark" mode="aspectFit" class="wordmark" alt="花间集" /></view>
      <view class="btn-wrapper">
        <!-- #ifdef H5 -->
        <button hover-class="none" @click="wechatLogin" class="btn1 bg-color">
          <text class="iconfont icon-weixin2"></text>立即登录
        </button>
        <!-- #endif -->
        <!-- #ifdef MP -->
        <button
          hover-class="none"
          @click="openPhoneLogin"
          class="btn1 bg-color"
        >
          立即登录
        </button>
        <!-- #endif -->
        <button hover-class="none" @click="cancelLogin" class="btn-cancel">
          取消登录
        </button>
      </view>
    </view>
    <block v-if="isUp">
      <mobileLogin
        :isUp="isUp"
        @close="maskClose"
        :authKey="authKey"
        @wechatPhone="wechatPhone"
      ></mobileLogin>
    </block>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { useAppStore } from "@/store/app.js";
import Cache from "@/utils/cache.js";
import util from "@/utils/util.js";
import mobileLogin from "@/components/login_mobile/index.vue";
import {
  LOGO_URL,
  EXPIRES_TIME,
  USER_INFO,
  BACK_URL,
  STATE_R_KEY,
} from "@/config/cache.js";
import { HTTP_REQUEST_URL } from "@/config/app.js";
import { getUserInfo } from "@/api/user.js";
// #ifdef H5
import wechat from "@/libs/wechat.js";
// #endif
import { useColor } from '@/composables/useColor.js';
import { FLOWER_BRAND } from '@/utils/flower-brand.js';

const app = getApp();
const { proxy } = getCurrentInstance();
const appStore = useAppStore();

let statusBarHeight = getStatusBarHeight() + "px";

const urlDomain = ref(Cache.get("imgHost"));
const isUp = ref(false); // 绑定手机号手动输入弹窗
const isHome = ref(false);
const code = ref("");
const authKey = ref("");
const options = ref("");
const theme = ref(app.globalData.theme);
const { colorStyle } = useColor();
const backBg = ref(""); //背景图片

function getStatusBarHeight() {
  if (typeof uni.getWindowInfo === "function") {
    return uni.getWindowInfo().statusBarHeight || 0;
  }
  return 0;
}

function decodeBackUrl(backUrl, fallbackUrl = "") {
  if (!backUrl) return fallbackUrl;
  try {
    return decodeURIComponent(decodeURIComponent(backUrl));
  } catch (error) {
    try {
      return decodeURIComponent(backUrl);
    } catch (e) {
      return fallbackUrl;
    }
  }
}

function normalizeBackUrl(backUrl) {
  if (!backUrl) return "/pages/index/index";
  if (
    backUrl.indexOf("/pages/users/wechat_login/index") !== -1 ||
    backUrl.indexOf("/pages/users/login/index") !== -1 ||
    backUrl.indexOf("/pages/users/app_login/index") !== -1
  ) {
    return "/pages/index/index";
  }
  return backUrl.indexOf("/pages/index/index") !== -1 ? "/" : backUrl;
}

function syncBackUrl(backUrl) {
  const decodedBackUrl = decodeBackUrl(backUrl);
  if (!decodedBackUrl) return "";
  const normalizedBackUrl = normalizeBackUrl(decodedBackUrl);
  uni.setStorageSync("snRouter", normalizedBackUrl);
  return normalizedBackUrl;
}

function getBackUrl() {
  const rawBackUrl =
    options.value.back_url ||
    uni.getStorageSync("snRouter") ||
    Cache.get(BACK_URL) ||
    "";
  return normalizeBackUrl(decodeBackUrl(rawBackUrl));
}

function finishWechatLogin() {
  Cache.clear("snsapiKey");
  const backUrl = getBackUrl();
  Cache.clear(BACK_URL);
  uni.removeStorageSync("snRouter");
  isUp.value = false;
  uni.showToast({
    title: "登录成功",
    icon: "none",
  });
  setTimeout(() => {
    location.href = backUrl;
  }, 800);
}

function getH5UserInfoData() {
  return getUserInfo().then((res) => {
    appStore.UPDATE_USERINFO(res.data);
    return res;
  });
}

onLoad((loadOptions) => {
  // #ifdef MP
  options.value = loadOptions || {};
  // #endif
  //背景图片
  switch (app.globalData.theme) {
    case "theme1":
      backBg.value = `${urlDomain.value}/crmebimage/perset/usersImg/wxbj1.png`;
      break;
    case "theme2":
      backBg.value = `${urlDomain.value}/crmebimage/perset/usersImg/wxbj2.png`;
      break;
    case "theme3":
      backBg.value = `${urlDomain.value}/crmebimage/perset/usersImg/wxbj3.png`;
      break;
    case "theme4":
      backBg.value = `${urlDomain.value}/crmebimage/perset/usersImg/wxbj4.png`;
      break;
    case "theme5":
      backBg.value = `${urlDomain.value}/crmebimage/perset/usersImg/wxbj5.png`;
      break;
  }

  // #ifdef H5
  document.body.addEventListener("focusout", () => {
    setTimeout(() => {
      const scrollHeight =
        document.documentElement.scrollTop || document.body.scrollTop || 0;
      window.scrollTo(0, Math.max(scrollHeight - 1, 0));
    }, 100);
  });
  const { code: authCode, state, scope } = loadOptions;
  options.value = loadOptions;
  syncBackUrl(loadOptions.back_url || "");
  // 获取确认授权code
  code.value = authCode || "";
  const registerAuthKey = loadOptions.authKey || Cache.get("snsapiKey") || "";
  if (!authCode && registerAuthKey) {
    authKey.value = registerAuthKey;
    isUp.value = true;
  }
  //if(!authCode) location.replace(decodeURIComponent(decodeURIComponent(option.query.back_url)));
  if (authCode && options.value.scope !== "snsapi_base") {
    let spreadId = app.globalData.spread ? app.globalData.spread : 0;
    //公众号授权登录回调 wechatAuth(code, Cache.get("spread"), loginType)
    wechat
      .auth(authCode, spreadId)
      .then((res) => {
        if (res.type === "register") {
          Cache.set("snsapiKey", res.key);
          authKey.value = res.key;
          isUp.value = true;
        }
        if (res.type === "login") {
          appStore.LOGIN({
            token: res.token,
          });
          appStore.SETUID(res.uid);
          getH5UserInfoData().then(() => {
            finishWechatLogin();
          }).catch(() => {
            finishWechatLogin();
          });
        }
      })
      .catch((error) => {});
  }
  // #endif
  let pages = getCurrentPages();
  // let prePage = pages[pages.length - 2];
  // if (prePage.route == 'pages/order_addcart/order_addcart') {
  // 	isHome.value = true
  // } else {
  // 	isHome.value = false
  // }
});

function back() {
  cancelLogin();
}

function cancelLogin() {
  Cache.clear(BACK_URL);
  uni.removeStorageSync('snRouter');
  const pages = getCurrentPages();
  const previous = pages.length > 1 ? pages[pages.length - 2].route : '';
  if (previous === 'pages/user/index' || pages.length <= 1) {
    uni.switchTab({ url: '/pages/user/index' });
    return;
  }
  uni.navigateBack({ delta: 1, fail: () => uni.switchTab({ url: '/pages/user/index' }) });
}

function home() {
  uni.switchTab({
    url: "/pages/index/index",
  });
}

function openPhoneLogin() {
  const requested = decodeBackUrl(options.value.back_url || Cache.get(BACK_URL), '/pages/index/index');
  const backUrl = requested.startsWith('/pages/') &&
    !/\/pages\/users\/(wechat_login|login|app_login)\//.test(requested)
    ? requested
    : '/pages/index/index';
  Cache.set(BACK_URL, backUrl);
  uni.navigateTo({ url: '/pages/users/login/index' });
}

// 弹窗关闭
function maskClose() {
  // isUp.value = false  //点击模态框会关闭登录弹框，防止用户误触而关闭
}

// #ifdef H5
// 获取url后面的参数
function getQueryString(name) {
  var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
  var reg_rewrite = new RegExp("(^|/)" + name + "/([^/]*)(/|$)", "i");
  var r = window.location.search.substr(1).match(reg);
  var q = window.location.pathname.substr(1).match(reg_rewrite);
  if (r != null) {
    return unescape(r[2]);
  } else if (q != null) {
    return unescape(q[2]);
  } else {
    return null;
  }
}

// 公众号登录
function wechatLogin() {
  if (!code.value && options.value.scope !== "snsapi_base") {
    proxy.$wechat.oAuth("snsapi_userinfo", "/pages/users/wechat_login/index");
  } else {
    // if (authKey.value) {
    // 	isUp.value = true;
    // }
    isUp.value = true;
  }
}

// 输入手机号后的回调
function wechatPhone() {
  Cache.clear("snsapiKey");
  finishWechatLogin();
}
// #endif
</script>
<style lang="scss">
page {
  background: #fff;
  height: 100%;
}
</style>
<style lang="scss" scoped>
.icon-weixin2 {
  margin-right: 10rpx;
}
.company {
  display: flex;
  justify-content: center;
  margin: 30rpx 0 96rpx;
}
.wordmark {
  width: 250rpx;
  height: 86rpx;
}

.page {
  background: #fff;
  height: 100vh;
  background-size: contain;
  background-repeat: no-repeat;
}

.wechat_login {
  padding-top: 238rpx;

  .img .image {
    width: 152rpx;
    height: 152rpx;
  }

  .btn-wrapper {
    margin-top: 86rpx;
    padding: 0 66rpx;

    button {
      width: 100%;
      height: 86rpx;
      line-height: 84rpx;
      margin-bottom: 32rpx;
      border-radius: 120rpx;
      font-size: 32rpx;

      &.btn1 {
        color: #fff;
      }

      &.btn2 {
        color: #666666;
        border: 2px solid #e4e4e4;
      }

      &.btn-cancel {
        color: #999;
        background: #fff;
        border: 2px solid #e4e4e4;
      }
    }
  }
}

.title-bar {
  position: fixed;
  z-index: 100;
  top: 0;
  left: 0;
  right: 0;
  box-sizing: content-box;
  background: #FFFEFA;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}

.icon {
  position: absolute;
  left: 30rpx;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 86rpx;
  height: 86rpx;

  image {
    width: 50rpx;
    height: 50rpx;
  }
}
</style>
