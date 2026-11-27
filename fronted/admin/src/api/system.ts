import { api } from '@/api/http'
import type { PageResult } from '@/types/api'
import type {
  AdminCreateResult,
  AdminDetail,
  AdminItem,
  OperationLogItem,
  PermissionNode,
  RoleItem,
} from '@/types/system'

const BASE = '/api/v1/admin/system'

export const systemApi = {
  // ===== 管理员 =====
  pageAdmins(params: {
    keyword?: string
    status?: number
    page?: number
    pageSize?: number
  }) {
    return api.get<PageResult<AdminItem>>(`${BASE}/admins`, { params })
  },
  getAdmin(id: string) {
    return api.get<AdminDetail>(`${BASE}/admins/${id}`)
  },
  createAdmin(body: { username: string; nickname?: string; roleIds: string[] }) {
    return api.post<AdminCreateResult>(`${BASE}/admins`, body)
  },
  updateAdmin(id: string, body: { nickname?: string; email?: string; status: number }) {
    return api.put<AdminDetail>(`${BASE}/admins/${id}`, body)
  },
  deleteAdmin(id: string) {
    return api.delete<void>(`${BASE}/admins/${id}`)
  },
  assignAdminRoles(id: string, roleIds: string[]) {
    return api.post<void>(`${BASE}/admins/${id}/roles`, { roleIds })
  },
  resetAdminPassword(id: string) {
    return api.post<{ password: string }>(`${BASE}/admins/${id}/reset-password`)
  },

  // ===== 角色 =====
  listRoles() {
    return api.get<RoleItem[]>(`${BASE}/roles`)
  },
  getRole(id: string) {
    return api.get<RoleItem>(`${BASE}/roles/${id}`)
  },
  createRole(body: {
    code: string
    name: string
    description?: string
    dataScope?: number
    sort?: number
  }) {
    return api.post<RoleItem>(`${BASE}/roles`, body)
  },
  updateRole(
    id: string,
    body: { name: string; description?: string; dataScope?: number; status: number; sort?: number },
  ) {
    return api.put<RoleItem>(`${BASE}/roles/${id}`, body)
  },
  deleteRole(id: string) {
    return api.delete<void>(`${BASE}/roles/${id}`)
  },
  assignRolePermissions(id: string, permissionIds: string[]) {
    return api.post<void>(`${BASE}/roles/${id}/permissions`, { permissionIds })
  },

  // ===== 权限 =====
  treePermissions() {
    return api.get<PermissionNode[]>(`${BASE}/permissions`)
  },
  updatePermission(
    id: string,
    body: { name: string; icon?: string; sort?: number; visible: number },
  ) {
    return api.put<void>(`${BASE}/permissions/${id}`, body)
  },

  // ===== 操作日志 =====
  pageLogs(params: {
    adminId?: string
    module?: string
    success?: number
    start?: string
    end?: string
    page?: number
    pageSize?: number
  }) {
    return api.get<PageResult<OperationLogItem>>(`${BASE}/logs`, { params })
  },
}
