import type { Router } from 'vue-router'

import { businessRoutes } from '@/router/routes'

const registeredNames: string[] = []
let ready = false

/**
 * 注册全部业务路由（不按权限过滤）。
 * 越权访问由全局路由守卫依据 route.meta.permission 统一拦截到 403；
 * 侧边栏菜单是否展示则在 AdminLayout 中按权限单独过滤。
 * 这样已知但无权限的路由会命中 403，而非被通配路由误判为 404。
 */
export function registerPermissionRoutes(router: Router): void {
  resetPermissionRoutes(router)

  for (const route of businessRoutes) {
    if (route.name) {
      router.addRoute('Root', route)
      registeredNames.push(String(route.name))
    }
  }

  ready = true
}

export function resetPermissionRoutes(router: Router): void {
  for (const name of registeredNames.splice(0)) {
    router.removeRoute(name)
  }
  ready = false
}

export function permissionRoutesReady(): boolean {
  return ready
}
