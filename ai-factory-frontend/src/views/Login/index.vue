<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/user'
import { error, success, info } from '@/utils/toast'
import Btn from '@/components/ui/Btn.vue'
import Input from '@/components/ui/Input.vue'
import type { CaptchaResult, RegisterForm } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// Form mode
const isLogin = ref(true)

// Form data
const loginName = ref('')
const password = ref('')
const confirmPassword = ref('')
const actualName = ref('')
const nickname = ref('')
const phone = ref('')
const email = ref('')
const captchaCode = ref('')
const rememberMe = ref(false)

// Captcha
const captcha = ref<CaptchaResult | null>(null)

// UI state
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

// Fetch captcha
const fetchCaptcha = async () => {
  try {
    captcha.value = await getCaptcha()
  } catch (error) {
    console.error('Failed to fetch captcha:', error)
  }
}

// Toggle mode
const toggleMode = () => {
  isLogin.value = !isLogin.value
  // Reset form
  loginName.value = ''
  password.value = ''
  confirmPassword.value = ''
  actualName.value = ''
  nickname.value = ''
  phone.value = ''
  email.value = ''
  captchaCode.value = ''
  // Refresh captcha
  fetchCaptcha()
}

// Handle login
const handleLogin = async () => {
  if (!loginName.value.trim()) {
    error('请输入用户名')
    return
  }
  if (!password.value.trim()) {
    error('请输入密码')
    return
  }
  if (captcha.value && !captchaCode.value.trim()) {
    error('请输入验证码')
    return
  }

  loading.value = true
  try {
    await userStore.login({
      loginName: loginName.value,
      password: password.value,
      captchaCode: captchaCode.value,
      captchaUuid: captcha.value?.captchaUuid
    })

    success('登录成功')

    // Redirect
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
  } catch (error) {
    // Error already handled by request interceptor
    fetchCaptcha()
    captchaCode.value = ''
  } finally {
    loading.value = false
  }
}

// Handle register
const handleRegister = async () => {
  if (!loginName.value.trim()) {
    error('请输入用户名')
    return
  }
  if (!password.value.trim()) {
    error('请输入密码')
    return
  }
  if (password.value !== confirmPassword.value) {
    error('两次输入的密码不一致')
    return
  }
  if (!actualName.value.trim()) {
    error('请输入真实姓名')
    return
  }
  if (!phone.value.trim()) {
    error('请输入手机号')
    return
  }
  if (captcha.value && !captchaCode.value.trim()) {
    error('请输入验证码')
    return
  }

  loading.value = true
  try {
    const registerData: RegisterForm = {
      loginName: loginName.value,
      password: password.value,
      actualName: actualName.value,
      nickname: nickname.value || undefined,
      phone: phone.value,
      email: email.value || undefined
    }

    const { register } = await import('@/api/user')
    await register(registerData)

    success('注册成功，请登录')
    isLogin.value = true
    fetchCaptcha()
  } catch (error) {
    // Error already handled by request interceptor
    fetchCaptcha()
    captchaCode.value = ''
  } finally {
    loading.value = false
  }
}

// Handle submit
const handleSubmit = () => {
  if (isLogin.value) {
    handleLogin()
  } else {
    handleRegister()
  }
}

// Handle WeChat login
const handleWeChatLogin = () => {
  info('微信登录功能开发中')
}

