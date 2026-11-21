import axios, { AxiosError } from 'axios'
import type { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import type { ApiResponse } from '@/types/api'
import type { LoginResult } from '@/types/auth'
import { createRequestId, getDeviceId } from '@/utils/device'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '@/utils/storage'

export const AUTH_EXPIRED_EVENT = 'shopping-mall:admin-auth-expired'

export class ApiError extends Error {
  readonly code: number
  readonly traceId: string

  constructor(code: number, message: string, traceId = '') {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
  }
}

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean }

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

let refreshPromise: Promise<void> | null = null

function notifyAuthExpired(): void {
  clearTokens()
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT))
}

function isRefreshRequest(url?: string): boolean {
  return url?.includes('/api/v1/admin/auth/refresh') ?? false
}

async function refreshTokens(): Promise<void> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new ApiError(40102, '登录状态已失效，请重新登录')
  }

  const response = await axios.post<ApiResponse<LoginResult>>(
    `${import.meta.env.VITE_API_BASE_URL}/api/v1/admin/auth/refresh`,
    { refreshToken, deviceId: getDeviceId() },
    {
      headers: { 'X-Client-Type': 'PC', 'X-Device-Id': getDeviceId() },
      timeout: 15000,
    },
  )

  if (response.data.code !== 0) {
    throw new ApiError(response.data.code, response.data.message, response.data.traceId)
  }

  saveTokens(response.data.data)
}

async function retryAfterRefresh(config: RetryConfig): Promise<AxiosResponse> {
  if (config._retry || isRefreshRequest(config.url)) {
    notifyAuthExpired()
    throw new ApiError(40102, '登录状态已失效，请重新登录')
  }

  config._retry = true

  try {
    refreshPromise ??= refreshTokens()
    await refreshPromise
    return await http.request(config)
  } catch (error) {
    notifyAuthExpired()
    throw error
  } finally {
    refreshPromise = null
  }
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  config.headers.set('X-Client-Type', 'PC')
  config.headers.set('X-Device-Id', getDeviceId())
  config.headers.set('X-Request-Id', createRequestId())
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body.code === 0) {
      return response
    }

    if (body.code === 40101) {
      return retryAfterRefresh(response.config)
    }

    if (body.code === 40102 || body.code === 40108) {
      notifyAuthExpired()
    }

    throw new ApiError(body.code, body.message || '请求失败', body.traceId)
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config as RetryConfig | undefined
    const body = error.response?.data
    const code = body?.code ?? error.response?.status ?? 50000

    if (code === 40101 && config) {
      return retryAfterRefresh(config)
    }

    if (code === 40102 || code === 40108) {
      notifyAuthExpired()
    }

    const message =
      body?.message ||
      (error.response ? '服务暂时不可用，请稍后重试' : '网络连接失败，请检查后端服务')
    return Promise.reject(new ApiError(code, message, body?.traceId))
  },
)

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}

export const api = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'GET', url })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'POST', url, data })
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'PUT', url, data })
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'DELETE', url })
  },
}
