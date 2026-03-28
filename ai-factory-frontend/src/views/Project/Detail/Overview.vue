<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PenTool, Users, Globe, Settings, BookOpen } from 'lucide-vue-next'
import { getProjectOverview } from '@/api/project'
import type { ProjectOverview } from '@/types/project'

const route = useRoute()
const router = useRouter()

const projectId = computed(() => route.params.id as string)

// 数据状态
const loading = ref(true)
const error = ref<string | null>(null)
const overview = ref<ProjectOverview | null>(null)

// 状态映射
const statusMap: Record<string, { label: string; bgClass: string; textClass: string }> = {
  draft: { label: '草稿', bgClass: 'bg-gray-100 dark:bg-gray-700', textClass: 'text-gray-600 dark:text-gray-300' },
  in_progress: { label: '连载中', bgClass: 'bg-green-100 dark:bg-green-900/30', textClass: 'text-green-600 dark:text-green-400' },
  completed: { label: '已完成', bgClass: 'bg-blue-100 dark:bg-blue-900/30', textClass: 'text-blue-600 dark:text-blue-400' },
  archived: { label: '已归档', bgClass: 'bg-yellow-100 dark:bg-yellow-900/30', textClass: 'text-yellow-600 dark:text-yellow-400' }
}

// 快捷操作
const quickActions = [
  { name: '开始创作', icon: PenTool, path: 'creation', color: 'bg-blue-500' },
  { name: '管理角色', icon: Users, path: 'characters', color: 'bg-purple-500' },
  { name: '世界观设定', icon: Globe, path: 'world-setting', color: 'bg-green-500' },
  { name: '项目设置', icon: Settings, path: 'settings', color: 'bg-gray-500' }
]

// 加载数据
const loadData = async () => {
  loading.value = true
  error.value = null
  try {
    overview.value = await getProjectOverview(projectId.value)
  } catch (e: any) {
    error.value = e.message || '加载失败'
    console.error('Failed to load project overview:', e)
  } finally {
    loading.value = false
  }
}

// 获取状态信息
const getStatusInfo = (status: string) => {
  return statusMap[status] || statusMap.draft
}

// 格式化字数
const formatWordCount = (count: number) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return (count / 1000).toFixed(1) + 'K'
}

// 跳转操作
const handleAction = (path: string) => {
  router.push(`/project/${projectId.value}/${path}`)
}

// 监听路由变化
watch(projectId, () => {
  loadData()
})

