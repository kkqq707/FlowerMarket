<template>
  <div class="period-filter" role="group" :aria-label="`${label}时间筛选`">
    <div class="period-presets">
      <button v-for="option in presets" :key="option.value" type="button" :aria-pressed="mode === option.value" :class="{ active:mode === option.value }" @click="select(option.value)">{{ option.label }}</button>
    </div>
    <div v-if="mode === 'custom'" class="custom-period">
      <el-date-picker v-model="customDates" type="daterange" unlink-panels value-format="YYYY-MM-DD" format="YYYY-MM-DD" :clearable="false" :disabled-date="disableFuture" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" :aria-label="`${label}日期范围`" />
      <el-button type="primary" :disabled="!!rangeError" :aria-label="`${label}按日期查询`" @click="applyCustom">查询</el-button>
      <span class="period-help" :class="{ 'period-help--error':rangeError }" role="status">{{ rangeError || '最多选择 31 天' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { reportDateRange, reportToday, validateReportRange } from './report-query.js';

const props = defineProps({ label:{type:String,required:true}, range:{type:Object,required:true} });
const emit = defineEmits(['apply']);
const mode = ref('7');
const customDates = ref([props.range.startDate,props.range.endDate]);
const presets = [{value:'7',label:'近 7 天'},{value:'30',label:'近 30 天'},{value:'custom',label:'自定义'}];
const selectedRange = computed(() => ({startDate:customDates.value?.[0],endDate:customDates.value?.[1]}));
const rangeError = computed(() => validateReportRange(selectedRange.value));
function select(value) {
  mode.value = value;
  if (value === 'custom') customDates.value = [props.range.startDate,props.range.endDate];
  else emit('apply',reportDateRange(Number(value)));
}
function applyCustom() { if (!rangeError.value) emit('apply',selectedRange.value); }
function disableFuture(date) {
  const localDate = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
  return localDate > reportToday();
}
</script>

<style scoped>
.period-filter { margin-bottom:18px; font-family:Arial,'Microsoft YaHei',sans-serif; }
.period-presets { display:inline-flex; gap:4px; padding:3px; border-radius:7px; background:#f1f3ee; }
.period-presets button { padding:7px 12px; border:0; border-radius:5px; color:#586250; background:transparent; font:inherit; font-size:12px; line-height:18px; cursor:pointer; }
.period-presets button:hover { background:#e4e9df; }
.period-presets button.active { color:#fff; background:#35452f; }
.period-presets button:focus-visible { outline:2px solid #778b6b; outline-offset:2px; }
.custom-period { display:flex; align-items:center; flex-wrap:wrap; gap:8px; margin-top:10px; }
.custom-period :deep(.el-date-editor) { flex:0 1 270px; width:270px; max-width:100%; }
.period-help { color:#63705c; font-size:12px; }
.period-help--error { color:#b44131; }
@media(max-width:600px) { .custom-period :deep(.el-date-editor) { flex-basis:100%; width:100%; } }
</style>
