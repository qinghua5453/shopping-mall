# 优惠券功能设计

> 状态：待实施（本文档与 SQL 为设计稿，尚未写 Java 代码）
>
> SQL：[docs/sql/V2__create_coupon_schema.sql](sql/V2__create_coupon_schema.sql)

## 1. 目标与范围

优惠券是电商交易链路的一部分，不能只做“领了不能用”的演示。本设计覆盖从后台建券到用户领券、下单锁券、核销、退款退券、过期的完整生命周期。

但当前工程只完成了管理端登录，会员模块和订单模块都还没有。因此按依赖关系分三期落地：

| 阶段 | 内容 | 依赖 | 状态 |
| --- | --- | --- | --- |
| P1 | 后台券模板管理、券码生成、用户券查询、叠加规则 | 现有 admin 登录 | 本轮设计重点 |
| P2 | 会员领券、我的券、券码兑换、试算选券 | 会员登录与 `member` 表 | 待会员模块 |
| P3 | 下单锁券、核销、释放、退款退券、过期任务 | 订单模块 | 待订单模块 |

P1 可以先独立上线：运营能在后台建券、发券、查券，即使会员端还没接，也不影响已有登录功能。

明确不做：

- 不做优惠券与商品域、订单域的强外键
- 不做分布式 ID、消息队列、分库分表
- 不做复杂的多券最优解搜索，一期只做单券最优，二期再加有限叠加
- 不做异步大批量发券任务，单次生成上限先控制在 5,000

## 2. 核心概念

| 概念 | 说明 |
| --- | --- |
| 券模板 `coupon_template` | 一套规则和库存，例如“满 100 减 20，总量 1 万张” |
| 券实例 `coupon_instance` | 某个会员实际持有的一张券，有独立券号和有效期 |
| 券码 `coupon_code` | 可兑换的凭证，兑换后生成券实例，适合线下或站外投放 |
| 券码批次 `coupon_code_batch` | 一次批量生成对应一个批次，便于统计和作废 |
| 领取记录 `coupon_receive_record` | 记录每次领取的成功或失败，提供幂等和风控依据 |
| 使用流水 `coupon_use_record` | 记录锁券、核销、释放、过期、退券、冻结、解冻 |
| 叠加组 `stack_group` | 同一组内的券互斥，不同组是否可叠加查 `coupon_stack_rule` |

关系：

```text
coupon_template 1 ── N coupon_instance
coupon_template 1 ── N coupon_code
coupon_code_batch 1 ── N coupon_code
coupon_template 1 ── N coupon_receive_record
coupon_instance 1 ── N coupon_use_record
coupon_stack_rule 独立配置表
```

不建外键：与现有 `sys_*` 表保持一致，由应用层保证引用完整性，避免后续分库和批量操作受限。

## 3. 数据模型

完整 DDL 见 [V2__create_coupon_schema.sql](sql/V2__create_coupon_schema.sql)，这里说明关键字段和索引意图。

### 3.1 coupon_template

| 字段 | 说明 |
| --- | --- |
| `template_no` | 对外模板编号，唯一 |
| `coupon_type` | 1 满减、2 折扣、3 无门槛 |
| `scope_type` / `scope_values` | 1 全场、2 类目、3 SPU、4 SKU；范围用 JSON 存 include/exclude |
| `threshold_amount` | 门槛，单位分，0 表示无门槛 |
| `discount_amount` | 固定优惠金额，单位分 |
| `discount_rate` | 折扣率，8.50 表示 8.5 折 |
| `max_discount_amount` | 折扣券封顶，0 表示不封顶 |
| `total_quantity` / `issued_quantity` | 总量与已发放量，0 表示不限量 |
| `per_user_limit` | 每人限领 |
| `validity_type` | 1 固定区间，2 领取后 N 天 |
| `stack_group` / `stackable` | 叠加组与是否允许跨组叠加 |
| `status` | 0 草稿、1 已发布、2 已下线 |
| `version` | 乐观锁 |

关键索引：

- `uk_template_no`：模板编号唯一
- `idx_status_receive`：领券中心按状态和领取时间筛选

### 3.2 coupon_instance

| 字段 | 说明 |
| --- | --- |
| `coupon_no` | 券号，唯一，对外展示 |
| `status` | 1 未使用、2 锁定、3 已使用、4 已过期、5 已冻结 |
| `source` | CLAIM 领取、CODE 券码兑换、ADMIN 后台发放 |
| `valid_start` / `valid_end` | 这一张券的实际有效期 |
| `order_no` | 核销订单号 |
| `lock_order_no` | 当前锁定的订单号 |
| `discount_amount` | 实际优惠金额 |

