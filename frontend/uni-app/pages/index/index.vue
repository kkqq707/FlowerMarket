<template>
  <view class="flower-home">
    <hero :brand="brand" :image="heroImage" @browse="browse" />
    <view v-if="loading" class="data-note">正在整理今日花事…</view>
    <view v-else-if="error" class="data-note">{{ error }} <button @click="loadHome">重新加载</button></view>
    <category-nav :items="categories" @select="openCategory" />
    <template v-if="products.length">
    <weekly-new :products="products" @select="openProduct" @add-cart="chooseSku" />
    <budget-nav @select="openBudget" />
    <view class="recommend"><flower-section-title title="日常花礼推荐" align="left" /><view class="product-grid"><flower-product-card v-for="item in recommendedProducts" :key="item.id" :product="item" @product-click="openProduct" @add-cart="chooseSku" /></view></view>
    <editorial-feature title="人气精选" :image="products[0] && products[0].image" :caption="products[0] ? products[0].store_name : '留给重要的人'" note="用一束当季鲜花，记住相聚的时刻。" @select="openProduct(products[0])" />
    <editorial-feature title="专属定制花礼" :image="products[2] && products[2].image" caption="你的心意，自有模样" note="从喜欢的色彩与预算开始，寻找属于你的花礼。" action="挑选心意花礼" compact @select="browse" />
    <view class="care-feature" role="button" tabindex="0" @click="openCare" @keyup.enter="openCare"><view><text class="care-feature__title">一束花的日常照看</text><text class="care-feature__copy">从醒花到换水，把花期留得久一点。</text><text class="care-feature__link">阅读养花指南 ↗</text></view><image :src="products[0] && products[0].image" mode="aspectFill" alt="鲜花养护" /></view>
    </template>
    <view v-else-if="!loading && !error" class="data-note">暂无在售花礼，请稍后再来看看。</view>
    <brand-story :brand="brand" :story="story" />
    <service-promise />
    <view v-if="isPreview" class="preview"><text>主题预览</text><button @click="exitPreview">退出预览</button></view>
  </view>
</template>

<script setup>
import { computed, ref, getCurrentInstance } from 'vue';
import { onLoad, onShow, onPullDownRefresh, onShareAppMessage } from '@dcloudio/uni-app';
import Hero from '@/components/flower-home/hero.vue';
import WeeklyNew from '@/components/flower-home/weekly-new.vue';
import CategoryNav from '@/components/flower-home/category-nav.vue';
import BudgetNav from '@/components/flower-home/budget-nav.vue';
import BrandStory from '@/components/flower-home/brand-story.vue';
import EditorialFeature from '@/components/flower-home/editorial-feature.vue';
import ServicePromise from '@/components/flower-home/service-promise.vue';
import FlowerSectionTitle from '@/components/flower-section-title/index.vue';
import FlowerProductCard from '@/components/flower-product-card/index.vue';
import { getFlowerProducts, getCategoryList, getFlowerStoreConfig, flowerProductsFromResponse } from '@/api/store.js';
import { getThemeInfo } from '@/api/api.js';
import { getShare } from '@/api/public.js';
import { getCartCounts } from '@/api/order.js';
import { useAppStore } from '@/store/app.js';
import { applyTheme } from '@/utils/theme.js';
import { FLOWER_BRAND } from '@/utils/flower-brand.js';

