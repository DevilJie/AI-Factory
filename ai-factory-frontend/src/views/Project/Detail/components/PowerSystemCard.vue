<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown, ChevronUp, Edit2, Trash2 } from 'lucide-vue-next'
import type { PowerSystem } from '@/api/powerSystem'

const props = defineProps<{
  system: PowerSystem
  disabled: boolean
}>()

const emit = defineEmits<{
  edit: []
  delete: []
}>()

const expanded = ref(false)
</script>

<template>
  <div
    class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden"
  >
    <!-- 卡片头部 -->
    <div
      class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
      @click="expanded = !expanded"
    >
      <div class="flex items-center gap-3 flex-1">
        <h4 class="text-sm font-semibold text-gray-900 dark:text-white">{{ system.name }}</h4>
        <span v-if="system.sourceFrom" class="text-xs text-gray-500 dark:text-gray-400">
          能量来源: {{ system.sourceFrom }}
        </span>
      </div>
      <div class="flex items-center gap-1">
        <button
          @click.stop="emit('edit')"
          :disabled="disabled"
          class="p-1 text-gray-400 hover:text-blue-500 dark:text-gray-500 dark:hover:text-blue-400 rounded transition-colors disabled:opacity-50"
        >
          <Edit2 class="w-3.5 h-3.5" />
        </button>
        <button
          @click.stop="emit('delete')"
          :disabled="disabled"
          class="p-1 text-gray-400 hover:text-red-500 dark:text-gray-500 dark:hover:text-red-400 rounded transition-colors disabled:opacity-50"
        >
          <Trash2 class="w-3.5 h-3.5" />
        </button>
        <component :is="expanded ? ChevronUp : ChevronDown" class="w-4 h-4 text-gray-400" />
      </div>
    </div>
    <!-- 展开内容 -->
    <div v-if="expanded" class="px-4 pb-4 space-y-3 border-t border-gray-100 dark:border-gray-700">
      <!-- 体系信息 -->
      <div v-if="system.description" class="text-xs text-gray-600 dark:text-gray-400 mt-2">
        {{ system.description }}
      </div>
      <div class="flex flex-wrap gap-2 text-xs">
        <span v-if="system.coreResource" class="px-2 py-0.5 bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300 rounded">
          核心资源: {{ system.coreResource }}
        </span>
        <span v-if="system.cultivationMethod" class="px-2 py-0.5 bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 rounded">
          修炼方式: {{ system.cultivationMethod }}
        </span>
      </div>
      <!-- 等级划分 -->
      <div v-if="system.levels && system.levels.length > 0" class="mt-3">
        <div class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">等级划分</div>
        <div class="space-y-2">
          <div
            v-for="level in system.levels"
            :key="level.id"
            class="flex items-start gap-2 px-3 py-2 bg-gray-50 dark:bg-gray-700 rounded-lg text-sm"
          >
            <span class="font-medium text-gray-800 dark:text-gray-200">
              {{ level.levelName }}
            </span>
            <span v-if="level.landmarkAbility" class="text-xs text-blue-600 dark:text-blue-400 ml-1">
              标志: {{ level.landmarkAbility }}
            </span>
            <span v-if="level.lifespan" class="text-xs text-gray-500 dark:text-gray-400">
              寿命: {{ level.lifespan }}
            </span>
            <div v-if="level.steps && level.steps.length > 0" class="flex gap-1 ml-2">
              <span
                v-for="step in level.steps"
                :key="step.id"
                class="px-1.5 py-0.5 bg-gray-100 dark:bg-gray-600 text-gray-600 dark:text-gray-400 rounded text-xs"
              >
                {{ step.levelName }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