// 初始化
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- 加载状态 - 骨架屏 -->
    <template v-if="loading">
      <!-- 标题骨架 -->
      <div class="space-y-2">
        <div class="h-8 w-32 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
        <div class="h-4 w-48 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
      </div>

      <!-- 项目信息骨架 -->
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex gap-6">
          <div class="w-28 h-40 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse flex-shrink-0"></div>
          <div class="flex-1 space-y-3">
            <div class="h-6 w-48 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
            <div class="h-4 w-full bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
            <div class="h-4 w-3/4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
            <div class="flex gap-2 mt-4">
              <div class="h-6 w-16 bg-gray-200 dark:bg-gray-700 rounded-full animate-pulse"></div>
              <div class="h-6 w-16 bg-gray-200 dark:bg-gray-700 rounded-full animate-pulse"></div>
            </div>
          </div>
          <!-- 统计数据骨架 -->
          <div class="flex gap-3 items-center">
            <div class="w-24 h-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
            <div class="w-24 h-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
            <div class="w-24 h-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
          </div>
        </div>
      </div>

      <!-- 快速操作骨架 -->
      <div>
        <div class="h-6 w-24 bg-gray-200 dark:bg-gray-700 rounded animate-pulse mb-4"></div>
        <div class="grid grid-cols-4 gap-4">
          <div v-for="i in 4" :key="i" class="h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse"></div>
        </div>
      </div>

      <!-- 进度骨架 -->
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex justify-between mb-4">
          <div class="h-6 w-24 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
          <div class="h-5 w-12 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
        </div>
        <div class="h-3 w-full bg-gray-200 dark:bg-gray-700 rounded-full animate-pulse"></div>
      </div>
    </template>

    <!-- 错误状态 -->
    <template v-else-if="error">
      <div class="flex flex-col items-center justify-center py-20">
        <p class="text-red-500 mb-4">{{ error }}</p>
        <button
          class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
          @click="loadData"
        >
          重试
        </button>
      </div>
    </template>

    <!-- 正常内容 -->
    <template v-else-if="overview">
      <!-- 页面标题 -->
      <div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">项目概览</h1>
        <p class="text-gray-500 dark:text-gray-400 mt-1">查看项目信息与快速导航</p>
      </div>

      <!-- 项目信息 + 统计数据（整合为一行）-->
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex gap-6 items-start">
          <!-- 封面 -->
          <div
            class="w-28 h-40 rounded-xl bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center flex-shrink-0 overflow-hidden"
          >
            <img
              v-if="overview.coverUrl"
              :src="overview.coverUrl"
              :alt="overview.name"
              class="w-full h-full object-cover"
            />
            <BookOpen v-else class="w-10 h-10 text-white" />
          </div>

          <!-- 信息 -->
          <div class="flex-1 min-w-0">
            <h2 class="text-xl font-semibold text-gray-900 dark:text-white truncate">
              {{ overview.name }}
            </h2>
            <p class="text-gray-500 dark:text-gray-400 mt-2 text-sm leading-relaxed line-clamp-2">
              {{ overview.description || '暂无描述' }}
            </p>
            <div class="flex items-center gap-2 mt-3 flex-wrap">
              <!-- 状态标签 -->
              <span
                :class="[
                  'px-3 py-1 rounded-full text-xs font-medium',
                  getStatusInfo(overview.status).bgClass,
                  getStatusInfo(overview.status).textClass
                ]"
              >
                {{ getStatusInfo(overview.status).label }}
              </span>
              <!-- 项目标签 -->
              <span
                v-for="tag in overview.tags"
                :key="tag"
                class="px-3 py-1 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-xs font-medium"
              >
                {{ tag }}
              </span>
            </div>
          </div>

          <!-- 统计数据（紧凑卡片）-->
          <div class="flex gap-3 flex-shrink-0">
            <div class="w-24 bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 text-center">
              <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ overview.chapterCount }}</p>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">章节</p>
            </div>
            <div class="w-24 bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 text-center">
              <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ overview.characterCount }}</p>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">角色</p>
            </div>
            <div class="w-24 bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 text-center">
              <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ formatWordCount(overview.totalWordCount) }}</p>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">字数</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 快速操作入口 -->
      <div>
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">快速操作</h3>
        <div class="grid grid-cols-4 gap-4">
          <button
            v-for="action in quickActions"
            :key="action.path"
            class="flex flex-col items-center justify-center bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:border-blue-500 dark:hover:border-blue-400 hover:shadow-md transition-all group min-h-[100px]"
            @click="handleAction(action.path)"
          >
            <div :class="['w-12 h-12 rounded-xl flex items-center justify-center transition-transform group-hover:scale-110', action.color]">
              <component :is="action.icon" class="w-6 h-6 text-white" />
            </div>
            <p class="text-sm font-medium text-gray-900 dark:text-white group-hover:text-blue-500 dark:group-hover:text-blue-400 mt-3">
              {{ action.name }}
            </p>
          </button>
        </div>
      </div>

      <!-- 创作进度 -->
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">创作进度</h3>
          <span class="text-sm font-medium text-gray-500 dark:text-gray-400">{{ overview.progress }}%</span>
        </div>
        <div class="h-3 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            class="h-full bg-gradient-to-r from-blue-500 to-purple-500 rounded-full transition-all duration-500"
            :style="{ width: `${overview.progress}%` }"
          ></div>
        </div>
        <div class="flex justify-between mt-3 text-sm text-gray-500 dark:text-gray-400">
          <span>已完成 {{ overview.chapterCount }} 章</span>
          <span>目标 {{ overview.targetChapterCount || '未设定' }} 章</span>
        </div>
      </div>
    </template>
  </div>
</template>
