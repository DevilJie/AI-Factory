<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Trash2, FileText, Calendar } from 'lucide-vue-next'
import type { Project } from '@/types'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'
import { deleteProject } from '@/api/project'
import { success, error as toastError } from '@/utils/toast'

const props = defineProps<{
  project: Project
  viewMode: 'grid' | 'list'
}>()

const emit = defineEmits<{
  refresh: []
}>()

const router = useRouter()
const deleting = ref(false)
const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>()

// 获取首字
const firstChar = computed(() => {
  return props.project.name?.charAt(0) || ''
})

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

// 显示删除确认对话框
const showDeleteConfirm = () => {
  confirmDialog.value?.show()
}

// 删除项目
const handleDelete = async () => {
  try {
    await deleteProject(props.project.id)
    success('删除成功')
    emit('refresh')
  } catch (error) {
    toastError('删除失败')
    console.error(error)
  }
}

// 查看详情
const handleDetail = () => {
  router.push(`/project/${props.project.id}/overview`)
}
</script>

<template>
  <!-- 网格视图 -->
  <div v-if="viewMode === 'grid'" class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 overflow-hidden hover:shadow-xl hover:border-blue-300 dark:hover:border-blue-600 transition-all duration-300 group flex flex-col">
    <!-- 封面 -->
    <div class="h-28 bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500 flex items-center justify-center relative flex-shrink-0">
      <img
        v-if="project.coverUrl"
        :src="project.coverUrl"
        :alt="project.name"
        class="w-full h-full object-cover"
      />
      <span v-else class="text-3xl font-bold text-white drop-shadow-lg">
        {{ firstChar }}
      </span>
    </div>

    <!-- 内容 -->
    <div class="p-4 flex-1 flex flex-col">
      <h3 class="font-semibold text-gray-900 dark:text-white truncate text-base mb-1">{{ project.name }}</h3>
      <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-2">
        {{ project.description || '暂无描述' }}
      </p>
      <p class="text-xs text-gray-400 dark:text-gray-500 flex items-center gap-1.5 mt-auto">
        <Calendar class="w-3 h-3" />
        {{ formatDate(project.createTime) }}
      </p>
    </div>

    <!-- 操作按钮 -->
    <div class="px-4 pb-4 flex gap-2">
      <button
        class="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 dark:text-red-400 dark:bg-red-900/20 dark:hover:bg-red-900/30 rounded-lg transition-colors"
        :disabled="deleting"
        @click="showDeleteConfirm"
      >
        <Trash2 class="w-4 h-4" />
        <span v-if="!deleting">删除</span>
        <span v-else>删除中...</span>
      </button>
      <button
        class="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 text-sm font-medium text-white bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 rounded-lg shadow-sm hover:shadow transition-all"
        @click="handleDetail"
      >
        <FileText class="w-4 h-4" />
        详情
      </button>
    </div>
  </div>

  <!-- 列表视图 -->
  <div
    v-else
    class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:shadow-lg hover:border-blue-300 dark:hover:border-blue-600 transition-all duration-300 flex items-center gap-4"
  >
    <!-- 首字图标 -->
    <div class="w-14 h-14 rounded-xl bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500 flex items-center justify-center flex-shrink-0 overflow-hidden shadow-sm">
      <img
        v-if="project.coverUrl"
        :src="project.coverUrl"
        :alt="project.name"
        class="w-full h-full object-cover"
      />
      <span v-else class="text-2xl font-bold text-white drop-shadow">
        {{ firstChar }}
      </span>
    </div>

    <!-- 内容 -->
    <div class="flex-1 min-w-0">
      <h3 class="font-semibold text-gray-900 dark:text-white truncate">{{ project.name }}</h3>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
        <span class="flex items-center gap-1 inline-flex">
          <Calendar class="w-3 h-3" />
          {{ formatDate(project.createTime) }}
        </span>
        <span class="mx-2">·</span>
        <span class="line-clamp-1">{{ project.description || '暂无描述' }}</span>
      </p>
    </div>

    <!-- 操作按钮 -->
    <div class="flex gap-2 flex-shrink-0">
      <button
        class="flex items-center justify-center gap-1.5 px-3 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 dark:text-red-400 dark:bg-red-900/20 dark:hover:bg-red-900/30 rounded-lg transition-colors"
        :disabled="deleting"
        @click="showDeleteConfirm"
      >
        <Trash2 class="w-4 h-4" />
        <span v-if="!deleting">删除</span>
        <span v-else>删除中...</span>
      </button>
      <button
        class="flex items-center justify-center gap-1.5 px-3 py-2 text-sm font-medium text-white bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 rounded-lg shadow-sm hover:shadow transition-all"
        @click="handleDetail"
      >
        <FileText class="w-4 h-4" />
        详情
      </button>
    </div>
  </div>

  <!-- 确认对话框 -->
  <ConfirmDialog
    ref="confirmDialog"
    title="删除确认"
    :message="`确定要删除项目「${project.name}」吗？此操作不可恢复。`"
    variant="danger"
    @confirm="handleDelete"
  />
</template>
