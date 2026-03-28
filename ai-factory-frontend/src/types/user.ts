export interface LoginForm {
  loginName: string
  password: string
  captchaCode?: string
  captchaUuid?: string
}

export interface RegisterForm {
  loginName: string
  password: string
  actualName: string
  nickname?: string
  phone: string
  email?: string
}

export interface LoginResult {
  token: string
  userId: number
  loginName: string
  actualName: string
  nickname?: string
  avatar?: string
}

export interface UserInfo {
  userId: number
  loginName: string
  actualName: string
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
}

export interface CaptchaResult {
  captchaBase64Image: string
  captchaUuid: string
  expireSeconds: number
}
