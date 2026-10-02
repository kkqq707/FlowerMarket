# 花卉商城系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 CRMEB Java 3.1 上交付一个可完整演示的单花店微信小程序与 Web 管理后台，覆盖花卉商品、同城预约配送、模拟支付、花束制作配送履约、售后及毕业设计截图材料。

**Architecture:** 保留原商品、SKU、购物车、订单、会员、支付、退款和权限主链路；新增 `flower` 领域的商品扩展、配送预约和履约服务。下单仍从 `OrderServiceImpl.createOrder` 进入，但花店数据校验、时段占用和扩展订单落库必须与原订单创建处于同一事务。支付成功、取消、退款和发货通过小而明确的花店领域服务接入，禁止复制原订单核心逻辑。

**Tech Stack:** Java 8、Spring Boot 2.2.6、MyBatis-Plus 3.3.1、MySQL 8、Redis 5、JUnit 4；Vue 3、Element Plus、Vite；uni-app Vue 3、微信小程序。

## Global Constraints

- 设计基线为 `docs/superpowers/specs/2026-09-14-flower-mall-design.md`，视觉颜色、字体、首页结构和商品图片规则不得自行更改。
- 顾客端只交付微信小程序/H5 调试版本；Web 端只交付花店管理后台，不开发顾客 PC 商城。
- 正式微信支付、实时骑手定位、多商户、供应链和花材级库存不在本期范围。
- 模拟支付只能在配置项 `flower_mock_pay_enabled=1` 时可用，接口还要校验当前环境不是生产环境；关闭时返回“模拟支付未开启”。
- 商品详情首屏图片区域只能显示鲜花图片，价格、划线价、促销、销量和按钮全部位于图片下方独立区域。
- 所有数据库变更同时写入 `backend/sql/Crmeb_v3.1.sql` 与新建升级脚本 `backend/sql/v3.1-to-flower-mall-upgrade.sql`。
- 新增后台接口必须使用 `@PreAuthorize`；新增菜单权限使用从 578 开始的固定 ID，执行前再次确认完整 SQL 最大 ID 仍为 577。
- 根目录当前没有 `.git` 元数据。执行时不得擅自初始化 Git；每个任务保留“建议提交边界”，只有用户提供 Git 仓库后才执行对应提交命令。
- 每个业务任务遵循红—绿—重构：先写会失败的测试，再写最小实现，再运行相关测试。纯视觉任务以构建、页面截图和检查清单作为验收。
- 不改动 `.superpowers/brainstorm/` 中的临时视觉预览；正式界面必须落在 `frontend/uni-app` 和 `frontend/admin`。

---

## Task 1: 建立花店领域模型、枚举与五张数据表

**Files:**

