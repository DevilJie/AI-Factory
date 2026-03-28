<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, LayoutDashboard, Globe, Settings, PenTool, Users } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const projectId = computed(() => route.params.id as string)

const navItems = [
  { name: '概览', icon: LayoutDashboard, path: 'overview' },
  { name: '世界观设定', icon: Globe, path: 'world-setting' },
  { name: '基础设置', icon: Settings, path: 'settings' },
  { name: '创作中心', icon: PenTool, path: 'creation' },
  { name: '人物管理', icon: Users, path: 'characters' }
]

const isActive = (path: string) => {
  const currentPath = route.path.split('/').pop()
  return currentPath === path
}

const handleNavigate = (path: string) => {
  router.push(`/project/${projectId.value}/${path}`)
}

const goBack = () => {
  router.push('/dashboard')
}

// TODO: 从API获取项目信息
const projectName = computed(() => '我的项目')
const projectStatus = computed(() => '创作中')
</script>

<template>
  <aside class="w-64 bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 flex flex-col">
    <!-- 顶部：返回按钮 + 项目名称 -->
    <div class="p-4 border-b border-gray-200 dark:border-gray-800">
      <button
        class="flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors mb-3"
        @click="goBack"
      >
        <ArrowLeft class="w-4 h-4" />
        <span class="text-sm">返回项目列表</span>
      </button>
      <h1 class="text-lg font-semibold text-gray-900 dark:text-white truncate">
        {{ projectName }}
      </h1>
    </div>

    <!-- 中间：导航项 -->
    <nav class="flex-1 p-3 space-y-1">
      <button
        v-for="item in navItems"
        :key="item.path"
        :class="[
          'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all',
          isActive(item.path)
            ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
            : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-white'
        ]"
        @click="handleNavigate(item.path)"
      >
        <component :is="item.icon" class="w-5 h-5" />
        <span>{{ item.name }}</span>
      </button>
    </nav>

    <!-- 底部：项目状态指示 -->
    <div class="p-4 border-t border-gray-200 dark:border-gray-800">
      <div class="flex items-center gap-2">
        <div class="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
        <span class="text-sm text-gray-600 dark:text-gray-400">{{ projectStatus }}</span>
      </div>
    </div>
  </aside>
</template>
