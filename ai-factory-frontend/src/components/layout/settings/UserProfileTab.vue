<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Mail, Phone, UserCircle, Pencil, X, Check, Loader2 } from 'lucide-vue-next'
import { getUserDetail, updateUserInfo } from '@/api/settings'
import { success, error } from '@/utils/toast'
import { useUserStore } from '@/stores/user'
import type { UserDetail, UpdateUserInfoRequest } from '@/types/settings'

// 用户 store
const userStore = useUserStore()

// 状态
const loading = ref(false)
const saving = ref(false)
const showEditDialog = ref(false)
const userDetail = ref<UserDetail | null>(null)

// 编辑表单
const editForm = ref<UpdateUserInfoRequest>({
  nickname: '',
  phone: '',
  email: '',
  bio: ''
})

// 加载用户详情
const loadUserDetail = async () => {
  loading.value = true
  try {
    userDetail.value = await getUserDetail()
  } catch (err: any) {
    error(err.message || '加载用户信息失败')
  } finally {
    loading.value = false
  }
}

// 打开编辑对话框
const openEditDialog = () => {
  if (userDetail.value) {
    editForm.value = {
      nickname: userDetail.value.nickname || '',
      phone: userDetail.value.phone || '',
      email: userDetail.value.email || '',
      bio: userDetail.value.bio || ''
    }
  }
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = () => {
  showEditDialog.value = false
}

// 保存用户信息
const handleSave = async () => {
  saving.value = true
  try {
    await updateUserInfo(editForm.value)
    success('保存成功')
    showEditDialog.value = false
    // 重新加载用户详情
    await loadUserDetail()
    // 刷新用户 store 中的用户信息
    await userStore.fetchUserInfo()
  } catch (err: any) {
    error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadUserDetail()
})

// 暴露刷新方法供父组件调用
defineExpose({
  refresh: loadUserDetail
})
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <Loader2 class="w-8 h-8 text-blue-500 animate-spin" />
    </div>

    <!-- 内容区域 -->
    <div v-else-if="userDetail" class="flex flex-col gap-6">
      <!-- 头像区域 -->
      <div class="flex flex-col items-center py-6">
        <div class="relative">
          <div class="w-24 h-24 rounded-full bg-gradient-to-br from-blue-100 to-purple-100 dark:from-blue-900/30 dark:to-purple-900/30 flex items-center justify-center overflow-hidden border-4 border-white dark:border-gray-700 shadow-lg">
            <img
              v-if="userDetail.avatar"
              :src="userDetail.avatar"
              :alt="userDetail.actualName"
              class="w-full h-full object-cover"
            />
            <User v-else class="w-12 h-12 text-blue-500 dark:text-blue-400" />
          </div>
          <!-- 预留头像上传按钮 -->
          <button
            class="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-blue-500 hover:bg-blue-600 text-white flex items-center justify-center shadow-md transition-colors"
            title="更换头像（暂未开放）"
            disabled
          >
            <Pencil class="w-4 h-4" />
          </button>
        </div>
        <h2 class="mt-4 text-xl font-semibold text-gray-900 dark:text-white">
          {{ userDetail.nickname || userDetail.actualName || '用户' }}
        </h2>
        <p class="text-sm text-gray-500 dark:text-gray-400">
          {{ userDetail.bio || '这个人很懒，什么都没留下~' }}
        </p>
      </div>

      <!-- 用户信息卡片 -->
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div class="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
          <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">基本信息</h3>
        </div>

        <div class="divide-y divide-gray-100 dark:divide-gray-700">
          <!-- 用户名 -->
          <div class="flex items-center gap-4 px-4 py-3">
            <div class="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center flex-shrink-0">
              <UserCircle class="w-5 h-5 text-blue-500 dark:text-blue-400" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs text-gray-500 dark:text-gray-400">用户名</p>
              <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ userDetail.loginName }}
              </p>
            </div>
          </div>

          <!-- 昵称 -->
          <div class="flex items-center gap-4 px-4 py-3">
            <div class="w-10 h-10 rounded-lg bg-purple-50 dark:bg-purple-900/30 flex items-center justify-center flex-shrink-0">
              <User class="w-5 h-5 text-purple-500 dark:text-purple-400" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs text-gray-500 dark:text-gray-400">昵称</p>
              <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ userDetail.nickname || userDetail.actualName || '未设置' }}
              </p>
            </div>
          </div>

          <!-- 手机 -->
          <div class="flex items-center gap-4 px-4 py-3">
            <div class="w-10 h-10 rounded-lg bg-green-50 dark:bg-green-900/30 flex items-center justify-center flex-shrink-0">
              <Phone class="w-5 h-5 text-green-500 dark:text-green-400" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs text-gray-500 dark:text-gray-400">手机</p>
              <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ userDetail.phone || '未绑定' }}
              </p>
            </div>
          </div>

          <!-- 邮箱 -->
          <div class="flex items-center gap-4 px-4 py-3">
            <div class="w-10 h-10 rounded-lg bg-amber-50 dark:bg-amber-900/30 flex items-center justify-center flex-shrink-0">
              <Mail class="w-5 h-5 text-amber-500 dark:text-amber-400" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs text-gray-500 dark:text-gray-400">邮箱</p>
              <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ userDetail.email || '未绑定' }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 编辑按钮 -->
      <button
        class="flex items-center justify-center gap-2 w-full px-4 py-3 rounded-xl text-sm font-medium
               bg-gradient-to-r from-blue-500 to-purple-500 text-white
               hover:from-blue-600 hover:to-purple-600 transition-all shadow-md hover:shadow-lg"
        @click="openEditDialog"
      >
        <Pencil class="w-4 h-4" />
        编辑资料
      </button>
    </div>

    <!-- 空状态 -->
    <div v-else class="flex flex-col items-center justify-center py-16 text-gray-500 dark:text-gray-400">
      <User class="w-12 h-12 mb-4 opacity-50" />
      <p>无法加载用户信息</p>
      <button
        class="mt-4 px-4 py-2 text-sm text-blue-500 hover:text-blue-600"
        @click="loadUserDetail"
      >
        重试
      </button>
    </div>

    <!-- 编辑对话框 -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition-opacity duration-200"
        leave-active-class="transition-opacity duration-200"
        enter-from-class="opacity-0"
        leave-to-class="opacity-0"
      >
        <div
          v-if="showEditDialog"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          @click.self="closeEditDialog"
        >
          <Transition
            enter-active-class="transition-all duration-200"
            leave-active-class="transition-all duration-200"
            enter-from-class="opacity-0 scale-95"
            leave-to-class="opacity-0 scale-95"
          >
            <div
              v-if="showEditDialog"
              class="w-full max-w-md mx-4 bg-white dark:bg-gray-800 rounded-2xl shadow-2xl overflow-hidden"
            >
              <!-- 对话框头部 -->
              <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
                <h3 class="text-lg font-semibold text-gray-900 dark:text-white">编辑资料</h3>
                <button
                  class="p-1.5 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  @click="closeEditDialog"
                >
                  <X class="w-5 h-5" />
                </button>
              </div>

              <!-- 表单内容 -->
              <div class="px-6 py-4 space-y-4">
                <!-- 昵称 -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    昵称
                  </label>
                  <input
                    v-model="editForm.nickname"
                    type="text"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600
                           bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                           focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    placeholder="请输入昵称"
                  />
                </div>

                <!-- 手机 -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    手机
                  </label>
                  <input
                    v-model="editForm.phone"
                    type="tel"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600
                           bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                           focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    placeholder="请输入手机号"
                  />
                </div>

                <!-- 邮箱 -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    邮箱
                  </label>
                  <input
                    v-model="editForm.email"
                    type="email"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600
                           bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                           focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    placeholder="请输入邮箱"
                  />
                </div>

                <!-- 个人简介 -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    个人简介
                  </label>
                  <textarea
                    v-model="editForm.bio"
                    rows="3"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600
                           bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                           focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all resize-none"
                    placeholder="介绍一下自己吧~"
                  ></textarea>
                </div>
              </div>

              <!-- 对话框底部 -->
              <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                <button
                  class="px-4 py-2 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-300
                         hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  :disabled="saving"
                  @click="closeEditDialog"
                >
                  取消
                </button>
                <button
                  class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
                         bg-gradient-to-r from-blue-500 to-purple-500 text-white
                         hover:from-blue-600 hover:to-purple-600 transition-all shadow-sm
                         disabled:opacity-50 disabled:cursor-not-allowed"
                  :disabled="saving"
                  @click="handleSave"
                >
                  <Loader2 v-if="saving" class="w-4 h-4 animate-spin" />
                  <Check v-else class="w-4 h-4" />
                  {{ saving ? '保存中...' : '保存' }}
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
