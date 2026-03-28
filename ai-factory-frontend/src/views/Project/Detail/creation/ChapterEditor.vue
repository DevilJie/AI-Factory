<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  Bold,
  Italic,
  Heading1,
  Heading2,
  List,
  ListOrdered,
  Quote,
  AlignLeft,
  AlignCenter,
  AlignRight,
  Save,
  Loader2,
  Sparkles
} from 'lucide-vue-next'
import { useEditorStore } from '@/stores/editor'

const editorStore = useEditorStore()

// Local state
const textareaRef = ref<HTMLTextAreaElement | null>(null)
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

// Computed
const content = computed({
  get: () => editorStore.content,
  set: (val) => editorStore.updateContent(val)
})
const chapterTitle = computed(() => editorStore.chapterTitle)
const wordCount = computed(() => editorStore.wordCount)
const isDirty = computed(() => editorStore.isDirty)
const isSaving = computed(() => editorStore.isSaving)
const lastSavedAt = computed(() => editorStore.lastSavedAt)
const isGenerating = computed(() => editorStore.isGenerating)
const isChapterGenerated = computed(() => editorStore.isChapterGenerated)
const currentChapterPlan = computed(() => editorStore.currentChapterPlan)

const saveStatus = computed(() => {
  if (isSaving.value) return { text: '保存中...', class: 'text-blue-500' }
  if (isDirty.value) return { text: '未保存', class: 'text-orange-500' }
  if (lastSavedAt.value) {
    const time = lastSavedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    return { text: `已保存 ${time}`, class: 'text-green-500' }
  }
  return { text: '', class: '' }
})

// Toolbar actions
const toolbarActions = [
  { icon: Bold, label: '加粗', action: () => insertFormat('**', '**') },
  { icon: Italic, label: '斜体', action: () => insertFormat('*', '*') },
  { icon: Heading1, label: '标题1', action: () => insertLineStart('# ') },
  { icon: Heading2, label: '标题2', action: () => insertLineStart('## ') },
  { icon: List, label: '无序列表', action: () => insertLineStart('- ') },
  { icon: ListOrdered, label: '有序列表', action: () => insertLineStart('1. ') },
  { icon: Quote, label: '引用', action: () => insertLineStart('> ') },
  { icon: AlignLeft, label: '左对齐', action: () => {} },
  { icon: AlignCenter, label: '居中', action: () => {} },
  { icon: AlignRight, label: '右对齐', action: () => {} }
] as const

// Methods
const insertFormat = (prefix: string, suffix: string) => {
  const textarea = textareaRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = content.value.substring(start, end)

  const newText =
    content.value.substring(0, start) +
    prefix + selectedText + suffix +
    content.value.substring(end)

  content.value = newText

  // Restore cursor position
  setTimeout(() => {
    textarea.focus()
    textarea.setSelectionRange(start + prefix.length, end + prefix.length)
  }, 0)
}

const insertLineStart = (prefix: string) => {
  const textarea = textareaRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const lineStart = content.value.lastIndexOf('\n', start - 1) + 1

  const newText =
    content.value.substring(0, lineStart) +
    prefix +
    content.value.substring(lineStart)

  content.value = newText

  setTimeout(() => {
    textarea.focus()
    textarea.setSelectionRange(start + prefix.length, start + prefix.length)
  }, 0)
}

const handleSave = async () => {
  await editorStore.saveContent()
}

// Auto-save every 2 seconds when dirty
const startAutoSave = () => {
  if (autoSaveTimer) return
  autoSaveTimer = setInterval(async () => {
    if (editorStore.isDirty && !editorStore.isSaving) {
      await editorStore.saveContent()
    }
  }, 2000)
}

const stopAutoSave = () => {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
}

// Lifecycle
onMounted(() => {
  startAutoSave()
})

onUnmounted(() => {
  stopAutoSave()
})

// Watch for content changes to reset auto-save timer
watch(content, (newContent) => {
  console.log('Content changed, length:', newContent?.length || 0)
  // Auto-save will handle saving when dirty
})

// Debug: watch state changes
watch([isChapterGenerated, isGenerating], ([generated, generating]) => {
  console.log('State changed - isChapterGenerated:', generated, 'isGenerating:', generating)
})

watch(chapterTitle, (title) => {
  console.log('Chapter title changed:', title)
})
</script>

<template>
  <div class="h-full flex flex-col bg-white dark:bg-gray-800 relative">
    <!-- Toolbar -->
    <div class="flex items-center gap-1 px-4 py-2 border-b border-gray-200 dark:border-gray-700 overflow-x-auto">
      <button
        v-for="(action, index) in toolbarActions"
        :key="index"
        class="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        :title="action.label"
        @click="action.action"
      >
        <component :is="action.icon" class="w-4 h-4" />
      </button>

      <div class="flex-1" />

      <!-- Save button -->
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
        <span class="hidden sm:inline">{{ isSaving ? '保存中...' : '保存' }}</span>
      </button>
    </div>

    <!-- Editor Content -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Chapter Title -->
      <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
        <h2 class="text-xl font-semibold text-gray-900 dark:text-white">
          {{ chapterTitle || currentChapterPlan?.title || '未选择章节' }}
        </h2>
      </div>

      <!-- Textarea -->
      <div class="flex-1 overflow-hidden relative">
        <!-- 未生成章节的占位提示 -->
        <div
          v-if="!isChapterGenerated && !isGenerating"
          class="absolute inset-0 flex flex-col items-center justify-center bg-gray-50 dark:bg-gray-800 z-10"
        >
          <Sparkles class="w-16 h-16 text-gray-300 dark:text-gray-600 mb-4" />
          <p class="text-gray-500 dark:text-gray-400 text-lg">
            点击上方"AI创作"按钮开始生成
          </p>
        </div>

        <textarea
          ref="textareaRef"
          v-model="content"
          class="w-full h-full p-6 resize-none focus:outline-none text-gray-800 dark:text-gray-200 bg-transparent leading-relaxed"
          placeholder="开始创作..."
          :disabled="!chapterTitle"
        />
      </div>
    </div>

    <!-- Status Bar -->
    <div class="flex items-center justify-between px-4 py-2 border-t border-gray-200 dark:border-gray-700 text-sm">
      <div class="flex items-center gap-4">
        <span class="text-gray-500 dark:text-gray-400">
          {{ wordCount.toLocaleString() }} 字
        </span>
        <span v-if="saveStatus.text" :class="saveStatus.class">
          {{ saveStatus.text }}
        </span>
      </div>
      <span class="text-gray-400 text-xs">
        自动保存已开启
      </span>
    </div>
  </div>
</template>

<style scoped>
</style>
