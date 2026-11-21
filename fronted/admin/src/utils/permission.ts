export function hasPermission(permissions: string[], required?: string): boolean {
  if (!required) {
    return true
  }

  return permissions.includes('*') || permissions.includes(required)
}
