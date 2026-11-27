# 管理端权限系统设计（RBAC）

> 状态：待实施（本文档为设计稿，SQL 草稿见 [docs/sql/V3__rbac_seed_and_operation_log.sql](sql/V3__rbac_seed_and_operation_log.sql)）
>
> 对应路线图 [04-roadmap.md](04-roadmap.md) 的 M2 阶段。

## 1. 目标与范围

为管理端（`/api/v1/admin/**`）提供生产可用的 RBAC 能力：管理员按角色获得权限，权限同时驱动「后端接口鉴权」和「前端菜单/按钮显隐」，并支持缓存、即时生效、审计。

### 1.1 现状盘点（已具备）

V1 迁移 [V1__create_admin_auth_schema.sql](../src/main/resources/db/migration/V1__create_admin_auth_schema.sql) 已落地：

| 项 | 现状 |
| --- | --- |
| 表 | `sys_admin` / `sys_role` / `sys_permission` / `sys_admin_role` / `sys_role_permission` / `sys_admin_login_log` 已建 |
| 认证 | JWT + Redis 会话（`AdminSessionService`），`LoginInterceptor` 仅校验登录态，不校验权限 |
| 超级管理员 | 内置角色 `SUPER_ADMIN`（id=1），`BootstrapAdminInitializer` 启动创建并绑定 |
| `/me` | `AdminAuthService.me()` 已实现 SUPER_ADMIN 短路、权限码列表、菜单树构建 |
| 权限种子 | 仅写入「目录/菜单」类型（permission_type 1/2），无按钮/接口级权限，也无 `sys_role_permission` 关联数据 |

### 1.2 缺失项（本期补齐）

1. **请求级鉴权**：每个管理端写操作/敏感读操作按权限码拦截，无权限返回 403。
2. **权限缓存**：权限码与菜单缓存到 Redis，避免每次请求/每次 `/me` 查 DB；变更后即时失效。
3. **管理接口**：管理员、角色、权限的 CRUD 与授权（分配角色、分配权限）。
4. **种子补齐**：按钮/接口级权限种子 + SUPER_ADMIN 全量授权 + 内置「运营管理员」角色。
5. **操作审计**：`@OperationLog` AOP 记录管理端写操作。
6. **数据权限**：`data_scope` 落地（全部 / 仅本人），为后续会员/订单查询预留。

### 1.3 明确不做

- 不引入 Spring Security，沿用现有纯 MVC 拦截器体系（与项目硬约束一致：单模块 MVC + 纯 MyBatis）。
- 不做权限的运行时动态表达式（SpEL），权限码即静态字符串。
- 不做字段级/行级细粒度数据权限（本期只有「全部 / 仅本人」两档）。
- 不做租户隔离、不做 ABAC。
- 不做权限申请审批流。

## 2. 核心模型

### 2.1 实体关系

```text
sys_admin  N ── N  sys_role  N ── N  sys_permission
              (sys_admin_role)    (sys_role_permission)
```

- 一个管理员可挂多个角色，权限取并集。
- 权限是树形（`parent_id`），类型分 4 档（见 §3）。
- 不建外键，与现有 `sys_*` / `coupon_*` 表保持一致，由应用层保证引用完整。

### 2.2 SUPER_ADMIN 约定

- `sys_role.code = 'SUPER_ADMIN'`，`id = 1`，内置不可删除、不可禁用、不可改 code。
- 拥有 SUPER_ADMIN 角色的管理员**短路鉴权**：权限码为 `["*"]`，菜单返回全量，不参与缓存命中计算（仍可缓存以减少 DB 压力，但失效策略需覆盖）。
- 任何管理员都不能移除自己身上的 SUPER_ADMIN 角色（防自锁）。

## 3. 权限模型与编码规范

### 3.1 权限类型（permission_type）

