# “花间集”品牌与商品展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改动 CRMEB 内部技术标识和版权注释的前提下，将小程序与花店管理后台的可见品牌统一为“花间集”，完善登录返回、商品价格对齐、12 款演示商品和本地授权图片素材。

**Architecture:** 新建一个小程序品牌常量模块，所有新增默认品牌展示都从该模块读取；用户提供的 Logo 和名称图分别复制到小程序与后台的本地静态目录。商品价格由结构化价格视图提供整数/小数部分，卡片通过等高 flex 布局统一底部基线；12 款演示商品仍由现有 `flowerDemoProducts` 提供，真实接口数据优先级保持不变。

**Tech Stack:** Vue 3、uni-app、Vite、Element Plus、Node.js `node:test`、H5、微信小程序。

## Global Constraints

- 对外名称固定为“花间集”。
- 用户可见区域不得显示“CRMEB”或“CRMEB-Java”；内部版权注释、`crmebimage` 路径、接口路径、包名和变量名保留。
- 商品图片容器只能包含图片，不得叠加价格、名称、销量、标签或按钮。
- 演示商品不可购买，真实商品继续沿用原 SKU、购物车和购买逻辑。
- 素材不得使用淘宝商品图，不得含水印、价格、第三方平台或品牌字样。
- 图片必须保存到项目本地并在来源清单中记录图库、作者、来源页面、许可页面和获取日期。
- 当前工作区没有 Git 仓库；禁止初始化 Git。各任务使用 SDD 进度文档记录检查点，跳过 commit 命令。

---

## File map

### New files

- `frontend/uni-app/utils/flower-brand.js`：唯一的默认品牌名称和静态资源路径。
- `frontend/uni-app/tests/flower-brand.test.mjs`：登录返回、品牌锁定和可见旧品牌扫描测试。
- `frontend/uni-app/static/images/huajianji/logo.png`：用户提供的圆形 Logo。
- `frontend/uni-app/static/images/huajianji/wordmark.png`：用户提供的“花间集”名称图。
- `frontend/uni-app/static/images/flower-products/product-01.jpg` 至 `product-12.jpg`：12 款本地商品主图。
- `frontend/admin/public/huajianji/logo.png`、`wordmark.png`：后台品牌资源。
- `docs/assets/huajianji-image-sources.md`：图片来源与许可记录。
- `.superpowers/sdd/2026-09-21-huajianji-brand-and-catalog/progress.md`：无 Git 环境下的执行账本。

### Modified files

- `frontend/uni-app/pages/users/login/index.vue`：返回按钮和本地品牌图。
- `frontend/uni-app/pages/index/index.vue`：锁定品牌名和分享标题。
- `frontend/uni-app/manifest.json`：应用名称与描述。
- `frontend/uni-app/api/store.js`：12 款演示商品和本地图路径。
- `frontend/uni-app/utils/flower-product-view.js`：结构化价格输出。
- `frontend/uni-app/tests/flower-product-view.test.mjs`：价格拆分和 12 款商品测试。
- `frontend/uni-app/components/flower-product-card/index.vue`：淘宝式价格层级与等高卡片。
- `frontend/uni-app/components/flower-product-row/index.vue`：一致的结构化价格显示。
- `frontend/uni-app/pages/goods/goods_details/index.vue`：详情价格使用相同组件结构。
- `frontend/admin/src/utils/get-page-title.js`：默认页面标题。
- `frontend/admin/src/layout/logo/index.vue`：本地 Logo 默认值，屏蔽旧远程品牌 Logo。
- `frontend/admin/src/views/login/index.vue`：登录 Logo、名称和默认页脚。
- `frontend/admin/src/views/dashboard/index.vue`：欢迎语。
- `frontend/admin/src/store/modules/themeConfig.js`：默认后台标题。
- `frontend/admin/index.html`：浏览器标题和 favicon；保留 author/copyright 元信息。

---

### Task 1: 品牌资产和图片来源账本

**Files:**
- Create: `frontend/uni-app/static/images/huajianji/logo.png`
- Create: `frontend/uni-app/static/images/huajianji/wordmark.png`
- Create: `frontend/admin/public/huajianji/logo.png`
- Create: `frontend/admin/public/huajianji/wordmark.png`
- Create: `frontend/uni-app/static/images/flower-products/product-01.jpg` … `product-12.jpg`
- Create: `docs/assets/huajianji-image-sources.md`
- Create: `.superpowers/sdd/2026-09-21-huajianji-brand-and-catalog/progress.md`

**Interfaces:**
- Produces: 小程序 `/static/images/huajianji/{logo,wordmark}.png`、商品图 `/static/images/flower-products/product-NN.jpg`；后台 `/huajianji/{logo,wordmark}.png`。

