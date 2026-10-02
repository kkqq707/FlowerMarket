# 花卉商城论文目标对齐版实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 CRMEB 演示商城收敛为开题报告定义的微信小程序花卉商城，并让管理后台只呈现花卉商品、履约配送、顾客员工、花卉装修与必要系统设置。

**Architecture:** 复用现有 Spring Boot、Vue 3、uni-app 与 CRMEB 通用订单/退款/权限模块；通过可重复执行的 SQL 升级脚本调整菜单、分类、商品、素材与主题数据；通过前端路由白名单和登录页样式保证界面与论文演示范围一致。通用商城源码保留但不再暴露入口，以降低删除公共依赖的风险。

**Tech Stack:** Java 8、Spring Boot、MySQL 8、Redis、Vue 3、Element Plus、Vite、uni-app、Node.js 内置测试运行器。

## Global Constraints

- 登录页按用户提供的目标图布局复刻，不显示任何版权公司文字。
- 商品与分类只允许花卉相关内容，删除无订单引用的非花卉演示数据。
- 装修素材、主题和广告只展示花卉内容，资源必须落在项目内或本地上传目录。
- 菜单必须支持论文要求的经营统计、商品库存、订单履约、配送预约、顾客、员工权限和系统安全。
- 公众号、分销、积分优惠、充值提现、佣金、内容文章、一号通和开发维护入口不在后台出现。
- 数据库变更可重复执行；执行前导出目标表备份，保留恢复路径。

---

### Task 1: 建立论文范围回归测试

**Files:**
- Create: `frontend/admin/tests/flower-thesis-alignment.test.mjs`
- Modify: `frontend/admin/package.json`

**Interfaces:**
- Consumes: 登录页、后台路由和数据库升级脚本源码。
- Produces: `npm run test:flower`，验证版权文字、登录布局标志、允许菜单和花卉数据脚本。

- [ ] 编写登录页、路由白名单、SQL 分类/商品/素材约束的失败测试。
- [ ] 执行测试，确认因功能尚未完成而失败。
- [ ] 后续每个任务完成后重复执行，直到全部通过。

### Task 2: 一比一收敛花间集登录页

**Files:**
- Modify: `frontend/admin/src/views/login/index.vue`
- Reuse: `frontend/admin/public/huajianji/login-flowers.jpg`
- Reuse: `frontend/admin/public/huajianji/wordmark-login.png`

**Interfaces:**
- Consumes: 现有登录接口、验证码流程、用户菜单加载流程。
- Produces: 三栏桌面登录布局、响应式移动布局、无版权页脚的登录界面。

- [ ] 删除目标图不存在的“欢迎回来”区块与多余文案。
- [ ] 调整画布、主卡片、花束图、品牌区、表单区尺寸和间距以匹配参考图。
- [ ] 保留用户名、密码、显隐密码、回车登录、错误次数验证码与可访问性标签。
- [ ] 执行前端回归测试和生产构建。

### Task 3: 收敛后台路由与菜单

**Files:**
- Modify: `frontend/admin/src/router/index.js`
- Modify: `frontend/admin/src/router/modules/flower.js`
- Modify: `frontend/admin/src/router/modules/store.js`
- Modify: `frontend/admin/src/router/modules/user.js`
- Modify: `frontend/admin/src/router/modules/design.js`
- Modify: `frontend/admin/src/router/modules/operation.js`
- Create: `backend/sql/flower-thesis-alignment.sql`

**Interfaces:**
- Consumes: `/admin/getMenus` 返回的数据库菜单和现有视图组件。
- Produces: 经营看板、花卉商品、花店订单、配送管理、顾客与员工、店铺装修、系统设置七类业务入口。

- [ ] 在路由模块中只注册论文范围内页面，并保留商品详情与订单详情隐藏路由。
- [ ] SQL 隐藏无关一级/二级菜单并重命名保留菜单。
- [ ] 保留管理员、角色、权限规则、协议、通知与操作日志相关权限入口。
- [ ] 更新花店运营员角色权限集合。
- [ ] 执行回归测试和前端构建。

### Task 4: 重建花卉分类、商品与装修数据

**Files:**
- Modify: `backend/sql/flower-demo-data.sql`
- Modify: `backend/sql/flower-thesis-alignment.sql`
- Create/Copy: `backend/crmebimage/public/flower/*`

**Interfaces:**
- Consumes: `frontend/uni-app/static/images/flower-products/product-01.jpg` 至 `product-12.jpg`。
- Produces: 九个纯花卉分类、十二个花卉商品、花卉素材目录、花卉主题与广告配置。

- [ ] 先验证现有非花卉商品无订单明细引用。
- [ ] SQL 软删除或清理非花卉分类及商品关联数据，并导入花卉分类。
- [ ] 为十二个花卉商品分配分类并补齐场景、对象、色系、预算与花材属性。
- [ ] 将花卉图片复制到本地上传目录，并在附件库建立花卉素材目录和记录。
- [ ] 将启用主题和广告替换为花卉资源。
- [ ] 执行 SQL 静态测试。

### Task 5: 备份、应用与端到端验证

**Files:**
- Create: `backend/sql/backups/flower-thesis-before-*.sql`
- Verify: 管理后台与小程序 API。

**Interfaces:**
- Consumes: `flower-thesis-alignment.sql` 与 `flower-demo-data.sql`。
- Produces: 已应用的本地数据库、构建产物和验证记录。

- [ ] 导出菜单、分类、商品、附件、主题和广告相关表备份。
- [ ] 应用升级脚本和花卉演示数据。
- [ ] 查询数据库确认只显示花卉分类/商品和论文范围菜单。
- [ ] 执行后台测试、生产构建及后端相关测试。
- [ ] 打开登录页、商品分类、素材管理和经营看板进行浏览器检查。
- [ ] 记录实际通过项、遗留限制与备份路径。
