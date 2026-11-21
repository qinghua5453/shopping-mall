# Admin 管理端技术架构

> 范围：仅 PC 运营管理后台。
> 技术基线：Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios。
> 后端契约：`server/docs/03-api.md`，接口前缀 `/api/v1/admin`。

## 1. 建设目标

- 提供可独立开发、测试、构建和部署的管理端工程。
- 支持管理员登录、Token 刷新、动态菜单、路由与按钮权限、会话失效处理。
- 为系统管理、会员管理、优惠券管理提供统一页面与交互规范。
- 从初始骨架开始落实类型安全、错误处理、代码检查、测试、环境隔离和构建优化。
- 前端权限只负责展示控制，真正的安全边界始终由后端鉴权保证。

## 2. 技术选型

| 分类 | 选型 | 用途 |
| --- | --- | --- |
| 运行环境 | Node.js 22 LTS、pnpm 10 | 本地与 CI 使用统一版本 |
| 框架 | Vue 3、TypeScript strict | Composition API、单文件组件 |
| 构建 | Vite | 开发服务、分包、生产构建 |
| 状态 | Pinia | 登录用户、权限、布局等全局客户端状态 |
| 路由 | Vue Router | 登录守卫、动态权限路由、页面懒加载 |
| UI | Element Plus、Sass | 后台组件、主题变量、页面样式 |
| HTTP | Axios | 请求拦截、统一错误、Token 刷新与请求重试 |
| 测试 | Vitest、Vue Test Utils | 工具、Store、组件单元测试 |
| 规范 | ESLint、Prettier、vue-tsc | 静态检查、格式与类型检查 |

不引入额外状态请求库、CSS 原子化框架或大型 Dashboard 模板，避免与现有技术栈重复。图表等能力在实际需求出现时再引入。

## 3. 工程结构

```text
fronted/admin/
├── public/
├── src/
│   ├── api/                 # Axios 实例、接口类型、按后端领域拆分的 API
│   ├── assets/              # 静态资源
│   ├── components/          # 可复用基础组件与业务组件
│   ├── composables/         # 分页、查询、对话框等可复用逻辑
│   ├── directives/          # v-permission 等按钮权限指令
│   ├── layouts/             # 后台整体布局
│   ├── router/              # 静态路由、权限路由、导航守卫
│   ├── stores/              # auth、permission、app Store
│   ├── styles/              # 全局变量、重置、主题与通用样式
│   ├── types/               # 统一响应、分页、用户、权限等类型
│   ├── utils/               # 请求 ID、存储、错误、格式化工具
│   ├── views/               # 登录、工作台、系统、会员、优惠券页面
│   ├── App.vue
│   └── main.ts
├── tests/                   # 单元测试
├── .env.development
├── .env.production
├── vite.config.ts
├── tsconfig.json
├── eslint.config.js
└── package.json
```

依赖方向：`views → api/stores/composables → utils/types`。组件不得反向依赖页面，API 模块不得操作路由或弹窗，Store 不直接编写 UI。

## 4. 页面与路由

### 4.1 静态路由

- `/login`：登录页。
- `/403`：无权限。
- `/404`：页面不存在。
- `/`：后台布局，要求已登录；默认跳转 `/dashboard`。

### 4.2 业务路由

| 路由 | 页面 | 权限码 |
| --- | --- | --- |
| `/dashboard` | 工作台 | 登录即可 |
| `/system/admins` | 管理员管理 | `system:admin:view` |
| `/system/roles` | 角色管理 | `system:role:view` |
| `/system/permissions` | 权限管理 | `system:permission:view` |
| `/system/logs` | 操作日志 | `system:log:view` |
| `/member/list` | 会员管理 | `member:member:view` |
| `/coupon/templates` | 券模板 | `coupon:template:view` |
| `/coupon/codes` | 券码管理 | `coupon:code:view` |
| `/coupon/instances` | 用户券 | `coupon:instance:view` |
| `/coupon/rules` | 叠加规则 | `coupon:rule:view` |

业务路由使用本地白名单注册，后端 `/auth/me` 只返回权限码和菜单，不直接执行远程组件地址。页面组件通过 `import()` 懒加载。无权限路由不注册，按钮通过 `v-permission` 控制显示。

### 4.3 导航守卫

1. 已登录访问 `/login` 时进入首页。
2. 无 Token 访问受保护页面时跳转登录，并记录 `redirect`。
3. 有 Token 但本地无用户信息时调用 `/auth/me` 恢复会话。
4. 恢复成功后按权限注册路由，再重新进入目标地址。
5. `/auth/me` 失败时清理会话并回登录页；接口异常不得造成守卫死循环。

## 5. 登录与状态管理

### 5.1 Auth Store

状态：`accessToken`、`refreshToken`、`deviceId`、`admin`、`permissions`、`menus`、`pwdResetRequired`。

动作：`login`、`fetchMe`、`logout`、`clearSession`、`hasPermission`。

- 登录成功后保存 Token，再获取用户与权限。
- 默认使用 `sessionStorage` 保存令牌，降低长期暴露风险；不使用 `localStorage` 持久保存 Token。
- `deviceId` 是随机 UUID，可保存在 `localStorage`，但不包含敏感信息。
- 超级管理员或拥有 `*` 时前端放行，后端仍需独立鉴权。
- 退出登录调用后端失败时也要清理本地会话。

### 5.2 App Store

只保存侧边栏折叠、主题等界面状态。列表数据、表单数据和接口加载状态留在页面或 composable 中，不全部放入 Pinia。