- [ ] **Step 1: 建立执行账本并记录源文件哈希**

记录两张用户图片的绝对来源路径、像素尺寸和 SHA-256；账本首行标记 `Task 1: in progress (no git repository)`。

- [ ] **Step 2: 复制用户品牌资产并验证尺寸**

将圆形花朵图保存为 `logo.png`，横向名称图保存为 `wordmark.png`，分别复制到两个前端的目标目录。不得重新压缩或裁掉花朵、叶片；运行图像读取确认四个目标文件都能解析。

- [ ] **Step 3: 从官方图库页面选择 12 张商品图**

只浏览 Pexels 或 Unsplash 的官方照片详情页，按以下主题各选择一张：白绿洋桔梗、向日葵花束、香槟玫瑰、粉玫瑰、缤纷混合花束、奶油郁金香、红玫瑰、紫色蝴蝶兰、白百合、橙色混合花束、蓝白花篮、开业桌花。每张图必须无水印、无文字、无第三方品牌，主体可安全裁成正方形。

- [ ] **Step 4: 下载、裁切并本地化商品图**

保存为 1200×1200、sRGB、JPEG 质量 88 的 `product-01.jpg` 至 `product-12.jpg`。裁切只能改变构图，不进行内容合成；不保留远程图片 URL 在运行时代码中。

- [ ] **Step 5: 写完整来源与许可记录**

`docs/assets/huajianji-image-sources.md` 每张图包含：本地文件、对应商品、图库、作者、照片详情页 URL、图库许可页 URL、获取日期 `2026-09-21`。文档顶部注明用户提供的两张品牌图由用户授权用于本毕业设计。

- [ ] **Step 6: 验证资产**

运行图像检查，断言 12 张商品图均为 1200×1200、品牌图可解析、运行时代码中不存在 Pexels/Unsplash 图片外链；完成账本 Task 1。

---

### Task 2: 品牌模块、登录返回和小程序可见品牌

**Files:**
- Create: `frontend/uni-app/utils/flower-brand.js`
- Create: `frontend/uni-app/tests/flower-brand.test.mjs`
- Modify: `frontend/uni-app/pages/users/login/index.vue`
- Modify: `frontend/uni-app/pages/index/index.vue`
- Modify: `frontend/uni-app/manifest.json`

**Interfaces:**
- Produces: `FLOWER_BRAND`, `goBackFromFlowerLogin()`。

- [ ] **Step 1: 写品牌和返回行为失败测试**

```js
test('locks customer-facing defaults to 花间集', () => {
  assert.equal(FLOWER_BRAND.name, '花间集');
  assert.equal(FLOWER_BRAND.logo, '/static/images/huajianji/logo.png');
  assert.equal(FLOWER_BRAND.wordmark, '/static/images/huajianji/wordmark.png');
});

test('login goes back when history exists and otherwise opens home', () => {
  const withHistory = createNavigation(2);
  goBackFromFlowerLogin(withHistory);
  assert.equal(withHistory.backCalls, 1);
  const direct = createNavigation(1);
  goBackFromFlowerLogin(direct);
  assert.equal(direct.homeCalls, 1);
});
```

再读取登录页、首页和 `manifest.json`，断言用户可见默认文本不含 `CRMEB`。

- [ ] **Step 2: 运行测试并确认 RED**

Run: `node --test tests/flower-brand.test.mjs`（工作目录 `frontend/uni-app`）

Expected: FAIL，原因是 `flower-brand.js` 或导出不存在。

- [ ] **Step 3: 实现品牌模块**

```js
export const FLOWER_BRAND = Object.freeze({
  name: '花间集',
  logo: '/static/images/huajianji/logo.png',
  wordmark: '/static/images/huajianji/wordmark.png',
});

export function goBackFromFlowerLogin(navigation) {
  if (navigation.pageCount() > 1) navigation.back();
  else navigation.home();
}
```

- [ ] **Step 4: 改造登录页**

模板首个子元素加入返回按钮，按钮调用 `goBackFromFlowerLogin`；`pageCount` 使用 `getCurrentPages().length`，`back` 使用 `uni.navigateBack({ delta: 1, fail: home })`，`home` 使用 `uni.switchTab({ url: '/pages/index/index' })`。Logo 使用 `FLOWER_BRAND.logo`，名称图使用 `FLOWER_BRAND.wordmark`，图片失败时显示文字“花间集”。

- [ ] **Step 5: 锁定小程序可见品牌**

首页 `brand` 默认值与分享标题改用 `FLOWER_BRAND.name`，不再把远程 `companyName/siteName` 作为可见品牌默认值；`manifest.json` 的 `name` 和 `description` 改为“花间集”和“花间集同城鲜花商城”。

