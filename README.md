# Huajianji · Flower Market

[简体中文](README_ZH.md) | English

Huajianji (花间集, FlowerMarket) is a single-store flower shop application adapted from CRMEB Java. It includes a customer storefront, a shop management console, and backend APIs for flower purchases, scheduled delivery, and daily shop operations.

## Features

- Customer storefront: categories and search, product variants, cart, scheduled delivery, greeting card messages, order progress, and flower care articles.
- Management console: products and categories, variant templates and SKU stock, order fulfillment, delivery zones and slots, dashboard, customers and staff permissions, storefront decoration, and shop settings.
- Flower materials: material records, stock batches, freshness and low-stock alerts, product recipes, and order reservations and consumption. Material consumption requires a configured recipe and batch management enabled for the product.
- Fulfillment: awaiting payment → awaiting acceptance → awaiting production → in production → awaiting delivery → delivering → completed.

## Technology

| Module | Technology and environment |
| --- | --- |
| Backend | JDK 8, Spring Boot 2.2.6, Spring Security, MyBatis-Plus 3.3.1, Maven |
| Database and cache | MySQL 8.0, Redis |
| Management console | Vue 3, Element Plus, Pinia, Vite 5 |
| Customer storefront | uni-app (Vue 3), Pinia; build scripts for H5, WeChat Mini Programs, and App |
| Frontend tools | Node.js 18+, npm 8+; the console test command below requires Node.js 22+ |

`frontend/pc` contains only a placeholder README. A standalone PC storefront is not implemented.

## Repository structure

```text
FlowerMarket/
├── backend/
│   ├── crmeb-admin/     Management API; base example port 8080
│   ├── crmeb-front/     Customer API; base example port 8081
│   ├── crmeb-service/   Business services and data access
│   ├── crmeb-common/    Models, requests, responses, and utilities
│   ├── crmebimage/      Image assets
│   └── sql/             Database initialization and upgrades
├── frontend/
│   ├── admin/           Shop management console
│   ├── uni-app/         Customer storefront
│   └── pc/              Placeholder for a standalone PC storefront
├── docs/                Designs, implementation records, and asset notes
├── deliverables/        Delivery notes and local verification records
└── LICENSE              Apache-2.0 license
```

## Local setup

### 1. Initialize the database

For a new database, import `backend/sql/Crmeb_v3.1.sql`, then run `v3.1-to-flower-mall-upgrade.sql`. For an existing v3.0 database, apply `v3.0-to-v3.1-upgrade.sql` first instead of importing the full database.

After the base flower upgrade, a local demo can apply demo data, branding (optional), and thesis alignment in table order, followed by the material and care upgrades. Demo products use categories created by thesis alignment, so use those two scripts together. A business database can skip demo data and thesis alignment and maintain its own products, categories, and shop configuration.

| Script (in `backend/sql/`) | Purpose |
| --- | --- |
| `flower-demo-data.sql` | Optional: local demo products, delivery, and shop configuration; enables mock payment |
| `flower-brand-polish.sql` | Optional: Huajianji branding and menu order |
| `flower-thesis-alignment.sql` | Companion to demo data: aligns menus, rebuilds product categories and the asset library, and cleans up some non-flower products |
| `flower-material-inventory-upgrade.sql` | Materials, stock batches, recipes, and console menus |
| `flower-care-knowledge-upgrade.sql` | Flower care categories, articles, and console menus |

Back up the database before applying upgrades or demo scripts. Thesis alignment clears and rebuilds asset records and asset categories; use it only on a resettable demo database. Apply it before the material and care upgrades to avoid hiding their new menus. For catalog expansion scripts and the import tool, see the [product and inventory notes](deliverables/花卉商品与库存补充说明.md) (Chinese).

### 2. Configure and start the backend

Both `crmeb-admin` and `crmeb-front` provide `application*.example.yml` in `src/main/resources/`. On first setup, copy each module's `application.example.yml` to `application.yml`. Keep existing local configuration files if already configured.

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `SWAGGER_PASSWORD` in the terminals that launch the services. Set `REDIS_PASSWORD` if required. Use a JDBC URL pointing to the initialized database, with UTF-8 encoding and timezone settings.

Check the Redis address, service ports, and `crmeb.imagePath` (the backend directory containing `crmebimage`, ending in `/`). For another environment, copy its `application-<environment>.example.yml` and explicitly activate that profile. Profile files may override base ports; update frontend API addresses accordingly.

```sh
cd backend
mvn clean package -DskipTests
```

After building, start the services in separate terminals:

```sh
java -jar crmeb-admin/target/Crmeb-admin.jar
```

```sh
java -jar crmeb-front/target/Crmeb-front.jar
```

Local configuration files and database backups are ignored by Git. Do not commit real credentials.

### 3. Start the management console

```sh
cd frontend/admin
npm install
npm run dev
```

The default development address is `http://localhost:9527`. Configure `VITE_APP_BASE_API` in `.env.development`; its default is `http://localhost:8080`. Use `npm run build` for a production build.

### 4. Start the customer storefront

```sh
cd frontend/uni-app
npm ci
npm run dev:h5
```

Configure `HTTP_REQUEST_URL` in `config/app.js`; its default is `http://127.0.0.1:8081`. Use a backend address accessible from the device when testing on a phone.

For WeChat, run `npm run dev:mp-weixin`, set your AppID in `manifest.json`, and import `dist/dev/mp-weixin` into WeChat Developer Tools. Use `npm run build:mp-weixin` for a release build or `npm run build:h5` for H5.

## Development checks

Run each command from its respective directory:

| Directory | Command |
| --- | --- |
| `backend/` | `mvn test` |
| `frontend/admin/` | `npm run test:flower` |
| `frontend/uni-app/` | `node --test tests/*.test.mjs` |

Mock payment is for local demonstrations and requires `flower_mock_pay_enabled` to be explicitly enabled. It is disabled in the `prod` and `production` profiles. Real payment, SMS, and WeChat services require their own configuration.

## Related documents

- [Frontend and backend data synchronization notes](deliverables/前后端数据同步修复说明.md) (Chinese)
- [Flower image sources](docs/assets/huajianji-image-sources.md) (Chinese)

This project retains attribution to CRMEB. See [LICENSE](LICENSE) for the license terms.