## 6. HTTP 与接口契约

### 6.1 请求规范

Axios 实例统一配置：

- `baseURL = import.meta.env.VITE_API_BASE_URL`，开发默认空串并使用 `/api` 代理。
- `timeout = 15000`。
- 自动携带 `Authorization: Bearer <token>`、`X-Client-Type: PC`、`X-Device-Id`、`X-Request-Id`。
- 写操作可按业务需要携带 `Idempotency-Key`。
- 请求和响应日志不得输出密码、Token、完整手机号。

### 6.2 响应处理

```ts
interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: string
}
```

- `code === 0` 返回 `data`。
- 业务错误统一抛出 `ApiError`，页面只展示可理解的信息。
- `40101`：使用 Refresh Token 刷新；同时只执行一个刷新请求，其余请求等待，成功后最多重试一次。
- `40102`、`40108`：清理会话并跳转登录。
- `403`：提示无权限；路由级无权限进入 `/403`。
- `429`：提示操作频繁，不自动重试。
- 网络错误或 5xx：提示服务不可用，保留 `traceId` 便于排查。

刷新接口自身失败、请求已重试、正在登录或退出时不得再次刷新，避免无限循环。

### 6.3 类型与分页

- 后端所有 Long ID 在前端统一声明为 `string`，禁止转 `number`。
- 金额统一使用整数分，展示层格式化为元，提交前由明确的工具函数转换。
- 分页参数 `page/pageSize`，最大 100；响应使用 `records/page/pageSize/total/pages`。
- 按 `auth/system/member/coupon` 拆分 API 文件，OpenAPI 类型自动生成在接口稳定后接入。

## 7. UI 与交互规范

- 后台布局：侧边导航、顶部用户信息、面包屑、主内容区；支持折叠菜单。
- 列表页统一包含查询区、操作区、表格、分页、加载/空数据/异常状态。
- 新增与编辑使用表单校验，提交期间禁用按钮，成功后刷新列表并关闭对话框。
- 删除、下线、冻结等危险操作必须二次确认。
- 表格数据始终后端分页，不在前端加载全量数据。
- 统一使用 Element Plus 消息、对话框、表单与分页组件。
- 样式采用 SCSS 和 CSS 变量，避免全局样式污染；组件样式默认 `scoped`。

## 8. 环境配置

| 变量 | 说明 |
| --- | --- |
| `VITE_API_BASE_URL` | API 基础地址，开发默认空串 |
| `VITE_APP_TITLE` | 页面标题 |
| `VITE_DEV_PROXY_TARGET` | 开发代理目标，默认 `http://localhost:8080` |

- `.env.development`、`.env.production` 只保存公开配置。
- `.env.local` 用于个人覆盖，不提交仓库。
- `VITE_` 变量会进入浏览器产物，禁止放密钥。
- 生产 API 优先使用同源 `/api`，由 Nginx 反向代理，减少 CORS 配置。

## 9. 安全设计

- 路由和按钮权限只用于 UI，所有接口仍由后端校验权限与资源归属。
- 不使用未经消毒的 `v-html`；必须展示富文本时引入 DOMPurify。
- 登录密码不写入日志、Store 持久化或错误上报。
- 生产 Source Map 不公网暴露；如接入错误平台则私有上传。
- 上传文件前端仅做体验校验，类型、大小和内容安全仍由服务端验证。
- 依赖安装锁定版本，CI 执行漏洞检查。
- Nginx 配置 HTTPS、CSP 等安全响应头，避免 iframe 嵌套与 MIME 嗅探。

## 10. 性能与构建

- 所有业务页面路由懒加载。
- Vite 将 Vue、Element Plus、Axios 等稳定依赖拆为独立 chunk。
- 静态资源使用内容哈希；`index.html` 不缓存，带哈希资源长期缓存。
- 表格分页、避免不必要的深度监听、超大列表使用虚拟滚动。
- 构建产物检查 chunk 体积，大依赖按需引入；压缩由 Nginx/CDN 负责。
- 首屏目标：常规企业网络下 LCP ≤ 2.5 秒，路由切换无明显阻塞。

## 11. 测试与质量门禁

- `pnpm typecheck`：TypeScript 与 Vue SFC 类型检查。
- `pnpm lint`：ESLint 检查。
- `pnpm test:unit`：Store、权限、请求错误等单元测试。
- `pnpm build`：生产构建验证。
- CI 使用 `pnpm install --frozen-lockfile`，依次执行以上命令。
- 后续接入 Playwright，覆盖登录、无权限访问、Token 失效、核心 CRUD 流程。

## 12. 部署

采用多阶段 Docker 构建：Node 阶段执行 pnpm build，Nginx 阶段只保留 `dist`。

Nginx 要点：

```nginx
location / {
  try_files $uri $uri/ /index.html;
}

location /api/ {
  proxy_pass http://backend:8080;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

- `/assets/` 长缓存，`index.html` 设置 `Cache-Control: no-cache`。
- 开启 HTTPS、gzip 或 Brotli。
- 管理端建议使用独立域名，仅开放给授权人员。

## 13. 当前实施范围

本次初始化交付：

- 可运行的 Vue 3 + TypeScript 工程；
- 登录页、后台布局、工作台；
- Pinia 登录状态、Axios 封装、路由守卫、权限指令；
- 系统管理、会员管理、优惠券管理的占位页面；
- 单元测试、ESLint、类型检查、生产构建配置。

真实业务 CRUD 在后端对应接口实现后逐项接入，不在前端伪造生产数据或绕过权限。