- Create: `backend/sql/v3.1-to-flower-mall-upgrade.sql`
- Modify: `backend/sql/Crmeb_v3.1.sql`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/model/flower/FlowerProductExt.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/model/flower/FlowerDeliveryZone.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/model/flower/FlowerDeliverySlot.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/model/flower/FlowerOrderExt.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/model/flower/FlowerOrderProgress.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/enums/FlowerOrderStageEnum.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/enums/FlowerSubstitutionPolicyEnum.java`
- Create: `backend/crmeb-common/src/test/java/com/zbkj/common/enums/FlowerOrderStageEnumTest.java`

- [ ] **Step 1: 先写状态顺序测试**

```java
@Test
public void shouldOnlyAllowNextFlowerStage() {
    assertTrue(WAIT_ACCEPT.canTransitTo(WAIT_PRODUCTION));
    assertTrue(WAIT_PRODUCTION.canTransitTo(PRODUCING));
    assertFalse(WAIT_ACCEPT.canTransitTo(PRODUCING));
    assertFalse(COMPLETED.canTransitTo(DELIVERING));
}
```

- [ ] **Step 2: 运行测试并确认因枚举不存在而失败**

Run: `mvn -pl crmeb-common -am -Dtest=FlowerOrderStageEnumTest test`

- [ ] **Step 3: 实现固定枚举契约**

`FlowerOrderStageEnum` 固定为：

```java
WAIT_PAY, WAIT_ACCEPT, WAIT_PRODUCTION, PRODUCING,
WAIT_DELIVERY, DELIVERING, COMPLETED
```

`FlowerSubstitutionPolicyEnum` 固定为：

```java
ACCEPT_EQUIVALENT, CONTACT_FIRST, NO_SUBSTITUTION
```

状态枚举提供 `canTransitTo(next)`，仅允许按上述顺序前进；重复提交当前状态由服务层按幂等成功处理。

- [ ] **Step 4: 新建五张表**

表结构除设计字段外补充必要的主键、审计和并发字段：

```sql
CREATE TABLE `eb_flower_delivery_slot` (
  `id` int NOT NULL AUTO_INCREMENT,
  `delivery_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `capacity` int NOT NULL DEFAULT 0,
  `booked_count` int NOT NULL DEFAULT 0,
  `cutoff_time` datetime NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flower_slot_date_time` (`delivery_date`,`start_time`,`end_time`)
);
```

其余表使用相同命名风格，并落实以下唯一/查询索引：

- `eb_flower_product_ext.uk_product_id(product_id)`；字段含 `flower_materials`、`flower_language`、`occasion`、`recipient`、`color_series`、`stem_count`、`packaging`、`care_tips`。
- `eb_flower_delivery_zone.idx_enabled(enabled)`；字段含 `zone_name`、`area_codes`、`delivery_fee`、`minimum_amount`、`enabled`。
- `eb_flower_order_ext.uk_order_id(order_id)` 与 `idx_slot_expire(slot_id,reservation_expire_time,reservation_released)`；补充 `current_stage`、`courier_name`、`courier_phone`、`delivery_remark`、`reservation_released`、`create_time`、`update_time`。
- `eb_flower_order_progress.idx_order_time(order_id,create_time)`；补充唯一键 `uk_order_stage(order_id,stage)` 防止同一阶段重复写入。

- [ ] **Step 5: 添加 MyBatis-Plus 实体映射并运行测试**

实体使用 `@TableName`、`@TableId(type = IdType.AUTO)`，布尔字段遵循项目现有 `Boolean` 映射方式；订单号使用 `String`，商品和操作人 ID 使用 `Integer`，金额使用 `BigDecimal`，日期使用 `LocalDate`/`LocalTime` 或项目已统一采用的日期类型。

Run: `mvn -pl crmeb-common -am -Dtest=FlowerOrderStageEnumTest test`

- [ ] **Step 6: 建议提交边界**

`git add backend/sql backend/crmeb-common && git commit -m "feat: add flower domain schema and models"`

---

## Task 2: 实现花卉商品扩展服务与场景化查询

**Files:**

- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/dao/FlowerProductExtDao.java`
- Create: `backend/crmeb-service/src/main/resources/mapper/FlowerProductExtDao.xml`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/FlowerProductService.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/FlowerProductServiceImpl.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/request/FlowerProductExtRequest.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/request/FlowerProductSearchRequest.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/response/FlowerProductDetailResponse.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/response/FlowerProductCardResponse.java`
- Create: `backend/crmeb-admin/src/main/java/com/zbkj/admin/controller/FlowerProductController.java`
- Create: `backend/crmeb-front/src/main/java/com/zbkj/front/controller/FlowerProductController.java`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/service/impl/FlowerProductServiceImplTest.java`

- [ ] **Step 1: 写保存与筛选失败测试**

覆盖：同一 `productId` 再次保存执行更新；空花材被校验拒绝；`occasion=生日`、`recipient=恋人`、`colorSeries=粉色` 和预算上下限能组合筛选；下架商品不返回。

- [ ] **Step 2: 定义服务接口**

```java
public interface FlowerProductService {
    FlowerProductExt saveOrUpdate(Integer productId, FlowerProductExtRequest request);
    FlowerProductDetailResponse getDetail(Integer productId);
    PageInfo<FlowerProductCardResponse> search(FlowerProductSearchRequest request,
                                                PageParamRequest page);
}
```

- [ ] **Step 3: 实现查询，不复制原商品数据**

DAO 联表 `eb_flower_product_ext`、`eb_store_product` 与最低可售 SKU 价格；花店表只保存扩展字段。列表响应返回 `productId`、`storeName`、`image`、`price`、`sales`、花材/场景/对象/色系摘要。

- [ ] **Step 4: 提供前后台接口**

- 管理端：`GET /api/admin/flower/product/{productId}`、`POST /api/admin/flower/product/{productId}`，权限分别为 `admin:flower:product:info`、`admin:flower:product:save`。
- 小程序：`GET /api/front/flower/product/{productId}`、`GET /api/front/flower/product/list`。

- [ ] **Step 5: 运行单测和模块编译**

Run: `mvn -pl crmeb-service -am -Dtest=FlowerProductServiceImplTest test`

Run: `mvn -pl crmeb-admin,crmeb-front -am -DskipTests package`

- [ ] **Step 6: 建议提交边界**

`git add backend && git commit -m "feat: add flower product extensions and filters"`

---

## Task 3: 实现配送区域、日期时段和原子容量占用

**Files:**

- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/dao/FlowerDeliveryZoneDao.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/dao/FlowerDeliverySlotDao.java`
- Create: `backend/crmeb-service/src/main/resources/mapper/FlowerDeliveryZoneDao.xml`
- Create: `backend/crmeb-service/src/main/resources/mapper/FlowerDeliverySlotDao.xml`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/FlowerDeliveryService.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/FlowerDeliveryServiceImpl.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/request/FlowerDeliveryZoneRequest.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/request/FlowerDeliverySlotRequest.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/response/FlowerDeliveryQuoteResponse.java`
- Create: `backend/crmeb-admin/src/main/java/com/zbkj/admin/controller/FlowerDeliveryController.java`
- Create: `backend/crmeb-front/src/main/java/com/zbkj/front/controller/FlowerDeliveryController.java`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/service/impl/FlowerDeliveryServiceImplTest.java`

- [ ] **Step 1: 写容量与范围失败测试**

覆盖：地址行政区编码不在任何启用区域；订单金额低于起送金额；已过截单时间；已满时段；最后一个名额只能成功占用一次；释放两次只减少一次。

- [ ] **Step 2: 定义原子 DAO 更新**

```sql
UPDATE eb_flower_delivery_slot
SET booked_count = booked_count + 1, version = version + 1, update_time = NOW()
WHERE id = #{slotId}
  AND enabled = 1
  AND cutoff_time > NOW()
  AND booked_count < capacity;
```

DAO 返回受影响行数；返回 0 统一转换为“该配送时段已满或已停止预约”。释放使用 `booked_count > 0` 条件更新，且必须先原子标记订单扩展记录 `reservation_released=1`。

- [ ] **Step 3: 定义服务契约**

```java
FlowerDeliveryQuoteResponse quote(Integer addressId, BigDecimal goodsAmount);
List<FlowerDeliverySlot> availableSlots(Integer zoneId, LocalDate start, LocalDate end);
void reserve(Integer slotId);
void release(String orderNo);
```

- [ ] **Step 4: 提供接口**

- 小程序：`GET /api/front/flower/delivery/quote?addressId=&goodsAmount=` 与 `GET /api/front/flower/delivery/slots?zoneId=&startDate=&endDate=`。
- 后台：区域和时段的分页、新增、修改、启停接口，统一前缀 `/api/admin/flower/delivery`，权限前缀 `admin:flower:delivery:`。

- [ ] **Step 5: 运行测试**

Run: `mvn -pl crmeb-service -am -Dtest=FlowerDeliveryServiceImplTest test`

- [ ] **Step 6: 建议提交边界**

`git add backend && git commit -m "feat: add local delivery zones and slot capacity"`

---

## Task 4: 将送花信息和预约容量原子接入创建订单

**Files:**

- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/request/FlowerOrderRequest.java`
- Modify: `backend/crmeb-common/src/main/java/com/zbkj/common/request/CreateOrderRequest.java`
- Modify: `backend/crmeb-common/src/main/java/com/zbkj/common/request/OrderComputedPriceRequest.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/dao/FlowerOrderExtDao.java`
- Create: `backend/crmeb-service/src/main/resources/mapper/FlowerOrderExtDao.xml`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/FlowerOrderService.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/FlowerOrderServiceImpl.java`
- Modify: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/service/impl/FlowerOrderCreateIntegrationTest.java`

- [ ] **Step 1: 写下单事务测试**

覆盖：送花必填信息缺失拒绝；配送地址快照取自服务端地址表；区域与地址不匹配拒绝；配送费加入应付金额；订单创建失败不占名额；占名额失败不创建订单；成功时原订单、扩展订单、首条 `WAIT_PAY` 进度和 `booked_count+1` 同时提交。

- [ ] **Step 2: 扩展请求对象但保持旧客户端兼容**

```java
public class CreateOrderRequest {
    // 原字段保持不变
    @Valid
    private FlowerOrderRequest flower;
}
```

`FlowerOrderRequest` 明确包含 `senderName`、`senderPhone`、`recipientName`、`recipientPhone`、`deliveryZoneId`、`deliveryDate`、`slotId`、`greetingCard`、`anonymous`、`contactRecipient`、`substitutionPolicy`。同城配送要求 `flower` 非空；到店自提只要求订花人、取花日期和联系方式。

- [ ] **Step 3: 在原事务前完成只读校验**

在 `OrderServiceImpl.createOrder` 中：加载地址与时段、验证区域、起送金额、日期一致性和截单时间；调用花店服务得到配送费并更新 `ComputedOrderPriceResponse`，不得相信客户端传来的费用或地址文本。

- [ ] **Step 4: 在原 `transactionTemplate.execute` 内完成三项写入**

在 `storeOrderService.create(storeOrder)` 之后、事务返回之前依次调用：

```java
flowerDeliveryService.reserve(request.getFlower().getSlotId());
flowerOrderService.createExtension(storeOrder, request.getFlower(), userAddress);
flowerOrderService.appendProgress(storeOrder.getOrderId(), WAIT_PAY, null, null);
```

任一步异常必须回滚原订单、库存扣减和时段占用。预约失效时间固定为订单创建后 15 分钟。

- [ ] **Step 5: 返回下单所需数据**

