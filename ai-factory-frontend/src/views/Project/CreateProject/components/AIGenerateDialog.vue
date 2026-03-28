<script setup lang="ts">
import { ref, watch } from 'vue'
import { Sparkles, X } from 'lucide-vue-next'
import Btn from '@/components/ui/Btn.vue'
import { error } from '@/utils/toast'

interface Props {
  visible: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'generate': [idea: string]
}>()

const idea = ref('')

const examples = [
  '现代程序员穿越修仙世界，用编程能力修炼法术',
  '宅男意外获得系统，在平行世界开启第二人生',
  '末世降临，觉醒异能的主角带领人类重建文明',
  '重生回到学生时代，利用前世记忆改变命运'
]

const handleGenerate = () => {
  if (!idea.value.trim()) {
    error('请先描述你的创作想法')
    return
  }
  emit('generate', idea.value)
}

const close = () => {
  emit('update:visible', false)
}

watch(() => props.visible, (newVal) => {
  if (!newVal) {
    idea.value = ''
  }
})

const selectExample = (example: string) => {
  idea.value = example
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/50 backdrop-blur-sm"
          @click="close"
        />

        <!-- Dialog -->
        <div
          class="relative w-full max-w-lg bg-white dark:bg-gray-800 rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700"
        >
          <!-- Header -->
          <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 flex items-center justify-center">
                <Sparkles class="w-5 h-5 text-white" />
              </div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white">AI 智能生成</h3>
            </div>
            <button
              type="button"
              class="w-8 h-8 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center justify-center transition-colors"
              @click="close"
            >
              <X class="w-5 h-5 text-gray-500" />
            </button>
          </div>

          <!-- Body -->
          <div class="p-6 space-y-4">
            <p class="text-sm text-gray-600 dark:text-gray-400">
              描述你的创作想法，AI 将为你生成项目名称和简介
            </p>
            <textarea
              v-model="idea"
              rows="4"
              placeholder="例如：一个现代程序员穿越到修仙世界，发现可以用编程的方式修炼功法..."
              class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-white/50 dark:bg-gray-800/50 text-gray-900 dark:text-gray-100 placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
            />

            <!-- Examples -->
            <div class="space-y-2">
              <p class="text-xs font-medium text-gray-500 dark:text-gray-400">灵感示例</p>
              <div class="space-y-1.5">
                <button
                  v-for="(example, index) in examples"
                  :key="index"
                  type="button"
                  :class="[
                    'w-full px-3 py-2 text-left text-sm rounded-lg transition-colors',
                    idea === example
                      ? 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400'
                      : 'bg-gray-50 dark:bg-gray-700/50 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                  ]"
                  @click="selectExample(example)"
                >
                  {{ example }}
                </button>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="flex justify-end gap-3 p-6 border-t border-gray-200 dark:border-gray-700">
            <Btn variant="secondary" @click="close">
              取消
            </Btn>
            <Btn :loading="loading" @click="handleGenerate">
              <Sparkles class="w-4 h-4" />
              开始生成
            </Btn>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