onMounted(() => {
  fetchCaptcha()
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500 dark:from-gray-900 dark:via-purple-900 dark:to-gray-900 p-4 relative overflow-hidden">
    <!-- Decorative gradient blobs -->
    <div class="absolute top-20 left-20 w-72 h-72 bg-blue-400/30 dark:bg-blue-600/20 rounded-full blur-3xl animate-pulse"></div>
    <div class="absolute bottom-20 right-20 w-96 h-96 bg-purple-400/30 dark:bg-purple-600/20 rounded-full blur-3xl animate-pulse delay-1000"></div>
    <div class="absolute top-1/2 left-1/3 w-64 h-64 bg-pink-400/20 dark:bg-pink-600/10 rounded-full blur-3xl animate-pulse delay-500"></div>

    <div class="w-full max-w-5xl grid md:grid-cols-2 gap-8 relative z-10">
      <!-- Left side - Brand area -->
      <div class="hidden md:flex flex-col justify-center text-white p-8">
        <div class="mb-8">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center">
              <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                <path d="M2 17l10 5 10-5"/>
                <path d="M2 12l10 5 10-5"/>
              </svg>
            </div>
            <h1 class="text-3xl font-bold">AI Factory</h1>
          </div>
          <p class="text-white/80 text-lg">AI驱动的智能小说创作平台</p>
        </div>

        <!-- Feature list -->
        <div class="space-y-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-white/10 backdrop-blur-md rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 3l1.912 5.813a2 2 0 001.272 1.272L21 12l-5.816 1.915a2 2 0 00-1.272 1.272L12 21l-1.912-5.813a2 2 0 00-1.272-1.272L3 12l5.816-1.915a2 2 0 001.272-1.272L12 3z"/>
              </svg>
            </div>
            <span class="text-white/90">AI智能辅助创作</span>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-white/10 backdrop-blur-md rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                <path d="M16 3.13a4 4 0 010 7.75"/>
              </svg>
            </div>
            <span class="text-white/90">人物角色管理</span>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-white/10 backdrop-blur-md rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/>
              </svg>
            </div>
            <span class="text-white/90">世界观构建工具</span>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-white/10 backdrop-blur-md rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M3 9h18"/>
                <path d="M9 21V9"/>
              </svg>
            </div>
            <span class="text-white/90">故事板可视化</span>
          </div>
        </div>
      </div>

      <!-- Right side - Login form -->
      <div class="flex items-center justify-center p-4">
        <div class="w-full max-w-md">
          <!-- Login/Register card -->
          <div class="bg-white/80 dark:bg-gray-800/80 backdrop-blur-md rounded-2xl shadow-2xl p-8 border border-white/20 dark:border-gray-700/50">
            <!-- Mobile logo -->
            <div class="md:hidden flex items-center justify-center gap-3 mb-6">
              <div class="w-10 h-10 bg-gradient-to-r from-blue-500 to-purple-500 rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
              </div>
              <h1 class="text-2xl font-bold text-gray-900 dark:text-white">AI Factory</h1>
            </div>

            <!-- Toggle tabs -->
            <div class="flex mb-6 bg-gray-100 dark:bg-gray-700/50 rounded-xl p-1">
              <button
                :class="[
                  'flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-all duration-200',
                  isLogin
                    ? 'bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                ]"
                @click="isLogin = true"
              >
                登录
              </button>
              <button
                :class="[
                  'flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-all duration-200',
                  !isLogin
                    ? 'bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                ]"
                @click="toggleMode"
              >
                注册
              </button>
            </div>

            <!-- Form -->
            <form @submit.prevent="handleSubmit" class="space-y-4">
              <!-- Username -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  用户名
                </label>
                <div class="relative">
                  <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                  </div>
                  <input
                    v-model="loginName"
                    type="text"
                    placeholder="请输入用户名"
                    class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                  />
                </div>
              </div>

              <!-- Register fields -->
              <template v-if="!isLogin">
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    真实姓名
                  </label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                        <circle cx="12" cy="7" r="4"/>
                      </svg>
                    </div>
                    <input
                      v-model="actualName"
                      type="text"
                      placeholder="请输入真实姓名"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                    />
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                      昵称 (可选)
                    </label>
                    <input
                      v-model="nickname"
                      type="text"
                      placeholder="昵称"
                      class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                      手机号
                    </label>
                    <input
                      v-model="phone"
                      type="tel"
                      placeholder="手机号"
                      class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                    />
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    邮箱 (可选)
                  </label>
                  <input
                    v-model="email"
                    type="email"
                    placeholder="请输入邮箱"
                    class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                  />
                </div>
              </template>

              <!-- Password -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  密码
                </label>
                <div class="relative">
                  <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0110 0v4"/>
                    </svg>
                  </div>
                  <input
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
                    class="w-full pl-10 pr-10 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                  />
                  <button
                    type="button"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center"
                    @click="showPassword = !showPassword"
                  >
                    <svg v-if="showPassword" class="w-5 h-5 text-gray-400 hover:text-gray-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                    <svg v-else class="w-5 h-5 text-gray-400 hover:text-gray-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  </button>
                </div>
              </div>

              <!-- Confirm password (register only) -->
              <div v-if="!isLogin">
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  确认密码
                </label>
                <div class="relative">
                  <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0110 0v4"/>
                    </svg>
                  </div>
                  <input
                    v-model="confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    placeholder="请再次输入密码"
                    class="w-full pl-10 pr-10 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                  />
                  <button
                    type="button"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center"
                    @click="showConfirmPassword = !showConfirmPassword"
                  >
                    <svg v-if="showConfirmPassword" class="w-5 h-5 text-gray-400 hover:text-gray-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                    <svg v-else class="w-5 h-5 text-gray-400 hover:text-gray-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  </button>
                </div>
              </div>

              <!-- Captcha -->
              <div v-if="captcha">
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  验证码
                </label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                      </svg>
                    </div>
                    <input
                      v-model="captchaCode"
                      type="text"
                      placeholder="请输入验证码"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
                    />
                  </div>
                  <div
                    class="h-[42px] min-w-[100px] rounded-xl overflow-hidden cursor-pointer border border-gray-200 dark:border-gray-600 hover:opacity-80 transition-opacity"
                    @click="fetchCaptcha"
                    title="点击刷新验证码"
                  >
                    <img
                      :src="captcha.captchaBase64Image"
                      alt="验证码"
                      class="h-full w-full object-cover"
                    />
                  </div>
                </div>
              </div>

              <!-- Remember me (login only) -->
              <div v-if="isLogin" class="flex items-center">
                <label class="flex items-center cursor-pointer">
                  <input
                    v-model="rememberMe"
                    type="checkbox"
                    class="w-4 h-4 rounded border-gray-300 text-blue-500 focus:ring-blue-500/50"
                  />
                  <span class="ml-2 text-sm text-gray-600 dark:text-gray-400">记住我</span>
                </label>
              </div>

              <!-- Submit button -->
              <Btn
                type="submit"
                :loading="loading"
                block
                size="lg"
                class="mt-6"
              >
                {{ isLogin ? '登录' : '注册' }}
              </Btn>
            </form>

            <!-- Divider -->
            <div class="relative my-6">
              <div class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-gray-200 dark:border-gray-700"></div>
              </div>
              <div class="relative flex justify-center text-sm">
                <span class="px-4 bg-white/80 dark:bg-gray-800/80 text-gray-500 dark:text-gray-400">
                  或者
                </span>
              </div>
            </div>

            <!-- Social login -->
            <div class="flex justify-center">
              <button
                type="button"
                class="flex items-center justify-center gap-2 px-6 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white/50 dark:bg-gray-700/50 backdrop-blur-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600/50 transition-all duration-200"
                @click="handleWeChatLogin"
              >
                <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 01.598.082l1.584.926a.272.272 0 00.14.045c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 01-.023-.156.49.49 0 01.201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-7.062-6.122zm-2.036 2.96c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982z"/>
                </svg>
                <span>微信登录</span>
              </button>
            </div>

            <!-- Switch mode link -->
            <p class="mt-6 text-center text-sm text-gray-600 dark:text-gray-400">
              {{ isLogin ? '还没有账号?' : '已有账号?' }}
              <button
                type="button"
                class="text-blue-500 hover:text-blue-600 font-medium"
                @click="toggleMode"
              >
                {{ isLogin ? '立即注册' : '立即登录' }}
              </button>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
