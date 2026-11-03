<template>
  <div class="divBox flower-order-workbench">
    <section class="order-filters" aria-label="订单查询">
      <div class="filter-fields">
        <label>时间口径<el-select v-model="query.timeType" aria-label="时间口径"><el-option label="下单时间" value="created"/><el-option label="支付时间" value="paid"/><el-option label="退款发生时间" value="refund"/><el-option label="退款完成时间" value="refunded"/></el-select></label>
        <label class="date-field">日期范围<el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" :clearable="true" aria-label="订单日期范围"/></label>
        <div class="date-presets"><el-button v-for="item in presets" :key="item.value" :type="preset===item.value?'primary':'default'" :plain="preset===item.value" @click="selectPreset(item.value)">{{ item.label }}</el-button></div>
      </div>
      <div class="filter-fields search-fields">
        <el-input v-model.trim="query.keyword" class="keyword-field" clearable placeholder="订单号 / 收花人 / 手机号" aria-label="订单搜索" @keyup.enter="search"/>
        <el-select v-model="query.refundState" class="refund-field" placeholder="全部退款状态" aria-label="退款状态"><el-option label="全部退款状态" value=""/><el-option label="无退款" value="none"/><el-option label="申请中 / 退款中" value="pending"/><el-option label="部分退款" value="partial"/><el-option label="已退款（含部分）" value="done"/></el-select>
        <el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button>
        <el-button class="export-button" :loading="exporting" :disabled="state.loading || !state.filters" @click="exportOrders">导出筛选结果</el-button>
      </div>
      <div class="query-scope"><span>{{ scopeText }}</span><el-tag v-if="query.salesOnly" closable @close="clearSalesOnly">仅销售额统计订单</el-tag><el-tag v-if="query.stage" closable @close="selectStage('')">{{ selectedStageLabel }}</el-tag><small>{{ timeHelp }}</small></div>
    </section>
    <section class="order-summary" aria-label="当前筛选结果汇总" v-loading="state.loading" :aria-busy="state.loading">
      <div><span>订单总数</span><strong>{{ state.summary.orderCount || 0 }}<small>单</small></strong><em>其中 {{ state.summary.paidCount || 0 }} 单已支付</em></div>
      <div><span>支付金额</span><strong><small>¥</small>{{ money(state.summary.paidAmount) }}</strong><em>含后续发生退款的支付</em></div>
      <div><span>退款中金额</span><strong><small>¥</small>{{ money(state.summary.pendingRefundAmount) }}</strong><em>申请中及退款处理中</em></div>
      <div><span>已退款金额</span><strong><small>¥</small>{{ money(state.summary.refundedAmount) }}</strong><em>已成功退回的金额</em></div>
    </section>
    <section class="order-results" aria-label="订单列表">
      <nav class="order-tabs" aria-label="订单状态"><button v-for="item in orderStages" :key="item.value" :class="{active:query.stage===item.value}" :aria-pressed="query.stage===item.value" @click="selectStage(item.value)">{{ item.label }}<span>{{ stageCount(item.value) }}</span></button></nav>
      <div v-if="query.stage==='PRODUCTION'||query.stage==='DELIVERY'" class="substage-hint">{{ query.stage==='PRODUCTION'?'包含待制作和制作中订单':'包含待配送和配送中订单' }}</div>
      <div v-if="state.error" class="order-error" role="alert">{{ state.error }}<el-button link type="primary" @click="load">重试</el-button></div>
      <el-table v-loading="state.loading" :data="state.rows" empty-text="当前筛选条件下暂无订单" @row-dblclick="openDetail">
        <el-table-column label="订单 / 商品" min-width="230"><template #default="{row}"><div class="order-product"><el-image v-if="row.productImage" :src="row.productImage" fit="cover"/><div><b>{{ row.productName || '花卉订单' }}</b><small>{{ row.orderNo }}</small><small>共 {{ row.totalNum }} 件</small></div></div></template></el-table-column>
        <el-table-column label="收花人" min-width="125"><template #default="{row}">{{ row.recipientName }}<small class="cell-note">{{ row.recipientPhone }}</small></template></el-table-column>
        <el-table-column label="实付 / 已退款" min-width="130" align="right"><template #default="{row}"><span class="amount">¥{{ money(row.paid?row.payPrice:0) }}</span><small class="cell-note" :class="{refunded:Number(row.refundStatus)===2}">已退 ¥{{ money(Number(row.refundStatus)===2?row.refundPrice:0) }}</small></template></el-table-column>
        <el-table-column label="订单状态" width="115"><template #default="{row}"><el-tag effect="plain" :type="row.orderState==='COMPLETED'?'success':['CLOSED','CANCELLED'].includes(row.orderState)?'info':'warning'">{{ stageLabels[row.orderState||row.currentStage] || row.currentStage }}</el-tag></template></el-table-column>
        <el-table-column label="退款状态" width="115"><template #default="{row}"><el-tag :type="[1,3].includes(Number(row.refundStatus))?'warning':Number(row.refundStatus)===2?'danger':'info'" effect="light">{{ refundLabel(row) }}</el-tag></template></el-table-column>
        <el-table-column :label="timeLabel" min-width="160"><template #default="{row}">{{ dateTime(row[query.timeType==='paid'?'payTime':query.timeType==='refund'?'refundApplyTime':query.timeType==='refunded'?'refundCompletedTime':'createTime']) }}<small v-if="query.timeType==='refund'&&row.refundCompletedTime" class="cell-note">退回 {{ dateTime(row.refundCompletedTime) }}</small></template></el-table-column>
        <el-table-column label="配送日期" width="115"><template #default="{row}">{{ String(row.deliveryDate||'').slice(0,10)||'—' }}</template></el-table-column>
        <el-table-column label="操作" width="105" fixed="right"><template #default="{row}"><el-button link type="primary" @click="openDetail(row)">{{ [1,3].includes(Number(row.refundStatus))?'处理售后':'查看 / 处理' }}</el-button></template></el-table-column>
      </el-table>
      <div class="order-footer"><span>按北京时间查询 · 汇总包含当前筛选的全部结果</span><el-pagination layout="total, prev, pager, next" :total="state.total" :page-size="query.limit" v-model:current-page="query.page" @current-change="load"/></div>
    </section>
  </div>
