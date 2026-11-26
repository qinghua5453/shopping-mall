# 优惠券（P1）前端联调文档

> 对应后端：`server`（单模块 MVC + JWT），已实现的 P1 范围 = 后台券模板管理、券码管理、用户券、叠加规则。
> 本文档以**后端实际代码**为准，与 `server/docs/06-coupon-design.md` 设计稿存在少量差异，差异处均已标注。
> 会员端（领券中心、我的券、核销等）属于 P2，本文档不涉及。

---

## 1. 联调前置

| 项 | 值 |
| --- | --- |
| 后端地址 | `http://localhost:8080` |
| 前端地址 | `http://127.0.0.1:5173` |
| 代理 | Vite 将 `/api` 转发到 `VITE_DEV_PROXY_TARGET`（默认 `http://localhost:8080`） |
| 登录账号 | `admin` / `Admin@123456` |
| 数据库 | MySQL `localhost:3307/mall`，Redis `localhost:6379` |

启动后端：

```bash
cd server
/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home/bin/java -jar target/mall-server-0.2.0.jar
```

健康检查：`GET http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。

> 前端看到"服务暂时不可用 / 网络连接失败"时，优先确认后端进程是否还在。

### 1.1 登录与请求头

```http
POST /api/v1/admin/auth/login
Content-Type: application/json
X-Client-Type: PC

{ "username": "admin", "password": "Admin@123456", "deviceId": "<uuid>" }
```

响应（注意字段是 `accessToken`，不是 `token`）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "traceId": "e9563e032d0d45c08823edfd50fc3492",
  "timestamp": "2026-11-26 13:25:04"
}
```

后续所有优惠券接口必须带：

```http
Authorization: Bearer <accessToken>
X-Client-Type: PC
```

现有 [http.ts](../src/api/http.ts) 的请求拦截器已自动注入 `Authorization`、`X-Client-Type`、`X-Device-Id`、`X-Request-Id`，联调时不需要额外处理。

### 1.2 统一响应与分页

```ts
interface ApiResponse<T> {
  code: number      // 0 表示成功
  message: string
  data: T
  traceId: string
  timestamp: string // yyyy-MM-dd HH:mm:ss
}

interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  list: T[]
}
```

分页参数统一为 `page`（默认 1）、`pageSize`（默认 20）。**后端对 `pageSize` 做了兜底：小于等于 0 或大于 100 时会被重置为 20**，前端传 100 以内即可。

---

## 2. 枚举字典（前端直接做常量映射）

### 2.1 模板状态 `status`

| 值 | 含义 | 可修改 | 可删除 | 可发布 | 可下线 |
| --- | --- | --- | --- | --- | --- |
| 0 | 草稿 DRAFT | ✅ | ✅ | ✅ | ❌ |
| 1 | 已发布 PUBLISHED | ❌ | ❌ | ❌ | ✅ |
| 2 | 已下线 OFFLINE | ✅ | ❌ | ✅ | ❌ |

### 2.2 券类型 `couponType`

| 值 | 含义 | 必填的优惠字段 |
| --- | --- | --- |
| 1 | 满减券 | `discountAmount`（分） |
| 2 | 折扣券 | `discountRate`（正数，如 `8.5` 表示 8.5 折；范围 `(0, 10]`） |
| 3 | 无门槛券 | `discountAmount`（分） |

### 2.3 适用范围 `scopeType` / `scopeValues`

| 值 | 含义 |
| --- | --- |
| 1 | 全场 |
| 2 | 类目 |
| 3 | SPU |
| 4 | SKU |

`scopeValues` 是 **JSON 字符串**（不是对象），空值后端会兜底存 `"{}"`。建议前端统一序列化后传：

```json
{ "include": [1, 2], "exclude": [] }
```

### 2.4 有效期类型 `validityType`

| 值 | 含义 | 必填字段 |
| --- | --- | --- |
| 1 | 固定区间 | `validStart`、`validEnd`（`yyyy-MM-dd HH:mm:ss`） |
| 2 | 领取后 N 天 | `validDays`（> 0） |

### 2.5 券码状态 `status`

| 值 | 含义 |
| --- | --- |
| 1 | 未使用 |
| 2 | 已使用 |
| 3 | 已作废 |