| 值 | 类型 | 用途 | 前端表现 |
| --- | --- | --- | --- |
| 1 | 目录 | 一级导航分组 | 侧边栏分组 |
| 2 | 菜单 | 可路由页面 | 菜单项 + 路由 |
| 3 | 按钮 | 页面内操作 | 按钮 `v-permission` 显隐 |
| 4 | 接口 | 后端 API 控制点 | 仅后端注解使用，不渲染 |

> 菜单（2）的 `code` 同时作为「进入该页面」的接口权限；按钮（3）是页面内子操作。类型 4 用于「无对应页面、仅 API」的控制点，本期以类型 3 为主，类型 4 保留扩展。

### 3.2 编码规范

格式：`模块:资源:动作`，全小写，冒号分隔。

- 模块：`system` / `member` / `coupon` / `order` / `product` …
- 资源：`admin` / `role` / `permission` / `template` / `code` …
- 动作：`view` / `create` / `update` / `delete` / `publish` / `offline` / `generate` / `void` / `grant` / `freeze` …

示例（按钮级）：

```
coupon:template:view       进入券模板列表页（菜单）
coupon:template:create     新建
coupon:template:update     编辑
coupon:template:delete     删除
coupon:template:publish    发布
coupon:template:offline    下线
coupon:code:generate       批量生成券码
coupon:code:void           作废券码
coupon:instance:grant      定向发券
coupon:instance:freeze     冻结
coupon:rule:upsert         编辑叠加规则
system:admin:create / update / delete / assign-role / reset-password
system:role:create / update / delete / assign-permission
system:permission:update
system:log:view
```

约束：

- `code` 全局唯一（已建 `uk_code`），一经上线不可改名（改名将导致历史授权失效与前端判断失效）。
- 删除权限前必须确认无角色引用与前端硬编码引用。

## 4. 鉴权流程

### 4.1 请求链路

```
请求 → LoginInterceptor（校验 Token + 会话，注入 adminId）
     → PermissionInterceptor（读取方法注解 @RequirePermission，校验权限码）
     → Controller → Service
```

- `LoginInterceptor` 保持现状，只做认证。
- 新增 `PermissionInterceptor`（或合并进 LoginInterceptor 的 preHandle），在认证通过后执行鉴权。

### 4.2 注解驱动

自定义注解（不引入 Spring Security）：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();          // 权限码，任一命中即通过
    Logical logical() default Logical.AND;  // AND/OR，默认 AND
}
```

用法：

```java
@PostMapping
@RequirePermission("coupon:template:create")
public ApiResponse<...> create(...) { ... }
```

- 未标注解的管理端方法：默认「已登录即可访问」（只读公开接口，如字典、自身资料）。需在 `WebConfig` 显式声明策略，避免误开放。
- 推荐在 `Coupon*Controller`、后续 `System*Controller` 的每个写方法上标注。

### 4.3 校验逻辑（PermissionInterceptor）

```
1. 从 request.getAttribute(ADMIN_ID_ATTRIBUTE) 取 adminId
2. 通过 AdminPermissionService 取该管理员的权限码集合（命中缓存 / 回源 DB）
3. 若包含 SUPER_ADMIN → 通过
4. 否则按注解 value 与 logical 比对：
   - AND：所有码都在集合中才通过
   - OR ：任一码在集合中即通过
