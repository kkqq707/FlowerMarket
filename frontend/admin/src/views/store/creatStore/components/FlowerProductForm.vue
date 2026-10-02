<template>
  <section class="flower-product-form">
    <div class="flower-section-title">花礼信息</div>
    <el-alert title="商品图片请只上传鲜花实拍图，不要把价格、促销文字或水印放进图片。" type="success" :closable="false" show-icon />
    <el-row :gutter="24" class="flower-form-grid">
      <el-col :span="12"><el-form-item label="主要花材" required><el-input v-model.trim="model.flowerMaterials" :disabled="disabled" placeholder="如：香槟玫瑰、尤加利叶" /></el-form-item></el-col>
      <el-col :span="12"><el-form-item label="花语"><el-input v-model.trim="model.flowerLanguage" :disabled="disabled" placeholder="如：温柔陪伴，真挚爱意" /></el-form-item></el-col>
      <el-col :span="12"><el-form-item label="适用场景"><el-select v-model="occasionValues" multiple allow-create filterable default-first-option :disabled="disabled" placeholder="选择或输入场景"><el-option v-for="item in occasions" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
      <el-col :span="12"><el-form-item label="赠送对象"><el-select v-model="recipientValues" multiple allow-create filterable default-first-option :disabled="disabled" placeholder="选择或输入对象"><el-option v-for="item in recipients" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
      <el-col :span="12"><el-form-item label="色系"><el-select v-model="colorValues" multiple allow-create filterable default-first-option :disabled="disabled" placeholder="选择或输入色系"><el-option v-for="item in colors" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
      <el-col :span="12"><el-form-item label="花材枝数"><el-input-number v-model="model.stemCount" :min="0" :max="999" :disabled="disabled" /></el-form-item></el-col>
      <el-col :span="12"><el-form-item label="包装方式"><el-input v-model.trim="model.packaging" :disabled="disabled" placeholder="如：韩式雾面纸圆形包装" /></el-form-item></el-col>
      <el-col :span="24"><el-form-item label="养护说明"><el-input v-model.trim="model.careTips" type="textarea" :rows="4" maxlength="300" show-word-limit :disabled="disabled" placeholder="填写醒花、换水、修剪等说明" /></el-form-item></el-col>
      <el-col :span="24"><el-form-item label="花材批次"><el-switch v-model="model.batchManaged" :disabled="disabled || !productId" active-text="启用批次库存校验" inactive-text="待配置（仅检查商品 SKU 库存）" /><div class="batch-hint">启用前先配置配方；订单将同时校验 SKU 与交付日可用花材。<router-link v-if="productId" :to="'/flower/material?productId=' + productId">前往配置花束配方 →</router-link><span v-else>请先保存商品，再配置配方。</span></div></el-form-item></el-col>
    </el-row>
  </section>
</template>

<script setup>
import { computed } from 'vue';
const props = defineProps({ modelValue: { type: Object, required: true }, disabled: Boolean, productId: Number });
const emit = defineEmits(['update:modelValue']);
const model = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) });
const occasions = ['生日祝福', '浪漫告白', '纪念日', '探望慰问', '开业庆典', '日常惊喜'];
const recipients = ['恋人', '朋友', '长辈', '家人', '同事', '自己'];
const colors = ['粉色系', '红色系', '香槟色系', '白绿色系', '紫色系', '橙黄色系'];
function values(field) { return computed({ get: () => model.value[field] ? model.value[field].split(',').filter(Boolean) : [], set: value => { model.value[field] = value.join(','); } }); }
const occasionValues = values('occasion');
const recipientValues = values('recipient');
const colorValues = values('colorSeries');
</script>

<style scoped>
.flower-product-form { max-width: 1080px; padding: 8px 20px 10px; }
.flower-form-grid { margin-top: 24px; }
.flower-product-form :deep(.el-select) { width: 100%; }
.flower-product-form :deep(.el-form-item__label) { width: 90px !important; }
.batch-hint { color:#667363;font-size:12px;line-height:1.6;margin-top:6px; }
.batch-hint a { display:inline-block;margin:0 8px;color:#35402f;text-decoration:underline;text-underline-offset:3px; }
</style>
