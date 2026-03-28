<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { FolderOpen, LayoutGrid, List, ArrowUpDown } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import Btn from '@/components/ui/Btn.vue'
import ProjectCard from './ProjectCard.vue'
import { getProjectList } from '@/api/project'
import type { Project, ProjectListRequest } from '@/types'
import { error as toastError } from '@/utils/toast'

import Pagination from '@/components/ui/Pagination.vue'

const router = useRouter()

const loading = ref(false)
const projects = ref<Project[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 12 // 网格视图 4 列 x 3 行

const sortBy = ref<'createTime' | 'updateTime'>('updateTime')
const sortOrder = ref<'asc' | 'desc'>('desc')
const viewMode = ref<'grid' | 'list'>('grid')

const totalPages = computed(() => Math.ceil(total.value / pageSize))

const sortOptions = [
  { value: 'updateTime', label: '最近更新' },
  { value: 'createTime', label: '创建时间' }
]

const fetchProjects = async () => {
  loading.value = true
  try {
    const params: ProjectListRequest = {
      page: currentPage.value,
      pageSize: pageSize,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }
    const res = await getProjectList(params)
    projects.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch projects:', error)
    toastError('获取项目列表失败')
  } finally {
    loading.value = false
  }
}

const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const handleRefresh = () => {
  fetchProjects()
}

// 暴露刷新方法给子组件
const emit = defineEmits(['refresh'])

onMounted(() => {
  fetchProjects()
})

watch([sortBy, sortOrder], () => {
  fetchProjects()
})

// 监听页码变化重新获取数据
watch(currentPage, () => {
  fetchProjects()
})
</script>

<template>
  <Card padding="none">
    <!-- 工具栏 -->
    <div class="p-4 border-b border-gray-200/50 dark:border-gray-700/50 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <FolderOpen class="w-5 h-5 text-blue-500" />
        <h3 class="font-semibold text-gray-900 dark:text-white">我的项目</h3>
        <span class="text-sm text-gray-500">({{ total }})</span>
      </div>

      <div class="flex items-center gap-2">
        <!-- 排序 -->
        <select
          v-model="sortBy"
          class="px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-sm text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
        >
          <option v-for="option in sortOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>

        <!-- 排序方向 -->
        <button
          class="p-1.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          @click="toggleSortOrder"
        >
          <ArrowUpDown :class="['w-4 h-4 text-gray-500', sortOrder === 'asc' ? 'rotate-180' : '']" />
        </button>

        <!-- 视图切换 -->
        <div class="flex rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
          <button
            :class="[
              'p-1.5 transition-colors',
              viewMode === 'grid'
                ? 'bg-blue-500 text-white'
                : 'bg-gray-50 dark:bg-gray-800 text-gray-500 hover:text-gray-700'
            ]"
            title="网格视图"
            @click="viewMode = 'grid'"
          >
            <LayoutGrid class="w-4 h-4" />
          </button>
          <button
            :class="[
              'p-1.5 transition-colors',
              viewMode === 'list'
                ? 'bg-blue-500 text-white'
                : 'bg-gray-50 dark:bg-gray-800 text-gray-500 hover:text-gray-700'
            ]"
            title="列表视图"
            @click="viewMode = 'list'"
          >
            <List class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="p-8 text-center">
      <div class="animate-spin w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full mx-auto" />
      <p class="mt-2 text-sm text-gray-500">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="projects.length === 0" class="p-8 text-center">
      <FolderOpen class="w-12 h-12 text-gray-300 mx-auto mb-2" />
      <p class="text-gray-500 dark:text-gray-400">暂无项目</p>
      <Btn variant="primary" size="sm" class="mt-4" @click="router.push('/project/create')">
        创建第一个项目
      </Btn>
    </div>

    <!-- 项目列表 -->
    <div v-else>
      <!-- 网格视图 -->
      <div v-if="viewMode === 'grid'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 p-4">
        <ProjectCard
          v-for="project in projects"
          :key="project.id"
          :project="project"
          :view-mode="viewMode"
          @refresh="handleRefresh"
        />
      </div>

      <!-- 列表视图 -->
      <div v-else class="divide-y divide-gray-200/50 dark:divide-gray-700/50">
        <ProjectCard
          v-for="project in projects"
          :key="project.id"
          :project="project"
          :view-mode="viewMode"
          @refresh="handleRefresh"
        />
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="p-4 border-t border-gray-200/50 dark:border-gray-700/50">
      <div class="flex justify-center">
        <Pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @update:currentPage="handlePageChange"
        />
      </div>
    </div>
  </Card>
</template>

<style scoped>
.pagination-dark {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #9ca3af;
  --el-pagination-button-disabled-bg-color: #4b5563;
}
</style>