5. 不通过 → 返回 ErrorCode.FORBIDDEN(40105)，HTTP 403
```

## 5. 权限缓存与失效

### 5.1 为什么要缓存

- `/me`、`PermissionInterceptor` 都会查权限；高频请求下每次查 4 张表 JOIN 不可接受。
- 「修改角色权限后，相关管理员下一次请求即生效」要求可控的失效策略。

### 5.2 Redis Key 设计

| Key | 类型 | 内容 | TTL |
| --- | --- | --- | --- |
| `auth:rbac:perms:{adminId}` | String(JSON) | `{ "roleCodes":[...], "permCodes":[...], "super":true/false }` | 30 分钟 |
| `auth:rbac:menu:{adminId}` | String(JSON) | 菜单树（与 /me 一致） | 30 分钟 |
| `auth:rbac:admin_ids` | Set | 当前有缓存的 adminId 集合，用于批量失效 | 持久（随失效清理） |
| `auth:rbac:version` | String(int) | 全局版本号，每次 RBAC 写操作 +1 | 持久 |

> SUPER_ADMIN 的 `permCodes` 存 `["*"]`，菜单存全量，同样走缓存以降低 DB 压力。

### 5.3 回源逻辑（AdminPermissionService）

```
getPermissionContext(adminId):
  ctx = redis.get("auth:rbac:perms:{adminId}")
  if ctx != null: return ctx
  roles = adminMapper.selectRoleCodes(adminId)
  if roles contains "SUPER_ADMIN":
    ctx = { super:true, permCodes:["*"], roleCodes:roles }
    menu = permissionMapper.selectAllActive() → buildMenus
  else:
    perms = permissionMapper.selectByAdminId(adminId)
    ctx = { super:false, permCodes: perms.code 去重, roleCodes:roles }
    menu = buildMenus(perms)
  redis.setex(perms key, 30min)
  redis.setex(menu key, 30min)
  redis.sadd("auth:rbac:admin_ids", adminId)
  return ctx
```

### 5.4 失效策略

任一 RBAC 写操作后，**清空全部管理员的权限与菜单缓存**（管理员规模 < 数千，全量失效可接受；实现简单、不易漏）：

```
invalidateAll():
  ids = redis.smembers("auth:rbac:admin_ids")
  for id in ids:
    redis.del("auth:rbac:perms:{id}", "auth:rbac:menu:{id}")
  redis.del("auth:rbac:admin_ids")
  redis.incr("auth:rbac:version")
```

触发点（由 Service 层在事务提交后调用，避免事务回滚却清缓存）：

- 角色权限变更（分配/回收权限）
- 管理员角色变更（分配/回收角色）
- 管理员启用/禁用、删除
- 权限的新增/编辑/状态变更
- 角色的新增/编辑/状态变更

> 若后续管理员规模增长，可升级为「按受影响 adminId 精准失效」：维护 `role→admins` 反向索引。本期不做。

### 5.5 与会话失效的协同

- 管理员被禁用/删除：除清权限缓存外，还需让其已登录会话立即失效。
- 复用 `AdminSessionService`：新增 `deleteAllByAdmin(adminId)`，用 Redis 模式匹配 `auth:admin:session:*` 难以按 adminId 反查。
- 方案：会话 key 改为 `auth:admin:session:{adminId}:{sessionId}`，禁用时 `keys`/`SCAN` 匹配前缀删除；或维护 `auth:admin:sessions:{adminId}` Set 存 sessionId。本期采用后者：创建会话时 `sadd`，校验时不依赖该 Set，删除全量时遍历 Set 删 key。详见 §10 安全约束。

## 6. 管理端接口设计

所有接口前缀 `/api/v1/admin`，均需登录（除 `/auth/login`）；写操作需对应权限码并记录操作日志。

### 6.1 认证（扩展）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/auth/me` | 登录 | 已存在，返回 profile / permissions / menus，改为走缓存 |
| GET | `/auth/menus` | 登录 | 仅返回菜单树（前端路由守卫用，可选） |
| POST | `/auth/logout` | 登录 | 已存在 |
| POST | `/auth/password` | 登录 | 修改自身密码（旧密码校验），成功后清自身会话与权限缓存 |

