<template>
  <div class="divBox flower-admin-page">
    <div class="page-heading">
      <div><h2>配送设置</h2><p>管理同城配送范围、费用和每日可预约时段。</p></div>
    </div>
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="配送区域" name="zones">
          <div class="toolbar"><el-button v-hasPermi="['admin:flower:delivery:save']" type="primary" @click="openZone()">新增区域</el-button></div>
          <el-table v-loading="loadingZones" :data="zones">
            <el-table-column prop="zoneName" label="区域名称" min-width="140" />
            <el-table-column prop="areaCodes" label="行政区编码" min-width="220" show-overflow-tooltip />
            <el-table-column label="配送费" width="120"><template #default="{ row }">¥{{ money(row.deliveryFee) }}</template></el-table-column>
            <el-table-column label="起送金额" width="130"><template #default="{ row }">¥{{ money(row.minimumAmount) }}</template></el-table-column>
            <el-table-column label="启用" width="100"><template #default="{ row }"><span v-hasPermi="['admin:flower:delivery:status']"><el-switch v-model="row.enabled" @change="toggleZone(row)" /></span></template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button v-hasPermi="['admin:flower:delivery:save']" link type="primary" @click="openZone(row)">编辑</el-button></template></el-table-column>
          </el-table>
          <el-pagination class="pager" layout="total, prev, pager, next" :total="zoneTotal" :page-size="20" @current-change="loadZones" />
        </el-tab-pane>
        <el-tab-pane label="预约时段" name="slots">
          <div class="toolbar"><el-button v-hasPermi="['admin:flower:delivery:save']" type="primary" @click="openSlot()">新增时段</el-button></div>
          <el-table v-loading="loadingSlots" :data="slots">
            <el-table-column label="配送日期" width="130"><template #default="{ row }">{{ dateOnly(row.deliveryDate) }}</template></el-table-column>
            <el-table-column label="配送时段" min-width="170"><template #default="{ row }">{{ timeOnly(row.startTime) }}–{{ timeOnly(row.endTime) }}</template></el-table-column>
            <el-table-column label="接单情况" width="150"><template #default="{ row }"><span :class="{ full: row.bookedCount >= row.capacity }">{{ row.bookedCount }}/{{ row.capacity }}</span></template></el-table-column>
            <el-table-column label="剩余容量" width="110"><template #default="{ row }">{{ Math.max(0, row.capacity - row.bookedCount) }}</template></el-table-column>
            <el-table-column label="截单时间" min-width="180"><template #default="{ row }">{{ dateTime(row.cutoffTime) }}</template></el-table-column>
            <el-table-column label="启用" width="100"><template #default="{ row }"><span v-hasPermi="['admin:flower:delivery:status']"><el-switch v-model="row.enabled" @change="toggleSlot(row)" /></span></template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button v-hasPermi="['admin:flower:delivery:save']" link type="primary" @click="openSlot(row)">编辑</el-button></template></el-table-column>
          </el-table>
          <el-pagination class="pager" layout="total, prev, pager, next" :total="slotTotal" :page-size="20" @current-change="loadSlots" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="zoneDialog" :title="zoneForm.id ? '编辑配送区域' : '新增配送区域'" width="520px">
      <el-form label-position="top">
        <el-form-item label="区域名称"><el-input v-model="zoneForm.zoneName" placeholder="例如：天河核心区" /></el-form-item>
        <el-form-item label="行政区域编码"><el-input v-model="zoneForm.areaCodes" placeholder="多个编码用英文逗号分隔" /></el-form-item>
        <div class="form-grid"><el-form-item label="配送费"><el-input-number v-model="zoneForm.deliveryFee" :min="0" :precision="2" /></el-form-item><el-form-item label="起送金额"><el-input-number v-model="zoneForm.minimumAmount" :min="0" :precision="2" /></el-form-item></div>
      </el-form>
      <template #footer><el-button @click="zoneDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitZone">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="slotDialog" :title="slotForm.id ? '编辑预约时段' : '新增预约时段'" width="580px">
      <el-form label-position="top">
        <el-form-item label="配送日期"><el-date-picker v-model="slotForm.deliveryDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <div class="form-grid"><el-form-item label="开始时间"><el-time-picker v-model="slotForm.startTime" value-format="HH:mm:ss" /></el-form-item><el-form-item label="结束时间"><el-time-picker v-model="slotForm.endTime" value-format="HH:mm:ss" /></el-form-item></div>
        <div class="form-grid"><el-form-item label="接单容量"><el-input-number v-model="slotForm.capacity" :min="1" /></el-form-item><el-form-item label="截单时间"><el-date-picker v-model="slotForm.cutoffTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></div>
      </el-form>
      <template #footer><el-button @click="slotDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitSlot">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/elementPlusFeedback';
