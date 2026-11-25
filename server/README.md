# Shopping Mall Server

管理端登录模块，采用适合入门和练手的经典结构：**单模块 MVC + 纯 MyBatis XML + JWT + Redis 会话**。

当前只实现管理端 `login / me / logout`，不包含会员、优惠券、Refresh Token 轮换等业务。

## 方案文档

| 文档 | 内容 |
| --- | --- |
| [05-simplified-mvc-plan.md](docs/05-simplified-mvc-plan.md) | 当前已实施的简化版架构、接口契约和实施步骤 |
| [01-architecture.md](docs/01-architecture.md) | 原生产级多模块架构参考 |
| [02-database.md](docs/02-database.md) | 原数据库设计参考 |
| [03-api.md](docs/03-api.md) | 原完整 API 契约参考 |
| [04-roadmap.md](docs/04-roadmap.md) | 原完整实施路线参考 |

旧四模块版本保留在 Git 提交 `f0f9bed`，可用于对照学习。

## 技术栈

- Java 21、Spring Boot 3.5.x
- Spring MVC + HandlerInterceptor
- MyBatis + XML Mapper
- MySQL 8 + Flyway
- Redis 7
- JJWT + BCrypt
- JUnit 5 + Mockito

不使用 Spring Security 过滤器链、MyBatis-Plus、DDD 分包和 Testcontainers。

## 工程结构

```text
server/
├── pom.xml
├── src/main/java/com/shopmall/
│   ├── MallApplication.java
│   ├── common/         # 统一响应、错误码、异常处理
│   ├── config/         # MyBatis、拦截器、认证配置、初始管理员
│   ├── controller/     # 登录、当前管理员、退出接口
│   ├── service/        # 登录业务和 Redis 会话
│   ├── interceptor/    # JWT 与 Redis 会话校验
│   ├── mapper/         # MyBatis Mapper 接口
│   ├── entity/         # 数据库实体
│   ├── dto/            # 请求与响应 DTO
│   └── util/           # JWT 工具
└── src/main/resources/
    ├── application.yml
    ├── application-prod.yml
    ├── mapper/                  # 所有 SQL 的 XML 文件
    └── db/migration/V1__create_admin_auth_schema.sql
```

请求链路：

```text
请求 → LoginInterceptor → Controller → Service → Mapper XML → MySQL
                         ↘ Redis 会话读写
```

## 本地启动

### 1. 启动 MySQL 与 Redis

```bash
cd server
docker compose -f deploy/docker-compose.yml up -d
```

本地连接：

| 服务 | 地址 | 账号/密码 |
| --- | --- | --- |
| MySQL | `localhost:3307/mall` | `mall / mall_dev_password` |
| Redis | `localhost:6379` | 无密码 |

### 2. 启动后端

需要 JDK 21+ 和 Maven 3.9+。

```bash
cd server
mvn spring-boot:run
```

也可以打包后运行：

```bash
mvn -DskipTests package
java -jar target/mall-server-0.2.0.jar
```

健康检查：`http://localhost:8080/actuator/health`

默认开发管理员：

- 用户名：`admin`
- 密码：`Admin@123456`
- 角色：`SUPER_ADMIN`
- 前端权限：`["*"]`

初始管理员只在开发配置下自动创建，生产配置默认关闭。

### 3. 启动 admin

```bash
cd fronted/admin
npm install
npm run dev
```

访问 `http://localhost:5173`，使用上面的开发账号登录。前端开发服务器将 `/api` 代理到 `http://localhost:8080`。

## 认证方式

登录成功后：

1. 后端生成 `sessionId`，向 Redis 写入 `auth:admin:session:{sessionId}`，值为管理员 ID。
2. 签发有效期 30 分钟的 JWT，包含管理员 ID 和 `sessionId`。
3. 后续请求由拦截器同时校验 JWT 和 Redis 会话。
4. 退出登录时删除 Redis 会话，同一 JWT 再访问会返回 `40102`。

当前没有 Refresh Token。登录响应保留 `refreshToken` 字段但值为空字符串，令牌过期后需要重新登录。

## 接口

### 登录

```bash
curl -s http://localhost:8080/api/v1/admin/auth/login \
  -H 'Content-Type: application/json' \
  -H 'X-Client-Type: PC' \
  -d '{"username":"admin","password":"Admin@123456","deviceId":"local-device-001"}'
```

响应中的 `data.accessToken` 用于后续请求：

```http
Authorization: Bearer <accessToken>
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/admin/auth/login` | 登录并创建 Redis 会话 |
| GET | `/api/v1/admin/auth/me` | 获取管理员、权限和菜单 |
| POST | `/api/v1/admin/auth/logout` | 删除 Redis 会话并退出 |

## 测试

```bash
cd server
mvn test
```

当前包含 JWT 签发/篡改/过期测试，以及登录成功、密码错误、账号禁用测试。真实接口验证使用本地 MySQL 和 Redis，不需要 Testcontainers。

## 生产配置

生产 profile 要求通过环境变量提供数据库、Redis 和 JWT 配置，不再使用开发默认值：

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_HOST=mysql.example.internal \
DB_PORT=3306 \
DB_NAME=mall \
DB_USERNAME=mall \
DB_PASSWORD='replace-with-strong-password' \
REDIS_HOST=redis.example.internal \
REDIS_PASSWORD='replace-with-strong-password' \
JWT_SECRET='replace-with-random-secret-at-least-32-bytes' \
java -jar target/mall-server-0.2.0.jar
```

部署前还需配置 HTTPS、CORS、限流、审计和密钥管理。数据库结构只由 Flyway 迁移管理，不使用自动建表。

## 后续可选增强

- Refresh Token
- 登录失败计数与锁定
- 服务端权限注解
- 登录日志落库
- 会员、优惠券及其他业务模块
