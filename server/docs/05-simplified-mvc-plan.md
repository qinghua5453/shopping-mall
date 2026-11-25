# 简化版后端方案：单模块 MVC + MyBatis + JWT + Redis 会话

> 状态：已实施
>
> 适用人群：前端经验丰富、后端约半年，目标是先跑通并理解 Java Web 的经典链路。

## 1. 背景与目标

现有后端是一套偏生产级的模块化单体：4 个 Maven 模块、DDD 四层分包、Spring Security 过滤器链、Redis 会话 + Refresh Token 轮换 + Lua 原子脚本、Testcontainers 集成测试，共 47 个 Java 文件。

功能本身不复杂，但基础设施叠加过多，导致“一个登录请求要跨 5 个包”才能读完。因此本项目作为练手项目，改为更直观的结构。

本方案目标：

- 只保留**管理端登录闭环**：`login / me / logout`
- 单 Maven 模块，按经典 MVC 分层
- 使用**纯 MyBatis + XML**，让 SQL 和映射关系可见
- 保留 **Redis 会话**，保证退出登录后令牌立即失效
- **不做 Refresh Token 轮换**，令牌过期后重新登录
- 接口路径与响应结构尽量保持不变，现有 admin 前端可继续使用

本方案非目标：

- 不实现会员、优惠券、商品等业务模块
- 不实现多端会话管理、权限缓存、登录失败锁定（列为后续可选项）
- 不引入 Spring Security 全家桶

## 2. 与现有实现的关系

- 本次**不新建 Git 分支**，直接在 `server/` 原地改造。
- 现有可运行版本已提交在 `f0f9bed`，任何阶段都可以通过 Git 找回或对照。
- `docs/01~04` 保留，作为“完整生产版”的参考，不随本次改造更新。
- 改造完成后，`server/pom.xml` 从聚合 POM 变成唯一应用 POM，4 个子模块目录删除。

改造前建议确认：

```bash
cd server
git log --oneline -3      # 确认 f0f9bed 存在
git status                # 确认工作区干净
```

## 3. 架构总览

### 3.1 目录结构

```text
server/
├── pom.xml                          # 唯一应用 POM（不再聚合子模块）
├── docs/                            # 方案文档
├── deploy/docker-compose.yml        # MySQL + Redis
├── .env.example
├── README.md
└── src/
    ├── main/java/com/shopmall/
    │   ├── MallApplication.java
    │   ├── common/
    │   │   ├── ApiResponse.java
    │   │   ├── ErrorCode.java
    │   │   ├── BizException.java
    │   │   └── GlobalExceptionHandler.java
    │   ├── config/
    │   │   ├── MyBatisConfig.java        # @MapperScan
    │   │   ├── WebConfig.java            # 注册登录拦截器
    │   │   └── AuthProperties.java       # jwt-secret / token-ttl
    │   ├── interceptor/
    │   │   └── LoginInterceptor.java
    │   ├── controller/
    │   │   └── AdminAuthController.java
    │   ├── service/
    │   │   ├── AdminAuthService.java
    │   │   └── AdminSessionService.java
    │   ├── mapper/
    │   │   ├── SysAdminMapper.java
    │   │   └── SysPermissionMapper.java
    │   ├── entity/
    │   │   ├── SysAdmin.java
    │   │   └── SysPermission.java
    │   ├── dto/
    │   │   ├── LoginRequest.java
    │   │   ├── LoginResponse.java
    │   │   ├── AdminProfile.java
    │   │   ├── AdminMenu.java
    │   │   └── MeResponse.java
    │   └── util/
    │       └── JwtUtil.java
    ├── main/resources/
    │   ├── application.yml
    │   ├── mapper/
    │   │   ├── SysAdminMapper.xml
    │   │   └── SysPermissionMapper.xml
    │   └── db/migration/
    │       └── V1__create_admin_auth_schema.sql   # 沿用现有文件
    └── test/java/com/shopmall/
        ├── util/JwtUtilTest.java
        └── service/AdminAuthServiceTest.java
```

预计 20 个左右 Java 文件、约 900 行代码。

### 3.2 分层职责

| 层 | 职责 | 不做什么 |
| --- | --- | --- |
| `controller` | 参数接收、调用 Service、返回 `ApiResponse` | 不写业务判断、不直接碰 Mapper |
| `interceptor` | 校验 JWT、校验 Redis 会话、写入当前用户 | 不查权限、不写业务 |
| `service` | 登录校验、令牌签发、会话读写、菜单组装 | 不处理 HTTP 细节 |
| `mapper` + XML | 只负责 SQL 与结果映射 | 不写业务逻辑 |
| `entity` | 数据库表映射 | 不混入 DTO 字段 |
| `dto` | 接口出入参 | 不直接暴露 entity |