const { proxy } = getCurrentInstance();
const appStore = useAppStore();
const brand = ref(FLOWER_BRAND.name), story = ref(''), heroImage = ref(''), products = ref([]), loading = ref(false), error = ref('');
const isPreview = ref(false), themeId = ref(''), shareInfo = ref({});
const defaultCategories = ['日常','生日','纪念日','祝贺'].map(name => ({ name, occasion: name }));
const categories = ref(defaultCategories);
const recommendedProducts = computed(() => products.value.length > 2 ? products.value.slice(2) : products.value);
function browse() { uni.switchTab({ url:'/pages/goods_cate/goods_cate' }); }
function openProduct(item) { if (item) uni.navigateTo({ url:'/pages/goods/goods_details/index?id=' + item.id }); }
function chooseSku(item) { openProduct(item); }
function openCategory(item) { uni.navigateTo({ url:'/pages/goods/goods_list/index?' + (item.id ? 'cid=' + item.id : 'occasion=' + encodeURIComponent(item.occasion)) }); }
function openBudget(index) { uni.navigateTo({ url:'/pages/goods/goods_list/index?budget=' + index }); }
function openCare() { uni.navigateTo({ url:'/pages/flower_care/index' }); }
function showTabs() { uni.showTabBar({ animation:false }); }
async function loadHome() {
  if (loading.value) return;
  loading.value = true;
  const results = await Promise.allSettled([
    getFlowerProducts({ page:1, limit:8 }), getCategoryList(),
    getThemeInfo('home', themeId.value ? { theme_id:themeId.value } : {}),
    getShare(), getFlowerStoreConfig()
  ]);
  const [goods, category, design, share, storeConfig] = results;
  const list = goods.status === 'fulfilled' ? flowerProductsFromResponse(goods.value) : [];
  error.value = goods.status === 'rejected' ? (goods.reason?.msg || '花礼暂时无法加载，请重试。') : '';
  products.value = list;
  story.value = '';
  heroImage.value = '';
  if (category.status === 'fulfilled') {
    const rows = Array.isArray(category.value.data) ? category.value.data : [];
    categories.value = rows.slice(0,4).map(item => ({ id:item.id, name:item.name || item.cateName || item.cate_name }));
  }
  if (design.status === 'fulfilled' && design.value.data) {
    const config = design.value.data;
    story.value = config.brandStory || '';
    // Do not inherit generic legacy carousel artwork: it may contain unrelated
    // category promotions. Only an explicitly configured flower hero is safe.
    heroImage.value = config.flowerHeroImage || '';
  }
  if (storeConfig.status === 'fulfilled' && storeConfig.value.data) {
    const config = storeConfig.value.data;
    brand.value = config.storeName || FLOWER_BRAND.name;
    if (!story.value) story.value = config.brandSlogan || '';
  }
  if (!heroImage.value) heroImage.value = products.value[0] && products.value[0].image;
  if (share.status === 'fulfilled') {
    shareInfo.value = share.value.data || {};
    // #ifdef H5
    if (proxy.$wechat && proxy.$wechat.isWeixin()) proxy.$wechat.wechatEvevt(['updateAppMessageShareData','updateTimelineShareData'], { title:brand.value, desc:shareInfo.value.synopsis, imgUrl:shareInfo.value.img, link:location.href });
    // #endif
  }
  loading.value = false; uni.stopPullDownRefresh();
}
function exitPreview() { uni.removeStorageSync('previewThemeId'); themeId.value='';isPreview.value=false;applyTheme().catch(()=>{});loadHome(); }
onLoad(options => {
  themeId.value=options.theme_id || uni.getStorageSync('previewThemeId') || '';
  isPreview.value=!!themeId.value;
  if (themeId.value) uni.setStorageSync('previewThemeId',themeId.value);
  if (options.spid) getApp().globalData.spid=options.spid;
  applyTheme(themeId.value).catch(()=>{});
});
onShow(() => {
  showTabs();
  loadHome();
  if (appStore.isLogin) getCartCounts(true,'total').then(res => {
    const count=Number(res.data.count)||0;
    if(count) uni.setTabBarBadge({index:2,text:count>99?'99+':String(count)});
    else uni.removeTabBarBadge({index:2});
  }).catch(()=>{});
});
onPullDownRefresh(loadHome);
onShareAppMessage(() => ({title:brand.value,path:'/pages/index/index?spid='+(appStore.uid||''),imageUrl:shareInfo.value.img || ''}));
</script>

<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.flower-home { min-height:100vh; background:$flower-background; color:$flower-text; padding-bottom:20rpx; overflow-x:hidden; font-family:-apple-system,BlinkMacSystemFont,'PingFang SC','Microsoft YaHei',sans-serif; font-size:28rpx; line-height:1.5; }
.recommend { padding:12rpx 0 20rpx; }
.product-grid { display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:32rpx 20rpx;padding:0 32rpx 44rpx; }
.data-note { padding:22rpx 32rpx;font-size:24rpx;line-height:1.6;color:$flower-primary;background:$flower-paper; }
.data-note button { display:inline;padding:0 12rpx;background:transparent;color:$flower-primary;font-size:24rpx;text-decoration:underline; }
.data-note button::after { border:0; }
.preview { position:fixed;right:20rpx;bottom:160rpx;padding:16rpx;background:$flower-paper;border:1px solid $flower-primary;font-size:24rpx; }
.preview button { font-size:24rpx; }
.care-feature { display:flex; min-height:280rpx; margin:20rpx 32rpx 48rpx; background:$flower-deep; color:$flower-paper; }
.care-feature>view { display:flex; flex:1; flex-direction:column; justify-content:center; gap:12rpx; padding:28rpx 24rpx; min-width:0; }
.care-feature__title { font:34rpx/1.35 $flower-serif-cn; }
.care-feature__copy { font-size:25rpx; color:#e5e9e2; line-height:1.5; }
.care-feature__link { margin-top:8rpx; font-size:24rpx; text-decoration:underline; text-underline-offset:5rpx; }
.care-feature image { width:34%; min-height:280rpx; }
button:focus-visible { outline:2px solid $flower-primary;outline-offset:3px; }
</style>