原响应中的 `orderNo` 保留，新增 `reservationExpireTime` 与 `deliveryFee`，旧端忽略新字段不受影响。

- [ ] **Step 6: 运行测试与原订单回归测试**

Run: `mvn -pl crmeb-service -am -Dtest=FlowerOrderCreateIntegrationTest,StoreOrderServiceImplTest test`

- [ ] **Step 7: 建议提交边界**

`git add backend && git commit -m "feat: integrate flower delivery reservation into checkout"`

---

## Task 5: 实现开发环境模拟支付并复用真实支付成功链路

**Files:**

- Create: `backend/crmeb-front/src/main/java/com/zbkj/front/controller/FlowerMockPayController.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/FlowerMockPayService.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/FlowerMockPayServiceImpl.java`
- Modify: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderPayServiceImpl.java`
- Modify: `backend/crmeb-common/src/main/java/com/zbkj/common/constants/SysConfigConstants.java`
- Modify: `backend/sql/Crmeb_v3.1.sql`
- Modify: `backend/sql/v3.1-to-flower-mall-upgrade.sql`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/service/impl/FlowerMockPayServiceImplTest.java`

- [ ] **Step 1: 写安全和幂等测试**

覆盖：开关关闭拒绝；生产环境拒绝；非本人订单拒绝；过期/取消订单拒绝；首次模拟支付更新支付状态；重复请求不重复增加销量、积分或写进度。

- [ ] **Step 2: 抽取统一支付成功入口**

新增 `OrderPayService.markPaidAndEnqueueSuccess(String orderNo, String payType, String transactionId)`，由真实余额/微信回调与模拟支付共同调用。该方法负责原子更新 `paid/pay_time/pay_type`，只有数据库从未支付更新为已支付时才加入现有 `OrderPaySuccessQueueService`。

- [ ] **Step 3: 实现模拟支付接口**

`POST /api/front/flower/pay/mock/{orderNo}` 返回原 `OrderPayResultResponse` 能表达的成功结果；交易号使用 `MOCK-` 加订单号。读取系统配置 `flower_mock_pay_enabled`，并通过 Spring profile/环境属性阻止 `prod`。

- [ ] **Step 4: 支付后追加花店进度**

在现有支付成功后置处理成功后调用 `flowerOrderService.transit(orderNo, WAIT_ACCEPT, null, null, "支付成功，等待花店接单")`。统一方法签名为 `transit(String orderNo, FlowerOrderStageEnum targetStage, Integer operatorId, String proofImage, String remark)`；如果订单没有花店扩展记录则直接跳过，保证原商城订单不受影响。

- [ ] **Step 5: 运行测试**

Run: `mvn -pl crmeb-service -am -Dtest=FlowerMockPayServiceImplTest,OrderPaySuccessImmediateProcessTest test`

- [ ] **Step 6: 建议提交边界**

`git add backend && git commit -m "feat: add safe idempotent mock payment"`

---

## Task 6: 实现花束制作、配送履约和预约超时释放

**Files:**

- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/dao/FlowerOrderProgressDao.java`
- Create: `backend/crmeb-service/src/main/resources/mapper/FlowerOrderProgressDao.xml`
- Modify: `backend/crmeb-service/src/main/java/com/zbkj/service/service/FlowerOrderService.java`
- Modify: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/FlowerOrderServiceImpl.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/request/FlowerOrderTransitRequest.java`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/response/FlowerOrderDetailResponse.java`
- Create: `backend/crmeb-admin/src/main/java/com/zbkj/admin/controller/FlowerOrderController.java`
- Create: `backend/crmeb-front/src/main/java/com/zbkj/front/controller/FlowerOrderController.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/task/FlowerReservationExpireTask.java`
- Modify: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/service/impl/FlowerOrderWorkflowServiceImplTest.java`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/task/FlowerReservationExpireTaskTest.java`

- [ ] **Step 1: 写状态机失败测试**

覆盖完整七阶段、越级拒绝、重复操作幂等、非花店订单跳过、未支付不能接单、未制作完成不能配送、完成配送时原订单状态同步为待评价。

- [ ] **Step 2: 用行锁保护状态流转**

`FlowerOrderExtDao.selectByOrderIdForUpdate(orderNo)` 锁定扩展行；服务比较 `currentStage`：等于目标阶段直接返回成功，只允许 `canTransitTo`，成功后同时更新 `current_stage` 和插入进度记录。

- [ ] **Step 3: 定义后台动作接口**

统一 `POST /api/admin/flower/order/{orderNo}/transition`，请求包含 `targetStage`、`remark`、`proofImage`、`courierName`、`courierPhone`。权限按动作细分为 `accept`、`produce`、`deliver`、`complete`。进入 `DELIVERING` 时复用原订单发货状态更新；进入 `COMPLETED` 时写送达凭证并同步原订单为待评价。

- [ ] **Step 4: 定义前台进度接口**

- `GET /api/front/flower/order/{orderNo}`：只允许订单本人访问，返回送花信息、预约时段、当前阶段、进度列表、成品照和送达凭证。
- `POST /api/front/flower/order/{orderNo}/confirm`：顾客确认送达，幂等进入完成状态。

- [ ] **Step 5: 接入取消与容量释放**

在原订单取消成功路径调用 `flowerDeliveryService.release(orderNo)`；只有 `reservation_released=0` 才减少 `booked_count`。已开始制作的订单拒绝顾客直接取消并提示联系花店。

- [ ] **Step 6: 实现每分钟执行的过期任务**

任务查询 `WAIT_PAY`、未支付、`reservation_expire_time < NOW()` 且未释放的订单，逐笔调用原取消服务和容量释放服务。异常订单单独记录，不阻断其他订单。测试使用固定时钟，不依赖真实等待 15 分钟。

- [ ] **Step 7: 运行测试**

Run: `mvn -pl crmeb-service -am -Dtest=FlowerOrderWorkflowServiceImplTest,FlowerReservationExpireTaskTest test`

- [ ] **Step 8: 建议提交边界**

`git add backend && git commit -m "feat: add bouquet fulfillment workflow"`

---

## Task 7: 完成花店售后规则与后台权限菜单

**Files:**

- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/FlowerAfterSaleService.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/FlowerAfterSaleServiceImpl.java`
- Modify: `backend/crmeb-front/src/main/java/com/zbkj/front/controller/StoreOrderController.java`
- Modify: `backend/crmeb-admin/src/main/java/com/zbkj/admin/controller/StoreOrderController.java`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/service/impl/FlowerAfterSaleServiceImplTest.java`
- Modify: `backend/sql/Crmeb_v3.1.sql`
- Modify: `backend/sql/v3.1-to-flower-mall-upgrade.sql`

- [ ] **Step 1: 写两小时售后和制作阶段测试**

品质原因固定为“不新鲜、花材不符、包装破损、配送超时”；送达后两小时内允许提交并保留图片，超过两小时返回联系客服提示。制作前取消可审批退款，制作后必须后台人工决定，重复退款被原退款状态与花店服务共同拦截。

- [ ] **Step 2: 在原退款入口增加花店规则校验**

非花店订单沿用原逻辑；花店订单先调用 `FlowerAfterSaleService.validateApply`。不另建售后表，复用原退款申请、退款原因、附件和订单日志字段。

- [ ] **Step 3: 写入后台菜单与权限**

从 578 开始写入“花店经营”“花卉商品”“花店订单”“配送管理”菜单和所有接口按钮权限；隐藏砍价、拼团、秒杀、分销的默认菜单记录，但不删除业务表和代码。升级脚本使用 `INSERT SELECT` 配合 `WHERE NOT EXISTS` 实现可重复执行，并使用精确菜单 ID 更新。

- [ ] **Step 4: 运行测试和 SQL 静态检查**

Run: `mvn -pl crmeb-service -am -Dtest=FlowerAfterSaleServiceImplTest test`

Run: `rg -n "578|admin:flower:" backend/sql/Crmeb_v3.1.sql backend/sql/v3.1-to-flower-mall-upgrade.sql`

- [ ] **Step 5: 建议提交边界**

`git add backend && git commit -m "feat: add flower after-sale rules and permissions"`

---

## Task 8: 建立小程序花店设计系统与可复用商品组件

**Files:**

- Create: `frontend/uni-app/static/css/flower-tokens.scss`
- Modify: `frontend/uni-app/App.vue`
- Modify: `frontend/uni-app/uni.scss`
- Modify: `frontend/uni-app/utils/theme.js`
- Modify: `frontend/uni-app/static/css/theme_color.scss`
- Create: `frontend/uni-app/components/flower-section-title/index.vue`
- Create: `frontend/uni-app/components/flower-product-card/index.vue`
- Create: `frontend/uni-app/components/flower-product-row/index.vue`
- Create: `frontend/uni-app/components/flower-empty/index.vue`
- Create: `frontend/uni-app/utils/flower-product-view.js`
- Create: `frontend/uni-app/tests/flower-product-view.test.mjs`

- [ ] **Step 1: 写商品视图数据测试**

测试价格格式、图片空值回退、英文副标题可选、销量文案以及商品图片对象中不混入价格覆盖层字段。

- [ ] **Step 2: 固化主题变量**

```scss
$flower-primary: #35402F;
$flower-deep: #243129;
$flower-background: #F7F4EC;
$flower-paper: #FFFEFA;
$flower-text: #2D312C;
$flower-accent: #ED3C2D;
$flower-serif-cn: "Songti SC", "SimSun", serif;
$flower-serif-en: Georgia, "Times New Roman", serif;
```

同步映射现有 CSS 自定义变量，让旧组件仍可使用主题色；移除花店页面上的橙色渐变、厚阴影和大面积红色背景。

- [ ] **Step 3: 实现三种展示组件**

- `flower-product-card`：首页双列、1:1 纯图、图下名称/价格，无边框厚阴影。
- `flower-product-row`：分类页左侧方图、右侧名称/花材/价格/购物车按钮。
- 详情大图不复用价格卡片，避免价格叠在图片上。

- [ ] **Step 4: 运行数据测试与小程序构建**

Run: `node --test tests/flower-product-view.test.mjs`（工作目录 `frontend/uni-app`）

Run: `npm run build:mp-weixin`（工作目录 `frontend/uni-app`）

- [ ] **Step 5: 建议提交边界**

`git add frontend/uni-app && git commit -m "feat: establish floral storefront design system"`

---

## Task 9: 重做小程序长首页、分类、列表与商品详情

**Files:**

- Modify: `frontend/uni-app/pages/index/index.vue`
- Create: `frontend/uni-app/components/flower-home/hero.vue`
- Create: `frontend/uni-app/components/flower-home/weekly-new.vue`
- Create: `frontend/uni-app/components/flower-home/category-nav.vue`
- Create: `frontend/uni-app/components/flower-home/budget-nav.vue`
- Create: `frontend/uni-app/components/flower-home/brand-story.vue`
- Create: `frontend/uni-app/components/flower-home/editorial-feature.vue`
- Create: `frontend/uni-app/components/flower-home/service-promise.vue`
- Modify: `frontend/uni-app/pages/goods_cate/goods_cate.vue`
- Modify: `frontend/uni-app/pages/goods/goods_list/index.vue`
- Modify: `frontend/uni-app/pages/goods/goods_details/index.vue`
- Modify: `frontend/uni-app/api/store.js`
- Modify: `frontend/uni-app/pages.json`

- [ ] **Step 1: 首页按批准顺序拆成十个连续区块**

顺序必须是：品牌主视觉、每周上新、分类导航、预算选花、品牌故事、人气精选、专属定制、双列推荐、服务保障、底部导航。首页内容从现有装修/商品接口读取，品牌名和图片不写死；无数据时用后台可替换的本地演示资源。

- [ ] **Step 2: 分类页改成花店浏览逻辑**

顶部提供搜索；筛选项为场景、赠送对象、色系、预算；商品结果使用 `flower-product-row`。保留购物车快捷入口和加载状态，去掉参考图中与鲜花无关的手机数码分类。

- [ ] **Step 3: 商品详情落实“纯图片区”规则**

轮播区域 DOM 内只允许 `swiper/image`、返回按钮、分享按钮和页码点；价格、划线价、促销标签、名称、销量全部移动到轮播容器结束标签之后。内容顺序为价格、名称、花材/花语、规格、配送提示、养护说明、推荐商品和底部购买栏。

- [ ] **Step 4: 检查不同屏宽**

在 375px、430px 与微信开发者工具常用尺寸检查：图片不拉伸、标题不遮挡、双列间距一致、底部购买栏不覆盖内容、中文宋体回退可读。

- [ ] **Step 5: 构建并保存验收截图**

Run: `npm run build:h5`

Run: `npm run build:mp-weixin`

截图：首页长页、分类横向卡片、商品详情首屏和详情下半页；重点确认大图内没有价格信息。

- [ ] **Step 6: 建议提交边界**

`git add frontend/uni-app && git commit -m "feat: redesign flower storefront catalog and details"`

---

## Task 10: 完成送花结算、配送时段、模拟支付和订单进度页面

**Files:**

- Modify: `frontend/uni-app/api/order.js`
- Create: `frontend/uni-app/api/flower.js`
- Modify: `frontend/uni-app/pages/order/order_confirm/index.vue`
- Modify: `frontend/uni-app/pages/order/order_payment/index.vue`
- Modify: `frontend/uni-app/pages/order/order_details/index.vue`
- Modify: `frontend/uni-app/pages/users/order_list/index.vue`
- Modify: `frontend/uni-app/pages/users/user_return_list/index.vue`
- Create: `frontend/uni-app/components/flower-delivery-slot-picker/index.vue`
- Create: `frontend/uni-app/components/flower-order-progress/index.vue`
- Create: `frontend/uni-app/utils/flower-checkout.js`
- Create: `frontend/uni-app/tests/flower-checkout.test.mjs`

- [ ] **Step 1: 写结算校验测试**

覆盖订花人与收花人必填、手机号格式、同城配送必须选区域/日期/时段、贺卡长度、三种替换策略、到店自提不要求配送时段、接口返回时段已满后清除旧选择。

- [ ] **Step 2: 改造确认订单页**

分区为收花地址、订花人、收花人、日期时段、贺卡与匿名、联系收花人、缺货替换、优惠与金额。先调用配送报价接口，再加载可选时段；提交按钮旁明确展示配送费和预约保留 15 分钟提示。

- [ ] **Step 3: 接入模拟支付**

开发演示时展示“模拟微信支付”方式，调用 `/api/front/flower/pay/mock/{orderNo}`；成功后跳转支付成功页并刷新订单，失败时保留原订单号，禁止重复创建订单。

- [ ] **Step 4: 展示订单履约进度**

订单详情加入七阶段时间轴、成品照、配送人员、送达凭证和贺卡信息。列表用用户可读文案显示“待付款、待接单、待制作、制作中、待配送、配送中、已送达”。

- [ ] **Step 5: 加入品质售后入口**

送达后两小时内显示四种品质原因和图片上传；超时显示联系客服；制作开始后取消按钮改为联系花店，不再直接提交取消。

- [ ] **Step 6: 测试与构建**

Run: `node --test tests/flower-checkout.test.mjs`（工作目录 `frontend/uni-app`）

Run: `npm run build:mp-weixin`（工作目录 `frontend/uni-app`）

- [ ] **Step 7: 建议提交边界**

`git add frontend/uni-app && git commit -m "feat: add floral checkout and order progress"`

---

## Task 11: 完成 Web 后台换肤与花卉商品管理

**Files:**

- Create: `frontend/admin/src/styles/flower-theme.scss`
- Modify: `frontend/admin/src/styles/index.scss`
- Modify: `frontend/admin/src/settings.js`
- Create: `frontend/admin/src/api/flower.js`
- Modify: `frontend/admin/src/views/store/creatStore/index.vue`
- Create: `frontend/admin/src/views/store/creatStore/components/FlowerProductForm.vue`
- Modify: `frontend/admin/src/views/store/index.vue`
- Modify: `frontend/admin/src/router/modules/store.js`

- [ ] **Step 1: 固化后台主题**

Element Plus 主色为 `#35402F`，侧栏深色为 `#243129`，页面背景为 `#F7F4EC`；危险/退款仍用 `#ED3C2D`。表格和表单保持高密度，不把小程序大图排版照搬到后台。

