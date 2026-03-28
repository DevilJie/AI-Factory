<script setup lang="ts">
import { Sun, Shield, Eye, Compass } from 'lucide-vue-next'
import type { StoryTone } from '@/types/project'

interface ToneOption {
  value: StoryTone
  label: string
  icon: typeof Sun
}

const tones: ToneOption[] = [
  { value: 'relaxed', label: '轻松', icon: Sun },
  { value: 'serious', label: '严肃', icon: Shield },
  { value: 'suspense', label: '悬疑', icon: Eye },
  { value: 'adventure', label: '冒险', icon: Compass }
]

interface Props {
  modelValue?: StoryTone
}

withDefaults(defineProps<Props>(), {
  modelValue: 'relaxed'
})

const emit = defineEmits<{
  'update:modelValue': [value: StoryTone]
}>()

const selectTone = (tone: StoryTone) => {
  emit('update:modelValue', tone)
}
</script>

<template>
  <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
    <button
      v-for="tone in tones"
      :key="tone.value"
      type="button"
      :class="[
        'relative flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all duration-200',
        modelValue === tone.value
          ? 'border-blue-500 dark:border-blue-400 bg-blue-50/50 dark:bg-blue-900/20'
          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 bg-white/50 dark:bg-gray-800/50'
      ]"
      @click="selectTone(tone.value)"
    >
      <div
        :class="[
          'w-10 h-10 rounded-xl flex items-center justify-center transition-colors',
          modelValue === tone.value
            ? 'bg-blue-500 dark:bg-blue-400 text-white'
            : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
        ]"
      >
        <component :is="tone.icon" class="w-5 h-5" />
      </div>
      <span
        :class="[
          'text-sm font-medium transition-colors',
          modelValue === tone.value
            ? 'text-blue-600 dark:text-blue-400'
            : 'text-gray-700 dark:text-gray-300'
        ]"
      >
        {{ tone.label }}
      </span>
      <div
        v-if="modelValue === tone.value"
        class="absolute top-2 right-2 w-5 h-5 rounded-full bg-blue-500 dark:bg-blue-400 flex items-center justify-center"
      >
        <svg class="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
          <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      </div>
    </button>
  </div>
</template>
