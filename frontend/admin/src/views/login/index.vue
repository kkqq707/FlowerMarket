<template>
  <main class="login-page">
    <div class="ambient-copy" aria-hidden="true">
      <svg class="ambient-lettering" viewBox="60 92 155 193" width="155" height="193"><title>花开有期，生意常青 · GOOD FLOWERS GOOD BUSINESS</title><image href="/huajianji/login-lettering-reference.png" width="2225" height="1154" /></svg>
    </div>
    <div class="ambient-note" aria-hidden="true"><svg class="ambient-lettering" viewBox="1880 86 292 166" width="292" height="166"><title>把日常，过成花的样子</title><image href="/huajianji/login-lettering-reference.png" width="2225" height="1154" /></svg></div>
    <span class="petal petal-one" aria-hidden="true"></span>
    <span class="petal petal-two" aria-hidden="true"></span>
    <svg class="ambient-foliage" viewBox="0 0 420 350" fill="none" aria-hidden="true">
      <path d="M310 365C291 260 214 188 166 50M263 284C190 269 125 261 61 200M280 310C326 236 344 155 349 70" stroke="currentColor" stroke-width="7" />
      <g fill="currentColor"><ellipse cx="176" cy="82" rx="19" ry="50" transform="rotate(-28 176 82)" /><ellipse cx="196" cy="142" rx="24" ry="48" transform="rotate(36 196 142)" /><ellipse cx="236" cy="203" rx="26" ry="51" transform="rotate(-40 236 203)" /><ellipse cx="149" cy="242" rx="25" ry="53" transform="rotate(-66 149 242)" /><ellipse cx="77" cy="210" rx="22" ry="48" transform="rotate(-55 77 210)" /><ellipse cx="328" cy="191" rx="25" ry="54" transform="rotate(28 328 191)" /><ellipse cx="347" cy="101" rx="20" ry="44" transform="rotate(-9 347 101)" /></g>
    </svg>

    <section class="login-worktable" aria-labelledby="login-title">
      <div class="flower-panel">
        <img
          src="/huajianji/login-bouquet-reference.png"
          width="520"
          height="720"
          alt="粉色花束与绿叶"
          fetchpriority="high"
        />
        <div class="flower-panel-copy" aria-hidden="true">以花为礼 · 让美好发生</div>
      </div>

      <div class="brand-panel">
        <div class="brand-heading">
        <img class="brand-wordmark" src="/huajianji/wordmark-login.png" width="240" height="82" alt="花间集" />
        <h1 id="login-title">花店管理后台</h1>
        <div class="brand-rule" aria-hidden="true"></div>
        <p>用花连接生活 · 让美好常在</p>
        </div>
        <div class="botanical-mark" aria-hidden="true">
          <svg viewBox="0 0 100 160" fill="none">
            <g stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 154C26 125 37 90 66 46M33 110C21 99 20 83 24 73M43 89C57 82 66 75 72 65M53 69C45 53 47 34 52 18M59 55C74 45 83 27 86 13" />
              <path d="M32 112C13 109 9 91 17 76C31 80 39 96 32 112ZM39 98C46 80 61 77 73 83C71 98 56 106 39 98ZM47 81C28 73 29 52 40 40C54 51 55 68 47 81ZM56 67C61 50 78 47 90 53C83 68 70 74 56 67ZM53 52C38 41 41 20 52 9C64 20 64 40 53 52ZM66 45C66 27 75 14 88 10C91 26 82 40 66 45Z" />
              <path d="M31 108L18 80M42 97L69 85M47 78L40 44M58 66L85 55M53 48L52 14M68 42L85 14" opacity=".55" />
            </g>
          </svg>
          <div>
          <span>FLOWERS</span>
          <span>A BRIGHTER</span>
          <span>TOMORROW</span>
          <i></i>
          </div>
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
          hide-required-asterisk
          @keyup.enter="handleLogin"
        >
          <el-form-item label="用户名" prop="account">
            <el-input
              ref="accountRef"
              v-model="loginForm.account"
              :prefix-icon="User"
              placeholder="用户名"
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
              placeholder="密码"
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
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M2 12C4.7 7.5 8 5.5 12 5.5S19.3 7.5 22 12C19.3 16.5 16 18.5 12 18.5S4.7 16.5 2 12Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" /><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.6" /><path v-if="passwordType === 'text'" d="M3 3L21 21" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" /></svg>
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
          <p class="security-note"><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 3L20 6V12C20 16 16.5 19.5 12 21C7.5 19.5 4 16 4 12V6L12 3Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round" /><path d="M8.5 11.5L11 14L15.5 9.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" /></svg>安全登录 · 保护您的账号安全</p>
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
:global(body) { margin: 0; background: #f7f4ef; }
:global(::selection) { color: #fff; background: #326746; }

.login-page {
  --forest: #2d6044;
  --ink: #303a33;
  position: relative;
  display: grid;
  box-sizing: border-box;
  min-height: 100vh;
  min-height: 100dvh;
  padding: clamp(32px, 8vh, 80px) clamp(36px, 10.8vw, 208px);
  overflow: hidden;
  color: var(--ink);
  background: #f7f4ef;
  place-items: center;
  isolation: isolate;
}
.ambient-copy {
  position: absolute;
  top: 8.5vh;
  left: 3.1vw;
  width: clamp(102px, 6.95vw, 139px);
  pointer-events: none;
  mix-blend-mode: darken;
}
.ambient-lettering { display: block; width: 100%; height: auto; overflow: hidden; }
.ambient-note {
  position: absolute;
  z-index: 1;
  pointer-events: none;
  bottom: calc(50% + clamp(280px, 35.5vh, 356px) + 12px);
  right: clamp(36px, 10.8vw, 208px);
  width: clamp(144px, 9.6vw, 184px);
  mix-blend-mode: darken;
}
.petal { position: absolute; width: 60px; height: 26px; pointer-events: none; background: linear-gradient(155deg, #e7b9b8, #f6e7e0); border-radius: 85% 8% 85% 10%; box-shadow: 3px 5px 7px rgba(157, 119, 109, .08); }
.petal-one { top: 27%; right: 1.2vw; transform: rotate(57deg); filter: blur(1.3px); }
.petal-two { bottom: 11%; left: 13.5vw; transform: rotate(24deg) scale(.7); opacity: .5; }
.ambient-foliage { position: absolute; z-index: 2; right: -100px; bottom: -140px; width: clamp(280px, 25vw, 480px); height: auto; color: #7c865b; opacity: .62; pointer-events: none; filter: blur(8px); transform: rotate(-13deg); }

.login-worktable {
  position: relative;
  display: grid;
  grid-template-columns: 30% 33.5% 36.5%;
  width: min(1500px, 100%);
  height: clamp(560px, 71vh, 712px);
  overflow: hidden;
  background: #fdfcf9;
  border-radius: 7px;
  box-shadow: 0 22px 50px rgba(78, 67, 51, .13);
}
.flower-panel { position: relative; grid-column: 1; width: 116.7%; height: 100%; overflow: hidden; background: #7b8974; clip-path: polygon(0 0, 44% 0, 100% 100%, 0 100%); }
.flower-panel img { display: block; width: 100%; height: 100%; object-fit: cover; object-position: center; }
.flower-panel-copy { position: absolute; top: 44px; left: 32px; width: 26px; height: 314px; color: #e8ebdf; font-family: 'STKaiti', 'KaiTi', serif; font-size: 17px; line-height: 26px; letter-spacing: .32em; white-space: nowrap; writing-mode: vertical-rl; }
.flower-panel-copy::after { position: absolute; top: 100%; left: 50%; width: 1px; height: 74px; margin-top: 16px; content: ''; background: rgba(232, 235, 223, .6); }
.brand-panel { position: relative; grid-column: 2; min-width: 0; }
.brand-heading { position: absolute; top: 23.5%; right: 26px; left: 0; display: flex; flex-direction: column; align-items: center; text-align: center; }
.brand-wordmark { display: block; width: min(300px, 88%); height: auto; }
.brand-panel h1 { margin: 18px 0 0; color: #323a33; font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif; font-size: clamp(24px, 1.65vw, 31px); font-weight: 400; line-height: 1.4; letter-spacing: .13em; white-space: nowrap; }
.brand-rule { width: 36px; margin: 27px 0 29px; border-top: 2px solid #49715a; }
.brand-panel p { margin: 0; color: #7b8478; font-family: 'Microsoft YaHei', sans-serif; font-size: clamp(11px, .85vw, 15px); line-height: 24px; letter-spacing: .18em; white-space: nowrap; }
.botanical-mark { position: absolute; right: 18%; bottom: 9%; display: flex; align-items: flex-end; gap: 12px; color: #a5ac9c; }
.botanical-mark svg { width: 88px; height: 154px; }
.botanical-mark > div { display: grid; gap: 6px; padding-bottom: 4px; font-family: Georgia, serif; font-size: 8px; line-height: 1.6; letter-spacing: .23em; }
.botanical-mark i { width: 19px; margin-top: 8px; border-top: 1px solid #b1b4a7; }
.form-panel { position: relative; grid-column: 3; display: flex; align-items: center; padding: 0 clamp(30px, 2.8vw, 54px) 0 clamp(30px, 3.1vw, 60px); }
.form-panel::before { position: absolute; top: 14%; bottom: 14%; left: 0; width: 1px; content: ''; background: #ecece5; }
.login-form { width: 100%; transform: translateY(24px); }
/* Override the administration theme's compact !important form defaults only here. */
.login-form :deep(.el-form-item) { margin-bottom: 29px !important; }
.login-form :deep(.el-form-item__label) { margin: 0; padding: 0 0 12px !important; color: #3d463e; font-family: 'Microsoft YaHei', sans-serif; font-size: 16px !important; font-weight: 500; line-height: 22px; }
.login-form :deep(.el-input__wrapper) { min-height: 64px; padding: 0 20px; background: #fff; border: 1px solid #dcded8; border-radius: 6px; box-shadow: none; transition: border-color 160ms ease; }
.login-form :deep(.el-input__wrapper:hover) { border-color: #a2b19f; }
.login-form :deep(.el-input__wrapper.is-focus) { border-color: #4b8b6f; box-shadow: none; }
.login-form :deep(.el-input__inner) { height: 62px; padding: 0 30px 0 12px; color: #35483b; font-family: 'Microsoft YaHei', sans-serif; font-size: 17px; caret-color: #3c7d5b; }
.login-form :deep(.el-input__inner::placeholder) { color: #92988f; }
.login-form :deep(input:-webkit-autofill) { -webkit-text-fill-color: #35483b; -webkit-box-shadow: 0 0 0 1000px #fff inset; caret-color: #3c7d5b; }
.login-form :deep(.el-input__prefix-inner .el-icon) { color: #8c958d; font-size: 23px; }
.show-pwd { position: absolute; z-index: 1; top: auto; right: 15px; bottom: 15px; display: grid; width: 32px; height: 32px; padding: 0; color: #879187; cursor: pointer; background: transparent; border: 0; border-radius: 4px; place-items: center; }
.show-pwd svg { width: 22px; height: 22px; }
.show-pwd:focus-visible { outline: 2px solid var(--forest); outline-offset: 2px; }
.login-button { width: 100%; height: 63px; margin-top: 10px; padding-left: 5px; color: #fff; font-size: 19px !important; font-weight: 500 !important; letter-spacing: .3em; background: var(--forest); border-color: var(--forest); border-radius: 5px; }
.login-button:hover, .login-button:focus-visible { background: #214d35; border-color: #214d35; }
.login-button:disabled { background: #7b9580; border-color: #7b9580; }
.security-note { display: flex; align-items: center; justify-content: center; gap: 10px; margin: 30px 0 0; color: #858d80; font-size: clamp(10px, .78vw, 14px); line-height: 22px; white-space: nowrap; }
.security-note svg { width: 21px; height: 21px; flex: none; color: #8a9486; }

@media (max-width: 1180px) {
  .login-page { padding: 60px 40px; }
  .ambient-copy { top: 18px; left: 40px; width: 94px; max-height: 50px; overflow: hidden; }
  .ambient-note { right: 40px; width: 144px; }
  .brand-heading { right: 16px; }
  .brand-panel h1 { font-size: 23px; }
  .brand-panel p { font-size: 11px; letter-spacing: .08em; }
  .botanical-mark { right: 14%; gap: 4px; }
  .botanical-mark svg { width: 70px; height: 120px; }
  .botanical-mark > div { font-size: 7px; }
  .form-panel { padding: 0 26px; }
  .login-form :deep(.el-input__wrapper) { padding: 0 14px; }
}
@media (max-width: 980px) {
  .login-page { padding: 48px 32px; }
  .login-worktable { grid-template-columns: minmax(0, .9fr) minmax(0, 1.1fr); height: 580px; }
  .flower-panel { display: none; }
  .ambient-note { bottom: calc(50% + 302px); right: 32px; }
  .brand-panel { grid-column: 1; }
  .brand-heading { right: 12px; left: 16px; }
  .form-panel { grid-column: 2; padding: 0 36px; }
  .ambient-foliage { right: -130px; bottom: -170px; }
}
@media (max-width: 640px) {
  .login-page { min-height: 100dvh; padding: 40px 20px; overflow: hidden; }
  .ambient-copy, .ambient-note, .ambient-foliage, .petal { display: none; }
  .login-worktable { display: flex; flex-direction: column; width: 100%; height: auto; min-height: 0; border-radius: 8px; }
  .brand-panel { padding: 42px 24px 30px; }
  .brand-heading { position: static; }
  .brand-wordmark { width: 210px; max-width: 90%; }
  .brand-panel h1 { margin-top: 12px; font-size: 21px; }
  .brand-rule { margin: 20px 0 16px; }
  .brand-panel p { font-size: 11px; }
  .botanical-mark { display: none; }
  .form-panel { padding: 6px 26px 36px; }
  .form-panel::before { display: none; }
  .login-form { transform: none; }
  .login-form :deep(.el-form-item__label) { font-size: 14px !important; }
  .login-form :deep(.el-input__wrapper) { min-height: 54px; }
  .login-form :deep(.el-input__inner) { height: 54px; font-size: 15px; }
  .show-pwd { bottom: 11px; }
  .login-button { height: 54px; margin-top: 0; font-size: 17px !important; }
  .security-note { margin-top: 22px; font-size: 11px; gap: 7px; }
}
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { transition-duration: .01ms !important; }
}
</style>
