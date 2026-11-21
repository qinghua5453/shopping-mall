# 电商系统后端（server）

Java 21 + Spring Boot 3.5 的**模块化单体**后端。当前已实现管理端登录与 RBAC 基础设施；会员、优惠券及其他业务模块待定。方案文档保留 PC / 移动 H5 / APP / 小程序的扩展设计。

## 方案文档

| 文档 | 内容 |
| --- | --- |
| [01-architecture.md](docs/01-architecture.md) | 总体架构、技术选型、工程结构、多端认证、权限、优惠券核心设计、非功能与部署 |
| [02-database.md](docs/02-database.md) | 数据库规范、ER 模型、完整 DDL、索引与初始化数据、Flyway 策略 |
| [03-api.md](docs/03-api.md) | API 规范、错误码、会员端 / 管理端接口清单、关键时序与请求示例 |
| [04-roadmap.md](docs/04-roadmap.md) | 迭代阶段、任务拆解、验收标准、测试与上线检查清单 |

## 关键决策速览

- **架构**：模块化单体（Modular Monolith），单进程部署，按领域拆 Maven 模块，边界用 ArchUnit 约束，后续可平滑拆分服务。
- **技术栈**：JDK 21、Spring Boot 3.5.x、Spring Security 6、MyBatis-Plus、MySQL 8、Redis 7、Flyway。
- **认证**：JWT Access Token + Redis 多端会话 + Refresh Token 轮换，会员端与管理端双 Realm 隔离。
- **权限**：管理端 RBAC（角色 - 权限 - 菜单/按钮/API），权限缓存 + 版本号失效；会员端不引入细粒度权限。
- **优惠券**：满减 / 折扣 / 无门槛；支持限领、库存防超发、券码兑换、叠加互斥规则、下单试算、锁券/核销/释放/退券。
- **金额**：统一使用 `BIGINT` 存储「分」，禁止浮点。
- **接口**：`/api/v1/member/**` 与 `/api/v1/admin/**` 两组，PC / H5 / APP / 小程序通过 `X-Client-Type` 区分，业务主流程共用一套接口。

## 当前状态

- `mall-common`：统一响应、错误码、异常处理和 TraceId。
- `mall-security`：管理员、RBAC、JWT、Redis 会话、登录失败锁定及认证过滤器。
- `mall-admin`：管理端认证接口。
- `mall-bootstrap`：启动入口、Flyway、环境配置和开发管理员初始化。
- 已实现 `login / refresh / me / logout`，支持 Refresh Token 轮换、重放拒绝和退出后会话失效。
- 会员、优惠券及其他后台 CRUD 尚未实现。

## 本地启动

### 环境要求

- JDK 21 或更高版本（编译目标为 Java 21）
- Maven 3.9+
- Docker 与 Docker Compose
- 后端端口 `8080`，MySQL 宿主机端口 `3307`，Redis 端口 `6379`

### 1. 启动 MySQL 和 Redis

```bash
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

MySQL 容器内仍使用 `3306`，映射到宿主机 `3307`，避免与本机已有 MySQL 冲突。开发环境默认数据库为 `mall`，用户为 `mall`，密码为 `mall_dev_password`。

### 2. 启动后端

```bash
mvn -pl mall-bootstrap -am -DskipTests package
java -jar mall-bootstrap/target/mall-bootstrap-0.1.0.jar
```

如果本机默认 JDK 低于 21，请先设置 `JAVA_HOME` 和 `PATH`。启动时 Flyway 自动执行 `V1__create_admin_auth_schema.sql`，并创建开发管理员：

- 用户名：`admin`
- 密码：`Admin@123456`
- 角色：`SUPER_ADMIN`

该账号和自动初始化仅用于本地开发；`prod` profile 默认关闭自动初始化，数据库、Redis 和 JWT 密钥必须通过环境变量配置。配置示例见 `.env.example`。

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

### 3. 启动 admin 并联调

```bash
cd ../fronted/admin
npm run dev
```

访问 `http://localhost:5173`，使用上述开发管理员登录。Vite 已将 `/api` 代理到 `http://localhost:8080`。

## 认证接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/admin/auth/login` | 登录，需请求头 `X-Client-Type: PC` |
| POST | `/api/v1/admin/auth/refresh` | 刷新并轮换令牌 |
| GET | `/api/v1/admin/auth/me` | 当前管理员、权限和菜单 |
| POST | `/api/v1/admin/auth/logout` | 退出并删除 Redis 会话 |

## 测试

```bash
mvn test
```

测试包含 JWT 单元测试及基于 Testcontainers 的 MySQL 8、Redis 7 认证集成测试，需要 Docker 正常运行。
