import { describe, expect, it } from 'vitest'

import { hasPermission } from '@/utils/permission'

describe('hasPermission', () => {
  it('allows public routes when no permission is required', () => {
    expect(hasPermission([], undefined)).toBe(true)
  })

  it('allows only explicitly assigned permissions', () => {
    const permissions = ['coupon:template:view']

    expect(hasPermission(permissions, 'coupon:template:view')).toBe(true)
    expect(hasPermission(permissions, 'coupon:template:delete')).toBe(false)
  })

  it('allows all permissions for wildcard administrators', () => {
    expect(hasPermission(['*'], 'system:admin:delete')).toBe(true)
  })
})
