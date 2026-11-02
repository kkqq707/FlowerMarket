<template>
  <div class="divBox flower-dashboard" v-loading="loading">
    <header><div><h2>花间集经营看板</h2><p>查看今日待办、销售趋势与热销花礼。</p></div><el-button type="primary" @click="$router.push('/flower/orders')">进入订单工作台</el-button></header>
    <section class="metrics">
      <article><span class="metric-label">今日销售额</span><strong class="metric-value"><small>¥</small><span>{{ money(data.todaySales) }}</span></strong><em>退款订单不计入</em></article>
      <article><span class="metric-label">今日订单</span><strong class="metric-value"><span>{{ data.todayOrders || 0 }}</span><small>单</small></strong><em>当前自然日</em></article>
      <article><span class="metric-label">平均客单价</span><strong class="metric-value"><small>¥</small><span>{{ money(data.averageOrderValue) }}</span></strong><em>实付金额平均值</em></article>
      <article><span class="metric-label">准时送达率</span><strong class="metric-value"><span>{{ money(data.onTimeRate) }}</span><small>%</small></strong><em>按承诺时段统计</em></article>
    </section>
    <section class="workbench-card">
      <div class="section-title"><div><h3>今日待办</h3><p>从接单到配送，依次完成每一个花礼。</p></div></div>
      <div class="flow"><button @click="goStage('WAIT_ACCEPT')"><b>{{ data.waitAcceptCount || 0 }}</b><span>待接单</span></button><i></i><button @click="goStage('WAIT_PRODUCTION')"><b>{{ data.waitProductionCount || 0 }}</b><span>待制作</span></button><i></i><button @click="goStage('WAIT_DELIVERY')"><b>{{ data.waitDeliveryCount || 0 }}</b><span>待配送</span></button><i></i><button class="alert" @click="$router.push('/order/index')"><b>{{ data.afterSaleCount || 0 }}</b><span>售后待处理</span></button></div>
    </section>
    <section class="panel-grid">
      <el-card shadow="never" class="trend-panel">
        <template #header><div class="card-title"><b>销售趋势</b><span>每日金额与单量</span></div></template>
        <DashboardPeriodFilter label="销售趋势" :range="trend.params" @apply="loadTrend" />
        <div v-loading="trend.loading" class="report-content" :aria-busy="trend.loading">
          <div v-if="trend.error" class="report-error" role="alert"><p>{{ trend.error }}</p><el-button @click="loadTrend()">重试</el-button></div>
          <SalesTrendChart v-else :records="trend.data" :start-date="trend.params.startDate" :end-date="trend.params.endDate" />
        </div>
      </el-card>
      <el-card shadow="never" class="ranking-panel">
        <template #header><div class="card-title"><b>热门花礼</b><el-select v-model="hotSort" aria-label="热门花礼排序" class="ranking-sort" size="small" @change="loadHot()"><el-option label="按销量排序" value="quantity" /><el-option label="按销售额排序" value="amount" /></el-select></div></template>
        <DashboardPeriodFilter label="热门花礼" :range="hot.params" @apply="loadHot" />
        <p class="report-range">{{ hot.params.startDate }} 至 {{ hot.params.endDate }} · 前 5 名</p>
        <div v-loading="hot.loading" class="report-content" :aria-busy="hot.loading">
          <div v-if="hot.error" class="report-error" role="alert"><p>{{ hot.error }}</p><el-button @click="loadHot()">重试</el-button></div>
          <ol v-else-if="hot.data.length" class="ranking"><li v-for="(item,index) in hot.data" :key="item.productId"><span class="rank">{{ String(index+1).padStart(2,'0') }}</span><el-image :src="item.image" fit="cover" /><div><b>{{ item.name }}</b><p>销量 {{ item.quantity }} 件 · 销售额 ¥{{ money(item.amount) }}</p></div></li></ol>
          <el-empty v-else description="该时段暂无成交花礼" />
        </div>
      </el-card>
    </section>
    <el-card shadow="never" class="slot-card"><template #header><div class="card-title"><b>热门配送时段</b><span>用于安排花艺师与配送员班次</span></div></template><div v-if="data.popularSlots?.length" class="slots"><div v-for="item in data.popularSlots" :key="item.label"><b>{{ item.label }}</b><span>{{ item.orderCount }} 单</span></div></div><el-empty v-else description="暂无配送时段数据" /></el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getFlowerDashboard, getFlowerSalesTrend, getFlowerHotProducts } from '@/api/flower';
import SalesTrendChart from './SalesTrendChart.vue';
import DashboardPeriodFilter from './DashboardPeriodFilter.vue';
import { createDashboardReport, reportDateRange } from './report-query.js';
defineOptions({ name:'FlowerDashboard' });
const router=useRouter(); const loading=ref(false); const data=ref({salesTrend:[],hotProducts:[],popularSlots:[]});
const trendReport = createDashboardReport(getFlowerSalesTrend,reportDateRange());
const hotReport = createDashboardReport(getFlowerHotProducts,{...reportDateRange(),sortBy:'quantity'});
const trend = trendReport.state;
const hot = hotReport.state;
const hotSort = ref('quantity');
const money = value => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
function loadTrend(range = trend.params) { return trendReport.run(range); }
function loadHot(range = hot.params) { return hotReport.run({...range,sortBy:hotSort.value}); }
function goStage(stage){router.push({path:'/flower/orders',query:{stage}});}
async function load(){loading.value=true;try{data.value=await getFlowerDashboard();}catch{/* Request feedback displays the error. */}finally{loading.value=false;}}
onMounted(() => { load(); loadTrend(); loadHot(); });
</script>

