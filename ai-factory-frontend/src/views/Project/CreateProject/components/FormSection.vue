<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  title: string
  description?: string
  icon?: Component
  required?: boolean
}

withDefaults(defineProps<Props>(), {
  description: '',
  required: false
})
</script>

<template>
  <div class="bg-white/80 dark:bg-gray-800/80 backdrop-blur-md rounded-2xl border border-gray-200/50 dark:border-gray-700/50 p-6 transition-all duration-200 hover:shadow-lg hover:shadow-gray-200/50 dark:hover:shadow-gray-900/50">
    <div class="flex items-start gap-4 mb-4">
      <div
        v-if="icon"
        class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500/10 to-purple-500/10 dark:from-blue-500/20 dark:to-purple-500/20 flex items-center justify-center flex-shrink-0"
      >
        <component :is="icon" class="w-5 h-5 text-blue-500 dark:text-blue-400" />
      </div>
      <div class="flex-1">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white">
          {{ title }}
          <span v-if="required" class="text-red-500 ml-1">*</span>
        </h3>
        <p v-if="description" class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          {{ description }}
        </p>
      </div>
    </div>
    <div class="pl-0 overflow-visible" :class="{ 'md:pl-14': icon }">
      <slot />
    </div>
  </div>
</template>
