import request from '@/utils/request'
import type { LoginForm, LoginResult, UserInfo, CaptchaResult } from '@/types'

export const login = (data: LoginForm) => {
  return request.post<LoginResult>('/api/user/login', data)
}

export const register = (data: any) => {
  return request.post<string>('/api/user/register', data)
}

export const getUserInfo = () => {
  return request.get<UserInfo>('/api/user/getLoginInfo')
}

export const logout = () => {
  return request.get<string>('/api/user/logout')
}

export const getCaptcha = () => {
  return request.get<CaptchaResult>('/api/user/getCaptcha')
}