### 6.2 管理员管理

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/admins` | `system:admin:view` | 分页列表，支持用户名/状态筛选 |
| GET | `/system/admins/{id}` | `system:admin:view` | 详情（含已分配角色 ID） |
| POST | `/system/admins` | `system:admin:create` | 新建（初始随机密码，强制首次改密） |
| PUT | `/system/admins/{id}` | `system:admin:update` | 编辑昵称/邮箱/状态 |
| DELETE | `/system/admins/{id}` | `system:admin:delete` | 删除（不可删自己、不可删 SUPER_ADMIN） |
| POST | `/system/admins/{id}/roles` | `system:admin:assign-role` | 分配角色（全量覆盖） |
| POST | `/system/admins/{id}/reset-password` | `system:admin:reset-password` | 重置密码，返回新密码（强制改密） |

### 6.3 角色管理

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/roles` | `system:role:view` | 列表（不分页，数量少） |
| GET | `/system/roles/{id}` | `system:role:view` | 详情（含已分配权限 ID） |
| POST | `/system/roles` | `system:role:create` | 新建角色 |
| PUT | `/system/roles/{id}` | `system:role:update` | 编辑（SUPER_ADMIN 的 code/状态不可改） |
| DELETE | `/system/roles/{id}` | `system:role:delete` | 删除（SUPER_ADMIN 不可删；删除前校验无管理员绑定） |
| POST | `/system/roles/{id}/permissions` | `system:role:assign-permission` | 分配权限（全量覆盖） |

### 6.4 权限管理

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/permissions` | `system:permission:view` 或 `system:role:view` | 返回权限树（含按钮/接口节点），供角色授权勾选 |
| PUT | `/system/permissions/{id}` | `system:permission:update` | 编辑名称/图标/排序/可见性（code/type 不可改） |

> 权限的新增/删除本期不开放接口，通过 Flyway 迁移维护种子，避免运营误删导致鉴权错乱。

### 6.5 操作日志

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/logs` | `system:log:view` | 分页查询操作日志，支持操作人/模块/时间筛选 |

> 登录日志（`sys_admin_login_log`）暂不开放查询接口，后续可合并展示。

## 7. 数据权限（data_scope）

`sys_role.data_scope` 已存在，取值：

| 值 | 含义 | 行为 |
| --- | --- | --- |
| 1 | 全部数据 | 查询不过滤创建人 |
| 2 | 仅本人 | 查询追加 `created_by = currentAdminId` 条件 |

落地方式：

- 新增 `DataScope` 上下文（ThreadLocal），在 `PermissionInterceptor` 中根据当前管理员角色的最小 `data_scope` 计算并写入（多角色取最严，即存在「仅本人」则为仅本人）。
- 提供 Mapper 基类辅助方法 `applyDataScope(sql, alias)`，在需要数据隔离的列表查询（如会员管理、订单管理）的 XML 中拼接条件。
- 本期仅落地框架与 `system` 模块自身验证，真正接入会员/订单查询待对应模块开发。

## 8. 操作审计日志

### 8.1 表（新增 `sys_operation_log`，见草稿 SQL）

关键字段：`id`、`admin_id`、`username`、`module`、`operation`、`method`（HTTP 方法）、`path`、`params`（脱敏摘要）、`result`（成功/失败）、`error_msg`、`cost_ms`、`trace_id`、`ip`、`user_agent`、`created_at`。索引：`(admin_id, created_at)`、`(module, created_at)`。

