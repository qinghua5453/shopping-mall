import { api } from '@/api/http'
import type { LoginParams, LoginResult, MeResult } from '@/types/auth'

const AUTH_BASE_URL = '/api/v1/admin/auth'

export function login(params: LoginParams): Promise<LoginResult> {
  return api.post<LoginResult>(`${AUTH_BASE_URL}/login`, params)
}

export function getMe(): Promise<MeResult> {
  return api.get<MeResult>(`${AUTH_BASE_URL}/me`)
}

export function logout(): Promise<void> {
  return api.post<void>(`${AUTH_BASE_URL}/logout`)
}
