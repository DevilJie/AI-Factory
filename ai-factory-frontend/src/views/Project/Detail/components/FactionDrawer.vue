<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Swords, Users, MapPin } from 'lucide-vue-next'
import type { Faction } from '@/api/faction'
import FactionRelationTab from './FactionRelationTab.vue'
import FactionCharacterTab from './FactionCharacterTab.vue'
import FactionRegionTab from './FactionRegionTab.vue'

const props = defineProps<{
  modelValue: boolean
  faction: Faction | null
  projectId: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const activeTab = ref<string>('relation')

const tabs = [
  { key: 'relation', label: '势力关系', icon: Swords },
  { key: 'character', label: '人物关联', icon: Users },
  { key: 'region', label: '地区关联', icon: MapPin }
] as const

const closeDrawer = () => {
  emit('update:modelValue', false)
}

const handleOverlayClick = () => {
  closeDrawer()
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    closeDrawer()
  }
}

watch(() => props.modelValue, (open) => {
  if (open) {
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', handleKeydown)
  } else {
    document.body.style.overflow = ''
    document.removeEventListener('keydown', handleKeydown)
  }
})
</script>

<template>
  <Teleport to="body">
    <!-- 遮罩层 -->
    <Transition
      enter-active-class="transition-opacity duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-300"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 bg-black/50 z-40"
        @click="handleOverlayClick"
      />
    </Transition>

    <!-- 抽屉面板 -->
    <Transition
      enter-active-class="transition-transform duration-300 ease-out"
      enter-from-class="translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition-transform duration-300 ease-in"
      leave-from-class="translate-x-0"
      leave-to-class="translate-x-full"
    >
      <div
        v-if="modelValue"
        class="fixed top-0 right-0 h-full w-full max-w-2xl bg-white dark:bg-gray-900 z-50 shadow-2xl flex flex-col"
      >
        <!-- 头部 -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-3">
            <h2 class="text-base font-semibold text-gray-900 dark:text-white">
              {{ faction?.name }} -- 关联管理
            </h2>
            <div class="flex gap-1 p-1 rounded-xl bg-gray-100 dark:bg-gray-800">
              <button
                v-for="tab in tabs"
                :key="tab.key"
                :class="[
                  'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
                  activeTab === tab.key
                    ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm'
                    : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                ]"
                @click="activeTab = tab.key"
              >
                <component :is="tab.icon" class="w-4 h-4" />
                {{ tab.label }}
              </button>
            </div>
          </div>
          <button
            class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            @click="closeDrawer"
          >
            <X class="w-5 h-5 text-gray-500" />
          </button>
        </div>

        <!-- 内容区域 -->
        <div class="flex-1 overflow-y-auto p-6">
          <FactionRelationTab
            v-if="activeTab === 'relation' && faction?.id"
            :faction-id="faction.id"
            :project-id="projectId"
          />
          <FactionCharacterTab
            v-if="activeTab === 'character' && faction?.id"
            :faction-id="faction.id"
            :project-id="projectId"
          />
          <FactionRegionTab
            v-if="activeTab === 'region' && faction?.id"
            :faction-id="faction.id"
            :project-id="projectId"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
