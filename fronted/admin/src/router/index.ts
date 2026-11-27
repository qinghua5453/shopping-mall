import { createRouter, createWebHistory } from 'vue-router'

import {
  registerPermissionRoutes,
  permissionRoutesReady,
  resetPermissionRoutes,
} from '@/router/permission'
import { dashboardRoute } from '@/router/routes'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      name: 'Root',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [dashboardRoute],
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('@/views/ErrorView.vue'),
      meta: { title: '无权限', requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/ErrorView.vue'),
      meta: { title: '页面不存在', requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.name === 'Login') {
    return auth.isLoggedIn ? { name: 'Dashboard' } : true
  }

  if (!to.meta.requiresAuth) {
    return true
  }

  if (!auth.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (!auth.loaded) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clearSession()
      resetPermissionRoutes(router)
      return { name: 'Login', query: { redirect: to.fullPath } }
    }
  }

  if (!permissionRoutesReady()) {
    registerPermissionRoutes(router)
    return { path: to.fullPath, replace: true }
  }

  if (!auth.checkPermission(to.meta.permission)) {
    return { name: 'Forbidden' }
  }

  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title ? `${to.meta.title} - ` : ''}${import.meta.env.VITE_APP_TITLE}`
})

export default router