关键索引：

- `idx_member_status_end`：我的券按会员、状态、有效期查询
- `idx_status_end`：过期任务扫描
- `idx_lock_order_no`：按订单查锁定的券

### 3.3 coupon_code / coupon_code_batch

券码只负责“兑换资格”，本身不承载优惠规则。兑换成功后生成 `coupon_instance`，券码状态变为已使用。

- `uk_code`：券码唯一
- `idx_batch_status`：按批次查看未使用/已使用数量
- 单次生成上限 5,000，超过则拒绝，一期不做异步任务

### 3.4 coupon_receive_record

- `uk_request_no`：客户端 `Idempotency-Key` 唯一，重复请求返回首次结果
- 成功和失败都记录，失败记录不占用新的幂等键

### 3.5 coupon_use_record

- `uk_coupon_action_order (coupon_id, action, order_no)`：保证同一张券对同一订单的动作幂等
- `order_no` 允许 NULL：过期、冻结、解冻不属于订单动作，可以重复记录；MySQL 唯一索引允许多个 NULL

### 3.6 coupon_stack_rule

- `(group_a, group_b)` 唯一，写入时按字典序排序
- 同组互斥是代码硬规则，不查表
- 未配置的组合默认互斥，失败安全

### 3.7 与旧文档的差异

`docs/02-database.md` 是生产级版本，本设计做两点简化：

| 项 | 旧方案 | 本方案 | 原因 |
| --- | --- | --- | --- |
| 主键 | 应用生成雪花 ID | `AUTO_INCREMENT` | 单体阶段少一个 ID 生成器，后续分库再换 |
| `coupon_use_record.order_no` | NOT NULL DEFAULT '' | 允许 NULL | 避免非订单动作被唯一键误判为重复 |

其余字段、状态和索引保持一致，后续需要时可以平滑迁移。

## 4. 状态机

### 4.1 模板

```text
DRAFT(0) ──发布──> PUBLISHED(1) ──下线──> OFFLINE(2)
   ↑                    │                    │
   └──────不允许─────────┘                    │
                        └────重新发布──────────┘
```

规则：

- 只有草稿可以修改全部字段、删除
- 已发布后，类型、面额、门槛、总量、有效期不可修改，只能下线后重建
- 下线后允许重新发布，但必须重新校验领取时间和有效期
- 有领取记录或券码的模板不允许删除

### 4.2 券实例

```text
UNUSED(1) ──锁券──> LOCKED(2) ──核销──> USED(3)
    │  ↑               │
    │  └────释放───────┘
    ├──过期──> EXPIRED(4)
    ├──冻结──> FROZEN(5) ──解冻──> UNUSED(1)
    └──核销（无锁）──> USED(3)

USED(3) ──退款退券──> UNUSED(1)   （仅 refund_policy=1）
```

规则：

- 只有 `UNUSED` 可以锁定、冻结、被过期任务处理
- `LOCKED` 只能核销或释放，不能冻结
- `FROZEN` 只能解冻，不能使用
- 退款退券要求订单已支付且券未过期，退券后有效期不变
- 所有状态变更用条件更新，受影响行数为 0 即视为并发冲突

### 4.3 券码

```text
UNUSED(1) ──兑换──> USED(2)
UNUSED(1) ──作废──> VOID(3)
```

已使用或已作废的券码不可再次兑换。

## 5. 优惠计算

金额统一用 `BIGINT` 存分，前端展示时除以 100，避免浮点误差。

### 5.1 单券

| 类型 | 条件 | 优惠金额 |
| --- | --- | --- |
| 满减 | `适用金额 >= threshold_amount` | `discount_amount` |
| 无门槛 | 无 | `min(discount_amount, 适用金额)` |
| 折扣 | `适用金额 >= threshold_amount` | `适用金额 - 适用金额 × discount_rate / 10` |

补充规则：

- 优惠金额不能超过适用金额，实付最低为 0
- 折扣券计算后若超过 `max_discount_amount`，按封顶值取
- 四舍五入统一用 `HALF_UP`，按分取整
- 适用范围按 `scope_type` 和 `scope_values` 计算“适用金额”，排除商品不计入门槛

### 5.2 多券叠加

一期只支持单券，二期再开放叠加：

1. 同 `stack_group` 的券互斥，只保留优惠最大的一张
2. 不同组查 `coupon_stack_rule`，未配置默认互斥
3. 叠加总张数不超过规则里的 `max_stack_count`，且不超过 3 张
4. 计算顺序：先固定金额券（满减、无门槛），再折扣券；折扣券基于剩余金额计算
5. 不做全局最优搜索，按“每组选最优 + 张数上限”的贪心策略，结果可解释