### 2.6 券实例状态 `status`

| 值 | 含义 |
| --- | --- |
| 1 | 未使用 UNUSED |
| 2 | 锁定中 LOCKED（下单锁券） |
| 3 | 已使用 USED |
| 4 | 已过期 EXPIRED |
| 5 | 已冻结 FROZEN |

### 2.7 叠加规则 `allowStack`

| 值 | 含义 |
| --- | --- |
| 0 | 不允许叠加 |
| 1 | 允许叠加 |

---

## 3. 接口清单速查

| # | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/admin/coupons/templates` | 模板分页 |
| 2 | GET | `/api/v1/admin/coupons/templates/{id}` | 模板详情 |
| 3 | POST | `/api/v1/admin/coupons/templates` | 新建草稿 |
| 4 | PUT | `/api/v1/admin/coupons/templates/{id}` | 修改（草稿/已下线） |
| 5 | DELETE | `/api/v1/admin/coupons/templates/{id}` | 删除（仅草稿） |
| 6 | POST | `/api/v1/admin/coupons/templates/{id}/publish` | 发布 |
| 7 | POST | `/api/v1/admin/coupons/templates/{id}/offline` | 下线 |
| 8 | POST | `/api/v1/admin/coupons/codes/generate?templateId={id}` | 批量生成券码 |
| 9 | GET | `/api/v1/admin/coupons/codes?batchId={id}` | 券码分页 |
| 10 | POST | `/api/v1/admin/coupons/codes/void?code={code}` | 作废券码 |
| 11 | GET | `/api/v1/admin/coupons/instances` | 用户券分页 |
| 12 | POST | `/api/v1/admin/coupons/instances/{id}/freeze` | 冻结 |
| 13 | POST | `/api/v1/admin/coupons/instances/{id}/unfreeze` | 解冻 |
| 14 | POST | `/api/v1/admin/coupons/instances/grant` | 定向发券 |
| 15 | GET | `/api/v1/admin/coupons/stack-rules` | 叠加规则列表 |
| 16 | POST | `/api/v1/admin/coupons/stack-rules` | 新增/更新叠加规则 |
| 17 | DELETE | `/api/v1/admin/coupons/stack-rules?groupA=&groupB=` | 删除叠加规则 |

> **与设计稿（`server/docs/06-coupon-design.md` 第 8.1 节）的差异：**
> 1. 生成券码实际为 `POST /coupons/codes/generate?templateId=`，不是 `.../templates/{id}/codes/generate`。
> 2. 券码分页实际为 `GET /coupons/codes?batchId=`，不是 `.../templates/{id}/codes`。
> 3. 作废实际为 `POST /coupons/codes/void?code=`，不是 `.../codes/{code}/void`。
> 4. 定向发券实际为 `POST /coupons/instances/grant`，不是 `/coupons/grant`。
> 5. 叠加规则实际为 `POST`（UPSERT）+ `DELETE`，不是设计稿写的 `PUT`。

---

## 4. 模板管理

### 4.1 分页 `GET /api/v1/admin/coupons/templates`

Query：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `status` | 否 | 0/1/2 |
| `keyword` | 否 | 匹配模板名/编号（模糊） |
| `page` | 否 | 默认 1 |
| `pageSize` | 否 | 默认 20，上限 100 |

响应 `PageResult<CouponTemplate>`，元素字段：

```ts
interface CouponTemplate {
  id: number
  templateNo: string        // 模板编号，如 CT20261126XXXX
  name: string
  couponType: number        // 1/2/3
  scopeType: number         // 1/2/3/4
  scopeValues: string       // JSON 字符串
  thresholdAmount: number   // 分
  discountAmount: number    // 分
  discountRate: number | null
  maxDiscountAmount: number | null
  totalQuantity: number
  issuedQuantity: number
  perUserLimit: number
  validityType: number
  validStart: string | null   // yyyy-MM-dd HH:mm:ss
  validEnd: string | null
  validDays: number | null
  receiveStart: string | null // yyyy-MM-dd HH:mm:ss
  receiveEnd: string | null
  stackGroup: string
  stackable: number
  refundPolicy: number
  receiveChannels: string
  sortWeight: number
  status: number
  remark: string
  createdAt: string
  updatedAt: string
}
```

