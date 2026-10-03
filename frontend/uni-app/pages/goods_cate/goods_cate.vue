<template>
  <view class="flower-catalog">
    <view class="catalog-heading"><text class="eyebrow">花礼精选</text><text class="catalog-title">{{ categoryMode ? '把心意，送给重要的人' : '寻找你的那束花' }}</text></view>
    <view class="search"><input v-model="keyword" confirm-type="search" placeholder="搜索花名、花材" aria-label="搜索花名或花材" @confirm="search" /><button @click="search">搜索</button></view>
    <view class="filters">
      <view v-for="group in flowerFilters" :key="group.key" class="filter-row"><text class="filter-label">{{ group.label }}</text><scroll-view scroll-x class="filter-scroll" :show-scrollbar="false"><button v-for="(value,index) in group.values" :key="value" :class="{active:selected[group.key] === index}" @click="setFilter(group.key,index)">{{ value }}</button></scroll-view></view>
    </view>
    <view class="results-heading"><text>{{ activeSummary }}</text><button @click="reset">重置筛选</button></view>
    <view v-if="error" class="data-note">{{ error }}<button @click="loadMore">重试</button></view>
    <view :class="categoryMode ? 'product-rows' : 'product-grid'">
      <template v-for="item in products" :key="item.id">
        <flower-product-row v-if="categoryMode" :product="item" @product-click="openProduct" @add-cart="chooseSku" />
        <flower-product-card v-else :product="item" @product-click="openProduct" @add-cart="chooseSku" />
      </template>
    </view>
    <flower-empty v-if="!loading && !error && !products.length" title="还没有符合心意的花礼" description="试试其他场景、颜色或预算，也可以重新搜索。" />
    <view class="loading">{{ loading ? '正在寻找花礼…' : (ended && products.length ? '花礼已全部呈现' : '') }}</view>
    <button v-if="!loading && !ended && products.length" class="load-more" @click="loadMore">查看更多花礼</button>
  </view>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { onLoad, onShow, onReachBottom } from '@dcloudio/uni-app';
