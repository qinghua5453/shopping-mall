const DEVICE_ID_KEY = 'shopping_mall_admin_device_id'

function uuid(): string {
  return crypto.randomUUID()
}

export function getDeviceId(): string {
  const existing = localStorage.getItem(DEVICE_ID_KEY)
  if (existing) {
    return existing
  }

  const deviceId = uuid()
  localStorage.setItem(DEVICE_ID_KEY, deviceId)
  return deviceId
}

export function createRequestId(): string {
  return uuid()
}