> 模板接口的时间字段由后端格式化为 `yyyy-MM-dd HH:mm:ss` 字符串，前端可直接展示。

### 4.2 详情 `GET /api/v1/admin/coupons/templates/{id}`

返回单个 `CouponTemplate`。

### 4.3 新建 `POST /api/v1/admin/coupons/templates`

Body（`@Valid` 校验，**标 * 为必填**）：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | * | 最长 128 |
| `couponType` | number | * | 1/2/3 |
| `scopeType` | number | * | 1/2/3/4 |
| `scopeValues` | string | | JSON 字符串，可空 |
| `thresholdAmount` | number | * | 分，≥ 0 |
| `discountAmount` | number | * | 分，≥ 0；**折扣券也必须传 0，不能省略** |
| `discountRate` | number | | 折扣券必填，`(0, 10]` |
| `maxDiscountAmount` | number | | 折扣券封顶，分 |
| `totalQuantity` | number | * | ≥ 0，0 表示不限量 |
| `perUserLimit` | number | * | 每人限领 |
| `validityType` | number | * | 1 固定区间 / 2 领取后 N 天 |
| `validStart` / `validEnd` | string | 条件 | `validityType=1` 必填 |
| `validDays` | number | 条件 | `validityType=2` 必填且 > 0 |
| `receiveStart` / `receiveEnd` | string | | 领取时间窗，可空 |
| `stackGroup` | string | * | 叠加组，如 `PLATFORM` |
| `stackable` | number | | 0/1 |
| `refundPolicy` | number | | 退款策略 |
| `receiveChannels` | string | | 如 `ALL` |
| `sortWeight` | number | | 排序权重 |
| `remark` | string | | 可为 null，后端兜底空串 |

示例（满减券）：

```json
{
  "name": "端午满减券",
  "couponType": 1,
  "scopeType": 1,
  "scopeValues": "{\"include\":[],\"exclude\":[]}",
  "thresholdAmount": 10000,
  "discountAmount": 2000,
  "totalQuantity": 1000,
  "perUserLimit": 1,
  "validityType": 2,
  "validDays": 30,
  "receiveChannels": "ALL",
  "stackGroup": "PLATFORM",
  "stackable": 1,
  "remark": "测试"
}
```

示例（折扣券，注意 `discountAmount` 传 0）：

```json
{
  "name": "夏装 8 折券",
  "couponType": 2,
  "scopeType": 2,
  "scopeValues": "{\"categoryIds\":[1,2]}",
  "thresholdAmount": 0,
  "discountAmount": 0,
  "discountRate": 8.0,
  "maxDiscountAmount": 5000,
  "totalQuantity": 500,
  "perUserLimit": 2,
  "validityType": 1,
  "validStart": "2026-06-01 00:00:00",
  "validEnd": "2026-08-31 23:59:59",
  "receiveStart": "2026-05-20 00:00:00",
  "receiveEnd": "2026-08-20 23:59:59",
  "receiveChannels": "ALL",
  "stackGroup": "SUMMER",
  "stackable": 0,
  "sortWeight": 100
}
```

服务端额外校验（返回 `PARAM_ERROR`）：

- `couponType` 必须是 1/2/3
- `thresholdAmount`、`discountAmount`、`totalQuantity` 不能为负
- `couponType=1` 必须有 `discountAmount`
- `couponType=2` 的 `discountRate` 必须在 `(0, 10]`
- 时间字符串必须严格匹配 `yyyy-MM-dd HH:mm:ss`

### 4.4 修改 `PUT /api/v1/admin/coupons/templates/{id}`

请求体与新建**完全一致**（同一个 DTO）。限制：

- 已发布（status=1）→ `42021 已发布模板关键字段不可修改`
- 草稿、已下线可改

### 4.5 删除 `DELETE /api/v1/admin/coupons/templates/{id}`

- 仅草稿（status=0）可删，否则 `42020`
- 模板已有领取/发放记录时返回 `42022`

### 4.6 发布 `POST .../{id}/publish`

发布前校验：

- 已发布 → `42020 模板已发布`
- `validityType=1` 必须有 `validStart`/`validEnd`
- `validityType=2` 必须有 `validDays > 0`
- `receiveStart` 不能晚于 `receiveEnd`

