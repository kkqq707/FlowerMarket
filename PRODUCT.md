# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

主要用户是花店店主和店员。他们在营业环境中高频使用管理后台，处理商品、订单、配送和经营数据。

## Product Purpose

花间集管理后台用于集中完成花店日常经营工作。成功意味着用户能够快速登录、清楚识别当前任务，并以较少等待完成高频操作。

## Positioning

产品围绕鲜花零售与履约场景组织管理能力，把通用商城后台转化为花店经营工作台。

## Operating Context

用户通常在桌面浏览器中使用系统，可能连续工作较长时间，并在订单高峰期频繁切换商品、订单、配送和经营看板。

## Capabilities and Constraints

- 现有 Vue 3、Element Plus、Pinia 与 Vite 技术栈保持不变。
- 登录流程、行为验证码、权限菜单和登录后跳转逻辑必须保持可用。
- 管理端接口由 Spring Boot 2.2.6 和 JDK 8 提供。
- 页面必须兼容桌面与移动窄屏，不增加新的运行时依赖。

## Brand Commitments

- 产品名称为“花间集”。
- 保留现有花间集品牌标识。
- 品牌表达自然、克制、可信，不能妨碍后台操作效率。

## Evidence on Hand

- 品牌 Logo：`frontend/admin/public/huajianji/logo.png`
- 品牌文字标识：`frontend/admin/public/huajianji/wordmark.png`
- 现有管理后台与花店经营模块代码。
- 当前登录页和后台截图由用户提供。

## Product Principles

1. 操作效率优先，品牌表达为任务服务。
2. 高频信息清晰可读，状态与主要动作一眼可见。
3. 首屏保持轻量，避免与登录无关的动画和网络请求。
4. 使用花店自身的语言，而不是通用科技产品话术。

## Accessibility & Inclusion

登录表单应支持键盘操作、清晰焦点、足够对比度和减少动态效果偏好。
