<script setup lang="ts">
import { computed } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

interface Props {
  currentPage: number
  total: number
  pageSize: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:currentPage': [page: number]
}>()

const totalPages = computed(() => Math.ceil(props.total / props.pageSize))

const visiblePages = computed(() => {
  const pages: number[] = []
  const total = totalPages.value
  const current = props.currentPage

  let start = Math.max(1, current - 2)
  let end = Math.min(total, current + 2)

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }

  return pages
})

const handlePageChange = (page: number) => {
  if (page >= 1 && page <= totalPages.value && page !== props.currentPage) {
    emit('update:currentPage', page)
  }
}

const handlePrev = () => {
  if (props.currentPage > 1) {
    handlePageChange(props.currentPage - 1)
  }
}

const handleNext = () => {
  if (props.currentPage < totalPages.value) {
    handlePageChange(props.currentPage + 1)
  }
}
</script>

<template>
  <div v-if="totalPages > 1" class="flex items-center justify-center gap-1">
    <!-- 上一页 -->
    <button
      :disabled="currentPage === 1"
      class="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      @click="handlePrev"
    >
      <ChevronLeft class="w-4 h-4" />
    </button>

    <!-- 页码 -->
    <template v-for="page in visiblePages" :key="page">
      <button
        :class="[
          'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
          page === currentPage
            ? 'bg-blue-500 text-white'
            : 'border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'
        ]"
        @click="handlePageChange(page)"
      >
        {{ page }}
      </button>
    </template>

    <!-- 下一页 -->
    <button
      :disabled="currentPage === totalPages"
      class="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      @click="handleNext"
    >
      <ChevronRight class="w-4 h-4" />
    </button>
  </div>
</template>