- [ ] **Step 2: 在商品编辑页增加“花礼信息”区块**

字段为花材、花语、适用场景、赠送对象、色系、枝数、包装方式和养护说明。先保存原商品取得 `productId`，再保存扩展信息；第二步失败时明确提示“基础商品已保存，花礼信息保存失败”，允许重新提交，不重复创建商品。

- [ ] **Step 3: 商品列表增加花店筛选和摘要**

支持场景/对象/色系筛选，列表显示主花材、色系、上架状态和推荐排序；商品主图仍为纯花束图，不把价格文字烙在图片文件里。

- [ ] **Step 4: 运行检查**

Run: `npm run lint`（工作目录 `frontend/admin`）

Run: `npm run build`（工作目录 `frontend/admin`）

- [ ] **Step 5: 建议提交边界**

`git add frontend/admin && git commit -m "feat: theme admin and add flower product fields"`

---

## Task 12: 完成配送配置、订单履约和经营看板后台

**Files:**

- Create: `frontend/admin/src/views/flower/delivery/index.vue`
- Create: `frontend/admin/src/views/flower/delivery/components/ZoneForm.vue`
- Create: `frontend/admin/src/views/flower/delivery/components/SlotForm.vue`
- Create: `frontend/admin/src/views/flower/order/index.vue`
- Create: `frontend/admin/src/views/flower/order/detail.vue`
- Create: `frontend/admin/src/views/flower/order/components/ProgressActions.vue`
- Create: `frontend/admin/src/views/flower/dashboard/index.vue`
- Create: `frontend/admin/src/router/modules/flower.js`
- Modify: `frontend/admin/src/router/index.js`
- Create: `backend/crmeb-common/src/main/java/com/zbkj/common/response/FlowerDashboardResponse.java`
- Create: `backend/crmeb-admin/src/main/java/com/zbkj/admin/controller/FlowerDashboardController.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/FlowerDashboardService.java`
- Create: `backend/crmeb-service/src/main/java/com/zbkj/service/service/impl/FlowerDashboardServiceImpl.java`
- Create: `backend/crmeb-service/src/test/java/com/zbkj/service/service/impl/FlowerDashboardServiceImplTest.java`

