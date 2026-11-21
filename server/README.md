# 电商系统后端（server）

Java 21 + Spring Boot 3.5 的**模块化单体**后端方案。一期范围：会员、认证授权（RBAC）、优惠券；同时覆盖 PC / 移动 H5 / APP，并为小程序预留接入能力。

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

本目录目前只有方案文档，尚未生成代码骨架。确认方案后再进入 M0 阶段（工程骨架与基础设施）。
