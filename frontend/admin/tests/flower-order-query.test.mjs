import test from 'node:test';
import assert from 'node:assert/strict';
import { orderQueryFromRoute, validateOrderQuery, dashboardOrderRoute, orderCsv, createOrderLoader } from '../src/views/flower/order/order-query.js';

test('dashboard entries preserve their exact date and status scope', () => {
  const all=orderQueryFromRoute(dashboardOrderRoute('orders','2026-11-03').query);
  assert.equal(all.startDate,'2026-11-03'); assert.equal(all.timeType,'created'); assert.equal(all.stage,'');
  for(const [entry,stage] of [['accept','WAIT_ACCEPT'],['production','PRODUCTION'],['delivery','DELIVERY'],['afterSale','AFTER_SALE']]) {
    const q=orderQueryFromRoute(dashboardOrderRoute(entry,'2026-11-03').query);
    assert.equal(q.stage,stage); assert.equal(q.startDate,''); assert.equal(q.endDate,'');
  }
  assert.equal(dashboardOrderRoute('sales','2026-11-03').query.salesOnly,'true');
  assert.equal(dashboardOrderRoute('refund','2026-11-03').query.timeType,'refunded');
});

test('order dates are paired, real calendar dates and bounded', () => {
  assert.equal(validateOrderQuery({startDate:'',endDate:''}),'');
  assert.equal(validateOrderQuery({startDate:'2026-11-03',endDate:'2026-11-03'}),'');
  for(const [startDate,endDate] of [['2026-02-30','2026-03-01'],['2026-11-03',''],['2026-11-03','2026-11-02'],['2025-01-01','2026-11-03']]) assert.ok(validateOrderQuery({startDate,endDate}));
});

test('export retains refund fields and neutralizes spreadsheet formulas', () => {
  const text=orderCsv([{orderNo:'=cmd()',recipientName:'测试,顾客',recipientPhone:'13800000000',productName:'白玫瑰',paid:true,payPrice:188,refundStatus:2,refundPrice:50,orderState:'COMPLETED'}]);
  assert.ok(text.startsWith('\uFEFF')); assert.ok(text.includes("'=cmd()"));
  assert.ok(text.includes('已退款金额')); assert.ok(text.includes('50.00')); assert.ok(text.includes('部分退款'));
});

test('a stale list response cannot replace the newest route or clear its loading state', async () => {
  const pending=[];
  const loader=createOrderLoader(q=>new Promise(resolve=>pending.push({q,resolve})),async()=>({orderCount:1}));
  const first=loader.run({stage:'WAIT_ACCEPT'}),second=loader.run({stage:'DELIVERY'});
  pending[1].resolve({list:[{orderNo:'latest'}],total:1});await second;
  pending[0].resolve({list:[{orderNo:'old'}],total:2});await first;
  assert.equal(loader.state.rows[0].orderNo,'latest');assert.equal(loader.state.total,1);assert.equal(loader.state.loading,false);
});
