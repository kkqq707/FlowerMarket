<template>
  <div class="divBox flower-admin-page" v-loading="loading">
    <div class="page-heading no-print"><div><el-button link @click="$router.back()">← 返回订单工作台</el-button><h2>订单履约详情</h2></div><div><el-button @click="print('production')">打印制作单</el-button><el-button @click="print('delivery')">打印配送单</el-button></div></div>
    <div v-if="detail.orderNo" class="detail-grid">
      <main>
        <el-card shadow="never" class="block"><template #header><b>商品与花礼</b><span class="order-no">{{ detail.orderNo }}</span></template>
          <div v-for="item in detail.products" :key="item.id" class="product"><el-image :src="item.image" fit="cover" /><div><b>{{ item.productName }}</b><p>{{ item.sku || '默认规格' }} · ¥{{ money(item.price) }} × {{ item.payNum }}</p></div></div>
          <div class="amount">共 {{ detail.totalNum }} 件　实付 <b>¥{{ money(detail.payPrice) }}</b></div>
        </el-card>
        <el-card shadow="never" class="block"><template #header><b>履约进度</b><el-tag effect="plain">{{ stageLabel(detail.currentStage) }}</el-tag></template>
          <el-timeline><el-timeline-item v-for="p in detail.progressList" :key="p.id" :timestamp="dateTime(p.createTime)" placement="top" color="#35402f"><b>{{ stageLabel(p.stage) }}</b><p>{{ p.remark || '进度已更新' }}</p><el-image v-if="p.proofImage" class="proof" :src="p.proofImage" :preview-src-list="[p.proofImage]" /></el-timeline-item></el-timeline>
        </el-card>
      </main>
      <aside>
        <el-card shadow="never" class="block"><template #header><b>送花信息</b></template><dl><dt>订花人</dt><dd>{{ detail.senderName }}　{{ detail.senderPhone }}<el-tag v-if="detail.anonymous" size="small" effect="plain">对收花人匿名</el-tag></dd><dt>收花人</dt><dd>{{ detail.recipientName }}　{{ detail.recipientPhone }}</dd><dt>配送地址</dt><dd>{{ detail.deliveryAddress }}</dd><dt>配送时间</dt><dd>{{ dateOnly(detail.deliveryDate) }}　{{ slotTime(detail) }}</dd><dt>联系要求</dt><dd>{{ detail.contactRecipient ? '配送前联系收花人' : '配送前无需联系收花人' }}</dd><dt>贺卡留言</dt><dd>{{ detail.greetingCard || '无' }}</dd><dt>替换规则</dt><dd>{{ policyLabel(detail.substitutionPolicy) }}</dd><template v-if="detail.courierName"><dt>配送员</dt><dd>{{ detail.courierName }}　{{ detail.courierPhone }}</dd></template><template v-if="detail.deliveryRemark"><dt>配送备注</dt><dd>{{ detail.deliveryRemark }}</dd></template></dl></el-card>
        <el-card v-if="next && canOperate" shadow="never" class="block action-card no-print"><template #header><b>下一步：{{ next.label }}</b></template><el-form label-position="top"><el-form-item v-if="next.proof" label="凭证图片地址"><el-input v-model="action.proofImage" placeholder="上传后粘贴图片地址" /></el-form-item><template v-if="next.courier"><el-form-item label="配送员姓名"><el-input v-model="action.courierName" /></el-form-item><el-form-item label="配送员手机"><el-input v-model="action.courierPhone" maxlength="11" /></el-form-item></template><el-form-item label="操作备注"><el-input v-model="action.remark" type="textarea" :rows="3" /></el-form-item><el-button type="primary" class="submit" :loading="submitting" @click="submitTransition">确认{{ next.label }}</el-button></el-form></el-card>
      </aside>
    </div>
    <section id="flower-print-sheet" class="print-sheet"><h1>花间集 · {{ printMode === 'production' ? '鲜花制作单' : '同城配送单' }}</h1><p class="print-order">订单号：{{ detail.orderNo }}</p><template v-if="printMode === 'production'"><h3>花礼内容</h3><div v-for="item in detail.products" :key="item.id"><p>{{ item.productName }} / {{ item.sku || '默认规格' }} / ×{{ item.payNum }}</p><p>花材：{{ item.flowerMaterials || '按商品标准制作' }}<span v-if="item.stemCount">（{{ item.stemCount }}枝）</span>　包装：{{ item.packaging || '按商品标准包装' }}</p></div><h3>制作要求</h3><p>替换规则：{{ policyLabel(detail.substitutionPolicy) }}</p><p>贺卡：{{ detail.greetingCard || '无' }}</p></template><template v-else><h3>配送信息</h3><p>收花人：{{ detail.recipientName }}　{{ detail.recipientPhone }}</p><p>地址：{{ detail.deliveryAddress }}</p><p>配送时间：{{ dateOnly(detail.deliveryDate) }}　{{ slotTime(detail) }}</p><p>联系要求：{{ detail.contactRecipient ? '配送前联系收花人' : '配送前无需联系收花人' }}</p><p v-if="!detail.anonymous">订花人：{{ detail.senderName }}　{{ detail.senderPhone }}</p><p v-else>订花人：匿名（配送单已隐藏身份）</p></template><div class="sign">经手人：________　完成时间：________</div></section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from '@/utils/elementPlusFeedback';
