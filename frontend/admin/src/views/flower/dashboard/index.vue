<template>
  <div class="divBox flower-dashboard" v-loading="loading">
    <header><div><p class="eyebrow">HUA JIAN JI · STORE DESK</p><h2>花间集经营看板</h2><p>今天的订单、履约和热销花礼一览。</p></div><el-button type="primary" @click="$router.push('/flower/orders')">进入订单工作台</el-button></header>
    <section class="metrics">
      <article><span>今日销售额</span><strong><small>¥</small>{{ money(data.todaySales) }}</strong><em>退款订单不计入</em></article>
      <article><span>今日订单</span><strong>{{ data.todayOrders || 0 }}<small> 单</small></strong><em>当前自然日</em></article>
      <article><span>平均客单价</span><strong><small>¥</small>{{ money(data.averageOrderValue) }}</strong><em>实付金额平均值</em></article>
      <article><span>准时送达率</span><strong>{{ money(data.onTimeRate) }}<small>%</small></strong><em>按承诺时段统计</em></article>
    </section>
    <section class="workbench-card">
      <div class="section-title"><div><h3>今日待办</h3><p>从接单到配送，依次完成每一个花礼。</p></div></div>
      <div class="flow"><button @click="goStage('WAIT_ACCEPT')"><b>{{ data.waitAcceptCount || 0 }}</b><span>待接单</span></button><i></i><button @click="goStage('WAIT_PRODUCTION')"><b>{{ data.waitProductionCount || 0 }}</b><span>待制作</span></button><i></i><button @click="goStage('WAIT_DELIVERY')"><b>{{ data.waitDeliveryCount || 0 }}</b><span>待配送</span></button><i></i><button class="alert" @click="$router.push('/order/index')"><b>{{ data.afterSaleCount || 0 }}</b><span>售后待处理</span></button></div>
    </section>
    <section class="panel-grid">
      <el-card shadow="never"><template #header><div class="card-title"><b>近 7 日销售趋势</b><span>金额 / 订单</span></div></template><div v-if="data.salesTrend?.length" class="trend"><div v-for="item in data.salesTrend" :key="item.label" class="trend-col"><div class="bar-wrap"><i :style="{ height: barHeight(item.amount) }"></i></div><b>¥{{ compact(item.amount) }}</b><span>{{ item.label }}</span></div></div><el-empty v-else description="暂无销售数据" /></el-card>
      <el-card shadow="never"><template #header><div class="card-title"><b>热门花礼</b><span>按销量排序</span></div></template><ol v-if="data.hotProducts?.length" class="ranking"><li v-for="(item,index) in data.hotProducts" :key="item.productId"><span class="rank">{{ String(index+1).padStart(2,'0') }}</span><el-image :src="item.image" fit="cover" /><div><b>{{ item.name }}</b><p>{{ item.quantity }} 件 · ¥{{ money(item.amount) }}</p></div></li></ol><el-empty v-else description="暂无热销商品" /></el-card>
    </section>
    <el-card shadow="never" class="slot-card"><template #header><div class="card-title"><b>热门配送时段</b><span>用于安排花艺师与配送员班次</span></div></template><div v-if="data.popularSlots?.length" class="slots"><div v-for="item in data.popularSlots" :key="item.label"><b>{{ item.label }}</b><span>{{ item.orderCount }} 单</span></div></div><el-empty v-else description="暂无配送时段数据" /></el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getFlowerDashboard } from '@/api/flower';
defineOptions({ name:'FlowerDashboard' });
const router=useRouter(); const loading=ref(false); const data=ref({salesTrend:[],hotProducts:[],popularSlots:[]});
const maxSales=computed(()=>Math.max(1,...(data.value.salesTrend||[]).map(i=>Number(i.amount||0)))); const money=(v)=>Number(v||0).toFixed(2); const compact=(v)=>Number(v||0)>=1000?`${(Number(v)/1000).toFixed(1)}k`:Number(v||0).toFixed(0); const barHeight=(v)=>`${Math.max(8,Number(v||0)/maxSales.value*112)}px`;
function goStage(stage){router.push({path:'/flower/orders',query:{stage}});} async function load(){loading.value=true;try{data.value=await getFlowerDashboard();}finally{loading.value=false;}} onMounted(load);
</script>

<style scoped lang="scss">
.flower-dashboard{padding:22px}.flower-dashboard>header{display:flex;justify-content:space-between;align-items:end;margin-bottom:20px}.eyebrow{margin:0 0 5px!important;font-size:11px;letter-spacing:2px;color:#899083}.flower-dashboard h2{margin:0;font-family:STSong,SimSun,serif;font-size:28px;color:var(--flower-deep)}header p{margin:8px 0 0;color:#7d8379}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.metrics article{padding:22px;border:1px solid #e7e3d9;border-radius:10px;background:var(--flower-paper)}.metrics span,.metrics em{display:block;color:#838a80;font-style:normal}.metrics strong{display:block;margin:14px 0 8px;color:var(--flower-deep);font:700 30px/1 Georgia,serif;font-variant-numeric:tabular-nums}.metrics small{font:600 15px/1 'Microsoft YaHei';}.workbench-card{margin-top:14px;padding:22px;border-radius:10px;background:var(--flower-forest);color:#fff}.section-title h3{margin:0;font-family:STSong,SimSun,serif;font-size:21px}.section-title p{margin:7px 0 20px;color:#cfd5cb}.flow{display:flex;align-items:center}.flow button{width:140px;padding:13px;border:0;border-radius:8px;background:rgba(255,255,255,.09);color:#fff;cursor:pointer}.flow button:hover{background:rgba(255,255,255,.16)}.flow b,.flow span{display:block}.flow b{font:700 27px Georgia}.flow span{margin-top:5px}.flow i{flex:1;height:1px;margin:0 10px;background:#73806d}.flow .alert b{color:#ffb8af}.panel-grid{display:grid;grid-template-columns:1.35fr 1fr;gap:14px;margin-top:14px}.card-title{display:flex;justify-content:space-between}.card-title span{color:#969b93;font-size:12px}.trend{display:flex;align-items:end;height:190px;gap:14px}.trend-col{display:flex;flex:1;flex-direction:column;align-items:center;gap:6px;color:#858b82;font-size:11px}.bar-wrap{display:flex;align-items:end;height:115px}.bar-wrap i{display:block;width:24px;border-radius:7px 7px 2px 2px;background:linear-gradient(#82927b,#35402f)}.trend-col b{color:var(--flower-deep);font-size:11px}.ranking{margin:0;padding:0;list-style:none}.ranking li{display:grid;grid-template-columns:28px 50px 1fr;align-items:center;gap:10px;padding:8px 0;border-bottom:1px solid #efede7}.ranking li:last-child{border:0}.ranking .el-image{width:50px;height:50px;border-radius:5px}.ranking p{margin:5px 0 0;color:#8d938a;font-size:12px}.rank{font:italic 600 14px Georgia;color:#7e8979}.slot-card{margin-top:14px}.slots{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.slots div{display:flex;justify-content:space-between;padding:16px;border-radius:7px;background:#f4f2eb}.slots span{color:#6f786d}@media(max-width:1100px){.metrics{grid-template-columns:repeat(2,1fr)}.panel-grid{grid-template-columns:1fr}}
</style>