成功返回发布后的 `CouponTemplate`（status=1）。

### 4.7 下线 `POST .../{id}/offline`

仅已发布（status=1）可下线，否则 `42020 只有已发布可以下线`。成功返回 status=2。

### 4.8 页面交互约束（前端按钮可用性）

```ts
const canEdit    = (t) => t.status !== 1
const canDelete  = (t) => t.status === 0
const canPublish = (t) => t.status !== 1
const canOffline = (t) => t.status === 1
const canGenCode = (t) => true                 // 后端只校验模板存在
const canGrant   = (t) => t.status === 1       // 定向发券要求已发布
```

---

## 5. 券码管理

### 5.1 批量生成 `POST /api/v1/admin/coupons/codes/generate?templateId={id}`

Query：`templateId`（必填）。

Body：

```json
{ "count": 100 }
```

- `count` 必填，范围 `1 ~ 5000`，超出返回 `42030`
- 后端**不校验模板状态**，只校验模板存在（不存在 → `42000`）

响应 `CouponCodeBatch`：

```ts
interface CouponCodeBatch {
  id: number
  batchNo: string
  templateId: number
  total: number
  status: number
  createdAt: string   // yyyy-MM-dd HH:mm:ss
}
```

```json
{
  "id": 2,
  "batchNo": "B20261126123456XXXX",
  "templateId": 7,
  "total": 100,
  "status": 1,
  "createdAt": "2026-11-26 13:40:00"
}
```

> **重要：后端没有"批次列表"接口。** 前端券码页必须先生成（或从上一次生成结果中记住 `batchId`），才能查该批次券码。建议：
> - 生成成功后把 `batchId` 存在页面状态（或 URL query）中；
> - 券码页顶部提供"选择模板 → 生成批次"的入口；
> - 若需要跨会话查询历史批次，需后端补 `GET /coupons/codes/batches?templateId=`，当前 P1 未提供。

### 5.2 分页 `GET /api/v1/admin/coupons/codes?batchId={id}`

Query：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `batchId` | **是** | 批次 ID |
| `status` | 否 | 1 未使用 / 2 已使用 / 3 已作废 |
| `page` / `pageSize` | 否 | 同上 |

响应 `PageResult<CouponCode>`：

```ts
interface CouponCode {
  id: number
  code: string
  batchId: number
  templateId: number
  status: number
  memberId: number | null
  usedTime: string | null     // ISO，如 2026-11-26T13:40:00
  createdAt: string           // ISO
  updatedAt: string           // ISO
}
```

> **时间格式差异**：券码与券实例接口直接返回实体，`LocalDateTime` 会被序列化为 ISO 格式（`2026-11-26T13:40:00`），与模板接口的 `yyyy-MM-dd HH:mm:ss` 不一致。前端展示时需单独格式化。

### 5.3 作废 `POST /api/v1/admin/coupons/codes/void?code={code}`

- 券码不存在 → `42006`
- 已使用 → `42007`
- 已作废 → `42013`
- 成功返回 `data: null`

---

## 6. 用户券

### 6.1 分页 `GET /api/v1/admin/coupons/instances`

Query（均可选）：`memberId`、`templateId`、`status`、`page`、`pageSize`。

响应 `PageResult<CouponInstance>`：

```ts
interface CouponInstance {
  id: number
  couponNo: string          // 券号，如 U202611266CWAEPGN
  templateId: number
  memberId: number
  status: number            // 1/2/3/4/5
  source: string            // 如 ADMIN / CLAIM / CODE
  receiveChannel: string
  receiveTime: string       // ISO
  validStart: string        // ISO
  validEnd: string          // ISO
  usedTime: string | null
  orderNo: string
  lockOrderNo: string
  discountAmount: number    // 分
  version: number
  createdAt: string
  updatedAt: string
}
```

### 6.2 冻结 `POST .../instances/{id}/freeze`

Body：

```json
{ "reason": "风控测试" }
```

- `reason` 必填，最长 255
- 仅 `UNUSED(1)` 可冻结，否则 `42008`
- 成功返回冻结后的 `CouponInstance`（status=5）