### 3.3 请求链路

```text
浏览器
  → LoginInterceptor
      ├─ /api/v1/admin/auth/login    放行
      ├─ OPTIONS                     放行
      └─ 其余 /api/v1/admin/**       校验 Bearer JWT + Redis 会话
  → AdminAuthController
  → AdminAuthService
  → SysAdminMapper / SysPermissionMapper（XML）
  → MySQL
  → ApiResponse<T> 统一包装
```

## 4. 技术栈与依赖

| 依赖 | 用途 | 说明 |
| --- | --- | --- |
| `spring-boot-starter-web` | MVC、Tomcat | 核心 |
| `spring-boot-starter-validation` | `@Valid` 参数校验 | 登录参数 |
| `spring-boot-starter-data-redis` | Redis 会话 | 用 `StringRedisTemplate` |
| `mybatis-spring-boot-starter` | MyBatis 集成 | 3.0.x，替代 MyBatis-Plus |
| `mysql-connector-j` | MySQL 驱动 | runtime |
| `flyway-core` + `flyway-mysql` | 数据库版本管理 | 沿用现有 V1 |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | JWT | 0.12.6 |
| `spring-security-crypto` | 只用 `BCryptPasswordEncoder` | 不引 `spring-boot-starter-security` |
| `lombok` | 减少样板代码 | optional，可按喜好移除 |
| `spring-boot-starter-test` | JUnit 5 + Mockito | 不引 Testcontainers |

版本基线：Java 21、Spring Boot 3.5.x、MySQL 8、Redis 7。

## 5. 认证设计

### 5.1 JWT

| 项 | 值 |
| --- | --- |
| 算法 | HS256 |
| 密钥 | 环境变量 `JWT_SECRET`，长度 ≥ 32 字节 |
| 有效期 | 默认 30 分钟（`app.auth.token-ttl`） |
| Claims | `sub`=adminId、`sid`=sessionId、`typ`="access"、`iat`、`exp` |

不再使用 `aud` 字段：当前只有管理端一个 Realm，等接入会员端再加。

### 5.2 Redis 会话

| 项 | 值 |
| --- | --- |
| Key | `auth:admin:session:{sessionId}` |
| Value | adminId（字符串） |
| TTL | 与 JWT 有效期一致，默认 30 分钟 |
| 写入 | 登录成功时 `SET ... EX 1800` |
| 删除 | 退出登录时 `DEL` |

为什么保留 Redis 会话：JWT 本身无法撤销，退出登录后令牌在有效期内仍可用。加一个 key 就能实现“退出即失效”和“后台踢人”，成本约 40 行。

### 5.3 登录流程

```text
POST /api/v1/admin/auth/login
→ 校验 X-Client-Type 是否受支持
→ 按 username 查 sys_admin
→ 账号不存在 / 密码不匹配：40103
→ 账号被禁用：40107
→ BCrypt 校验通过
→ sessionId = UUID
→ SET auth:admin:session:{sessionId} = adminId EX 1800
→ 签发 JWT（sub, sid, typ, exp）
→ 返回 accessToken / expiresIn
```

### 5.4 鉴权流程

```text
请求携带 Authorization: Bearer <jwt>
→ 解析 JWT
   ├─ 过期：40101
   └─ 签名错误 / 格式错误：40102
→ 取 sid，GET auth:admin:session:{sid}
   └─ 不存在：40102（已退出、已过期或被踢）
→ 将 adminId、sessionId 写入 request attribute
→ 放行
```

### 5.5 退出流程

```text
POST /api/v1/admin/auth/logout
→ 从 request attribute 取 sessionId
→ DEL auth:admin:session:{sessionId}
→ 返回成功
```

退出后，同一 Access Token 再访问会因会话不存在返回 `40102`。

### 5.6 错误码

沿用现有 `ErrorCode`，本次实际使用：

| Code | HTTP | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 40001 | 400 | 参数校验失败 |
| 40003 | 400 | 不支持的客户端类型 |
| 40100 | 401 | 未登录或缺少 Token |
| 40101 | 401 | Access Token 已过期 |
| 40102 | 401 | Token 已失效 / 会话不存在 |
| 40103 | 401 | 账号或密码错误 |
| 40107 | 403 | 账号已被禁用 |
| 50000 | 500 | 系统内部错误 |

以下错误码保留定义但暂不使用：`40104` 登录锁定、`40108` Refresh 无效、`40109` 身份类型不匹配。

## 6. 与前端契约

### 6.1 保持不变