<style scoped lang="scss">
.flower-dashboard {
  --dashboard-number-font: Arial, 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  padding: 22px;
}
.flower-dashboard > header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 20px; }
.flower-dashboard h2 { margin: 0; font-family: STSong, SimSun, serif; font-size: 28px; color: var(--flower-deep); }
header p { margin: 8px 0 0; color: #7d8379; }
.metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.metrics article { display: grid; grid-template-rows: 20px 44px 18px; gap: 10px; padding: 22px; border: 1px solid #e7e3d9; border-radius: 10px; background: var(--flower-paper); }
.metric-label, .metrics em { color: #838a80; font-style: normal; line-height: 20px; }
.metrics em { font-size: 12px; line-height: 18px; }
.metric-value { display: flex; align-items: baseline; align-self: center; gap: 4px; min-width: 0; color: var(--flower-deep); font-family: var(--dashboard-number-font); font-size: clamp(26px, 2.25vw, 34px); font-weight: 600; line-height: 40px; font-variant-numeric: lining-nums tabular-nums; letter-spacing: -.7px; white-space: nowrap; }
.metric-value small { font-family: var(--dashboard-number-font); font-size: 15px; font-weight: 500; line-height: 1; letter-spacing: 0; }
.workbench-card { margin-top: 14px; padding: 22px; border-radius: 10px; background: var(--flower-forest); color: #fff; }
.section-title h3 { margin: 0; font-family: STSong, SimSun, serif; font-size: 21px; }
.section-title p { margin: 7px 0 20px; color: #cfd5cb; }
.flow { display: flex; align-items: center; }
.flow button { width: 140px; padding: 13px; border: 0; border-radius: 8px; background: rgba(255,255,255,.09); color: #fff; cursor: pointer; }
.flow button:hover { background: rgba(255,255,255,.16); }
.flow b, .flow span { display: block; }
.flow b { font-family: var(--dashboard-number-font); font-size: 29px; font-weight: 600; line-height: 34px; font-variant-numeric: lining-nums tabular-nums; }
.flow span { margin-top: 5px; }
.flow i { flex: 1; height: 1px; margin: 0 10px; background: #73806d; }
.flow .alert b { color: #ffb8af; }
.panel-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr); gap: 14px; margin-top: 14px; }
.panel-grid > .el-card { min-width: 0; }
.card-title { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.card-title b { font-size: 14px; }
.card-title span { color: #969b93; font-size: 12px; font-weight: 400; }
.ranking-sort { width:130px; flex:none; }
.report-range { margin:-4px 0 10px; color:#66715f; font-family:var(--dashboard-number-font); font-size:12px; line-height:20px; font-variant-numeric:tabular-nums; }
.report-content { min-height:270px; }
.report-error { display:flex; min-height:270px; flex-direction:column; align-items:center; justify-content:center; color:#8b4035; font-size:13px; }
.ranking { margin: 0; padding: 0; list-style: none; }
.ranking li { display: grid; grid-template-columns: 28px 50px minmax(0,1fr); align-items: center; gap: 12px; min-height: 66px; padding: 8px 0; border-bottom: 1px solid #efede7; }
.ranking li:last-child { border: 0; }
.ranking .el-image { width: 50px; height: 50px; border-radius: 5px; }
.ranking li > div { min-width: 0; }
.ranking b { display: block; overflow: hidden; font-size: 14px; font-weight: 600; line-height: 20px; text-overflow: ellipsis; white-space: nowrap; }
.ranking p { margin: 5px 0 0; color: #8d938a; font-family: var(--dashboard-number-font); font-size: 12px; line-height: 18px; font-variant-numeric: lining-nums tabular-nums; }
.rank { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; color: #7e8979; font-family: var(--dashboard-number-font); font-size: 14px; font-weight: 600; font-style: normal; line-height: 28px; font-variant-numeric: lining-nums tabular-nums; }
.slot-card { margin-top: 14px; }
.slots { display: grid; grid-template-columns: repeat(auto-fit,minmax(180px,1fr)); gap: 12px; }
.slots div { display: flex; justify-content: space-between; gap: 12px; padding: 16px; border-radius: 7px; background: #f4f2eb; font-family: var(--dashboard-number-font); font-variant-numeric: lining-nums tabular-nums; }
.slots span { color: #6f786d; }
@media (max-width: 1100px) { .metrics { grid-template-columns: repeat(2,minmax(0,1fr)); } .panel-grid { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .flower-dashboard { padding: 14px; } .flower-dashboard > header { align-items: flex-start; flex-direction: column; } .metrics { gap: 10px; } .metrics article { padding: 16px; } .flow { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; } .flow button { width: 100%; } .flow i { display: none; } }
</style>