### 6.3 解冻 `POST .../instances/{id}/unfreeze`

无 Body。

- 仅 `FROZEN(5)` 可解冻，否则 `42008`
- 若券在冻结期间已过 `validEnd`，解冻后直接置为 `EXPIRED(4)`，不会回到 `UNUSED`
- 成功返回解冻后的 `CouponInstance`

> 联调验证时若 `validEnd` 是过去时间，解冻后看到的 status=4 是**正确行为**，不是 bug。

### 6.4 定向发券 `POST .../instances/grant`

Body：

```json
{
  "templateId": 8,
  "memberIds": [9001, 9002],
  "reason": "客服补偿"
}
```

- `templateId` 必填，且模板**必须已发布（status=1）**，否则 `42020`
- `memberIds` 非空，单次最多 **500** 个
- 返回 `data: null`（当前实现不返回成功/失败明细）
- 逐人幂等发放，失败会记录但不影响其他人（服务端日志可查 `定向发券完成，templateId=..., total=..., success=...`）

> 注意：`memberIds` 必须是 `member` 表已存在的会员 ID，否则发放会失败。

---

## 7. 叠加规则

### 7.1 列表 `GET /api/v1/admin/coupons/stack-rules`

响应 `data` 直接是数组：

```ts
interface CouponStackRule {
  id: number
  groupA: string
  groupB: string
  allowStack: number      // 0/1
  maxStackCount: number   // 1~3
  status: number
  createdAt: string       // ISO
  updatedAt: string       // ISO
}
```

### 7.2 新增/更新 `POST /api/v1/admin/coupons/stack-rules`

UPSERT 语义：`groupA + groupB` 已存在则更新，否则新增。

```json
{
  "groupA": "PLATFORM",
  "groupB": "SUMMER",
  "allowStack": 0,
  "maxStackCount": 1,
  "status": 1
}
```

- `groupA`、`groupB` 必填
- `allowStack` 必填，**Integer 0/1**（不是 boolean）
- `maxStackCount` 范围 `1~3`
- 返回保存后的 `CouponStackRule`

### 7.3 删除 `DELETE /api/v1/admin/coupons/stack-rules?groupA={a}&groupB={b}`

两个 Query 参数都必填，返回 `data: null`。

---

## 8. 错误码

| Code | HTTP | 含义 | 前端处理建议 |
| --- | --- | --- | --- |
| 0 | 200 | 成功 | — |
| 40001 | 400 | 参数校验失败（含 `@Valid` 失败） | 表单提示 |
| 40002 | 400 | 请求体格式错误 | 检查 JSON |
| 40003 | 400 | 不支持的客户端类型 | 检查 `X-Client-Type` |
| 40100 | 401 | 未登录或缺少 Token | 跳登录 |
| 40101 | 401 | Access Token 过期 | 拦截器自动刷新（见 9.1） |
| 40102 | 401 | Token 已失效 | 跳登录 |
| 40103 | 401 | 账号或密码错误 | 登录页提示 |
| 40105 | 403 | 无权限 | 提示无权限 |
| 40108 | 401 | 刷新令牌无效 | 跳登录 |
| 42000 | 404 | 优惠券/模板不存在 | 刷新列表 |
| 42008 | 409 | 券状态不允许该操作 | 提示并刷新 |
| 42013 | 409 | 券码已作废 | 提示 |
| 42020 | 409 | 模板状态不允许该操作 | 提示并刷新 |
| 42021 | 409 | 已发布模板关键字段不可修改 | 提示先下线 |
| 42022 | 409 | 模板存在领取记录，不可删除 | 提示 |
| 42030 | 400 | 单次生成数量超过 5000 | 表单限制 |
| 42031 | 404 | 券码批次不存在 | 重新生成 |
| 42040 | 409 | 叠加规则重复 | 提示 |
| 50000 | 500 | 服务端未处理异常 | 提示并上报 traceId |

完整错误码见 `server/src/main/java/com/shopmall/common/ErrorCode.java`。

---

## 9. 前端落地建议

### 9.1 Token 过期行为（重要）

`http.ts` 在收到 `40101` 时会调用 `refreshTokens()`，但后端当前登录响应的 `refreshToken` 是**空字符串**。因此：

