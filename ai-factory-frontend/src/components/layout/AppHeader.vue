<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles, Search, Sun, Moon, Monitor, LogOut, User, Settings } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { useTheme, type Theme } from '@/composables/useTheme'
import { useSettingsDrawer } from '@/composables/useSettingsDrawer'
import { success } from '@/utils/toast'
import SettingsDrawer from './SettingsDrawer.vue'

const router = useRouter()
const userStore = useUserStore()
const { theme, setTheme } = useTheme()
const { openDrawer } = useSettingsDrawer()

const searchQuery = ref('')
const showUserMenu = ref(false)

const themeOptions: { value: Theme; icon: any; label: string }[] = [
  { value: 'light', icon: Sun, label: '浅色' },
  { value: 'dark', icon: Moon, label: '深色' },
  { value: 'system', icon: Monitor, label: '跟随系统' }
]

const handleLogout = async () => {
  await userStore.logout()
  success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <header class="sticky top-0 z-50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl border-b border-gray-200/50 dark:border-gray-800/50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <router-link to="/dashboard" class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center">
            <Sparkles class="w-4 h-4 text-white" />
          </div>
          <span class="font-bold text-gray-900 dark:text-white hidden sm:block">AI创作工厂</span>
        </router-link>

        <!-- 搜索框 -->
        <div class="flex-1 max-w-md mx-4">
          <div class="relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索项目..."
              class="w-full pl-10 pr-4 py-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500 transition-all"
            />
          </div>
        </div>

        <!-- 右侧操作 -->
        <div class="flex items-center gap-2">
          <!-- 主题切换 -->
          <div class="flex items-center p-1 rounded-xl bg-gray-100 dark:bg-gray-800">
            <button
              v-for="option in themeOptions"
              :key="option.value"
              :class="[
                'p-1.5 rounded-lg transition-all',
                theme === option.value
                  ? 'bg-white dark:bg-gray-700 text-blue-500 shadow-sm'
                  : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'
              ]"
              :title="option.label"
              @click="setTheme(option.value)"
            >
              <component :is="option.icon" class="w-4 h-4" />
            </button>
          </div>

          <!-- 用户菜单 -->
          <div class="relative">
            <button
              class="flex items-center gap-2 p-1.5 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
              @click="showUserMenu = !showUserMenu"
            >
              <div class="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-white text-sm font-medium">
                {{ userStore.displayName?.charAt(0) || 'U' }}
              </div>
            </button>

            <Transition
              enter-active-class="transition duration-200 ease-out"
              enter-from-class="opacity-0 scale-95"
              enter-to-class="opacity-100 scale-100"
              leave-active-class="transition duration-150 ease-in"
              leave-from-class="opacity-100 scale-100"
              leave-to-class="opacity-0 scale-95"
            >
              <div
                v-if="showUserMenu"
                class="absolute right-0 mt-2 w-48 py-1 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-lg"
              >
                <div class="px-4 py-2 border-b border-gray-200 dark:border-gray-700">
                  <p class="font-medium text-gray-900 dark:text-white">{{ userStore.displayName }}</p>
                  <p class="text-sm text-gray-500">{{ userStore.userInfo?.email || '未设置邮箱' }}</p>
                </div>
                <button
                  class="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                  @click="showUserMenu = false; openDrawer('user-profile')"
                >
                  <User class="w-4 h-4" />
                  个人资料
                </button>
                <button
                  class="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                  @click="showUserMenu = false; openDrawer('ai-model')"
                >
                  <Settings class="w-4 h-4" />
                  设置
                </button>
                <button
                  class="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20"
                  @click="handleLogout"
                >
                  <LogOut class="w-4 h-4" />
                  退出登录
                </button>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </div>
  </header>
  <SettingsDrawer />
</template>
