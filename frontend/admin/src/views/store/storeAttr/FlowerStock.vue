<template>
  <el-card shadow="never">
    <el-form inline @submit.prevent="search">
      <el-form-item label="商品搜索：">
        <el-input v-model="keyword" placeholder="搜索花名或花材" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="listError" :title="listError" type="error" :closable="false" show-icon class="stock-alert" />
    <el-table v-loading="loading" :data="products" row-key="id">
      <el-table-column prop="id" label="商品 ID" width="100" />
      <el-table-column label="商品图片" width="90">
        <template #default="{ row }"><el-image :src="row.image" fit="cover" style="width: 48px; height: 48px" /></template>
      </el-table-column>
      <el-table-column prop="storeName" label="商品名称" min-width="220" />
      <el-table-column prop="price" label="起售价（元）" width="140" />
      <el-table-column prop="stock" label="总库存" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ row.isShow ? '上架' : '下架' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openStock(row)" v-hasPermi="['admin:product:info']">查看规格与库存</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="没有匹配的花卉商品" /></template>
    </el-table>
    <div class="block">
      <el-pagination v-model:current-page="page" v-model:page-size="limit" :total="total" :page-sizes="[20,40,60]"
        layout="total, sizes, prev, pager, next" background @current-change="load" @size-change="search" />
    </div>
    <el-drawer v-model="drawer" :title="selectedName + ' · 规格与库存'" size="min(900px, 95vw)">
      <el-table v-loading="detailLoading" :data="skus" row-key="id">
        <el-table-column prop="suk" label="规格" min-width="120" />
        <el-table-column prop="price" label="售价（元）" width="120" />
        <el-table-column prop="stock" label="当前库存" width="110" />
        <el-table-column prop="barCode" label="商品编号" min-width="130" />
        <el-table-column label="补货数量" min-width="160" v-if="checkPermi(['admin:product:quick:stock:add'])">
          <template #default="{ row }"><el-input-number v-model="row.addStock" :min="0" :max="999999" :precision="0" controls-position="right" /></template>
        </el-table-column>
      </el-table>
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon class="stock-alert" />
      <p class="stock-note">总库存为各规格库存之和。补货后商城会读取最新库存。</p>
      <el-button type="primary" :loading="saving" :disabled="detailLoading || !skus.length" @click="saveStock"
        v-hasPermi="['admin:product:quick:stock:add']">确认补货</el-button>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue';
import { getFlowerProductList } from '@/api/flower';
import { productDetailApi, stockAddApi } from '@/api/store';
import { checkPermi } from '@/utils/permission';
import { ElMessage } from '@/utils/elementPlusFeedback';

const keyword = ref('');
const page = ref(1);
const limit = ref(20);
const products = ref([]);
const total = ref(0);
const loading = ref(false);
const drawer = ref(false);
const selectedId = ref(0);
const selectedName = ref('');
const skus = ref([]);
const detailLoading = ref(false);
const saving = ref(false);
const listError = ref('');
const detailError = ref('');
let listVersion = 0;
let detailVersion = 0;
let activated = false;

async function load() {
  const version = ++listVersion;
  loading.value = true;
  listError.value = '';
  try {
    const result = await getFlowerProductList({ page: page.value, limit: limit.value, keyword: keyword.value.trim() });
    if (version !== listVersion) return;
    products.value = result.list || [];
    total.value = result.total || 0;
  } catch {
    if (version === listVersion) listError.value = '商品库存暂时无法加载，请点击搜索重试';
  } finally {
    if (version === listVersion) loading.value = false;
  }
}
function search() { page.value = 1; load(); }
function reset() { keyword.value = ''; search(); }
async function openStock(row) {
  const version = ++detailVersion;
  selectedId.value = row.id;
  selectedName.value = row.storeName;
  skus.value = [];
  drawer.value = true;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const result = await productDetailApi(row.id);
    if (version !== detailVersion) return;
    skus.value = (result.attrValue || []).map(sku => ({ ...sku, addStock: 0 }));
  } catch {
    if (version === detailVersion) detailError.value = '规格库存暂时无法加载，请关闭后重新打开';
  } finally {
    if (version === detailVersion) detailLoading.value = false;
  }
}
async function saveStock() {
  if (saving.value) return;
  const changed = skus.value.filter(sku => Number(sku.addStock) > 0);
  if (!changed.length) return ElMessage.warning('请填写至少一个规格的补货数量');
  saving.value = true;
  try {
    await stockAddApi({ id: selectedId.value, attrValueList: changed.map(sku => ({ id: sku.id, addStock: Number(sku.addStock), version: sku.version || 0 })) });
    ElMessage.success('补货成功');
    drawer.value = false;
    await load();
  } catch {
    ElMessage.error('补货未完成，请重新加载库存后重试');
  } finally { saving.value = false; }
}
onMounted(load);
onActivated(() => { if (activated) load(); activated = true; });
</script>

<style scoped>
.stock-note { margin: 20px 0; color: #737b72; font-size: 14px; }
.stock-alert { margin: 12px 0; }
</style>