- 路径：`/api/v1/admin/auth/login`、`/me`、`/logout`
- 请求头：`X-Client-Type: PC`、`Authorization: Bearer ...`
- 响应结构：`code / message / data / traceId / timestamp`
- `me` 返回：`admin / permissions / menus / pwdResetRequired`
- 菜单结构：`id / name / code / routePath / icon / children`

### 6.2 需要说明的差异

现有前端 `fronted/admin/src/api/http.ts` 在收到 `40101` 时会自动调用 `/api/v1/admin/auth/refresh` 并重试一次。本方案没有 refresh 接口，有两种处理方式：

| 方式 | 做法 | 结果 |
| --- | --- | --- |
| A. 后端兼容（零前端改动） | 登录响应仍返回 `refreshToken` 字段，值为空字符串 | 前端可正常登录；令牌过期后刷新请求会失败，自动清会话并跳登录页 |
| B. 前端小改（推荐） | 删除 `http.ts` 中的 `refreshTokens` 逻辑，`40101` 直接触发登录失效 | 行为更清晰，少一次无效请求 |

第一阶段先用 A 保证联调不停，随后再做 B。无论哪种，`refreshToken` 字段保留即可避免类型报错。

## 7. 数据库

### 7.1 表

直接沿用现有迁移文件 `V1__create_admin_auth_schema.sql`：

| 表 | 用途 |
| --- | --- |
| `sys_admin` | 管理员账号 |
| `sys_role` | 角色 |
| `sys_permission` | 权限 / 菜单 |
| `sys_admin_role` | 管理员-角色 |
| `sys_role_permission` | 角色-权限 |
| `sys_admin_login_log` | 登录日志（第一阶段可不写） |

### 7.2 查询设计

登录查询：

```sql
SELECT id, username, password_hash, nickname, avatar_url, status
FROM sys_admin
WHERE username = #{username}
LIMIT 1
```

权限与菜单查询（一次 JOIN，避免引入角色实体）：

```sql
SELECT DISTINCT p.id, p.name, p.code, p.route_path, p.icon, p.type, p.parent_id
FROM sys_permission p
JOIN sys_role_permission rp ON rp.permission_id = p.id
JOIN sys_admin_role ar ON ar.role_id = rp.role_id
WHERE ar.admin_id = #{adminId}
  AND p.status = 1
ORDER BY p.sort_order
```

### 7.3 迁移注意事项

本地数据库已经执行过 V1，Flyway 记录了校验和。改造时务必**原样复制**现有 SQL 文件，否则 `validate` 会因 checksum 不一致启动失败。

如果确实要调整 SQL，二选一：

- 新增 `V2__xxx.sql`，不要改 V1
- 本地开发库执行 `flyway repair`，或删除数据卷后重建

## 8. API 契约

### 8.1 登录

```http
POST /api/v1/admin/auth/login
Content-Type: application/json
X-Client-Type: PC

{
  "username": "admin",
  "password": "Admin@123456",
  "deviceId": "local-device-001"
}
```

成功：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "traceId": "...",
  "timestamp": "2026-11-25 10:00:00"
}
```

### 8.2 当前管理员

```http
GET /api/v1/admin/auth/me
Authorization: Bearer <accessToken>
```

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "admin": {
      "id": "1",
      "username": "admin",
      "nickname": "超级管理员",
      "avatarUrl": null
    },
    "permissions": ["*"],
    "menus": [
      {
        "id": "100",
        "name": "系统管理",
        "code": "system",
        "routePath": "/system",
        "icon": "Setting",
        "children": []
      }
    ],
    "pwdResetRequired": false
  }
}
```

`SUPER_ADMIN` 返回通配权限 `["*"]`，与现有前端权限判断保持一致。

### 8.3 退出

```http
POST /api/v1/admin/auth/logout
Authorization: Bearer <accessToken>
```

```json
{ "code": 0, "message": "ok", "data": null, "traceId": "...", "timestamp": "..." }
```

## 9. 关键类清单

| 类 | 职责 | 预估行数 |
| --- | --- | --- |
| `MallApplication` | 启动类 | 15 |
| `ApiResponse` | 统一响应 | 40 |
| `ErrorCode` | 错误码枚举 | 50 |
| `BizException` | 业务异常 | 20 |
| `GlobalExceptionHandler` | 参数异常、业务异常、兜底异常 | 60 |
| `MyBatisConfig` | `@MapperScan` | 10 |
| `WebConfig` | 注册拦截器与排除路径 | 30 |
| `AuthProperties` | JWT 密钥、有效期配置 | 25 |
| `JwtUtil` | 签发与解析 JWT | 80 |
| `LoginInterceptor` | 校验 JWT 与 Redis 会话 | 60 |
| `AdminSessionService` | 会话 set/get/delete | 45 |
| `AdminAuthService` | 登录、me、退出 | 100 |
| `AdminAuthController` | 3 个接口 | 70 |
| `SysAdminMapper` + XML | 查询账号、更新登录时间 | 50 |
| `SysPermissionMapper` + XML | 查询权限与菜单 | 60 |
| `SysAdmin` / `SysPermission` | 表映射 | 70 |
| DTO 5 个 | 请求与响应 | 120 |

