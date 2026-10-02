<template>
  <div class="flower-care-admin">
    <header class="page-heading"><div><h2>养花指南</h2><p>整理收到花之后的养护方法，已发布内容会出现在小程序。</p></div><el-button type="primary" v-hasPermi="['admin:flower:care:save']" @click="openEditor()">写一篇指南</el-button></header>
    <el-card shadow="never">
      <div class="toolbar"><el-input v-model="keyword" placeholder="搜索文章标题" clearable @keyup.enter="load"/><el-button @click="load">搜索</el-button></div>
      <el-table :data="articles" v-loading="loading" empty-text="还没有养花文章，先写一篇醒花或换水指南吧">
        <el-table-column label="封面" width="100"><template #default="{row}"><el-image :src="row.imageInput" class="cover" fit="cover"/></template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="210"/>
        <el-table-column prop="synopsis" label="摘要" min-width="240" show-overflow-tooltip/>
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.status||row.hide?'info':'success'">{{ row.status||row.hide?'草稿/隐藏':'已发布' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" @click="openEditor(row)">编辑</el-button></template></el-table-column>
      </el-table>
      <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="20" v-model:current-page="page" @current-change="load"/>
    </el-card>
    <el-dialog v-model="editorOpen" :title="form.id?'编辑养花指南':'新增养花指南'" width="min(860px,95vw)" destroy-on-close>
      <el-form label-position="top" class="editor-form"><el-form-item label="标题"><el-input v-model.trim="form.title" maxlength="100"/></el-form-item><el-form-item label="作者"><el-input v-model.trim="form.author" maxlength="50"/></el-form-item><el-form-item label="封面"><div class="cover-field"><el-image v-if="form.imageInput" :src="form.imageInput" class="preview" fit="cover"/><el-button @click="chooseCover">从花卉素材选择</el-button></div></el-form-item><el-form-item label="摘要"><el-input v-model.trim="form.synopsis" type="textarea" :rows="2" maxlength="200" show-word-limit/></el-form-item><el-form-item label="正文"><WangEditor v-model="form.content"/></el-form-item><el-form-item label="发布"><el-switch v-model="form.published" active-text="已发布" inactive-text="草稿"/><span class="publish-note">草稿不会显示在小程序中</span></el-form-item></el-form>
      <template #footer><el-button @click="editorOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存指南</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/elementPlusFeedback';
import WangEditor from '@/components/wangEditor/index.vue';
import { getFlowerCareArticles, getFlowerCareArticle, saveFlowerCareArticle, updateFlowerCareArticle } from '@/api/flower';

const { proxy }=getCurrentInstance();
const articles=ref([]),total=ref(0),page=ref(1),keyword=ref(''),loading=ref(false),saving=ref(false),editorOpen=ref(false);
const form=reactive({id:null,title:'',author:'花间集花艺师',imageInput:'',synopsis:'',content:'',published:false});
async function load(){loading.value=true;try{const result=await getFlowerCareArticles({keyword:keyword.value,page:page.value,limit:20});articles.value=result?.list||[];total.value=result?.total||0;}finally{loading.value=false;}}
async function openEditor(row){Object.assign(form,{id:null,title:'',author:'花间集花艺师',imageInput:'',synopsis:'',content:'',published:false});if(row){const data=await getFlowerCareArticle(row.id);Object.assign(form,{id:data.id,title:data.title,author:data.author,imageInput:data.imageInput,synopsis:data.synopsis,content:data.content,published:!data.status&&!data.hide});}editorOpen.value=true;}
function chooseCover(){proxy.$modalUpload(images=>{if(images?.[0])form.imageInput=images[0].sattDir;},'1','content');}
async function submit(){if(!form.title||!form.author||!form.imageInput||!form.synopsis||!form.content)return ElMessage.warning('请填写标题、作者、封面、摘要和正文');const data={cid:'flower-care',title:form.title,author:form.author,imageInput:form.imageInput,synopsis:form.synopsis,shareTitle:form.title,shareSynopsis:form.synopsis,content:form.content,isBanner:false,isHot:false,status:!form.published,hide:false};saving.value=true;try{if(form.id)await updateFlowerCareArticle(form.id,data);else await saveFlowerCareArticle(data);ElMessage.success('养花指南已保存');editorOpen.value=false;await load();}finally{saving.value=false;}}
onMounted(load);
</script>

<style scoped>
.flower-care-admin{padding:20px;color:#27352b}.page-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:16px;margin-bottom:18px}.page-heading h2{font-family:STSong,SimSun,serif;font-size:26px;margin:0}.page-heading p{color:#667363;margin:8px 0 0}.toolbar{display:flex;gap:10px;margin-bottom:18px;max-width:370px}.cover{width:58px;height:58px}.cover-field{display:flex;align-items:center;gap:14px}.preview{width:90px;height:90px}.editor-form :deep(.w-e-text-container){min-height:220px}.publish-note{margin-left:12px;color:#667363;font-size:12px}.pager{justify-content:flex-end;margin-top:18px}@media(max-width:650px){.page-heading{align-items:flex-start;flex-direction:column}}
</style>
