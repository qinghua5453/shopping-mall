import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import type { AdminInfo, LoginParams, MenuItem } from '@/types/auth'
import { getDeviceId } from '@/utils/device'
import { hasPermission } from '@/utils/permission'
import { clearTokens, getAccessToken, saveTokens } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(getAccessToken())
  const admin = ref<AdminInfo | null>(null)
  const permissions = ref<string[]>([])
  const menus = ref<MenuItem[]>([])
  const pwdResetRequired = ref(false)
  const loaded = ref(false)
  const loggingIn = ref(false)

  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const displayName = computed(() => admin.value?.nickname || admin.value?.username || '管理员')

  function checkPermission(required?: string): boolean {
    return hasPermission(permissions.value, required)
  }

  async function login(params: Pick<LoginParams, 'username' | 'password'>): Promise<void> {
    loggingIn.value = true
    try {
      const tokens = await authApi.login({ ...params, deviceId: getDeviceId() })
      saveTokens(tokens)
      accessToken.value = tokens.accessToken
      await fetchMe()
    } finally {
      loggingIn.value = false
    }
  }

  async function fetchMe(): Promise<void> {
    const result = await authApi.getMe()
    admin.value = result.admin
    permissions.value = result.permissions
    menus.value = result.menus
    pwdResetRequired.value = result.pwdResetRequired
    loaded.value = true
  }

  function clearSession(): void {
    clearTokens()
    accessToken.value = ''
    admin.value = null
    permissions.value = []
    menus.value = []
    pwdResetRequired.value = false
    loaded.value = false
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }

  return {
    accessToken,
    admin,
    permissions,
    menus,
    pwdResetRequired,
    loaded,
    loggingIn,
    isLoggedIn,
    displayName,
    checkPermission,
    login,
    fetchMe,
    clearSession,
    logout,
  }
})