### 8.2 注解 + AOP

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();     // 如 "coupon"
    String operation();  // 如 "创建券模板"
    boolean recordParams() default true;
}
```

- AOP 环绕通知：在目标方法执行后记录（成功/失败、耗时、异常摘要），**异步落库**（线程池，不阻塞主流程；失败只记日志不影响业务）。
- 参数脱敏：`password` / `token` / `Authorization` 等字段在序列化前 mask。
- 覆盖范围：所有管理端写操作（标注 `@OperationLog`）；只读操作默认不记，可按需开启。

## 9. 前端集成

- **菜单**：登录后调 `/auth/me` 拿菜单树，动态注册路由（与现有 `06-coupon-design` 前端路由约定一致）。
- **按钮**：自定义指令 `v-permission="'coupon:template:create'"`，从 Pinia 读取当前权限码集合，不在集合中则移除元素。
- **路由守卫**：进入页面前校验对应菜单权限码，无权限跳 403 页。
- **权限码集合**：登录成功后存入 Pinia，`/auth/me` 刷新时更新。

## 10. 错误码与安全约束

### 10.1 新增错误码（追加到 `ErrorCode`）

| code | 含义 | HTTP |
| --- | --- | --- |
| 40105 | 无权限（已存在 `FORBIDDEN`） | 403 |
| 40301 | 角色不存在 | 404 |
| 40302 | 权限不存在 | 404 |
| 40303 | 内置角色不可删除/修改 | 409 |
| 40304 | 不可删除自己 / 不可移除自身超级管理员角色 | 409 |
| 40305 | 角色下存在管理员，不可删除 | 409 |
| 40306 | 原密码不正确（改密） | 400 |

### 10.2 安全约束

- SUPER_ADMIN 角色（id=1）：不可删除、不可禁用、不可改 code、不可被取消（防止系统自锁）。
- 不允许删除最后一个 SUPER_ADMIN 管理员。
- 管理员被禁用/删除/重置密码后：清权限缓存 + 清全部会话（立即下线）。
- 所有写操作必须同时满足「认证 + 权限码」，两层校验不可绕过。
- 操作日志与登录日志中不得出现明文密码、Token；密码字段一律 BCrypt，不回显。
- 接口权限码与 `@RequirePermission` 保持一一对应，Code Review 时核对。

## 11. 实施计划与验收

### 11.1 P1（本期核心，可独立上线）

1. V3 迁移：补齐按钮/接口权限种子、SUPER_ADMIN 全量授权、内置「运营管理员」角色、`sys_operation_log` 表。
2. 后端：
   - `SysRole` 实体 + Mapper + XML（列表、详情、CRUD、权限关联）。
   - `AdminPermissionService`：缓存读写、失效、SUPER_ADMIN 短路。
   - `@RequirePermission` 注解 + `PermissionInterceptor`，注册到 `WebConfig`。
   - `AdminController` / `RoleController` / `PermissionController` / `OperationLogController` 及 DTO。
   - `SystemAdminService` / `SystemRoleService` 写操作事务 + 缓存失效 + 会话清理。
   - 给现有 `Coupon*Controller` 写方法补 `@RequirePermission`。
3. 验收：
   - [ ] 新建「运营」角色并授予优惠券权限，分配给子账号后，子账号登录菜单仅显示优惠券，调用无权限接口返回 403。
   - [ ] 调整角色权限后，相关管理员**下一次请求**即生效（无需重新登录）。
   - [ ] SUPER_ADMIN 可见全部菜单、可调用全部接口。
   - [ ] 禁用某管理员后，其现有 Token 立即失效（返回 401/403）。
   - [ ] 删除/禁用 SUPER_ADMIN 角色、移除自身超管角色均被拒绝。
   - [ ] 管理端写操作在 `sys_operation_log` 留痕，敏感字段已脱敏。

### 11.2 P2（增强，可延后）

- 数据权限（data_scope）接入会员/订单查询。
- 登录失败锁定与自动解锁（错误次数计数到 Redis）。
- 首次登录强制改密的前端拦截闭环。
- 操作日志支持导出、登录日志合并查询。
- 权限缓存按受影响 adminId 精准失效（替换全量失效）。
- 权限变更的变更前/后快照记录。

## 12. 与现有代码的改动清单

| 文件 | 改动 |
| --- | --- |
| `ErrorCode.java` | 追加 §10.1 错误码 |
| `WebConfig.java` | 注册 `PermissionInterceptor`，排在 `LoginInterceptor` 之后 |
| `LoginInterceptor` | 基本不动；会话 key 调整以支持按 adminId 清理（§5.5） |
| `AdminAuthService.me()` | 权限/菜单改走 `AdminPermissionService` 缓存 |
| `AdminSessionService` | 新增 `deleteAllByAdmin` |
| 新增 | `annotation/RequirePermission`、`annotation/OperationLog`、`interceptor/PermissionInterceptor`、`aspect/OperationLogAspect`、`service/AdminPermissionService`、`service/SystemAdminService`、`service/SystemRoleService`、`service/OperationLogService`、对应 Controller / Mapper / XML / DTO / Entity(`SysRole`) |
| 现有 `Coupon*Controller` | 写方法补注解 |
| `db/migration` | 新增 `V3__rbac_seed_and_operation_log.sql` |