## 6. 幂等、并发与库存

### 6.1 领券

```text
1. 校验模板已发布、在领取时间内、渠道匹配
2. INSERT coupon_receive_record(request_no 唯一)
   - 唯一键冲突：返回首次结果
3. 校验每人限领：COUNT(instance) < per_user_limit
4. 扣库存：
   UPDATE coupon_template
   SET issued_quantity = issued_quantity + 1
   WHERE id = ?
     AND (total_quantity = 0 OR issued_quantity < total_quantity)
   - 影响行数 0：已领完，返回 42003
5. INSERT coupon_instance
6. 回写 receive_record.coupon_id、success=1
7. 同一事务提交
```

限领并发说明：

- 一期用“事务内 COUNT + 索引”保证常规场景
- 同一用户高频并发仍可能超领，二期可加 Redis 计数前置拦截，或用生成列唯一索引兜底
- 库存以数据库条件更新为准，Redis 只做前置拦截，不作为最终依据

### 6.2 锁券与核销

锁券：

```sql
UPDATE coupon_instance
SET status = 2, lock_order_no = #{orderNo}, version = version + 1
WHERE id = #{couponId}
  AND member_id = #{memberId}
  AND status = 1
  AND valid_end > NOW(3)
```

影响行数为 0 时，按当前状态返回 `42008` / `42010` / `42005`。

核销：

```sql
UPDATE coupon_instance
SET status = 3, order_no = #{orderNo}, used_time = NOW(3),
    discount_amount = #{discountAmount}, lock_order_no = '', version = version + 1
WHERE id = #{couponId}
  AND status = 2
  AND lock_order_no = #{orderNo}
```

锁券、核销、释放、退券都要写 `coupon_use_record`，靠唯一键保证重复回调不重复扣减。

### 6.3 冻结

管理端冻结只允许 `UNUSED`：

```sql
UPDATE coupon_instance
SET status = 5, version = version + 1
WHERE id = #{id} AND status = 1
```

解冻时若券已过期，直接置为 `EXPIRED`，不回到 `UNUSED`。

## 7. Redis 使用

一期可以完全不用 Redis 也能跑通后台管理。会员领券上线后再加：

| Key | 类型 | TTL | 用途 |
| --- | --- | --- | --- |
| `coupon:stock:{templateId}` | String | 活动结束后 1 天 | 库存预扣，前置拦截 |
| `coupon:user:{templateId}:{memberId}` | String | 活动结束后 1 天 | 用户已领数量，减少数据库查询 |
| `coupon:center:page:{page}` | String | 30 秒 | 领券中心缓存，可选 |

原则：Redis 只做加速和拦截，最终一致性以 MySQL 条件更新为准，Redis 异常不能导致超发。

## 8. 接口设计

统一响应结构沿用现有 `ApiResponse`，分页参数 `page` / `pageSize`，`pageSize` 最大 100。

