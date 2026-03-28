<script setup lang="ts">
import { Book, Notebook, Library } from 'lucide-vue-next'
import type { TargetLength } from '@/types/project'

interface LengthOption {
  value: TargetLength
  label: string
  desc: string
  icon: typeof Book
}

const lengths: LengthOption[] = [
  { value: 'short', label: '短篇', desc: '< 20万字', icon: Book },
  { value: 'medium', label: '中篇', desc: '20-50万字', icon: Notebook },
  { value: 'long', label: '长篇', desc: '> 50万字', icon: Library }
]

interface Props {
  modelValue?: TargetLength
}

withDefaults(defineProps<Props>(), {
  modelValue: 'medium'
})

const emit = defineEmits<{
  'update:modelValue': [value: TargetLength]
}>()

const selectLength = (length: TargetLength) => {
  emit('update:modelValue', length)
}
</script>

<template>
  <div class="flex flex-col sm:flex-row gap-3">
    <button
      v-for="length in lengths"
      :key="length.value"
      type="button"
      :class="[
        'flex-1 flex items-center gap-3 p-4 rounded-xl border-2 transition-all duration-200',
        modelValue === length.value
          ? 'border-blue-500 dark:border-blue-400 bg-blue-50/50 dark:bg-blue-900/20'
          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 bg-white/50 dark:bg-gray-800/50'
      ]"
      @click="selectLength(length.value)"
    >
      <div
        :class="[
          'w-10 h-10 rounded-xl flex items-center justify-center transition-colors',
          modelValue === length.value
            ? 'bg-blue-500 dark:bg-blue-400 text-white'
            : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
        ]"
      >
        <component :is="length.icon" class="w-5 h-5" />
      </div>
      <div class="text-left">
        <div
          :class="[
            'text-sm font-medium transition-colors',
            modelValue === length.value
              ? 'text-blue-600 dark:text-blue-400'
              : 'text-gray-900 dark:text-white'
          ]"
        >
          {{ length.label }}
        </div>
        <div class="text-xs text-gray-500 dark:text-gray-400">
          {{ length.desc }}
        </div>
      </div>
    </button>
  </div>
</template>
