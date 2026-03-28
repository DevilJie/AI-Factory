<script setup lang="ts">
import { computed } from 'vue'
import {
  Save,
  Sparkles,
  Wand2,
  ListTree,
  Download,
  Loader2,
  Users
} from 'lucide-vue-next'
import { useEditorStore } from '@/stores/editor'

const emit = defineEmits<{
  save: []
  aiContinue: []
  aiPolish: []
  generateOutline: []
  export: []
}>()

const editorStore = useEditorStore()

// Computed
const currentChapter = computed(() => editorStore.currentChapter)
const wordCount = computed(() => editorStore.wordCount)
const isDirty = computed(() => editorStore.isDirty)
const isSaving = computed(() => editorStore.isSaving)
const lastSavedAt = computed(() => editorStore.lastSavedAt)

const formattedUpdateTime = computed(() => {
  if (!currentChapter.value?.updateTime) return '-'
  const date = new Date(currentChapter.value.updateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

const formattedLastSaved = computed(() => {
  if (!lastSavedAt.value) return null
  return lastSavedAt.value.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
})

// Methods
const handleSave = () => {
  emit('save')
}

const quickActions = [
  { icon: Sparkles, label: 'AI续写', event: 'aiContinue' },
  { icon: Wand2, label: 'AI润色', event: 'aiPolish' },
  { icon: ListTree, label: '生成大纲', event: 'generateOutline' },
  { icon: Download, label: '导出章节', event: 'export' }
] as const

const handleAction = (event: 'aiContinue' | 'aiPolish' | 'generateOutline' | 'export') => {
  emit(event as any)
}
</script>

<template>
  <div class="h-full flex flex-col bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700">
    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700">
      <h3 class="text-sm font-medium text-gray-900 dark:text-white">章节信息</h3>
      <button
        class="flex items-center gap-1 px-3 py-1.5 text-sm font-medium rounded-lg transition-colors"
        :class="[
          isDirty
            ? 'bg-blue-500 hover:bg-blue-600 text-white'
            : 'bg-gray-100 text-gray-400 cursor-not-allowed dark:bg-gray-700 dark:text-gray-500'
        ]"
        :disabled="!isDirty || isSaving"
        @click="handleSave"
      >
        <Loader2 v-if="isSaving" class="w-4 h-4 animate-spin" />
        <Save v-else class="w-4 h-4" />
        <span>{{ isSaving ? '保存中...' : '保存' }}</span>
      </button>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-4">
      <!-- No chapter selected -->
      <div v-if="!currentChapter" class="text-center py-8 text-sm text-gray-500 dark:text-gray-400">
        请从左侧选择要编辑的章节
      </div>

      <!-- Chapter details -->
      <div v-else class="space-y-6">
        <!-- Title -->
        <div>
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">章节标题</label>
          <p class="text-sm text-gray-900 dark:text-white">{{ currentChapter.title }}</p>
        </div>

        <!-- Stats -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">字数统计</label>
            <p class="text-sm text-gray-900 dark:text-white">{{ wordCount.toLocaleString() }} 字</p>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">状态</label>
            <p class="text-sm">
              <span
                :class="currentChapter.status === 'published' ? 'text-green-500' : 'text-orange-500'"
              >
                {{ currentChapter.status === 'published' ? '已发布' : '草稿' }}
              </span>
            </p>
          </div>
        </div>

        <!-- Update time -->
        <div>
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">更新时间</label>
          <p class="text-sm text-gray-900 dark:text-white">{{ formattedUpdateTime }}</p>
        </div>

        <!-- Last saved -->
        <div v-if="formattedLastSaved">
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">上次保存</label>
          <p class="text-sm text-green-600 dark:text-green-400">{{ formattedLastSaved }}</p>
        </div>

        <!-- Summary -->
        <div v-if="currentChapter.summary">
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">章节摘要</label>
          <p class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{{ currentChapter.summary }}</p>
        </div>

        <!-- Characters (placeholder) -->
        <div>
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">涉及角色</label>
          <div class="flex items-center gap-2 text-sm text-gray-400">
            <Users class="w-4 h-4" />
            <span>暂无关联角色</span>
          </div>
        </div>

        <!-- Quick Actions -->
        <div>
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">快捷操作</label>
          <div class="grid grid-cols-2 gap-2">
            <button
              v-for="action in quickActions"
              :key="action.event"
              class="flex items-center gap-2 px-3 py-2 text-sm rounded-lg border border-gray-200 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              @click="handleAction(action.event)"
            >
              <component :is="action.icon" class="w-4 h-4 text-gray-500 dark:text-gray-400" />
              <span class="text-gray-700 dark:text-gray-300">{{ action.label }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