### 8.1 管理端

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/coupons/templates` | `coupon:template:view` | 模板分页 |
| GET | `/api/v1/admin/coupons/templates/{id}` | `coupon:template:view` | 模板详情 |
| POST | `/api/v1/admin/coupons/templates` | `coupon:template:create` | 新建草稿 |
| PUT | `/api/v1/admin/coupons/templates/{id}` | `coupon:template:update` | 修改草稿或已下线模板 |
| DELETE | `/api/v1/admin/coupons/templates/{id}` | `coupon:template:delete` | 删除草稿 |
| POST | `/api/v1/admin/coupons/templates/{id}/publish` | `coupon:template:publish` | 发布 |
| POST | `/api/v1/admin/coupons/templates/{id}/offline` | `coupon:template:offline` | 下线 |
| POST | `/api/v1/admin/coupons/templates/{id}/codes/generate` | `coupon:code:generate` | 批量生成券码 |
| GET | `/api/v1/admin/coupons/templates/{id}/codes` | `coupon:code:view` | 券码分页 |
| POST | `/api/v1/admin/coupons/codes/{code}/void` | `coupon:code:void` | 作废券码 |
| GET | `/api/v1/admin/coupons/instances` | `coupon:instance:view` | 用户券分页 |
| POST | `/api/v1/admin/coupons/instances/{id}/freeze` | `coupon:instance:freeze` | 冻结 |
| POST | `/api/v1/admin/coupons/instances/{id}/unfreeze` | `coupon:instance:unfreeze` | 解冻 |
| POST | `/api/v1/admin/coupons/grant` | `coupon:template:publish` | 定向发券 |
| GET | `/api/v1/admin/coupons/stack-rules` | `coupon:rule:view` | 叠加规则列表 |
| PUT | `/api/v1/admin/coupons/stack-rules` | `coupon:rule:update` | 更新叠加规则 |

新建模板请求：

```json
{
  "name": "满 100 减 20",
  "couponType": 1,
  "scopeType": 1,
  "scopeValues": { "include": [], "exclude": [] },
  "thresholdAmount": 10000,
  "discountAmount": 2000,
  "totalQuantity": 10000,
  "perUserLimit": 1,
  "validityType": 1,
  "validStart": "2026-12-01 00:00:00",
  "validEnd": "2026-12-31 23:59:59",
  "receiveStart": "2026-11-25 00:00:00",
  "receiveEnd": "2026-12-30 23:59:59",
  "stackGroup": "PLATFORM",
  "stackable": 1,
  "refundPolicy": 1,
  "receiveChannels": "ALL",
  "sortWeight": 100
}
```

批量生成券码：

```json
{ "count": 1000 }
```

返回：

```json
{
  "batchNo": "B202611260001",
  "count": 1000,
  "status": 1
}
```

定向发券：

```json
{
  "templateId": "100",
  "memberIds": ["9001", "9002"],
  "reason": "客服补偿"
}
```

要求：单次最多 500 人，逐人幂等，返回成功/失败明细。

冻结/解冻请求需带 `reason`，写入使用流水。

### 8.2 会员端（P2）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/member/coupons/center` | 领券中心 |
| GET | `/api/v1/member/coupons/center/{templateId}` | 模板详情 |
| POST | `/api/v1/member/coupons/{templateId}/claim` | 领取 |
| POST | `/api/v1/member/coupons/redeem` | 券码兑换 |
| GET | `/api/v1/member/coupons` | 我的券 |
| GET | `/api/v1/member/coupons/{couponId}` | 券详情 |
| GET | `/api/v1/member/coupons/counts` | 各状态数量 |
| POST | `/api/v1/member/coupons/preview` | 试算与智能选券 |

会员端依赖会员登录，当前工程没有会员模块，因此只保留契约，不实现。

### 8.3 错误码

沿用 `docs/03-api.md` 的优惠券错误码：

| Code | HTTP | 含义 |
| --- | --- | --- |
| 42001 | 409 | 活动未开始 |
| 42002 | 409 | 活动已结束 |
| 42003 | 409 | 优惠券已领完 |
| 42004 | 409 | 超过每人限领数量 |
| 42005 | 404 | 优惠券不存在或不属于当前用户 |
| 42006 | 400 | 券码无效 |
| 42007 | 409 | 券码已被使用 |
| 42008 | 409 | 券状态不允许该操作 |
| 42009 | 400 | 未满足使用条件 |
| 42010 | 409 | 优惠券已过期 |
| 42011 | 409 | 不满足叠加规则 |
| 42012 | 409 | 优惠券已冻结 |
| 42013 | 409 | 券码已作废 |
| 42014 | 409 | 幂等键已被不同请求使用 |

需要在现有 `ErrorCode` 枚举中补充这些码，HTTP 状态与上表一致。

### 8.4 权限说明

V1 已写入 4 个优惠券菜单权限：

- `coupon:template:view`
- `coupon:code:view`
- `coupon:instance:view`
- `coupon:rule:view`

SQL 文件中补充了按钮权限。当前后端只有登录拦截器，没有 `@RequirePermission`，因此：

- P1 先用登录保护，权限码用于前端菜单和按钮显隐
- `SUPER_ADMIN` 返回 `["*"]`，不受影响
- 后续增加权限注解时，再在后端强制校验

## 9. 后台页面

菜单已在 V1 中初始化，页面规划：

| 路由 | 页面 |
| --- | --- |
| `/coupon/templates` | 券模板列表、筛选、发布、下线 |
| `/coupon/templates/:id` | 模板编辑、券码生成入口 |
| `/coupon/codes` | 券码批次与明细、作废 |
| `/coupon/instances` | 用户券查询、冻结、解冻 |
| `/coupon/rules` | 叠加规则矩阵 |

P1 只需要模板列表和编辑页就能闭环，其余页面可以后补。

## 10. 关键流程

### 10.1 领券（P2）

```text
会员请求 claim + Idempotency-Key
→ 校验登录、渠道、模板状态、领取时间
→ 幂等记录插入，冲突则返回首次结果
→ 校验每人限领
→ 条件更新库存
→ 生成券实例（计算 valid_start/valid_end）
→ 回写领取记录
→ 返回券信息
```