- [ ] **Step 6: 运行测试并确认 GREEN**

Run: `node --test tests/flower-brand.test.mjs`

Expected: PASS。

- [ ] **Step 7: 记录检查点**

账本追加 `Task 2: complete (brand defaults and login return tests pass; no git repository)`。

---

### Task 3: 结构化价格与等高商品卡片

**Files:**
- Modify: `frontend/uni-app/utils/flower-product-view.js`
- Modify: `frontend/uni-app/tests/flower-product-view.test.mjs`
- Modify: `frontend/uni-app/components/flower-product-card/index.vue`
- Modify: `frontend/uni-app/components/flower-product-row/index.vue`
- Modify: `frontend/uni-app/pages/goods/goods_details/index.vue`

**Interfaces:**
- Produces: `splitFlowerPrice(value): { symbol: '¥', integer: string, decimal: string, text: string }`。

- [ ] **Step 1: 写价格拆分失败测试**

```js
test('splits yuan prices into aligned visual parts', () => {
  assert.deepEqual(splitFlowerPrice(198), {
    symbol: '¥', integer: '198', decimal: '.00', text: '¥198.00'
  });
  assert.deepEqual(splitFlowerPrice('62.5'), {
    symbol: '¥', integer: '62', decimal: '.50', text: '¥62.50'
  });
});
```

静态断言卡片图片区仍只有 `<image>`，价格模板在图片区结束标签之后。

- [ ] **Step 2: 运行测试并确认 RED**

Run: `node --test tests/flower-product-view.test.mjs`

Expected: FAIL，`splitFlowerPrice` 未定义。

- [ ] **Step 3: 实现最小价格拆分**

```js
function splitFlowerPrice(value) {
  const fixed = toNonNegativeNumber(value).toFixed(2);
  const [integer, fraction] = fixed.split('.');
  return { symbol: '¥', integer, decimal: `.${fraction}`, text: `¥${fixed}` };
}
```

`buildFlowerProductView` 增加 `priceParts`，保留 `priceText` 兼容现有调用。

- [ ] **Step 4: 修改三个价格展示位置**

卡片、横向商品行和详情页分别渲染：

```vue
<text class="flower-price-symbol">{{ price.symbol }}</text>
<text class="flower-price-integer">{{ price.integer }}</text>
<text class="flower-price-decimal">{{ price.decimal }}</text>
```

三个部分使用无衬线字体、`font-variant-numeric: tabular-nums`、`display:inline-flex` 和 `align-items:baseline`。商品卡片改为纵向 flex，详情内容区固定两行名称高度，meta 使用 `margin-top:auto`，保证同排价格和按钮对齐。

- [ ] **Step 5: 运行测试并确认 GREEN**

Run: `node --test tests/flower-product-view.test.mjs`

Expected: 所有测试通过。

- [ ] **Step 6: 记录检查点**

账本追加 `Task 3: complete (structured prices and aligned cards; no git repository)`。

---

### Task 4: 十二款演示商品与本地图片映射

**Files:**
- Modify: `frontend/uni-app/api/store.js`
- Modify: `frontend/uni-app/tests/flower-product-view.test.mjs`

**Interfaces:**
- Consumes: `/static/images/flower-products/product-01.jpg` … `product-12.jpg`。
- Produces: `flowerDemoProducts` 长度 12，四个场景各三款。

- [ ] **Step 1: 写商品目录失败测试**

