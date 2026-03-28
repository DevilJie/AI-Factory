<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'

interface Props {
  title: string
  value: number | string
  icon: Component
  iconClass?: string
  unit?: string
}

const props = withDefaults(defineProps<Props>(), {
  iconClass: 'from-blue-500 to-purple-500',
  unit: ''
})

// Format number with Chinese units
const formattedValue = computed(() => {
  const num = typeof props.value === 'string' ? parseFloat(props.value) : props.value

  // If not a valid number, return original value
  if (isNaN(num)) {
    return props.value
  }

  // Less than 1000: display directly
  if (num < 1000) {
    return num.toString()
  }

  // 1000-9999: display as "1.2k" format
  if (num < 10000) {
    const k = num / 1000
    return k.toFixed(1).replace(/\.0$/, '') + 'k'
  }

  // 10000-99999: display as "1.2万" format
  if (num < 100000) {
    const wan = num / 10000
    return wan.toFixed(1).replace(/\.0$/, '') + '万'
  }

  // 100000+: display as "12.5万" format
  const wan = num / 10000
  return wan.toFixed(1).replace(/\.0$/, '') + '万'
})
</script>

<template>
  <div class="bg-white/90 dark:bg-gray-800/50 backdrop-blur-md rounded-2xl border border-gray-200/50 dark:border-gray-700/50 p-5 hover:shadow-lg hover:shadow-blue-500/10 transition-all duration-300">
    <div class="flex items-center gap-4">
      <div :class="['w-12 h-12 rounded-xl bg-gradient-to-br flex items-center justify-center', iconClass]">
        <component :is="icon" class="w-6 h-6 text-white" />
      </div>
      <div>
        <p class="text-sm text-gray-500 dark:text-gray-400">{{ title }}</p>
        <p class="text-2xl font-bold text-gray-900 dark:text-white">
          {{ formattedValue }}<span v-if="unit" class="text-base font-normal ml-1">{{ unit }}</span>
        </p>
      </div>
    </div>
  </div>
</template>