### 10.2 锁券与核销（P3）

```text
下单页 preview 选券
→ 创建订单时 lock：UNUSED → LOCKED，绑定 order_no
→ 支付成功回调 use：LOCKED → USED，记录优惠金额
→ 支付超时/取消 release：LOCKED → UNUSED
→ 退款 refund：USED → UNUSED（refund_policy=1 且未过期）
```

### 10.3 过期

```text
定时任务分批扫描 status=1 AND valid_end <= NOW()
→ 条件更新为 EXPIRED
→ 写使用流水 action=4
```

建议每 5 分钟执行一次，每批 500 条，避免长事务。

## 11. 定时任务

使用 Spring `@Scheduled`，不引入任务调度中间件：

| 任务 | 频率 | 说明 |
| --- | --- | --- |
| 券过期 | 每 5 分钟 | 批量更新未使用且已过期的券 |
| 模板自动下线 | 每小时 | `receive_end` 已过且仍发布的模板置为下线，可选 |
| 券码批次状态修正 | 每小时 | 处理生成中断的批次，可选 |

多实例部署时需要加分布式锁，单机阶段不处理。

## 12. 代码落点（单模块）

按现有目录规划，不新增 Maven 模块：

```text
src/main/java/com/shopmall/
├── controller/
│   ├── CouponTemplateController.java
│   ├── CouponCodeController.java
│   ├── CouponInstanceController.java
│   └── CouponStackRuleController.java
├── service/
│   ├── CouponTemplateService.java
│   ├── CouponCodeService.java
│   ├── CouponInstanceService.java
│   ├── CouponStackRuleService.java
│   └── CouponCalculator.java
├── mapper/
│   ├── CouponTemplateMapper.java
│   ├── CouponInstanceMapper.java
│   ├── CouponCodeMapper.java
│   ├── CouponCodeBatchMapper.java
│   ├── CouponReceiveRecordMapper.java
│   ├── CouponUseRecordMapper.java
│   └── CouponStackRuleMapper.java
├── entity/        # 7 个表对应实体
├── dto/           # 请求与响应
└── util/          # 券号、模板编号生成
```

SQL 全部放 `resources/mapper/*.xml`，与现有登录模块保持一致，不使用注解 SQL。

## 13. 实施步骤

| 阶段 | 内容 | 验收 |
| --- | --- | --- |
| C0 | 确认设计，把 SQL 移入 `db/migration` 并执行 | 7 张表创建成功，Flyway 校验通过 |
| C1 | 模板 CRUD + 发布/下线 | 后台能建券、发布、下线；非法字段被拒绝 |
| C2 | 券码批次生成、分页、作废 | 生成 1000 条券码，重复码为 0 |
| C3 | 用户券分页、冻结/解冻、定向发券 | 状态流转正确，流水可查 |
| C4 | 叠加规则查询与更新 | 未配置组合默认互斥 |
| C5 | 会员领券 | 依赖会员模块，另排期 |
| C6 | 锁券核销 | 依赖订单模块，另排期 |

C1~C4 可以在当前工程内完成，不需要会员和订单。

## 14. 测试策略

| 测试 | 内容 |
| --- | --- |
| `CouponCalculatorTest` | 满减、无门槛、折扣、封顶、门槛不满足、范围排除 |
| `CouponTemplateServiceTest` | 发布校验、状态机、关键字段不可改 |
| `CouponCodeServiceTest` | 批量生成唯一性、作废、重复兑换 |
| `CouponInstanceServiceTest` | 冻结/解冻、条件更新失败分支 |
| 接口验证 | curl 或 admin 页面完成建券、发布、生成券码 |

并发与库存暂不做自动化压测，先用条件更新保证正确性，压测放到会员端上线前。

## 15. 风险与边界

- **会员模块缺失**：领券、我的券无法端到端验证，契约先冻结，避免后期改接口
- **权限未强制**：当前只有登录校验，后台按钮权限是前端控制，不能视为安全边界
- **JSON 字段映射**：`scope_values` 需要统一 TypeHandler 或在 Service 层转换，避免 MyBatis 直接映射失败
- **金额单位**：所有接口和数据库都用分，前端展示层转换，禁止在服务端用 `double`
- **时间边界**：有效期判断统一用数据库 `NOW(3)` 或同一时区，避免应用与数据库时区不一致
- **券码生成**：单次上限 5,000，字符集去掉易混淆的 0/O、1/I，唯一冲突时重试
- **重复回调**：订单支付回调必须靠 `coupon_use_record` 唯一键兜底，不能只靠状态判断
- **数据增长**：领取和使用流水只增不改，后续按月归档