- `getRefreshToken()` 取不到值 → 直接抛 `40102` → 触发 `notifyAuthExpired()` 并跳登录页。
- 也就是说，**30 分钟后 token 过期会直接要求重新登录**，不会静默续期。联调时若突然被踢，属于预期行为。

### 9.2 建议新增 `src/api/coupon.ts`

> `CouponTemplateForm` 字段见 4.3（新建/修改共用），`CouponStackRuleForm` 见 7.2。

```ts
import { api } from '@/api/http'
import type { CouponTemplate, CouponCode, CouponInstance, CouponStackRule, PageResult } from '@/types/coupon'

const BASE = '/api/v1/admin/coupons'

export const couponApi = {
  // 模板
  pageTemplates: (params: { status?: number; keyword?: string; page?: number; pageSize?: number }) =>
    api.get<PageResult<CouponTemplate>>(`${BASE}/templates`, { params }),
  getTemplate: (id: number) => api.get<CouponTemplate>(`${BASE}/templates/${id}`),
  createTemplate: (body: CouponTemplateForm) => api.post<CouponTemplate>(`${BASE}/templates`, body),
  updateTemplate: (id: number, body: CouponTemplateForm) =>
    api.put<CouponTemplate>(`${BASE}/templates/${id}`, body),
  deleteTemplate: (id: number) => api.delete<void>(`${BASE}/templates/${id}`),
  publishTemplate: (id: number) => api.post<CouponTemplate>(`${BASE}/templates/${id}/publish`),
  offlineTemplate: (id: number) => api.post<CouponTemplate>(`${BASE}/templates/${id}/offline`),

  // 券码
  generateCodes: (templateId: number, count: number) =>
    api.post<CouponCodeBatch>(`${BASE}/codes/generate?templateId=${templateId}`, { count }),
  pageCodes: (params: { batchId: number; status?: number; page?: number; pageSize?: number }) =>
    api.get<PageResult<CouponCode>>(`${BASE}/codes`, { params }),
  voidCode: (code: string) => api.post<void>(`${BASE}/codes/void?code=${encodeURIComponent(code)}`),

  // 用户券
  pageInstances: (params: {
    memberId?: number
    templateId?: number
    status?: number
    page?: number
    pageSize?: number
  }) => api.get<PageResult<CouponInstance>>(`${BASE}/instances`, { params }),
  freezeInstance: (id: number, reason: string) =>
    api.post<CouponInstance>(`${BASE}/instances/${id}/freeze`, { reason }),
  unfreezeInstance: (id: number) => api.post<CouponInstance>(`${BASE}/instances/${id}/unfreeze`),
  grant: (body: { templateId: number; memberIds: number[]; reason?: string }) =>
    api.post<void>(`${BASE}/instances/grant`, body),

  // 叠加规则
  listStackRules: () => api.get<CouponStackRule[]>(`${BASE}/stack-rules`),
  upsertStackRule: (body: CouponStackRuleForm) => api.post<CouponStackRule>(`${BASE}/stack-rules`, body),
  deleteStackRule: (groupA: string, groupB: string) =>
    api.delete<void>(`${BASE}/stack-rules?groupA=${encodeURIComponent(groupA)}&groupB=${encodeURIComponent(groupB)}`),
}
```

### 9.3 页面与路由

现有 `src/router/routes.ts` 中 4 个优惠券路由已注册，组件均为 `PlaceholderView.vue`，按以下对应关系替换即可：

| 路由 | 权限码 | 替换为 |
| --- | --- | --- |
| `/coupon/templates` | `coupon:template:view` | `views/coupon/CouponTemplateListView.vue` |
| `/coupon/codes` | `coupon:code:view` | `views/coupon/CouponCodeView.vue` |
| `/coupon/instances` | `coupon:instance:view` | `views/coupon/CouponInstanceListView.vue` |
| `/coupon/rules` | `coupon:rule:view` | `views/coupon/CouponStackRuleView.vue` |

按钮级权限建议使用设计稿中的权限码（后端当前只做登录校验，按钮权限由前端 `v-permission` 控制）：

