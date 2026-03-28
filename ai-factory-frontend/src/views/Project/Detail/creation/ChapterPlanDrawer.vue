<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { X } from 'lucide-vue-next'
import { useEditorStore } from '@/stores/editor'
import { updateChapter } from '@/api/chapter'
import { success, error } from '@/utils/toast'
import type { Chapter } from '@/types/project'

const editorStore = useEditorStore()

// Local state
const form = ref({
  chapterNumber: 1,
  title: '',
  summary: '',
  keyEvents: '',
  characters: [] as string[],
  foreshadowing: ''
})

// Computed
const visible = computed({
  get: () => editorStore.isDrawerVisible,
  set: (val) => editorStore.setDrawerVisible(val)
})

const currentChapter = computed(() => editorStore.currentChapter)
const currentChapterPlan = computed(() => editorStore.currentChapterPlan)
const projectId = computed(() => editorStore.projectId)

// 计算当前编辑的章节ID（优先使用已生成的章节，否则使用规划ID）
const editTargetId = computed(() => currentChapter.value?.id || currentChapterPlan.value?.id)

// Computed for characters input (array to string conversion)
const charactersInput = computed({
  get: () => form.value.characters.join(','),
  set: (val: string) => {
    form.value.characters = val.split(',').map(s => s.trim()).filter(Boolean)
  }
})

// 更新表单的通用方法
const updateFormFromData = (data: {
  chapterNumber?: number
  title?: string
  summary?: string
  keyEvents?: string
  characters?: string[]
  foreshadowing?: string
}) => {
  form.value = {
    chapterNumber: data.chapterNumber || 1,
    title: data.title || '',
    summary: data.summary || '',
    keyEvents: data.keyEvents || '',
    characters: data.characters || [],
    foreshadowing: data.foreshadowing || ''
  }
}

// Watch currentChapter to update form
watch(currentChapter, (chapter) => {
  if (chapter) {
    updateFormFromData({
      chapterNumber: chapter.chapterNumber,
      title: chapter.chapterTitle || chapter.title,
      summary: chapter.summary,
      keyEvents: chapter.keyEvents,
      characters: chapter.newCharacters ? chapter.newCharacters.split(',').filter(Boolean) : [],
      foreshadowing: chapter.foreshadowingSetup
    })
  }
}, { immediate: true })

// Watch currentChapterPlan to update form (for ungenerated chapters)
watch(currentChapterPlan, (plan) => {
  if (plan && !currentChapter.value) {
    updateFormFromData({
      chapterNumber: plan.chapterNumber,
      title: plan.title,
      summary: plan.summary,
      keyEvents: plan.keyEvents,
      characters: plan.characters,
      foreshadowing: plan.foreshadowing
    })
  }
}, { immediate: true })

// Methods
const handleClose = () => {
  visible.value = false
}

const handleSave = async () => {
  if (!editTargetId.value || !projectId.value) return

  try {
    const updateData: Partial<Chapter> = {
      chapterNumber: form.value.chapterNumber,
      title: form.value.title,
      summary: form.value.summary,
      keyEvents: form.value.keyEvents,
      newCharacters: form.value.characters.join(','),
      foreshadowingSetup: form.value.foreshadowing
    }

    await updateChapter(projectId.value, editTargetId.value, updateData)
    success('章节规划已保存')
    handleClose()
  } catch (e: any) {
    error(e.message || '保存失败')
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="visible" class="fixed inset-0 z-50">
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/30"
          @click="handleClose"
        />

        <!-- Drawer -->
        <div class="absolute top-0 right-0 h-full w-[400px] bg-white dark:bg-gray-800 shadow-xl">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">章节规划详情</h3>
            <button
              class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded"
              @click="handleClose"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Content -->
          <div class="p-6 overflow-y-auto" style="height: calc(100% - 140px)">
            <div class="space-y-5">
              <!-- Chapter Number -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  章节号
                </label>
                <input
                  v-model.number="form.chapterNumber"
                  type="number"
                  min="1"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>

              <!-- Title -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  标题
                </label>
                <input
                  v-model="form.title"
                  type="text"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="输入章节标题"
                />
              </div>

              <!-- Summary -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  摘要
                </label>
                <textarea
                  v-model="form.summary"
                  rows="3"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                  placeholder="输入章节摘要"
                />
              </div>

              <!-- Key Events -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  关键事件
                </label>
                <textarea
                  v-model="form.keyEvents"
                  rows="4"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                  placeholder="输入关键事件"
                />
              </div>

              <!-- Characters -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  涉及角色
                </label>
                <input
                  v-model="charactersInput"
                  type="text"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="逗号分隔多个角色"
                />
              </div>

              <!-- Foreshadowing -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  伏笔设置
                </label>
                <textarea
                  v-model="form.foreshadowing"
                  rows="3"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                  placeholder="输入伏笔设置"
                />
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="absolute bottom-0 left-0 right-0 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
            <div class="flex justify-end gap-3">
              <button
                class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                @click="handleClose"
              >
                取消
              </button>
              <button
                class="px-4 py-2 text-sm font-medium text-white bg-blue-500 rounded-lg hover:bg-blue-600 transition-colors"
                @click="handleSave"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.2s ease;
}

.drawer-enter-active .absolute.top-0.right-0,
.drawer-leave-active .absolute.top-0.right-0 {
  transition: transform 0.2s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .absolute.top-0.right-0,
.drawer-leave-to .absolute.top-0.right-0 {
  transform: translateX(100%);
}
</style>