</template>
<script setup>
import { computed, ref, reactive, watch } from 'vue';
import { useRoute,useRouter } from 'vue-router';
import { ElMessage } from '@/utils/elementPlusFeedback';
import { getFlowerOrders,getFlowerOrderSummary } from '@/api/flower';
import { orderStages,stageLabels,orderQueryFromRoute,validateOrderQuery,shanghaiToday,refundLabel,orderCsv,createOrderLoader } from './order-query.js';
defineOptions({name:'FlowerOrders'});
const route=useRoute(),router=useRouter(),query=reactive(orderQueryFromRoute(route.query)),dates=ref([]),exporting=ref(false);
const loader=createOrderLoader(getFlowerOrders,getFlowerOrderSummary),state=loader.state;
const presets=[{value:'today',label:'今天'},{value:'yesterday',label:'昨天'},{value:'week',label:'近7天'},{value:'all',label:'全部日期'}];
const preset=computed(()=>{const a=dates.value||[];if(!a.length)return 'all';const today=shanghaiToday();const ago=n=>{const d=new Date(today+'T00:00:00Z');d.setUTCDate(d.getUTCDate()-n);return d.toISOString().slice(0,10);};if(a[0]===today&&a[1]===today)return 'today';if(a[0]===ago(1)&&a[1]===a[0])return 'yesterday';return a[0]===ago(6)&&a[1]===today?'week':'';});
const selectedStageLabel=computed(()=>orderStages.find(s=>s.value===query.stage)?.label||stageLabels[query.stage]||query.stage);
const scopeText=computed(()=>query.startDate?`${query.startDate} 至 ${query.endDate}`:'全部日期');
const timeLabel=computed(()=>({created:'下单时间',paid:'支付时间',refund:'退款发生时间',refunded:'退款完成时间'}[query.timeType]));
const timeHelp=computed(()=>query.timeType==='refunded'?'按退款成功退回日期查询，不包含处理中金额':query.timeType==='refund'?'包括本期申请退款或成功退回的订单，申请与完成时间分别展示':query.timeType==='paid'?'按支付成功日期查询，保留后续退款状态':'按下单日期查询，包含取消订单及最新退款状态');
const money=v=>Number(v||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2});
const dateTime=v=>v?String(v).replace('T',' ').slice(0,16):'—';
function stageCount(value){const counts=state.summary.stageCounts||{};if(!value)return Object.values(counts).reduce((s,n)=>s+Number(n),0);if(value==='PRODUCTION')return Number(counts.WAIT_PRODUCTION||0)+Number(counts.PRODUCING||0);if(value==='DELIVERY')return Number(counts.WAIT_DELIVERY||0)+Number(counts.DELIVERING||0);return counts[value]||0;}
function load(){return loader.run({...query});}
function syncRoute(){const next={};for(const key of ['stage','keyword','timeType','startDate','endDate','refundState'])if(query[key])next[key]=query[key];if(query.salesOnly)next.salesOnly='true';if(JSON.stringify(next)!==JSON.stringify(route.query))router.replace({path:'/flower/orders',query:next});else load();}
function search(){query.startDate=dates.value?.[0]||'';query.endDate=dates.value?.[1]||'';const error=validateOrderQuery(query);if(error)return ElMessage.warning(error);if(query.refundState&&query.refundState!=='none')query.salesOnly=false;query.page=1;syncRoute();}
function selectStage(value){query.stage=value;if(['AFTER_SALE','CLOSED','CANCELLED','WAIT_PAY'].includes(value))query.salesOnly=false;search();}
function reset(){Object.assign(query,orderQueryFromRoute({}));dates.value=[];syncRoute();}
function clearSalesOnly(){query.salesOnly=false;search();}
function selectPreset(value){const today=shanghaiToday();const previous=days=>{const d=new Date(today+'T00:00:00Z');d.setUTCDate(d.getUTCDate()-days);return d.toISOString().slice(0,10);};dates.value=value==='all'?[]:value==='yesterday'?[previous(1),previous(1)]:value==='week'?[previous(6),today]:[today,today];search();}
function openDetail(row){router.push(`/flower/orders/${encodeURIComponent(row.orderNo)}`);}
async function exportOrders(){if(!state.filters)return ElMessage.warning('请先查询订单');const error=validateOrderQuery(state.filters);if(error)return ElMessage.warning(error);exporting.value=true;try{const snapshot={...state.filters,limit:100};let rows=[];for(let page=1;page<=100;page++){const res=await getFlowerOrders({...snapshot,page});rows.push(...(res.list||[]));if(rows.length>=Number(res.total||0)||!(res.list||[]).length)break;if(page===100)throw new Error('结果超过10000条，请缩小日期范围后导出');}const url=URL.createObjectURL(new Blob([orderCsv(rows)],{type:'text/csv;charset=utf-8;'}));const link=document.createElement('a');link.href=url;link.download=`花店订单-${snapshot.startDate||'全部日期'}-${snapshot.endDate||''}.csv`;document.body.appendChild(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch(error){ElMessage.error(error.message||'导出失败，请重试');}finally{exporting.value=false;}}
watch(()=>route.fullPath,()=>{if(route.path!=='/flower/orders')return;Object.assign(query,orderQueryFromRoute(route.query));dates.value=query.startDate?[query.startDate,query.endDate]:[];const error=validateOrderQuery(query);if(error){state.error=error;state.rows=[];return;}load();},{immediate:true});
</script>
<style scoped lang="scss">
.flower-order-workbench{padding:20px;color:var(--flower-deep);font-variant-numeric:tabular-nums}.order-filters,.order-results{background:#fff;border-radius:10px}.order-filters{padding:20px;margin-bottom:16px}.filter-fields{display:flex;align-items:flex-end;flex-wrap:wrap;gap:12px}.filter-fields label{display:flex;flex-direction:column;gap:7px;color:#687568;font-size:12px}.filter-fields label>.el-select{width:140px}.date-field{flex:0 1 330px;min-width:250px}.date-field :deep(.el-date-editor){width:100%;box-sizing:border-box}.date-presets{display:flex;flex-wrap:wrap;gap:7px}.filter-fields .el-button+.el-button{margin-left:0}.search-fields{margin-top:15px;align-items:center}.keyword-field{flex:1 1 230px;max-width:480px}.refund-field{width:185px}.export-button{margin-left:auto!important}.query-scope{display:flex;align-items:center;flex-wrap:wrap;gap:10px;border-top:1px solid #edf0eb;padding-top:13px;margin-top:16px;font-size:12px;color:#50634c}.query-scope small{font-size:12px;color:#7b8577}.order-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:16px}.order-summary>div{padding:18px 20px;background:var(--flower-paper);border-radius:10px}.order-summary span,.order-summary em{display:block;color:#75816f;font-size:12px;font-style:normal}.order-summary strong{display:flex;align-items:baseline;gap:5px;font:600 28px/1.5 Arial,'Microsoft YaHei',sans-serif;margin:6px 0;white-space:nowrap}.order-summary small{font-size:13px;font-weight:400}.order-results{padding:0 18px}.order-tabs{display:flex;flex-wrap:wrap;gap:6px;padding:15px 0;border-bottom:1px solid #edf0eb;margin-bottom:15px}.order-tabs button{border:0;padding:9px 11px;border-radius:5px;background:transparent;color:#697563;cursor:pointer;font:inherit;font-size:13px}.order-tabs button.active{background:var(--flower-forest);color:#fff}.order-tabs span{margin-left:7px;font:500 12px Arial,sans-serif}.substage-hint{margin-bottom:14px;font-size:12px;color:#75816f}.order-product{display:flex;gap:10px;align-items:center;min-width:0}.order-product .el-image{flex:0 0 45px;width:45px;height:45px;border-radius:5px}.order-product b{font-size:13px;font-weight:500}.order-product small,.cell-note{display:block;color:#879080;font-size:12px;margin-top:4px}.amount{font:600 14px Arial,sans-serif;color:#35452f}.refunded{color:#a85a45}.order-footer{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:15px;padding:18px 0}.order-footer>span{font-size:12px;color:#879080}.order-error{padding:14px;color:#a2503a}.order-tabs button:focus-visible{outline:2px solid #657a5c;outline-offset:2px}
@media(max-width:1000px){.order-summary{grid-template-columns:repeat(2,minmax(0,1fr))}.date-presets{width:100%}}@media(max-width:600px){.flower-order-workbench{padding:12px}.order-filters{padding:15px}.date-field{min-width:100%;flex:1}.order-summary{gap:10px}.order-summary>div{padding:14px}.order-summary strong{font-size:23px}.keyword-field{max-width:100%}.export-button{margin-left:0!important}.order-results{padding:0 12px}.order-footer :deep(.el-pagination){max-width:100%;flex-wrap:wrap}.filter-fields label>.el-select{width:160px}}
</style>