- [x] **Step 1: 写看板统计测试**

固定同一时间范围与时区，验证今日销售额、订单数、客单价、待接单/待制作/待配送/售后数、准时率、热门时段与热销鲜花排行；退款订单不计销售额。

- [x] **Step 2: 实现配送配置页**

区域页维护覆盖行政编码、配送费、起送金额和启停；时段页按日期展示容量、已预约数、剩余数、截单时间和临时关闭。容量不得设置为小于当前 `booked_count`。

- [x] **Step 3: 实现花店订单工作台**

顶部按履约阶段显示数量；详情完整展示订/收花人、地址、时段、贺卡、替换策略、商品、成品照、配送人和进度日志。动作按钮只显示当前阶段允许的下一步，并在关键动作前二次确认。

- [x] **Step 4: 实现打印视图**

制作单显示花材、规格、包装、贺卡和替换策略；配送单显示收花人、电话、地址、时段和联系要求。匿名订单的配送单不得显示订花人给收花人查看的身份信息。

- [x] **Step 5: 实现经营看板**

后台首页采用数据卡、折线趋势、热销排行和热门时段，图表沿用项目现有图表依赖；无数据时显示 0 和空态，不生成虚假随机数据。

- [x] **Step 6: 全量构建**

Run: `mvn -pl crmeb-admin -am -Dtest=FlowerDashboardServiceImplTest test`

