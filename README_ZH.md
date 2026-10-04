# 花间集 · 花卉商城

简体中文 | [English](README.md)

花间集（FlowerMarket）是基于 CRMEB Java 二次开发的单商户花卉商城，包含顾客端、花店管理后台和后端 API，面向鲜花选购、预约配送与花店日常经营。

## 主要功能

- 顾客端：花卉分类与搜索、商品规格选择、购物车、预约配送、贺卡留言、订单进度和养花指南。
- 管理后台：花卉商品与分类、规格模板与 SKU 库存、订单履约、配送区域与时段、经营看板、顾客与员工权限、商城装修及门店设置。
- 花材管理：花材档案、批次入库、鲜度与低库存提醒、商品配方，以及订单花材预占和扣减。商品需配置配方并启用批次管理后才参与花材扣减。
- 订单履约：待支付 → 待接单 → 待制作 → 制作中 → 待配送 → 配送中 → 已完成。

## 技术栈

| 模块 | 技术与环境 |
| --- | --- |
| 后端 | JDK 8、Spring Boot 2.2.6、Spring Security、MyBatis-Plus 3.3.1、Maven |
| 数据库与缓存 | MySQL 8.0、Redis |
| 管理后台 | Vue 3、Element Plus、Pinia、Vite 5 |
| 顾客端 | uni-app（Vue 3）、Pinia，提供 H5 / 微信小程序 / App 构建脚本 |
| 前端工具 | Node.js 18+、npm 8+；运行下方后台测试命令需要 Node.js 22+ |

`frontend/pc` 目前仅有说明文件，尚未提供独立 PC 商城。

## 项目结构

```text
FlowerMarket/
├── backend/
│   ├── crmeb-admin/     管理端 API，基础示例端口 8080
│   ├── crmeb-front/     顾客端 API，基础示例端口 8081
│   ├── crmeb-service/   业务服务与数据访问
│   ├── crmeb-common/    实体、请求响应与公共工具
│   ├── crmebimage/      图片素材
│   └── sql/             数据库初始化与升级脚本
├── frontend/
│   ├── admin/           花店管理后台
│   ├── uni-app/         顾客端
│   └── pc/              独立 PC 商城占位目录
├── docs/                设计方案、实施记录与素材说明
├── deliverables/        交付说明与本地验证资料
├── DESIGN.md            管理后台设计规范
└── LICENSE              Apache-2.0 许可证
```

## 本地运行

### 1. 初始化数据库

新数据库先导入 `backend/sql/Crmeb_v3.1.sql`，再执行 `v3.1-to-flower-mall-upgrade.sql`。已有 v3.0 数据库先执行 `v3.0-to-v3.1-upgrade.sql`，无需重新导入完整库。

花卉基础升级后，本地演示可按表中顺序执行演示数据、品牌调整（可选）、论文整理，再执行花材库存和养花指南升级。演示商品使用的分类由论文整理脚本创建，两者应配套使用；业务数据库可跳过演示与论文整理，自行维护商品、分类和门店配置。

| 脚本（位于 `backend/sql/`） | 用途 |
| --- | --- |
| `flower-demo-data.sql` | 可选：本地演示商品、配送与门店配置，会开启模拟支付 |
| `flower-brand-polish.sql` | 可选：花间集品牌标识与菜单排序 |
| `flower-thesis-alignment.sql` | 演示配套：整理菜单、重建商品分类和素材库，并清理部分非花卉商品 |
| `flower-material-inventory-upgrade.sql` | 花材、批次库存、配方与后台菜单 |
| `flower-care-knowledge-upgrade.sql` | 养花指南分类、文章与后台菜单 |

执行升级或演示脚本前备份数据库。论文整理会清空并重建素材记录及素材分类，应仅用于可重置的演示库，并在花材库存、养花指南升级之前执行，以免隐藏这些新增菜单。商品扩充脚本及导入工具见[花卉商品与库存补充说明](deliverables/花卉商品与库存补充说明.md)。

### 2. 配置并启动后端

`crmeb-admin` 和 `crmeb-front` 的 `src/main/resources/` 均提供 `application*.example.yml`。首次运行将各自的 `application.example.yml` 复制为 `application.yml`，已有本地配置时保留原文件。

在启动两个服务的终端中设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 和 `SWAGGER_PASSWORD`；Redis 有密码时设置 `REDIS_PASSWORD`。`DB_URL` 使用指向已初始化数据库的 JDBC URL，并配置 UTF-8 编码与时区。

核对 Redis 地址、`crmeb.imagePath`（指向包含 `crmebimage` 的后端目录，以 `/` 结尾）及服务端口。使用其他环境时，复制对应的 `application-<环境>.example.yml` 并显式启用该 profile；环境文件可能覆盖基础端口，前端接口地址需同步调整。

```sh
cd backend
mvn clean package -DskipTests
```

构建后分别在两个终端启动：

```sh
java -jar crmeb-admin/target/Crmeb-admin.jar
```

```sh
java -jar crmeb-front/target/Crmeb-front.jar
```

实际配置文件和数据库备份已被 Git 忽略，不提交真实凭据。

### 3. 启动管理后台

```sh
cd frontend/admin
npm install
npm run dev
```

默认开发地址为 `http://localhost:9527`。接口地址由 `.env.development` 中的 `VITE_APP_BASE_API` 配置，默认指向 `http://localhost:8080`。生产构建使用 `npm run build`。

### 4. 启动顾客端

```sh
cd frontend/uni-app
npm ci
npm run dev:h5
```

接口地址位于 `config/app.js` 的 `HTTP_REQUEST_URL`，默认指向 `http://127.0.0.1:8081`。真机调试需改为设备可访问的后端地址。

微信小程序使用 `npm run dev:mp-weixin`，并在 `manifest.json` 配置自己的 AppID。将 `dist/dev/mp-weixin` 导入微信开发者工具；发布构建使用 `npm run build:mp-weixin`。H5 发布构建使用 `npm run build:h5`。

## 开发验证

分别在对应目录执行：

| 目录 | 命令 |
| --- | --- |
| `backend/` | `mvn test` |
| `frontend/admin/` | `npm run test:flower` |
| `frontend/uni-app/` | `node --test tests/*.test.mjs` |

模拟支付仅用于本地演示，需显式开启 `flower_mock_pay_enabled`；`prod` / `production` 环境禁用。真实支付、短信和微信能力需另外配置对应服务。

## 相关资料

- [管理后台设计规范](DESIGN.md)
- [前后端数据同步修复说明](deliverables/前后端数据同步修复说明.md)
- [花卉图片来源说明](docs/assets/huajianji-image-sources.md)

本项目保留 CRMEB 来源说明，许可证见 [LICENSE](LICENSE)。
