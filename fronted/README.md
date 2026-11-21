# 电商系统前端（fronted）

当前只建设 PC 运营管理后台，不规划或初始化其他前端工程。

## 目录

| 目录 | 说明 |
| --- | --- |
| `admin/` | Vue 3 管理端工程 |
| `docs/01-admin-architecture.md` | 管理端技术架构与工程规范 |

## 技术栈

Vue 3、Vite、TypeScript、Pinia、Vue Router、Element Plus、Axios、Sass、Vitest、ESLint、Prettier。

## 本地运行

```bash
cd fronted/admin
pnpm install
pnpm dev
```

默认地址：`http://localhost:5173`。

开发环境通过 Vite 将 `/api` 代理到 `http://localhost:8080`。后端未启动时页面仍可访问，登录等真实接口会返回服务不可用提示。

## 常用命令

```bash
pnpm typecheck
pnpm lint
pnpm test:unit
pnpm build
pnpm preview
```

后端接口契约见 `../server/docs/03-api.md`。
