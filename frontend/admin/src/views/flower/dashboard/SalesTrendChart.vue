<template>
  <section class="sales-trend" aria-label="每日销售额和订单数">
    <div class="trend-summary">
      <div class="trend-total"><span>区间销售额</span><strong><small>¥</small>{{ money(totalSales) }}</strong></div>
      <div class="trend-orders"><span>累计订单</span><strong>{{ count(totalOrders) }}<small>单</small></strong></div>
    </div>
    <p class="trend-range">{{ days[0].date }} — {{ days.at(-1).date }}<span>共 {{ days.length }} 天</span></p>
    <div class="daily-scroll" role="region" :aria-label="`${days[0].date}至${days.at(-1).date}每日销售明细`" :tabindex="days.length > 7 ? 0 : undefined">
      <table class="daily-table">
        <caption class="sr-only">每日销售额与订单数，横条长度仅表示销售额</caption>
        <colgroup><col class="date-column" /><col class="bar-column" /><col class="amount-column" /><col class="orders-column" /></colgroup>
        <thead><tr><th scope="col">日期</th><th scope="col" class="bar-column">销售额对比</th><th scope="col" class="daily-amount">销售额（元）</th><th scope="col" class="daily-orders">订单数</th></tr></thead>
        <tbody>
          <tr v-for="day in days" :key="day.date">
            <th scope="row"><button class="day-link" :aria-label="`查看${day.date}的销售订单`" @click="$emit('select-date',day.date)"><time :datetime="day.date">{{ day.label }}</time></button></th>
            <td class="bar-column"><div class="sales-track" aria-hidden="true"><span v-if="day.amount > 0" class="sales-bar" :style="{ width: `${day.amount / maxSales * 100}%` }"></span></div></td>
            <td class="daily-amount">¥{{ money(day.amount) }}</td>
            <td class="daily-orders">{{ count(day.orderCount) }}<span class="order-unit"> 单</span></td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="chart-footnote">{{ totalOrders ? '按支付日期统计，退款订单不计入' : '暂无成交记录，未成交日期按零值展示' }}</p>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import { buildDashboardTrend } from './trend.js';
defineEmits(['select-date']);

const props = defineProps({
  records: { type: Array, default: () => [] },
  startDate: { type: String, default: '' },
  endDate: { type: String, default: '' },
});
const days = computed(() => buildDashboardTrend(props.records, props.endDate || undefined, props.startDate || undefined));
const totalSales = computed(() => days.value.reduce((sum, day) => sum + day.amount, 0));
const totalOrders = computed(() => days.value.reduce((sum, day) => sum + day.orderCount, 0));
const maxSales = computed(() => Math.max(0, ...days.value.map(day => day.amount)));
const money = value => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const count = value => Number(value || 0).toLocaleString('zh-CN');
</script>

<style scoped>
.sales-trend {
  container-type: inline-size;
  min-width: 0;
  color: #35402f;
  font-family: Arial, 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  font-variant-numeric: lining-nums tabular-nums;
}
.day-link{padding:0;border:0;background:none;color:#446b35;text-decoration:none;font:inherit;cursor:pointer}.day-link:focus-visible{outline:2px solid #446b35;outline-offset:3px}
.trend-summary { display: flex; align-items: flex-end; flex-wrap: wrap; gap: 16px 28px; padding-bottom: 20px; }
.trend-total > span, .trend-orders > span { display: block; margin-bottom: 8px; color: #667363; font-size: 12px; line-height: 18px; }
.trend-total strong, .trend-orders strong { display: flex; align-items: baseline; gap: 5px; color: #243129; font-size: clamp(24px, 4cqi, 30px); font-weight: 600; line-height: 36px; white-space: nowrap; }
.trend-total strong { letter-spacing: -.5px; }
.trend-total small { font-size: 15px; font-weight: 500; letter-spacing: 0; }
.trend-orders { padding-left: 27px; border-left: 1px solid #e8ece5; }
.trend-orders strong { color: #44573b; }
.trend-orders small { color: #667363; font-size: 12px; font-weight: 400; }
.trend-range { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 4px 12px; margin: 0 0 10px; color: #667363; font-size: 12px; line-height: 20px; }
.daily-scroll { max-height: 360px; overflow: auto; border-top: 1px solid #e8ece5; border-bottom: 1px solid #e8ece5; scrollbar-width: thin; scrollbar-color: #b6bfae #f4f2eb; }
.daily-scroll:focus-visible { outline: 2px solid #44573b; outline-offset: 3px; }
.daily-table { width: 100%; border-spacing: 0; table-layout: fixed; font-size: 13px; line-height: 20px; }
.date-column { width: 68px; }
.amount-column { width: 142px; }
.orders-column { width: 84px; }
.daily-table thead th { position: sticky; top: 0; z-index: 1; background: #fffefa; color: #667363; font-size: 12px; font-weight: 400; }
.daily-table th, .daily-table td { padding: 10px 8px; text-align: left; }
.daily-table th:first-child { padding-left: 0; }
.daily-table th:last-child, .daily-table td:last-child { padding-right: 0; }
.daily-table tbody th { font-weight: 500; }
.daily-table tbody tr + tr > * { border-top: 1px solid #f0f2ec; }
.daily-table .daily-amount, .daily-table .daily-orders { text-align: right; white-space: nowrap; }
.daily-table tbody .daily-amount { font-weight: 600; }
.daily-orders { color: #44573b; }
.order-unit { color: #667363; font-size: 11px; }
.sales-track { height: 10px; }
.sales-bar { display: block; height: 100%; border-radius: 2px; background: #6f8664; }
.chart-footnote { margin: 10px 0 0; color: #667363; font-size: 11px; line-height: 18px; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
@container (max-width: 420px) {
  .bar-column { display: none; }
  .date-column { width: 60px; }
  .amount-column { width: auto; }
  .orders-column { width: 74px; }
  .trend-summary { gap: 16px; }
  .trend-orders { padding-left: 16px; }
}
</style>
