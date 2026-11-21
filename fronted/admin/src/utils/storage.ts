import type { LoginResult } from '@/types/auth'

const ACCESS_TOKEN_KEY = 'shopping_mall_admin_access_token'
const REFRESH_TOKEN_KEY = 'shopping_mall_admin_refresh_token'

export function getAccessToken(): string {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
}

export function getRefreshToken(): string {
  return sessionStorage.getItem(REFRESH_TOKEN_KEY) ?? ''
}

export function saveTokens(tokens: Pick<LoginResult, 'accessToken' | 'refreshToken'>): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export function clearTokens(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
}
