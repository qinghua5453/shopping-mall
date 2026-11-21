import type { Router } from 'vue-router'

import { businessRoutes } from '@/router/routes'
import { hasPermission } from '@/utils/permission'

const registeredNames: string[] = []
let ready = false

export function registerPermissionRoutes(router: Router, permissions: string[]): void {
  resetPermissionRoutes(router)

  for (const route of businessRoutes) {
    if (route.name && hasPermission(permissions, route.meta?.permission)) {
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
