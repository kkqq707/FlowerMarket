<template>
  <main class="login-page">
    <!--
    THESIS: A calm flower studio worktable turns administration into an extension of the 花间集 brand.
    OWN-WORLD: Warm ivory paper, sage green, real flowers, editorial spacing, and restrained botanical line work.
    STORY: Flowers introduce the brand, the wordmark establishes trust, and the compact form completes the task.
    FIRST VIEWPORT: One centered three-part worktable with no footer, carousel, particle canvas, or remote decoration.
    FORM: Visible labels, clear focus states, keyboard submission, and a single primary action. Seed key: 3b142eac.
    FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
    -->
    <div class="ambient-copy" aria-hidden="true">
      <span>花开有期</span>
      <span>生意常青</span>
      <i></i>
    </div>
    <div class="ambient-note" aria-hidden="true">把日常 · 过成花的样子</div>
    <span class="petal petal-one" aria-hidden="true"></span>
    <span class="petal petal-two" aria-hidden="true"></span>

    <section class="login-worktable" aria-labelledby="login-title">
      <div class="flower-panel">
        <img
          src="/huajianji/login-bouquet-v2.png"
          width="520"
          height="720"
          alt="粉色花束与绿叶"
          fetchpriority="high"
        />
        <div class="flower-panel-copy" aria-hidden="true">
          <span>以花为礼</span>
          <span>让美好发生</span>
        </div>
      </div>

      <div class="brand-panel">
        <img class="brand-wordmark" src="/huajianji/wordmark-login.png" width="240" height="82" alt="花间集" />
        <h1 id="login-title">花店管理后台</h1>
        <div class="brand-rule" aria-hidden="true"></div>
        <p>用花连接生活 · 让美好常在</p>
        <div class="botanical-mark" aria-hidden="true">
          <span>FLOWERS</span>
          <span>A BRIGHTER TOMORROW</span>
        </div>
      </div>

      <div class="form-panel">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          autocomplete="on"
          label-position="top"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="用户名" prop="account">
            <el-input
              ref="accountRef"
              v-model="loginForm.account"
              :prefix-icon="User"
              placeholder="请输入用户名"
              name="username"
              type="text"
              tabindex="1"
              autocomplete="username"
              aria-label="用户名"
              @blur="onBlurAccount"
            />
          </el-form-item>

          <el-form-item label="密码" prop="pwd">
            <el-input
              :key="passwordType"
              ref="pwdRef"
              v-model="loginForm.pwd"
              :prefix-icon="Lock"
              :type="passwordType"
              placeholder="请输入密码"
              name="password"
              tabindex="2"
              autocomplete="current-password"
              aria-label="密码"
            />
            <button
              class="show-pwd"
              type="button"
              :aria-label="passwordType === 'password' ? '显示密码' : '隐藏密码'"
              @click="showPwd"
            >
              <svg-icon :icon-class="passwordType === 'password' ? 'eye' : 'eye-open'" />
            </button>
          </el-form-item>

          <el-button
            class="login-button"
            :loading="loading"
            type="primary"
            :disabled="disabled"
            @click.prevent="handleLogin"
          >
            登录
          </el-button>
          <p class="security-note">安全登录 · 保护您的账号安全</p>
        </el-form>
        <verifition-verify ref="verifyRef" @success="handlerOnVerSuccess" />
      </div>
    </section>
  </main>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import Cookies from 'js-cookie'
import VerifitionVerify from './verifition/Verify.vue'
import { accountDetectionApi } from '@/api/authInformation'
import { frontDomainApi, getSiteLogoApi, mediaDomainApi } from '@/api/systemConfig'
import { useUserStore } from '@/store/modules/user'
import { useSettingsStore } from '@/store/modules/settings'
import { ElLoading } from 'element-plus'

