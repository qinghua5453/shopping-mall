# Shopping Mall Admin

电商系统 PC 运营管理后台，基于 Vue 3、Vite、TypeScript、Pinia、Vue Router、Element Plus 和 Axios。

## 启动

要求 Node.js 22.12+ 和 pnpm 10。

```bash
pnpm install
pnpm dev
```

访问 `http://localhost:5173`。开发服务器将 `/api` 代理到 `http://localhost:8080`，后端未启动时登录会提示网络连接失败，这是预期行为。

## 质量检查

```bash
pnpm typecheck
pnpm lint
pnpm test:unit
pnpm build
```

## 已实现

- 登录页和管理员认证 Store；
- Access Token / Refresh Token 请求处理，刷新请求单飞与失败退出；
- 路由守卫、按权限动态注册业务路由、按钮权限指令；
- 后台布局、工作台、403/404 页面；
- 系统管理、会员管理、优惠券管理的权限页面骨架；
- 开发代理、环境配置、SCSS、ESLint、Prettier、Vitest。

业务列表和 CRUD 暂不填入模拟数据，等待后端接口实现后按 `../../server/docs/03-api.md` 接入。