Run: `npm run lint`（工作目录 `frontend/admin`）

Run: `npm run build`（工作目录 `frontend/admin`）

- [ ] **Step 7: 建议提交边界**

`git add backend frontend/admin && git commit -m "feat: add flower shop operations console"`

---

## Task 13: 演示数据、配置、全链路验收与毕业设计材料

**Files:**

- Create: `backend/sql/flower-demo-data.sql`
- Modify: `backend/crmeb-front/src/main/resources/application-dev.yml`
- Create: `docs/flower-mall/demo-script.md`
- Create: `docs/flower-mall/test-report.md`
- Create: `docs/flower-mall/screenshot-checklist.md`
- Create: `docs/flower-mall/architecture.md`

- [ ] **Step 1: 准备可重复导入的演示数据**

包含不少于 12 个花卉商品、4 个场景分类、4 个预算区间、2 个配送区域、未来 7 天每天 3 个时段和一个演示管理员角色。图片使用项目内已获授权或用户提供的纯鲜花素材；不得把参考图中的品牌、价格或水印直接作为商品图片。

- [ ] **Step 2: 配置开发演示**

开发配置只写非敏感开关，不写真实密钥；`flower_mock_pay_enabled` 默认在演示 SQL 中开启，在生产配置中明确关闭。门店名称、电话、地址、营业时间和首页品牌文案全部通过系统配置读取。

- [ ] **Step 3: 跑完整自动检查**

Run: `mvn clean test`（工作目录 `backend`）

Run: `mvn clean package -DskipTests`（工作目录 `backend`）

Run: `npm run build:mp-weixin`（工作目录 `frontend/uni-app`）

Run: `npm run build`（工作目录 `frontend/admin`）

- [ ] **Step 4: 执行正常全链路演示**

按顺序记录结果：小程序选花 → 填写送花信息 → 选择时段 → 创建订单占容量 → 模拟支付 → 后台接单 → 开始制作 → 上传成品照 → 待配送 → 配送中 → 上传送达凭证 → 用户确认 → 评价。

- [ ] **Step 5: 执行异常链路**

逐项验证并写入测试报告：范围外地址、低于起送额、满时段、截单时段、15 分钟未支付释放、重复模拟支付、重复接单、越级配送、制作后取消、售后超过两小时、员工无系统配置权限。

- [ ] **Step 6: 准备答辩截图和图表清单**

至少包含：小程序长首页、分类、纯图商品详情、送花结算、时段选择、模拟支付、订单时间轴、成品照/送达凭证、品质售后；后台看板、花卉商品、配送区域、时段容量、订单工作台、履约详情；系统结构图、五表关系图、订单状态图和测试结果。

- [ ] **Step 7: 最终核对完成标准**

- 小程序和后台均能构建。
- 五张花店业务表可从空库升级脚本创建。
- 原商品、购物车、订单、支付成功、退款、会员与权限主流程回归通过。
- 首页结构、颜色、字体、商品卡片和详情纯图符合批准设计。
- 一条正常订单和全部关键异常流程有可复现记录。

- [ ] **Step 8: 建议提交边界**

`git add backend frontend docs && git commit -m "test: complete flower mall demo and acceptance"`

---

## 执行顺序与里程碑

1. **里程碑 A（Task 1–3）**：数据表、花卉商品和配送容量独立可测。
2. **里程碑 B（Task 4–7）**：下单、模拟支付、履约和售后后端形成闭环。
3. **里程碑 C（Task 8–10）**：小程序达到参考图风格并跑通顾客流程。
4. **里程碑 D（Task 11–12）**：Web 花店后台完成商品、配送、订单和看板。
5. **里程碑 E（Task 13）**：全量构建、演示数据、异常测试和答辩材料完成。

每个里程碑结束后先由用户查看可运行页面与截图，再进入下一里程碑；如视觉仅需微调，不改变已批准的数据结构和业务状态机。