import FlowerProductCard from '@/components/flower-product-card/index.vue';
import FlowerProductRow from '@/components/flower-product-row/index.vue';
import FlowerEmpty from '@/components/flower-empty/index.vue';
import { getFlowerProducts, getFlowerFilterOptions, flowerProductsFromResponse, flowerFiltersFromResponse, flowerBudgets } from '@/api/store.js';
const categoryMode=true;
const keyword=ref(''),cid=ref(''),products=ref([]),loading=ref(false),ended=ref(false),error=ref(''),page=ref(1);
const selected=reactive({occasion:0,recipient:0,colorSeries:0,budget:0});
let generation=0;
let entryOptions=null;
const flowerFilters=ref(flowerFiltersFromResponse(null));
const activeSummary=computed(()=>flowerFilters.value.map(group=>selected[group.key] ? group.values[selected[group.key]] : '').filter(Boolean).join(' · ') || '所有花礼');
function query() {
  const data={keyword:keyword.value.trim(),page:page.value,limit:12,...flowerBudgets[selected.budget]};
  if(cid.value)data.cid=cid.value;
  flowerFilters.value.filter(group=>group.key!=='budget').forEach(group=>{if(selected[group.key])data[group.key]=group.values[selected[group.key]];});
  return data;
}
function setFilter(key,index){selected[key]=index;cid.value='';search();}
function search(){generation++;page.value=1;products.value=[];ended.value=false;loading.value=false;error.value='';loadMore();}
function reset(){keyword.value='';cid.value='';Object.keys(selected).forEach(key=>selected[key]=0);search();}
async function loadMore(){
  if(loading.value||ended.value)return;
  const current=generation;loading.value=true;error.value='';
  try {
    const data=query();
    const response=await getFlowerProducts(data);
    if(current!==generation)return;
    const list=flowerProductsFromResponse(response);
    products.value=products.value.concat(list);ended.value=list.length<12;page.value++;
  }catch(err){
    if(current!==generation)return;
    error.value=err.msg || '花礼暂时无法加载，请重试。';
  }finally{if(current===generation)loading.value=false;}
}
function openProduct(item){uni.navigateTo({url:'/pages/goods/goods_details/index?id='+item.id});}
function chooseSku(item){openProduct(item);}
function init(options={}){
  entryOptions=options;
  keyword.value=options.searchValue||options.keyword||'';cid.value=options.cid||'';
  flowerFilters.value.forEach(group=>{if(options[group.key])selected[group.key]=group.key==='budget'?Math.min(4,Math.max(0,Number(options.budget)||0)):Math.max(0,group.values.indexOf(options[group.key]));});
}
async function refreshCatalog(){
  const chosen=Object.fromEntries(flowerFilters.value.map(group=>[group.key,group.values[selected[group.key]]]));
  try {
    flowerFilters.value=flowerFiltersFromResponse(await getFlowerFilterOptions());
    flowerFilters.value.forEach(group=>{
      const value=entryOptions?.[group.key] || chosen[group.key];
      selected[group.key]=group.key==='budget' ? (entryOptions?.budget ? Math.min(4,Math.max(0,Number(entryOptions.budget)||0)) : selected.budget) : Math.max(0,group.values.indexOf(value));
    });
    entryOptions=null;
  }catch(err){ /* Keep existing filters while the product request supplies the retry state. */ }
  search();
}
onLoad(options=>init(options));
onShow(()=>{uni.showTabBar();refreshCatalog();});
onReachBottom(loadMore);
defineExpose({loadMore});
</script>
<style scoped lang="scss">
@import '@/static/css/flower-tokens.scss';
.flower-catalog { min-height:100vh;padding:42rpx 32rpx 120rpx;background:$flower-background;color:$flower-text;box-sizing:border-box; }
.eyebrow { display:block;font:18rpx $flower-serif-en;letter-spacing:3rpx;color:$flower-primary; }
.catalog-title { display:block;margin:14rpx 0 32rpx;font:39rpx/1.5 $flower-serif-cn;letter-spacing:1rpx; }
.search { display:flex;align-items:center;gap:16rpx;border-bottom:1rpx solid $flower-primary;padding:16rpx 0;margin-bottom:26rpx; }
.search input { flex:1;min-width:0;font-size:26rpx; }
button { padding:0;background:transparent;color:$flower-primary;font-size:23rpx;line-height:2;border-radius:0; }
button::after { border:0; }button:focus-visible,input:focus-visible { outline:2px solid $flower-primary;outline-offset:3px; }
.search button { margin:0;width:88rpx; }
.filters { padding-bottom:16rpx;border-bottom:1rpx solid rgba($flower-primary,.18); }
.filter-row { display:flex;align-items:center;padding:10rpx 0;gap:18rpx; }
.filter-label { flex:0 0 58rpx;font-size:24rpx; }
.filter-scroll { flex:1;min-width:0;width:0;white-space:nowrap; }
.filter-scroll button { display:inline-block;margin:0 8rpx 0 0;padding:3rpx 18rpx;font-size:22rpx; }
.filter-scroll .active { background:$flower-primary;color:$flower-paper; }
.results-heading { display:flex;align-items:center;justify-content:space-between;gap:12rpx;margin:26rpx 0 12rpx;font-size:22rpx; }
.results-heading button { flex-shrink:0;margin:0;font-size:21rpx;opacity:.7; }
.product-grid { display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:32rpx 24rpx; }
.data-note { background:$flower-paper;padding:20rpx;margin:20rpx 0;font-size:22rpx;line-height:1.8; }
.data-note button { display:inline-block;margin:0 0 0 12rpx;text-decoration:underline; }
.loading { text-align:center;font-size:22rpx;opacity:.6;padding:36rpx 0; }
.load-more { border:1rpx solid rgba($flower-primary,.4);padding:12rpx; }
</style>
