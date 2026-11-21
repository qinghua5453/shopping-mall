import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { useAuthStore } from '@/stores/auth'

describe('auth store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('checks permissions without requiring an API request', () => {
    const auth = useAuthStore()
    auth.permissions = ['member:member:view']

    expect(auth.checkPermission('member:member:view')).toBe(true)
    expect(auth.checkPermission('member:member:update-status')).toBe(false)
  })

  it('clears identity and permissions when the session ends', () => {
    const auth = useAuthStore()
    auth.permissions = ['*']
    auth.loaded = true

    auth.clearSession()

    expect(auth.isLoggedIn).toBe(false)
    expect(auth.permissions).toEqual([])
    expect(auth.loaded).toBe(false)
  })
})
