export interface AdminInfo {
  id: string
  username: string
  nickname: string
  avatarUrl?: string
}

export interface MenuItem {
  id: string
  name: string
  code: string
  routePath: string
  icon?: string
  children?: MenuItem[]
}

export interface LoginParams {
  username: string
  password: string
  deviceId: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface MeResult {
  admin: AdminInfo
  permissions: string[]
  menus: MenuItem[]
  pwdResetRequired: boolean
}
