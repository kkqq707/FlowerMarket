<template>
  <div class="flower-material-page">
    <header class="page-heading"><div><h2>花材库存</h2><p>按批次记录鲜度。订单按交付日期优先占用临近到期的合格花材。</p></div><el-button @click="refresh">刷新库存</el-button></header>
    <div class="summary">
      <div><strong>{{ materials.length }}</strong><span>花材品种</span></div>
      <div><strong>{{ availableTotal }}</strong><span>可分配数量</span></div>
      <div><strong>{{ nearExpiryCount }}</strong><span>两天内临期批次</span></div>
      <div><strong>{{ lowStockCount }}</strong><span>低库存花材</span></div>
    </div>
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="花材资料" name="materials">
          <div class="toolbar"><el-button type="primary" v-hasPermi="['admin:flower:material:save']" @click="openMaterial()">新增花材</el-button></div>
          <el-table :data="materials" v-loading="loading" empty-text="暂无花材，请先登记常用花材">
            <el-table-column prop="name" label="花材" min-width="130" />
            <el-table-column prop="unit" label="单位" width="75" />
            <el-table-column prop="freshDays" label="默认仓储保鲜" width="130"><template #default="{row}">{{ row.freshDays }} 天</template></el-table-column>
            <el-table-column prop="displayDays" label="建议观赏期" width="125"><template #default="{row}">{{ row.displayDays ? row.displayDays + ' 天' : '未填写' }}</template></el-table-column>
            <el-table-column label="现存 / 锁定 / 可分配" min-width="230"><template #default="{row}">{{ totals(row.id).current }} / {{ totals(row.id).reserved }} / <b>{{ totals(row.id).available }}</b></template></el-table-column>
            <el-table-column label="预警" width="115"><template #default="{row}"><el-tag v-if="totals(row.id).available < row.lowStockThreshold" type="warning">低于阈值</el-tag><span v-else>正常</span></template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="{row}">{{ row.enabled ? '启用' : '停用' }}</template></el-table-column>
            <el-table-column label="操作" width="105"><template #default="{row}"><el-button link type="primary" @click="openMaterial(row)">编辑</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="批次与保鲜" name="batches">
          <div class="toolbar"><el-select v-model="batchFilter" clearable placeholder="全部花材" style="width:180px"><el-option v-for="item in materials" :key="item.id" :value="item.id" :label="item.name" /></el-select><el-select v-model="freshnessFilter" style="width:145px"><el-option label="全部批次" value="all"/><el-option label="临期（两天内）" value="near"/><el-option label="已过期" value="expired"/></el-select><el-button type="primary" v-hasPermi="['admin:flower:material:receive']" @click="openReceive">批次入库</el-button></div>
          <el-table :data="filteredBatches" empty-text="没有符合条件的批次">
            <el-table-column label="花材" min-width="120"><template #default="{row}">{{ materialName(row.materialId) }}</template></el-table-column>
            <el-table-column prop="batchNo" label="批次号" min-width="160" />
            <el-table-column label="入库日" width="120"><template #default="{row}">{{ dateOnly(row.receivedDate) }}</template></el-table-column>
            <el-table-column label="到期日" width="120"><template #default="{row}">{{ dateOnly(row.expiryDate) }}</template></el-table-column>
            <el-table-column label="现存 / 锁定 / 可分配" min-width="205"><template #default="{row}">{{ row.currentQty }} / {{ row.reservedQty }} / {{ row.currentQty - row.reservedQty }}</template></el-table-column>
            <el-table-column label="鲜度" width="90"><template #default="{row}"><el-tag :type="freshness(row)==='已过期'?'danger':freshness(row)==='临期'?'warning':'success'">{{ freshness(row) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" v-hasPermi="['admin:flower:material:adjust']" @click="openAdjust(row)">盘点</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="花束配方" name="recipe">
          <p class="help">按商品编号维护每束花的材料用量。保存配方后，到商品编辑页启用「批次库存校验」。</p>
          <div class="toolbar recipe-search"><el-input-number v-model="productId" :min="1" :controls="false" placeholder="商品编号"/><el-button @click="loadRecipe">查看配方</el-button></div>
          <div v-if="recipeLoaded" class="recipe-edit">
            <div class="recipe-heading"><strong>{{ productName || '商品 ' + productId }}</strong><span>{{ batchManaged ? '批次管理已启用' : '待配置：仍走原 SKU 库存' }}</span></div>
            <div v-for="(item,index) in recipe" :key="index" class="recipe-line"><el-select v-model="item.materialId" placeholder="选择花材" filterable><el-option v-for="material in materials" :key="material.id" :label="material.name + ' / ' + material.unit" :value="material.id"/></el-select><el-input-number v-model="item.qtyPerItem" :min="1"/><span>每束用量</span><el-button link type="danger" @click="recipe.splice(index,1)">移除</el-button></div>
            <div class="recipe-actions"><el-button @click="recipe.push({materialId:null,qtyPerItem:1})">添加花材</el-button><el-button type="primary" v-hasPermi="['admin:flower:product:save']" :loading="saving" @click="submitRecipe">保存配方</el-button></div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="materialDialog" :title="materialForm.id ? '编辑花材' : '新增花材'" width="500px"><el-form label-position="top"><el-form-item label="花材名称"><el-input v-model.trim="materialForm.name" maxlength="100"/></el-form-item><el-form-item label="计量单位"><el-input v-model.trim="materialForm.unit" placeholder="枝 / 把"/></el-form-item><el-form-item label="默认仓储保鲜天数"><el-input-number v-model="materialForm.freshDays" :min="1"/></el-form-item><el-form-item label="建议观赏天数"><el-input-number v-model="materialForm.displayDays" :min="1"/></el-form-item><el-form-item label="低库存预警阈值"><el-input-number v-model="materialForm.lowStockThreshold" :min="0"/></el-form-item><el-form-item label="启用"><el-switch v-model="materialForm.enabled"/></el-form-item></el-form><template #footer><el-button @click="materialDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitMaterial">保存花材</el-button></template></el-dialog>
    <el-dialog v-model="receiveDialog" title="花材批次入库" width="520px"><el-form label-position="top"><el-form-item label="花材"><el-select v-model="receiveForm.materialId" filterable style="width:100%"><el-option v-for="item in materials" :key="item.id" :value="item.id" :label="item.name"/></el-select></el-form-item><el-form-item label="批次号"><el-input v-model.trim="receiveForm.batchNo"/></el-form-item><el-form-item label="入库日期"><el-date-picker v-model="receiveForm.receivedDate" type="date" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="到期日期（可按实物修改）"><el-date-picker v-model="receiveForm.expiryDate" type="date" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="入库数量"><el-input-number v-model="receiveForm.receivedQty" :min="1"/></el-form-item><el-form-item label="备注"><el-input v-model="receiveForm.remark"/></el-form-item></el-form><template #footer><el-button @click="receiveDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitReceive">确认入库</el-button></template></el-dialog>
    <el-dialog v-model="adjustDialog" title="批次盘点" width="420px"><p>仅调整现存数量；不能低于该批次已锁定的数量。</p><el-input-number v-model="adjustQty" :min="adjusting?.reservedQty || 0"/><template #footer><el-button @click="adjustDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAdjust">保存盘点</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from '@/utils/elementPlusFeedback';
