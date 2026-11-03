import { reactive } from 'vue';
export const orderStages = [{value:'',label:'全部订单'},{value:'WAIT_PAY',label:'待支付'},{value:'WAIT_ACCEPT',label:'待接单'},{value:'PRODUCTION',label:'待制作'},{value:'DELIVERY',label:'待配送'},{value:'COMPLETED',label:'已完成'},{value:'CANCELLED',label:'已取消'},{value:'CLOSED',label:'已关闭'},{value:'AFTER_SALE',label:'售后待处理'}];
export const stageLabels = {WAIT_PAY:'待支付',WAIT_ACCEPT:'待接单',WAIT_PRODUCTION:'待制作',PRODUCING:'制作中',WAIT_DELIVERY:'待配送',DELIVERING:'配送中',COMPLETED:'已完成',CANCELLED:'已取消',CLOSED:'已关闭'};
export const shanghaiToday = () => new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
export function orderQueryFromRoute(route = {}) {
  const text=value=>typeof value==='string'?value:'';
  return {stage:text(route.stage),keyword:text(route.keyword),timeType:text(route.timeType)||'created',startDate:text(route.startDate),endDate:text(route.endDate),refundState:text(route.refundState),salesOnly:route.salesOnly==='true'||route.salesOnly===true,page:1,limit:20};
}
export function validateOrderQuery(q) {
  if(!q.startDate&&!q.endDate)return '';
  const valid=value=>/^\d{4}-\d{2}-\d{2}$/.test(value||'')&&Number.isFinite(Date.parse(value))&&new Date(value).toISOString().slice(0,10)===value;
  if(!valid(q.startDate)||!valid(q.endDate)||q.startDate>q.endDate)return '请选择有效的起止日期';
  return (Date.parse(q.endDate)-Date.parse(q.startDate))/86400000>365?'日期范围最多366天':'';
}
export function dashboardOrderRoute(entry,day=shanghaiToday()) {
  const stages={accept:'WAIT_ACCEPT',production:'PRODUCTION',delivery:'DELIVERY',afterSale:'AFTER_SALE'};
  const query=stages[entry]?{stage:stages[entry]}:{timeType:entry==='refund'?'refunded':entry==='sales'?'paid':'created',startDate:day,endDate:day};
  if(entry==='sales')query.salesOnly='true';if(entry==='refund')query.refundState='done';
  return {path:'/flower/orders',query};
}
export function refundLabel(row) {const status=Number(row.refundStatus);return status===1?'申请退款':status===3?'退款中':status===2?(Number(row.refundPrice)<Number(row.payPrice)?'部分退款':'已退款'):'无退款';}
export function orderCsv(rows) {
  const cell=value=>{let text=String(value??'');if(/^[\s]*[=+@\-\t\r]/.test(text))text="'"+text;return '"'+text.replaceAll('"','""')+'"';};const money=v=>Number(v||0).toFixed(2);
  const header=['订单编号','商品','收花人','联系电话','下单时间','支付时间','实付金额','订单状态','退款状态','退款中金额','已退款金额','申请退款时间','退款完成时间'];
  return '\uFEFF'+[header,...rows.map(r=>[r.orderNo,r.productName,r.recipientName,r.recipientPhone,r.createTime,r.payTime,money(r.paid?r.payPrice:0),stageLabels[r.orderState||r.currentStage]||r.currentStage,refundLabel(r),money([1,3].includes(Number(r.refundStatus))?r.refundPrice:0),money(Number(r.refundStatus)===2?r.refundPrice:0),r.refundApplyTime,r.refundCompletedTime])].map(row=>row.map(cell).join(',')).join('\r\n');
}
export function createOrderLoader(getList,getSummary) {
  const state=reactive({rows:[],total:0,summary:{stageCounts:{}},loading:false,error:'',filters:null});let sequence=0;
  async function run(query){const id=++sequence;state.loading=true;state.error='';try{const [list,summary]=await Promise.all([getList({...query}),getSummary({...query})]);if(id!==sequence)return;state.rows=list?.list||[];state.total=list?.total||0;state.summary=summary||{stageCounts:{}};state.filters={...query};}catch(error){if(id===sequence){state.rows=[];state.total=0;state.summary={stageCounts:{}};state.filters=null;state.error=error?.message||'订单加载失败，请重试';}}finally{if(id===sequence)state.loading=false;}}
  return {state,run};
}
