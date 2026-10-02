<template>
  <div class="divBox flower-admin-page">
    <div class="page-heading"><div><h2>订单工作台</h2><p>按花店制作流程集中处理当天订单。</p></div></div>
    <div class="stage-strip">
      <button v-for="item in stages" :key="item.value" :class="{ active: query.stage === item.value }" @click="selectStage(item.value)"><span>{{ item.label }}</span><b v-if="item.count !== null">{{ item.count }}</b></button>
    </div>
    <el-card shadow="never">
      <div class="toolbar"><el-input v-model="query.keyword" clearable placeholder="订单号 / 收花人 / 手机号" style="width:300px" @keyup.enter="loadOrders(1)" /><el-button type="primary" @click="loadOrders(1)">查询</el-button><el-button @click="reset">重置</el-button></div>
      <el-table v-loading="loading" :data="rows" @row-dblclick="openDetail">
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column label="进度" width="120"><template #default="{ row }"><el-tag :type="stageType(row.currentStage)" effect="plain">{{ stageLabel(row.currentStage) }}</el-tag></template></el-table-column>
        <el-table-column label="配送日期" width="130"><template #default="{ row }">{{ String(row.deliveryDate || '').slice(0, 10) }}</template></el-table-column>
        <el-table-column prop="recipientName" label="收花人" width="110" />
        <el-table-column prop="recipientPhone" label="联系电话" width="130" />
        <el-table-column prop="deliveryAddress" label="配送地址" min-width="230" show-overflow-tooltip />
        <el-table-column label="商品" width="80"><template #default="{ row }">{{ row.totalNum }} 件</template></el-table-column>
        <el-table-column label="实付" width="110"><template #default="{ row }"><span class="price">¥{{ Number(row.payPrice || 0).toFixed(2) }}</span></template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">处理订单</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="当前阶段暂无订单" />
      <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="query.limit" :current-page="query.page" @current-change="loadOrders" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getFlowerOrders } from '@/api/flower';
defineOptions({ name: 'FlowerOrders' });
const router = useRouter(); const route = useRoute(); const loading = ref(false); const rows = ref([]); const total = ref(0);
const query = reactive({ stage: String(route.query.stage || ''), keyword: '', page: 1, limit: 20 });
const stages = ref([{ label:'全部订单', value:'', count:null },{ label:'待接单', value:'WAIT_ACCEPT', count:0 },{ label:'待制作', value:'WAIT_PRODUCTION', count:0 },{ label:'制作中', value:'PRODUCING', count:null },{ label:'待配送', value:'WAIT_DELIVERY', count:0 },{ label:'配送中', value:'DELIVERING', count:null },{ label:'已完成', value:'COMPLETED', count:null }]);
const labels = { WAIT_PAY:'待付款', WAIT_ACCEPT:'待接单', WAIT_PRODUCTION:'待制作', PRODUCING:'制作中', WAIT_DELIVERY:'待配送', DELIVERING:'配送中', COMPLETED:'已完成' };
const stageLabel = (v) => labels[v] || v || '-'; const stageType = (v) => v === 'COMPLETED' ? 'success' : ['WAIT_ACCEPT','WAIT_DELIVERY'].includes(v) ? 'warning' : 'primary';
async function loadOrders(page = query.page) { query.page = page; loading.value = true; try { const res = await getFlowerOrders(query); rows.value = res?.list || res?.data || []; total.value = res?.total || 0; } finally { loading.value = false; } }
async function loadCounts() { try { const items = stages.value.filter((s) => s.value); const results = await Promise.all(items.map((s) => getFlowerOrders({ stage:s.value, page:1, limit:1 }))); results.forEach((result,index) => { items[index].count = result?.total || 0; }); } catch { /* 列表仍可正常使用 */ } }
function selectStage(value) { query.stage = value; loadOrders(1); } function reset() { Object.assign(query,{ stage:'', keyword:'', page:1, limit:20 }); loadOrders(); } function openDetail(row) { router.push(`/flower/orders/${row.orderNo}`); }
onMounted(() => { loadOrders(); loadCounts(); });
</script>

<style scoped lang="scss">
.flower-admin-page{padding:20px}.page-heading{margin-bottom:16px}.page-heading h2{margin:0;color:var(--flower-deep);font-family:STSong,SimSun,serif;font-size:24px}.page-heading p{margin:7px 0 0;color:#7b8177}.stage-strip{display:grid;grid-template-columns:repeat(7,1fr);gap:1px;margin-bottom:14px;overflow:hidden;border:1px solid #e8e5dc;border-radius:10px;background:#e8e5dc}.stage-strip button{display:flex;justify-content:center;gap:9px;padding:16px 8px;border:0;background:var(--flower-paper);color:#586056;cursor:pointer}.stage-strip button.active{background:var(--flower-forest);color:#fff}.stage-strip b{min-width:22px;border-radius:12px;background:#efece3;font-size:12px;line-height:22px;color:var(--flower-deep)}.toolbar{display:flex;gap:10px;margin-bottom:16px}.price{font-weight:700;color:var(--flower-accent);font-variant-numeric:tabular-nums}.pager{justify-content:flex-end;margin-top:18px}
</style>