defineOptions({ name: 'Login' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const FLOWER_HOME = '/flower/dashboard'
const ALLOWED_REDIRECT_PREFIXES = ['/flower', '/store', '/user', '/design', '/operation']

const loginFormRef = ref(null)
const accountRef = ref(null)
const pwdRef = ref(null)
const verifyRef = ref(null)
const loginForm = reactive({ account: '', pwd: '', captchaVO: {} })
const loginRules = reactive({
  account: [{ required: true, trigger: 'blur', message: '请输入用户名' }],
  pwd: [{ required: true, trigger: 'blur', message: '请输入密码' }]
})
const passwordType = ref('password')
const loading = ref(false)
const redirect = ref(undefined)
const otherQuery = ref({})
const disabled = ref(false)
const errorsNumber = ref(0)

watch(
  route,
  (currentRoute) => {
    const query = currentRoute.query
    if (query) {
      redirect.value = query.redirect
      otherQuery.value = getOtherQuery(query)
    }
  },
  { immediate: true }
)

onMounted(() => {
  localStorage.setItem('singleAdminSiteName', '花间集管理后台')
  onBlurAccount()
  if (loginForm.account === '') accountRef.value?.focus()
  else if (loginForm.pwd === '') pwdRef.value?.focus()
})

function handlerOnVerSuccess(repData) {
  loginForm.captchaVO = repData
  success(null, true)
}

async function onBlurAccount() {
  if (loginForm.account) errorsNumber.value = await accountDetectionApi({ account: loginForm.account })
}

async function getUrl() {
  const res = await getSiteLogoApi()
  Cookies.set('logoInfo', JSON.stringify(res))
  frontDomainApi().then((domain) => settingsStore.SET_FrontDomain(domain))
  mediaDomainApi().then((domain) => settingsStore.SET_mediaDomain(domain))
}

function showPwd() {
  passwordType.value = passwordType.value === 'password' ? 'text' : 'password'
  nextTick(() => pwdRef.value?.focus())
}

function handleLogin() {
  loginFormRef.value.validate((valid) => {
    if (!valid) return
    if (Number(errorsNumber.value) > 2) verifyRef.value.show()
    else success(null)
  })
}

function success(params, type) {
  loading.value = true
  const loadingInstance = ElLoading.service({ lock: true, text: '正在登录中…' })
  userStore
    .login(loginForm)
    .then(() => {
      disabled.value = true
      getUrl()
      return userStore.getMenus({ that: null })
    })
    .then(() => router.push({ path: resolvePostLoginPath(), query: otherQuery.value }))
    .catch(async () => {
      await onBlurAccount()
      if (Number(errorsNumber.value) > 2 && !type) await verifyRef.value.show()
      disabled.value = false
    })
    .finally(() => {
      loading.value = false
      loadingInstance.close()
    })
}

function resolvePostLoginPath() {
  const target = typeof redirect.value === 'string' ? redirect.value : ''
  const isAllowed = ALLOWED_REDIRECT_PREFIXES.some((prefix) => target === prefix || target.startsWith(`${prefix}/`))
  return isAllowed ? target : FLOWER_HOME
}

function getOtherQuery(query) {
  return Object.keys(query).reduce((acc, cur) => {
    if (cur !== 'redirect') acc[cur] = query[cur]
    return acc
  }, {})
}
</script>

<style lang="scss" scoped>
:global(body) { margin: 0; background: #f5f1e9; }
:global(::selection) { color: #fff; background: #326746; }

.login-page {
  --forest: #245d3c;
  --ink: #24312a;
  --muted: #788079;
  position: relative;
  display: grid;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 84px 56px;
  overflow: hidden;
  color: var(--ink);
  background: #f7f3ec;
  place-items: center;
  isolation: isolate;
}

.login-page::before,
.login-page::after {
  position: absolute;
  z-index: -1;
  width: 340px;
  height: 340px;
  content: '';
  background: rgba(110, 133, 103, 0.12);
  border-radius: 50%;
}
.login-page::before { top: -220px; left: -110px; }
.login-page::after { right: -180px; bottom: -170px; width: 430px; height: 430px; }

.ambient-copy {
  position: absolute;
  top: 78px;
  left: 5.5vw;
  display: grid;
  gap: 5px;
  color: #768778;
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 17px;
  letter-spacing: 0.34em;
}
.ambient-copy i { width: 28px; margin-top: 10px; border-top: 1px solid #819083; }
.ambient-note {
  position: absolute;
  top: 82px;
  right: 6vw;
  color: rgba(76, 92, 78, 0.48);
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 22px;
  letter-spacing: 0.14em;
  transform: rotate(-7deg);
}
.petal {
  position: absolute;
  z-index: -1;
  width: 48px;
  height: 24px;
  background: rgba(226, 156, 167, 0.32);
  border-radius: 80% 15% 80% 15%;
  transform: rotate(24deg);
}
.petal-one { top: 23%; right: 3vw; }
.petal-two { bottom: 7%; left: 14vw; transform: rotate(-18deg) scale(0.7); }

.login-worktable {
  display: grid;
  grid-template-columns: minmax(330px, 1.08fr) minmax(310px, 0.9fr) minmax(380px, 1.12fr);
  width: min(1300px, 100%);
  min-height: 620px;
  overflow: hidden;
  background: #fffdf9;
  border: 1px solid rgba(80, 92, 82, 0.1);
  border-radius: 16px;
  box-shadow: 0 24px 65px rgba(67, 62, 50, 0.13);
}
.flower-panel { position: relative; min-height: 620px; overflow: hidden; background: #687967; }
.flower-panel::after { position: absolute; inset: 0; content: ''; background: rgba(40, 69, 48, 0.2); }
.flower-panel img { width: 100%; height: 100%; object-fit: cover; object-position: 46% center; }
.flower-panel-copy {
  position: absolute;
  z-index: 1;
  top: 42px;
  left: 30px;
  display: flex;
  gap: 14px;
  color: rgba(255, 255, 255, 0.9);
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 18px;
  letter-spacing: 0.24em;
}
.flower-panel-copy span {
  line-height: 1.4;
  writing-mode: vertical-rl;
}

.brand-panel,
.form-panel { display: flex; flex-direction: column; justify-content: center; }
.brand-panel {
  align-items: center;
  padding: 58px 36px;
  text-align: center;
  border-right: 1px solid #ece8df;
}
.brand-wordmark { width: min(246px, 100%); height: auto; }
.brand-panel h1 {
  margin: 20px 0 0;
  color: #30372f;
  font-family: 'Noto Serif SC', 'Songti SC', 'SimSun', serif;
  font-size: 27px;
  font-weight: 500;
  letter-spacing: 0.15em;
}
.brand-rule { width: 34px; margin: 26px 0; border-top: 2px solid var(--forest); }
.brand-panel p { margin: 0; color: #667068; font-size: 14px; letter-spacing: 0.18em; white-space: nowrap; }
.botanical-mark {
  display: grid;
  gap: 5px;
  margin-top: 78px;
  color: #9ba19b;
  font-family: Georgia, serif;
  font-size: 9px;
  letter-spacing: 0.28em;
}

.form-panel { padding: 70px clamp(48px, 4.4vw, 68px); }
.login-form :deep(.el-form-item) { margin-bottom: 24px; }
.login-form :deep(.el-form-item__label) { padding-bottom: 9px; color: #343b36; font-size: 14px; font-weight: 600; }
.login-form :deep(.el-input__wrapper) {
  min-height: 54px;
  padding: 0 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d9ddd7 inset;
  transition: box-shadow 160ms ease;
}
.login-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #9cac9e inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px rgba(36, 93, 60, 0.85) inset; }
.login-form :deep(.el-input__inner) { padding-right: 32px; font-size: 15px; }
.show-pwd {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: grid;
  width: 28px;
  height: 28px;
  padding: 0;
  color: #7d8780;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 5px;
  place-items: center;
}
.show-pwd:focus-visible { outline: 2px solid var(--forest); outline-offset: 2px; }
.login-button {
  width: 100%;
  height: 55px;
  margin-top: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.2em;
  background: var(--forest);
  border-color: var(--forest);
  border-radius: 8px;
}
.login-button:hover,
.login-button:focus { background: #194d30; border-color: #194d30; }
.security-note { margin: 18px 0 0; color: #929991; font-size: 12px; text-align: center; }

@media (max-width: 980px) {
  .login-page { padding: 64px 28px; }
  .login-worktable { grid-template-columns: minmax(280px, 0.9fr) minmax(390px, 1.1fr); }
  .flower-panel,
  .ambient-copy,
  .ambient-note { display: none; }
}

@media (max-width: 700px) {
  .login-page { align-items: start; min-height: 100dvh; padding: 24px 16px; overflow: auto; }
  .login-worktable { grid-template-columns: 1fr; min-height: 0; border-radius: 12px; }
  .brand-panel { padding: 36px 28px 28px; border-right: 0; border-bottom: 1px solid #ece8df; }
  .brand-wordmark { width: 180px; }
  .brand-panel h1 { margin-top: 12px; font-size: 21px; }
  .brand-rule,
  .brand-panel p,
  .botanical-mark { display: none; }
  .form-panel { padding: 34px 28px 40px; }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after { scroll-behavior: auto !important; transition-duration: 0.01ms !important; }
}
</style>