import { getFlowerOrderDetail, transitionFlowerOrder } from '@/api/flower';
import { checkPermi } from '@/utils/permission';
defineOptions({ name:'FlowerOrderDetail' });
const route=useRoute(); const loading=ref(false); const submitting=ref(false); const detail=ref({ products:[], progressList:[] }); const printMode=ref('production');
const action=reactive({ targetStage:'', proofImage:'', courierName:'', courierPhone:'', remark:'' });
const transitions={ WAIT_ACCEPT:{target:'WAIT_PRODUCTION',label:'接单'}, WAIT_PRODUCTION:{target:'PRODUCING',label:'开始制作'}, PRODUCING:{target:'WAIT_DELIVERY',label:'完成制作',proof:true}, WAIT_DELIVERY:{target:'DELIVERING',label:'开始配送',courier:true}, DELIVERING:{target:'COMPLETED',label:'确认送达',proof:true} };
const labels={WAIT_PAY:'待付款',WAIT_ACCEPT:'待接单',WAIT_PRODUCTION:'待制作',PRODUCING:'制作中',WAIT_DELIVERY:'待配送',DELIVERING:'配送中',COMPLETED:'已完成'};
const next=computed(()=>transitions[detail.value.currentStage]); const permissionByStage={WAIT_ACCEPT:'admin:flower:order:accept',WAIT_PRODUCTION:'admin:flower:order:produce',PRODUCING:'admin:flower:order:produce',WAIT_DELIVERY:'admin:flower:order:deliver',DELIVERING:'admin:flower:order:complete'}; const canOperate=computed(()=>checkPermi([permissionByStage[detail.value.currentStage]])); const stageLabel=(v)=>labels[v]||v||'-'; const money=(v)=>Number(v||0).toFixed(2); const dateOnly=(v)=>v?String(v).slice(0,10):'-'; const dateTime=(v)=>v?String(v).replace('T',' ').slice(0,19):'-'; const timeOnly=(v)=>v?String(v).replace('T',' ').slice(11,16):'-'; const slotTime=(d)=>`${timeOnly(d.slotStartTime)}–${timeOnly(d.slotEndTime)}`; const policyLabel=(v)=>({CONTACT_FIRST:'缺货时先联系',ALLOW_SIMILAR:'允许相近花材替换',NO_SUBSTITUTION:'不可替换'}[v]||v||'缺货时先联系');
async function load(){loading.value=true;try{detail.value=await getFlowerOrderDetail(route.params.orderNo);}finally{loading.value=false;}}
async function submitTransition(){const n=next.value;if(!n)return;if(n.proof&&!action.proofImage)return ElMessage.warning('请填写凭证图片地址');if(n.courier&&(!action.courierName||!/^1\d{10}$/.test(action.courierPhone)))return ElMessage.warning('请填写配送员姓名和正确手机号');await ElMessageBox.confirm(`确定要${n.label}吗？`,'履约确认');submitting.value=true;try{await transitionFlowerOrder(detail.value.orderNo,{...action,targetStage:n.target});ElMessage.success('订单进度已更新');Object.assign(action,{targetStage:'',proofImage:'',courierName:'',courierPhone:'',remark:''});await load();}finally{submitting.value=false;}}
function print(mode){printMode.value=mode;setTimeout(()=>window.print(),30);} onMounted(load);
</script>

<style scoped lang="scss">
.flower-admin-page{padding:20px}.page-heading{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:16px}.page-heading h2{margin:7px 0 0;color:var(--flower-deep);font-family:STSong,SimSun,serif;font-size:24px}.detail-grid{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(320px,1fr);gap:16px}.block{margin-bottom:16px}.block :deep(.el-card__header){display:flex;justify-content:space-between}.order-no{color:#90968d;font-weight:400}.product{display:flex;gap:14px;padding:12px 0;border-bottom:1px solid #eeeae1}.product .el-image{width:70px;height:70px;border-radius:7px}.product p{margin:9px 0;color:#7a8076}.amount{text-align:right;padding-top:18px}.amount b{color:var(--flower-accent);font-size:20px;font-variant-numeric:tabular-nums}dl{display:grid;grid-template-columns:76px 1fr;gap:14px 10px;margin:0}dt{color:#92978f}dd{margin:0;color:var(--flower-deep);line-height:1.55}.proof{width:110px;height:82px;border-radius:6px}.submit{width:100%}.print-sheet{display:none}
@media print{.no-print,.detail-grid{display:none!important}.flower-admin-page{padding:0}.print-sheet{display:block;color:#111;font-size:14px}.print-sheet h1{text-align:center;font-family:STSong,SimSun,serif}.print-order{text-align:center;border-bottom:1px solid #222;padding-bottom:14px}.print-sheet h3{margin-top:24px;border-left:4px solid #35402f;padding-left:9px}.sign{margin-top:60px}}
</style>