`coupon:template:create`、`coupon:template:update`、`coupon:template:delete`、`coupon:template:publish`、`coupon:template:offline`、`coupon:code:generate`、`coupon:code:void`、`coupon:instance:freeze`、`coupon:instance:unfreeze`、`coupon:rule:update`。

### 9.4 金额与格式化

- 所有金额字段单位是**分**（integer），展示时 `(v / 100).toFixed(2)`，提交时 `Math.round(元 * 100)`。
- `discountRate=8.5` 展示为"8.5 折"。
- `scopeValues` 是字符串，编辑表单需要 `JSON.parse` / `JSON.stringify`，并做 parse 失败兜底。

---

## 10. 联调冒烟脚本

后端启动后可直接复制执行（macOS / Linux）：

```bash
BASE=http://localhost:8080

TOKEN=$(curl -s -X POST "$BASE/api/v1/admin/auth/login" \
  -H 'Content-Type: application/json' -H 'X-Client-Type: PC' \
  -d '{"username":"admin","password":"Admin@123456","deviceId":"smoke-001"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')

AUTH=(-H "Authorization: Bearer $TOKEN" -H 'X-Client-Type: PC')

# 1 新建草稿
TPL=$(curl -s -X POST "$BASE/api/v1/admin/coupons/templates" "${AUTH[@]}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"冒烟满减券","couponType":1,"scopeType":1,"thresholdAmount":10000,"discountAmount":2000,"totalQuantity":100,"perUserLimit":1,"validityType":2,"validDays":30,"stackGroup":"PLATFORM","stackable":1,"receiveChannels":"ALL","remark":"smoke"}')
echo "$TPL"
TPL_ID=$(echo "$TPL" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')

# 2 发布
curl -s -X POST "$BASE/api/v1/admin/coupons/templates/$TPL_ID/publish" "${AUTH[@]}"; echo

# 3 生成券码
BATCH=$(curl -s -X POST "$BASE/api/v1/admin/coupons/codes/generate?templateId=$TPL_ID" "${AUTH[@]}" \
  -H 'Content-Type: application/json' -d '{"count":10}')
echo "$BATCH"
BATCH_ID=$(echo "$BATCH" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')

# 4 查券码
curl -s "$BASE/api/v1/admin/coupons/codes?batchId=$BATCH_ID&page=1&pageSize=5" "${AUTH[@]}"; echo

# 5 定向发券
curl -s -X POST "$BASE/api/v1/admin/coupons/instances/grant" "${AUTH[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateId\":$TPL_ID,\"memberIds\":[9001],\"reason\":\"smoke\"}"; echo

# 6 用户券分页
curl -s "$BASE/api/v1/admin/coupons/instances?page=1&pageSize=5" "${AUTH[@]}"; echo

# 7 叠加规则
curl -s -X POST "$BASE/api/v1/admin/coupons/stack-rules" "${AUTH[@]}" \
  -H 'Content-Type: application/json' \
  -d '{"groupA":"PLATFORM","groupB":"SUMMER","allowStack":0,"maxStackCount":1,"status":1}'; echo
curl -s "$BASE/api/v1/admin/coupons/stack-rules" "${AUTH[@]}"; echo
```

---

## 11. 已知坑清单

1. **后端进程容易被 IDE 沙箱回收**：前端报"服务暂时不可用"时，先看 8080 是否还有进程。
2. **登录字段是 `accessToken`**，不是 `token`；且必须带 `X-Client-Type` 和 `deviceId`。
3. **折扣券也必须传 `discountAmount: 0`**，DTO 上是 `@NotNull`，省略会 400。
4. **`stackGroup` 必填**，即使该券不参与叠加。
5. **券码生成字段是 `count`**，不是 `quantity`。
6. **`allowStack` 是 0/1 整数**，传 boolean 会 400。
7. **券码页必须先有 `batchId`**，后端没有批次列表接口。
8. **定向发券要求模板已发布**，草稿/下线模板会 42020。
9. **时间格式两套**：模板接口 `yyyy-MM-dd HH:mm:ss`，券码/券实例接口 ISO（`T` 分隔）。
10. **解冻后可能是 EXPIRED**：冻结期间过了有效期就是正常行为。
11. **token 30 分钟过期且无法静默刷新**（`refreshToken` 为空字符串），会被直接踢回登录页。