## 10. 配置示例

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3307/mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:mall}
    password: ${DB_PASSWORD:mall_dev_password}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2s
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

app:
  auth:
    jwt-secret: ${JWT_SECRET:local-dev-only-jwt-secret-change-me-2026}
    token-ttl: 30m
  bootstrap-admin:
    enabled: true
    username: admin
    password: Admin@123456
```

开发连接信息：MySQL `localhost:3307`，Redis `localhost:6379`（无密码），详见 `README.md`。

## 11. 测试策略

不做 Testcontainers，降低门槛：

| 测试 | 方式 |
| --- | --- |
| `JwtUtilTest` | 纯单元测试：签发、解析、过期、篡改 |
| `AdminAuthServiceTest` | Mockito 模拟 Mapper 与 SessionService，验证密码错误、账号禁用、登录成功 |
| 接口验证 | 启动本地 MySQL/Redis 后用 `curl` 跑完整链路 |

手工验收命令：

```bash
curl -s http://localhost:8080/api/v1/admin/auth/login \
  -H 'Content-Type: application/json' \
  -H 'X-Client-Type: PC' \
  -d '{"username":"admin","password":"Admin@123456","deviceId":"local-device-001"}'
```

## 12. 与现版本的差异

| 能力 | 现版本 | 简化版 |
| --- | --- | --- |
| Maven 模块 | 4 个 | 1 个 |
| 分包 | DDD 四层 | MVC 六层 |
| 安全框架 | Spring Security 过滤器链 | 拦截器 + JwtUtil |
| ORM | MyBatis-Plus | 纯 MyBatis + XML |
| 会话 | Redis + Refresh 轮换 + Lua | Redis + 单一 Access Token |
| 退出即失效 | 支持 | 支持 |
| 令牌续期 | Refresh Token | 过期重新登录 |
| 多端会话 / 踢人 | 支持 | 支持单会话删除，未做列表 |
| 登录失败锁定 | 支持 | 暂不实现 |
| 权限缓存 | 支持 | 每次查询 |
| 集成测试 | Testcontainers | 单测 + curl |

## 13. 实施步骤

| 阶段 | 内容 | 验收 |
| --- | --- | --- |
| P0 准备 | 确认 `f0f9bed`、停旧服务、备份本地数据卷说明 | `git status` 干净 |
| P1 骨架 | 单 POM、启动类、统一响应、全局异常、`/ping` | `mvn spring-boot:run` 启动，curl 返回统一结构 |
| P2 数据层 | 复制 V1、entity、mapper + XML | 单元测试能按用户名查出 admin |
| P3 认证 | BCrypt、JwtUtil、SessionService、login/me/logout、拦截器 | curl 完成登录 → me → 退出 → me 返回 401 |
| P4 前端联调 | 启动 admin，真实登录、刷新页面、退出 | 能进入工作台；退出后回登录页 |
| P5 收尾 | 改写 README、删除旧模块说明 | 新同学按 README 可独立跑通 |
| P6 可选 | Refresh Token、登录锁定、权限注解、前端去 refresh 逻辑 | 按需排期 |

P1~P4 每个阶段结束时项目都应处于**可运行**状态，不要一次性删完再补。

## 14. 风险与注意事项

- **Flyway 校验和**：V1 必须原样复制，否则启动即失败。
- **JWT 密钥长度**：HS256 要求 ≥ 32 字节，太短会在启动或首次签发自检时报错。
- **Redis 必须先启动**：登录与鉴权都依赖 Redis，未启动时登录会直接失败。
- **前端 401 重试**：不处理 `/refresh` 兼容会导致控制台出现一次无效请求，按第 6.2 节处理即可。
- **残留配置**：删除旧模块后，检查 IDE 运行配置、`server/README.md`、根目录脚本里是否还引用 `mall-bootstrap`。
- **不要顺手加功能**：`sys_admin_login_log`、角色管理、权限管理页面属于后续迭代，保持本次范围。

## 15. 后续可选增强

1. Refresh Token（先做简单版：Redis 存映射，刷新时删旧建新，不引入 Lua）
2. 登录失败计数与锁定（复用 Redis，`INCR` + `EXPIRE`）
3. 权限注解 `@RequirePermission`，替代前端通配权限
4. 登录日志落库
5. 自动化集成测试（届时再评估是否引入 Testcontainers）