import { getDeliveryZones, saveDeliveryZone, setDeliveryZoneEnabled, getDeliverySlots, saveDeliverySlot, setDeliverySlotEnabled } from '@/api/flower';

defineOptions({ name: 'FlowerDelivery' });
const activeTab = ref('zones'); const zones = ref([]); const slots = ref([]); const zoneTotal = ref(0); const slotTotal = ref(0);
const loadingZones = ref(false); const loadingSlots = ref(false); const saving = ref(false); const zoneDialog = ref(false); const slotDialog = ref(false);
const zoneForm = reactive({ id: null, zoneName: '', areaCodes: '', deliveryFee: 0, minimumAmount: 0, enabled: true });
const slotForm = reactive({ id: null, deliveryDate: '', startTime: '', endTime: '', capacity: 10, cutoffTime: '', enabled: true });
const unwrap = (res) => ({ list: res?.list || res?.data || [], total: res?.total || 0 });
async function loadZones(page = 1) { loadingZones.value = true; try { const data = unwrap(await getDeliveryZones({ page, limit: 20 })); zones.value = data.list; zoneTotal.value = data.total; } finally { loadingZones.value = false; } }
async function loadSlots(page = 1) { loadingSlots.value = true; try { const data = unwrap(await getDeliverySlots({ page, limit: 20 })); slots.value = data.list; slotTotal.value = data.total; } finally { loadingSlots.value = false; } }
function openZone(row = {}) { Object.assign(zoneForm, { id: null, zoneName: '', areaCodes: '', deliveryFee: 0, minimumAmount: 0, enabled: true }, row); zoneDialog.value = true; }
function openSlot(row = {}) { Object.assign(slotForm, { id: null, deliveryDate: '', startTime: '', endTime: '', capacity: 10, cutoffTime: '', enabled: true }, row, { deliveryDate: row.deliveryDate ? dateOnly(row.deliveryDate) : '', startTime: row.startTime ? timeOnly(row.startTime) : '', endTime: row.endTime ? timeOnly(row.endTime) : '' }); slotDialog.value = true; }
async function submitZone() { if (!zoneForm.zoneName || !zoneForm.areaCodes) return ElMessage.warning('请填写区域名称和行政区编码'); saving.value = true; try { await saveDeliveryZone({ ...zoneForm }); ElMessage.success('配送区域已保存'); zoneDialog.value = false; loadZones(); } finally { saving.value = false; } }
async function submitSlot() { if (!slotForm.deliveryDate || !slotForm.startTime || !slotForm.endTime || !slotForm.cutoffTime) return ElMessage.warning('请完整填写配送日期和时间'); saving.value = true; try { await saveDeliverySlot({ ...slotForm }); ElMessage.success('预约时段已保存'); slotDialog.value = false; loadSlots(); } finally { saving.value = false; } }
async function toggleZone(row) { try { await setDeliveryZoneEnabled(row.id, row.enabled); } catch { row.enabled = !row.enabled; } }
async function toggleSlot(row) { try { await setDeliverySlotEnabled(row.id, row.enabled); } catch { row.enabled = !row.enabled; } }
const money = (v) => Number(v || 0).toFixed(2); const dateOnly = (v) => v ? String(v).slice(0, 10) : '-'; const timeOnly = (v) => v ? String(v).slice(11, 19) : '-'; const dateTime = (v) => v ? String(v).replace('T', ' ').slice(0, 19) : '-';
onMounted(() => { loadZones(); loadSlots(); });
</script>

<style scoped lang="scss">
.flower-admin-page{padding:20px}.page-heading{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:16px}.page-heading h2{margin:0;color:var(--flower-deep);font-family:STSong,SimSun,serif;font-size:24px}.page-heading p{margin:7px 0 0;color:#7b8177}.toolbar{display:flex;justify-content:flex-end;margin-bottom:14px}.pager{justify-content:flex-end;margin-top:18px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.full{color:var(--flower-accent);font-weight:600}
</style>
