# 管理端登录页 Surface Brief

- Scope / mode: 管理后台登录页；Operate。
- Audience: 花店店主和店员，在营业环境中高频登录并处理商品、订单、配送和经营数据。
- Job / action: 快速、安全地完成登录并进入经营工作台。
- Content: 花间集品牌标识、花店管理后台、用户名、密码、登录按钮、行为验证码。
- Constraints: 保留现有登录逻辑；桌面与窄屏可用；不增加依赖；不出现版权或公司名称页脚；不使用粒子、轮播、远程首屏背景。
- Direction: 暖白画布上的三段式横向花艺工作台；墨绿、鼠尾草绿与柔和玫瑰粉；本地花材图片。
- Approved comp: `.impeccable/mocks/admin-login-approved.png`。
- Memorable moment: 花材图片区、品牌文字与登录动作在一个连续工作台中自然衔接。
- Implementation grammar: 12–16px 圆角；单一柔和阴影；1px 分隔线；中文无衬线；按钮与焦点态使用墨绿；窄屏隐藏花材图片区并纵向排列。
- Asset inventory:
  - 花材图片区：现有本地 `product-04.jpg`，复制为管理端公共资源。
  - 品牌文字标识：现有 `/huajianji/wordmark.png`，小尺寸呈现并明确宽高。
  - 表单与文字：语义 HTML、Element Plus 与 SCSS。
  - 装饰线条：CSS 几何，不使用生成位图。
- Unresolved decisions: 无。
