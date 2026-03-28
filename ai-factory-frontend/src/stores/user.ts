import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, LoginForm, LoginResult } from '@/types'
import * as userApi from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.actualName || '用户')

  const login = async (form: LoginForm): Promise<LoginResult> => {
    const result = await userApi.login(form)
    token.value = result.token
    localStorage.setItem('token', result.token)
    return result
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    try {
      userInfo.value = await userApi.getUserInfo()
    } catch (error) {
      console.error('Failed to fetch user info:', error)
    }
  }

  const logout = async () => {
    try {
      await userApi.logout()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      token.value = null
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    displayName,
    login,
    fetchUserInfo,
    logout
  }
})
