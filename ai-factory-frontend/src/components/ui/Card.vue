<script setup lang="ts">
interface Props {
  hoverable?: boolean
  padding?: 'none' | 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  hoverable: false,
  padding: 'md'
})

const paddingClasses: Record<string, string> = {
  none: '',
  sm: 'p-3',
  md: 'p-4',
  lg: 'p-6'
}
</script>

<template>
  <div
    :class="[
      'bg-white/90 dark:bg-gray-800/50 backdrop-blur-md rounded-2xl border border-gray-200/50 dark:border-gray-700/50',
      hoverable ? 'hover:shadow-lg hover:shadow-blue-500/10 hover:border-blue-500/30 transition-all duration-300 cursor-pointer' : '',
      paddingClasses[padding]
    ]"
  >
    <slot />
  </div>
</template>
