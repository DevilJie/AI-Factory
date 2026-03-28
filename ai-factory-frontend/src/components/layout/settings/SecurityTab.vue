<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Shield, Lock, Eye, EyeOff, Loader2, Check, AlertCircle } from 'lucide-vue-next'
import { changePassword } from '@/api/settings'
import { useUserStore } from '@/stores/user'
import { success, error } from '@/utils/toast'
import type { ChangePasswordRequest } from '@/types/settings'

const router = useRouter()
const userStore = useUserStore()

// 表单数据
const form = ref<ChangePasswordRequest>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 状态
const saving = ref(false)
const showOldPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

// 验证错误
const errors = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证
const validate = (): boolean => {
  let isValid = true
  errors.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }

  // 原密码验证
  if (!form.value.oldPassword) {
    errors.value.oldPassword = '请输入原密码'
    isValid = false
  }

  // 新密码验证
  if (!form.value.newPassword) {
    errors.value.newPassword = '请输入新密码'
    isValid = false
  } else if (form.value.newPassword.length < 6) {
    errors.value.newPassword = '密码长度不能少于6位'
    isValid = false
  }

  // 确认密码验证
  if (!form.value.confirmPassword) {
    errors.value.confirmPassword = '请再次输入新密码'
    isValid = false
  } else if (form.value.newPassword !== form.value.confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致'
    isValid = false
  }

  return isValid
}

// 提交表单
const handleSubmit = async () => {
  if (!validate()) return

  saving.value = true
  try {
    await changePassword(form.value)
    success('密码修改成功，请重新登录')

    // 清空表单
    form.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }

    // 退出登录并跳转到登录页
    await userStore.logout()
    router.push('/login')
  } catch (err: any) {
    error(err.message || '密码修改失败')
  } finally {
    saving.value = false
  }
}

// 清除单个字段错误
const clearError = (field: keyof typeof errors.value) => {
  errors.value[field] = ''
}

// 暴露刷新方法供父组件调用
defineExpose({
  refresh: () => {
    form.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
    errors.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  }
})
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- 密码修改卡片 -->
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
      <!-- 卡片头部 -->
      <div class="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-red-50 dark:bg-red-900/30 flex items-center justify-center flex-shrink-0">
            <Shield class="w-5 h-5 text-red-500 dark:text-red-400" />
          </div>
          <div>
            <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">密码修改</h3>
            <p class="text-xs text-gray-500 dark:text-gray-400">修改您的登录密码</p>
          </div>
        </div>
      </div>

      <!-- 表单内容 -->
      <div class="p-4 space-y-4">
        <!-- 原密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            原密码
          </label>
          <div class="relative">
            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Lock class="w-4 h-4 text-gray-400" />
            </div>
            <input
              v-model="form.oldPassword"
              :type="showOldPassword ? 'text' : 'password'"
              class="w-full pl-10 pr-10 py-2 rounded-lg border transition-all
                     bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                     focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              :class="errors.oldPassword
                ? 'border-red-300 dark:border-red-600 focus:ring-red-500'
                : 'border-gray-300 dark:border-gray-600'"
              placeholder="请输入原密码"
              @input="clearError('oldPassword')"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              @click="showOldPassword = !showOldPassword"
            >
              <Eye v-if="showOldPassword" class="w-4 h-4" />
              <EyeOff v-else class="w-4 h-4" />
            </button>
          </div>
          <p v-if="errors.oldPassword" class="mt-1 text-xs text-red-500 flex items-center gap-1">
            <AlertCircle class="w-3 h-3" />
            {{ errors.oldPassword }}
          </p>
        </div>

        <!-- 新密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            新密码
          </label>
          <div class="relative">
            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Lock class="w-4 h-4 text-gray-400" />
            </div>
            <input
              v-model="form.newPassword"
              :type="showNewPassword ? 'text' : 'password'"
              class="w-full pl-10 pr-10 py-2 rounded-lg border transition-all
                     bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                     focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              :class="errors.newPassword
                ? 'border-red-300 dark:border-red-600 focus:ring-red-500'
                : 'border-gray-300 dark:border-gray-600'"
              placeholder="请输入新密码（至少6位）"
              @input="clearError('newPassword')"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              @click="showNewPassword = !showNewPassword"
            >
              <Eye v-if="showNewPassword" class="w-4 h-4" />
              <EyeOff v-else class="w-4 h-4" />
            </button>
          </div>
          <p v-if="errors.newPassword" class="mt-1 text-xs text-red-500 flex items-center gap-1">
            <AlertCircle class="w-3 h-3" />
            {{ errors.newPassword }}
          </p>
        </div>

        <!-- 确认密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            确认新密码
          </label>
          <div class="relative">
            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Lock class="w-4 h-4 text-gray-400" />
            </div>
            <input
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="w-full pl-10 pr-10 py-2 rounded-lg border transition-all
                     bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                     focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              :class="errors.confirmPassword
                ? 'border-red-300 dark:border-red-600 focus:ring-red-500'
                : 'border-gray-300 dark:border-gray-600'"
              placeholder="请再次输入新密码"
              @input="clearError('confirmPassword')"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              @click="showConfirmPassword = !showConfirmPassword"
            >
              <Eye v-if="showConfirmPassword" class="w-4 h-4" />
              <EyeOff v-else class="w-4 h-4" />
            </button>
          </div>
          <p v-if="errors.confirmPassword" class="mt-1 text-xs text-red-500 flex items-center gap-1">
            <AlertCircle class="w-3 h-3" />
            {{ errors.confirmPassword }}
          </p>
        </div>

        <!-- 提交按钮 -->
        <button
          class="flex items-center justify-center gap-2 w-full px-4 py-3 rounded-xl text-sm font-medium
                 bg-gradient-to-r from-red-500 to-orange-500 text-white
                 hover:from-red-600 hover:to-orange-600 transition-all shadow-md hover:shadow-lg
                 disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="saving"
          @click="handleSubmit"
        >
          <Loader2 v-if="saving" class="w-4 h-4 animate-spin" />
          <Check v-else class="w-4 h-4" />
          {{ saving ? '修改中...' : '确认修改' }}
        </button>
      </div>
    </div>

    <!-- 安全提示 -->
    <div class="mt-4 p-4 bg-amber-50 dark:bg-amber-900/20 rounded-xl border border-amber-200 dark:border-amber-800/30">
      <div class="flex items-start gap-3">
        <AlertCircle class="w-5 h-5 text-amber-500 dark:text-amber-400 flex-shrink-0 mt-0.5" />
        <div class="text-sm text-amber-700 dark:text-amber-300">
          <p class="font-medium mb-1">安全提示</p>
          <ul class="list-disc list-inside space-y-1 text-xs text-amber-600 dark:text-amber-400">
            <li>密码长度至少6位</li>
            <li>建议使用字母、数字和特殊字符的组合</li>
            <li>修改密码后需要重新登录</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>
