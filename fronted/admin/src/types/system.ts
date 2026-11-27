export interface AdminItem {
  id: string
  username: string
  nickname: string
  email?: string
  status: number
  pwdResetRequired?: number
  lastLoginAt?: string | null
  createdAt: string
}

export interface AdminDetail extends AdminItem {
  roleIds: string[]
}

export interface AdminCreateResult {
  id: string
  username: string
  password: string
}

export interface RoleItem {
  id: string
  code: string
  name: string
  description?: string
  dataScope: number
  status: number
  sort: number
  permissionIds: string[]
}

export interface PermissionNode {
  id: string
  parentId: string
  permissionType: number
  code: string
  name: string
  routePath?: string
  component?: string
  apiMethod?: string
  apiPath?: string
  icon?: string
  sort: number
  visible: number
  status: number
  children: PermissionNode[]
}

export interface OperationLogItem {
  id: string
  adminId: string
  username: string
  module: string
  operation: string
  method: string
  path: string
  params?: string
  success: number
  errorMsg?: string
  costMs: number
  traceId: string
  ip?: string
  userAgent?: string
  createdAt: string
}

export type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

export const STATUS_LABEL: Record<number, string> = { 1: '启用', 0: '禁用' }
export const STATUS_TYPE: Record<number, TagType> = { 1: 'success', 0: 'info' }

export const DATA_SCOPE_LABEL: Record<number, string> = { 1: '全部数据', 2: '仅本人' }

export const PERMISSION_TYPE_LABEL: Record<number, string> = {
  1: '目录',
  2: '菜单',
  3: '按钮',
  4: '接口',
}

export const PERMISSION_TYPE_TAG: Record<number, TagType | undefined> = {
  1: undefined,
  2: undefined,
  3: 'warning',
  4: 'info',
}

export const LOG_SUCCESS_LABEL: Record<number, string> = { 1: '成功', 0: '失败' }
export const LOG_SUCCESS_TYPE: Record<number, TagType> = { 1: 'success', 0: 'danger' }

export const MODULE_OPTIONS = [
  { label: '系统管理', value: 'system' },
  { label: '优惠券', value: 'coupon' },
]