import { getFlowerMaterials, saveFlowerMaterial, getFlowerBatches, receiveFlowerBatch, adjustFlowerBatch, getFlowerRecipe, saveFlowerRecipe, getFlowerProduct } from '@/api/flower';

const route = useRoute();
const activeTab=ref('materials'), materials=ref([]), batches=ref([]), loading=ref(false), saving=ref(false);
const batchFilter=ref(null), freshnessFilter=ref('all'), materialDialog=ref(false), receiveDialog=ref(false), adjustDialog=ref(false);
const materialForm=reactive({id:null,name:'',unit:'枝',freshDays:7,displayDays:5,lowStockThreshold:0,enabled:true});
const receiveForm=reactive({materialId:null,batchNo:'',receivedDate:'',expiryDate:'',receivedQty:1,remark:''});
const adjusting=ref(null),adjustQty=ref(0),productId=ref(Number(route.query.productId)||null),recipe=ref([]),recipeLoaded=ref(false),productName=ref(''),batchManaged=ref(false);
const localDate=(value)=>{const d=new Date(value);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
const today=()=>localDate(new Date());
const dateOnly=(v)=>v?String(v).slice(0,10):'';
function materialName(id){return materials.value.find(item=>item.id===id)?.name||`花材 #${id}`;}
function totals(id){const rows=batches.value.filter(b=>b.materialId===id && dateOnly(b.expiryDate)>=today());return {current:rows.reduce((n,b)=>n+b.currentQty,0),reserved:rows.reduce((n,b)=>n+b.reservedQty,0),available:rows.reduce((n,b)=>n+b.currentQty-b.reservedQty,0)};}
const availableTotal=computed(()=>materials.value.reduce((n,m)=>n+totals(m.id).available,0));
const lowStockCount=computed(()=>materials.value.filter(m=>m.enabled && totals(m.id).available<m.lowStockThreshold).length);
function freshness(row){const day=dateOnly(row.expiryDate),near=new Date();near.setDate(near.getDate()+2);return day<today()?'已过期':day<=localDate(near)?'临期':'新鲜';}
const nearExpiryCount=computed(()=>batches.value.filter(b=>b.currentQty>0 && freshness(b)==='临期').length);
const filteredBatches=computed(()=>batches.value.filter(b=>(!batchFilter.value||b.materialId===batchFilter.value)&&(freshnessFilter.value==='all'||(freshnessFilter.value==='near'&&freshness(b)==='临期')||(freshnessFilter.value==='expired'&&freshness(b)==='已过期'))));
async function refresh(){loading.value=true;try{const [m,b]=await Promise.all([getFlowerMaterials(),getFlowerBatches()]);materials.value=Array.isArray(m)?m:[];batches.value=Array.isArray(b)?b:[];}finally{loading.value=false;}}
function openMaterial(row={}){Object.assign(materialForm,{id:null,name:'',unit:'枝',freshDays:7,displayDays:5,lowStockThreshold:0,enabled:true},row);materialDialog.value=true;}
async function submitMaterial(){if(!materialForm.name||!materialForm.unit)return ElMessage.warning('请填写花材名称和单位');saving.value=true;try{await saveFlowerMaterial({...materialForm});materialDialog.value=false;ElMessage.success('花材已保存');await refresh();}finally{saving.value=false;}}
function openReceive(){Object.assign(receiveForm,{materialId:null,batchNo:'',receivedDate:today(),expiryDate:'',receivedQty:1,remark:''});receiveDialog.value=true;}
watch(()=>[receiveForm.materialId,receiveForm.receivedDate],([materialId,receivedDate])=>{if(!receiveDialog.value||!materialId||!receivedDate)return;const material=materials.value.find(item=>item.id===materialId);if(!material)return;const date=new Date(`${receivedDate}T12:00:00`);date.setDate(date.getDate()+Number(material.freshDays||7));receiveForm.expiryDate=localDate(date);});
async function submitReceive(){if(!receiveForm.materialId||!receiveForm.batchNo||!receiveForm.receivedDate||!receiveForm.expiryDate)return ElMessage.warning('请填写完整的批次资料');saving.value=true;try{await receiveFlowerBatch({...receiveForm});receiveDialog.value=false;ElMessage.success('批次已入库');await refresh();}finally{saving.value=false;}}
function openAdjust(row){adjusting.value=row;adjustQty.value=row.currentQty;adjustDialog.value=true;}
async function submitAdjust(){saving.value=true;try{await adjustFlowerBatch(adjusting.value.id,adjustQty.value);adjustDialog.value=false;ElMessage.success('盘点已保存');await refresh();}finally{saving.value=false;}}
async function loadRecipe(){if(!productId.value)return ElMessage.warning('请输入商品编号');try{const [parts,product]=await Promise.all([getFlowerRecipe(productId.value),getFlowerProduct(productId.value)]);recipe.value=Array.isArray(parts)?parts.map(p=>({materialId:p.materialId,qtyPerItem:p.qtyPerItem})):[];productName.value=product?.storeName||'';batchManaged.value=!!product?.batchManaged;recipeLoaded.value=true;activeTab.value='recipe';}catch{recipeLoaded.value=false;}}
async function submitRecipe(){if(!recipe.value.length||recipe.value.some(p=>!p.materialId||!p.qtyPerItem))return ElMessage.warning('至少配置一种有效花材');saving.value=true;try{await saveFlowerRecipe(productId.value,recipe.value);ElMessage.success('配方已保存；可在商品编辑页启用批次管理');await loadRecipe();}finally{saving.value=false;}}
onMounted(async()=>{await refresh();if(productId.value)loadRecipe();});
</script>

<style scoped>
.flower-material-page{padding:20px;color:#27352b}.page-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:16px;margin-bottom:18px}.page-heading h2{font-family:STSong,SimSun,serif;font-size:26px;margin:0}.page-heading p,.help{color:#667363;margin:8px 0 0}.summary{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:18px}.summary>div{background:#fffefa;padding:18px 22px;border:1px solid #e5e8dd;display:flex;flex-direction:column;gap:5px}.summary strong{font:28px Georgia,serif;color:#35402f}.summary span{font-size:13px;color:#667363}.toolbar{display:flex;justify-content:flex-end;gap:10px;margin-bottom:15px}.recipe-search{justify-content:flex-start;margin-top:18px}.recipe-edit{max-width:680px;padding:20px;background:#f7f4ec}.recipe-heading{display:flex;justify-content:space-between;margin-bottom:16px}.recipe-heading span{color:#667363}.recipe-line{display:flex;align-items:center;gap:12px;margin-bottom:12px}.recipe-line .el-select{width:240px}.recipe-line span{font-size:12px;white-space:nowrap}.recipe-actions{display:flex;justify-content:space-between;margin-top:20px}@media(max-width:850px){.summary{grid-template-columns:repeat(2,1fr)}.page-heading{align-items:flex-start;flex-direction:column}}@media(max-width:540px){.summary{grid-template-columns:1fr 1fr}.recipe-line{flex-wrap:wrap}}
</style>
