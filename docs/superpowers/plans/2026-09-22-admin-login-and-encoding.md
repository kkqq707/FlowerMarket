# Admin Login and Encoding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复后台菜单乱码，重建轻量花艺品牌登录页，并移除后台用户可见版权页脚。

**Architecture:** 后端在响应过滤器边界统一用 UTF-8 写出 JSON；前端登录页保留业务逻辑但替换首屏结构与资源加载；主布局直接取消版权页脚挂载。

**Tech Stack:** Java 8, Spring Boot 2.2.6, JUnit 4, Vue 3, Element Plus, Vite, SCSS

## Global Constraints

- 不增加运行时依赖。
- 保留登录、验证码、菜单获取与跳转行为。
- 保留“花间集”品牌，移除所有用户可见版权/公司名称页脚。
- 桌面和移动窄屏均可用。

---

### Task 1: UTF-8 响应回归

**Files:**
- Create: `backend/crmeb-admin/src/test/java/com/zbkj/admin/filter/ResponseFilterTest.java`
- Modify: `backend/crmeb-admin/src/main/java/com/zbkj/admin/filter/ResponseFilter.java`

**Interfaces:**
- Consumes: Servlet `FilterChain` 写出的 UTF-8 JSON 字节。
- Produces: 保持内容不变的 UTF-8 响应字节和明确的 UTF-8 响应字符集。

- [ ] 写入一个包含 `运营` 的真实过滤器测试，断言响应字节严格等于 UTF-8 字节。
- [ ] 使用 `-Dfile.encoding=GBK` 运行测试，确认旧实现失败。
- [ ] 将过滤器输出改为 `str.getBytes(StandardCharsets.UTF_8)` 并设置响应字符集。
- [ ] 再次运行测试，确认通过。

### Task 2: 全局版权页脚移除

**Files:**
- Modify: `frontend/admin/src/layout/component/main.vue`
- Modify: `frontend/admin/src/views/login/index.vue`

**Interfaces:**
- Consumes: 现有布局与登录页面。
- Produces: 不再渲染版权/公司名称信息的后台界面。

- [ ] 从主布局移除 `Footers` 组件挂载与导入。
- [ ] 从登录页移除底部公司名称节点和状态。
- [ ] 构建后搜索产物，确认目标公司名称不在用户可见模板中。

### Task 3: 登录页重设计与首屏优化

**Files:**
- Modify: `frontend/admin/src/views/login/index.vue`
- Create: `frontend/admin/public/huajianji/login-flowers.jpg`

**Interfaces:**
- Consumes: 现有表单状态、验证规则、行为验证码与登录动作。
- Produces: 三段式花艺品牌登录页，响应式布局，保留原登录能力。

- [ ] 复制已有本地花材图片到管理端公共资源。
- [ ] 删除 Swiper、粒子动画、远程登录图请求和相关状态。
- [ ] 依据批准构图实现图片区、品牌区和表单区。
- [ ] 保留用户名、密码、显隐密码、验证、加载态与验证码组件。
- [ ] 添加移动端布局、焦点态、错误态、减少动态效果适配。
- [ ] 运行前端生产构建。

### Task 4: 集成验证

**Files:**
- Verify: `frontend/admin/dist/**`
- Verify: running admin API on port 8080

**Interfaces:**
- Consumes: 编译后的前后端。
- Produces: API 字节、桌面截图和移动截图验证证据。

- [ ] 重启管理端 Java 服务以载入编码修复。
- [ ] 登录并请求菜单，核对中文及接口耗时。
- [ ] 在桌面和移动视口检查登录页与后台页脚。
- [ ] 运行设计检测器并处理机械问题。
- [ ] 记录最终验证结果。

