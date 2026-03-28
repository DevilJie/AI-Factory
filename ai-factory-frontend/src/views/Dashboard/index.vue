<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { FolderOpen, BookOpen, Users, PenTool } from 'lucide-vue-next'
import AppHeader from '@/components/layout/AppHeader.vue'
import StatsCard from './components/StatsCard.vue'
import ProjectList from './components/ProjectList.vue'
import Btn from '@/components/ui/Btn.vue'
import { useUserStore } from '@/stores/user'
import { getDashboardStats } from '@/api/project'
import type { DashboardStats } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const stats = ref<DashboardStats>({
  projectCount: 0,
  chapterCount: 0,
  characterCount: 0,
  totalWordCount: 0
})
const loading = ref(true)

const fetchStats = async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    stats.value = res
  } catch (error) {
    console.error('Failed to fetch stats:', error)
    stats.value = {
      projectCount: 0,
      chapterCount: 0,
      characterCount: 0,
      totalWordCount: 0
    }
  } finally {
    loading.value = false
  }
}

const handleCreateProject = () => {
  router.push('/project/create')
}

onMounted(() => {
  fetchStats()
  if (!userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <AppHeader />

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Welcome Section -->
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          欢迎回来，{{ userStore.displayName }}
        </h1>
      </div>

      <!-- Data Overview Section -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">数据概览</h2>
          <Btn @click="handleCreateProject">
            <PenTool class="w-4 h-4 mr-2" />
            创建项目
          </Btn>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <StatsCard
            title="项目总数"
            :value="stats.projectCount"
            :icon="FolderOpen"
            icon-class="from-blue-500 to-cyan-500"
          />
          <StatsCard
            title="章节数量"
            :value="stats.chapterCount"
            :icon="BookOpen"
            icon-class="from-purple-500 to-pink-500"
          />
          <StatsCard
            title="总字数"
            :value="stats.totalWordCount"
            :icon="PenTool"
            icon-class="from-green-500 to-emerald-500"
          />
        </div>
      </div>

      <!-- Project List -->
      <ProjectList />
    </main>
  </div>
</template>