```js
test('provides twelve local demo flowers across four occasions', async () => {
  const { flowerDemoProducts } = await loadFlowerStoreData();
  assert.equal(flowerDemoProducts.length, 12);
  for (const occasion of ['日常', '生日', '纪念日', '祝贺']) {
    assert.equal(flowerDemoProducts.filter(item => item.occasion === occasion).length, 3);
  }
  assert.ok(flowerDemoProducts.every((item, index) =>
    item.demo && item.image === `/static/images/flower-products/product-${String(index + 1).padStart(2, '0')}.jpg`
  ));
});
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `node --test tests/flower-product-view.test.mjs`

Expected: FAIL，当前只有 4 款演示商品。

- [ ] **Step 3: 实现 12 款目录**

按设计文档表格顺序加入 12 款数据；每条包含唯一 `demo-NN` ID、中文名、英文副标题、价格、花材、花语、`occasion`、`recipient`、`colorSeries`、本地图片路径、`sales: 0` 和 `demo: true`。预算分布覆盖四个既有区间。

- [ ] **Step 4: 运行测试并确认 GREEN**

Run: `node --test tests/flower-product-view.test.mjs`

Expected: 所有测试通过，既有筛选测试同步更新为基于明确 ID 的断言。

- [ ] **Step 5: 记录检查点**

账本追加 `Task 4: complete (12 local demo flowers; no git repository)`。

---

### Task 5: 管理后台可见品牌替换

**Files:**
- Modify: `frontend/admin/src/utils/get-page-title.js`
- Modify: `frontend/admin/src/layout/logo/index.vue`
- Modify: `frontend/admin/src/views/login/index.vue`
- Modify: `frontend/admin/src/views/dashboard/index.vue`
- Modify: `frontend/admin/src/store/modules/themeConfig.js`
- Modify: `frontend/admin/index.html`
- Test: `frontend/uni-app/tests/flower-brand.test.mjs`

**Interfaces:**
- Consumes: 后台静态资源 `/huajianji/logo.png`、`/huajianji/wordmark.png`。

- [ ] **Step 1: 扩展可见品牌扫描测试并确认 RED**

测试读取上述后台文件，剔除源码注释和 `<meta name="author|copyright">` 后，断言可见模板/运行时默认值不包含 `/CRMEB(?:-Java| Java)?/i`，并断言页面标题默认值为“花间集管理后台”。

Run: `node --test tests/flower-brand.test.mjs`（工作目录 `frontend/uni-app`）

Expected: FAIL，命中后台标题、欢迎语或登录默认版本文字。

- [ ] **Step 2: 替换后台运行时默认品牌**

- `get-page-title.js` 默认标题改为“花间集管理后台”，并对 localStorage 中旧品牌值回退到新默认值。
- 登录页 `loginLogo` 默认 `/huajianji/wordmark.png`，接口失败或返回旧品牌图时仍保持本地默认；页脚默认“花间集 · 花店管理后台”，移除可见 `version` 的 CRMEB 默认值。
- 侧栏展开状态使用 `/huajianji/wordmark.png`，折叠状态使用 `/huajianji/logo.png`；不使用远程旧品牌 Logo 作为回退。
- 仪表盘欢迎语改为“欢迎使用花间集花店管理后台，祝您工作愉快！”。
- `themeConfig.globalTitle` 改为“花间集管理后台”。
- `index.html` 的 `<title>` 改为“花间集管理后台”，favicon 指向 `/huajianji/logo.png`；author/copyright 元信息不改。

- [ ] **Step 3: 运行扫描测试并确认 GREEN**

Run: `node --test tests/flower-brand.test.mjs`

Expected: PASS。

- [ ] **Step 4: 构建管理后台**

Run: `npm run build`（工作目录 `frontend/admin`）

Expected: exit 0；不得新增构建错误。

- [ ] **Step 5: 记录检查点**

账本追加 `Task 5: complete (customer-visible admin branding replaced; no git repository)`。

---

### Task 6: 双端构建与浏览器视觉验收

**Files:**
- Create: `.superpowers/sdd/2026-09-21-huajianji-brand-and-catalog/browser-check.py`
- Create: `.superpowers/sdd/2026-09-21-huajianji-brand-and-catalog/*.png`
- Modify: `.superpowers/sdd/2026-09-21-huajianji-brand-and-catalog/progress.md`

**Interfaces:**
- Consumes: Tasks 1–5 的最终页面与资源。
- Produces: 登录页、首页商品区、分类页截图和机器可读检查结果。

- [ ] **Step 1: 运行完整自动测试**

Run: `node --test tests/flower-brand.test.mjs tests/flower-product-view.test.mjs`（工作目录 `frontend/uni-app`）

Expected: 0 failed。

- [ ] **Step 2: 构建 H5 和微信小程序**

Run: `npm run build:h5`

Run: `npm run build:mp-weixin`

Expected: 两条命令均 exit 0 并输出 `DONE Build complete`。

- [ ] **Step 3: 编写并运行浏览器检查**

使用项目现有 Playwright/Edge 检查 375 和 430 宽度：登录页返回按钮可见且最小点击区域 44px；Logo/名称图加载成功；首页和分类页各显示至少 12 个商品；同一商品行价格 baseline 差值不超过 1px；价格整数大于符号/小数字号；商品图片容器文本为空；无水平溢出和破图。

- [ ] **Step 4: 搜描可见旧品牌**

扫描小程序构建产物与后台构建产物的可见文本，允许版权元信息和内部技术路径，禁止页面内容出现 `CRMEB-Java`、`CRMEB Java` 或独立品牌词 `CRMEB`。

- [ ] **Step 5: 独立复核**

复核设计文档、实现计划、来源记录、测试输出、双端构建输出和截图；任何 Critical/Important 问题必须修复并重新验证。

- [ ] **Step 6: 完成账本**

账本追加 `Task 6: complete (independent review approved, tests and all builds passed, responsive screenshots captured; no git repository)`。
