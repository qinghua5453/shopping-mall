import type { RouteRecordRaw } from 'vue-router'

export const dashboardRoute: RouteRecordRaw = {
  path: 'dashboard',
  name: 'Dashboard',
  component: () => import('@/views/DashboardView.vue'),
  meta: { title: '工作台', requiresAuth: true, menu: true },
}

export const businessRoutes: RouteRecordRaw[] = [
  {
    path: 'system/admins',
    name: 'SystemAdmins',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { title: '管理员管理', requiresAuth: true, permission: 'system:admin:view', menu: true },
  },
  {
    path: 'system/roles',
    name: 'SystemRoles',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { title: '角色管理', requiresAuth: true, permission: 'system:role:view', menu: true },
  },
  {
    path: 'system/permissions',
    name: 'SystemPermissions',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: {
      title: '权限管理',
      requiresAuth: true,
      permission: 'system:permission:view',
      menu: true,
    },
  },
  {
    path: 'system/logs',
    name: 'SystemLogs',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { title: '操作日志', requiresAuth: true, permission: 'system:log:view', menu: true },
  },
  {
    path: 'member/list',
    name: 'MemberList',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { title: '会员管理', requiresAuth: true, permission: 'member:member:view', menu: true },
  },
  {
    path: 'coupon/templates',
    name: 'CouponTemplates',
    component: () => import('@/views/coupon/CouponTemplateListView.vue'),
    meta: { title: '券模板', requiresAuth: true, permission: 'coupon:template:view', menu: true },
  },
  {
    path: 'coupon/codes',
    name: 'CouponCodes',
    component: () => import('@/views/coupon/CouponCodeView.vue'),
    meta: { title: '券码管理', requiresAuth: true, permission: 'coupon:code:view', menu: true },
  },
  {
    path: 'coupon/instances',
    name: 'CouponInstances',
    component: () => import('@/views/coupon/CouponInstanceListView.vue'),
    meta: { title: '用户券', requiresAuth: true, permission: 'coupon:instance:view', menu: true },
  },
  {
    path: 'coupon/rules',
    name: 'CouponRules',
    component: () => import('@/views/coupon/CouponStackRuleView.vue'),
    meta: { title: '叠加规则', requiresAuth: true, permission: 'coupon:rule:view', menu: true },
  },
]
