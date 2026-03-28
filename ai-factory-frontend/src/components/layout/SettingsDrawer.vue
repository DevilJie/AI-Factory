<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Bot, User, Shield } from 'lucide-vue-next'
import { useSettingsDrawer } from '@/composables/useSettingsDrawer'
import AiModelTab from './settings/AiModelTab.vue'
import UserProfileTab from './settings/UserProfileTab.vue'
import SecurityTab from './settings/SecurityTab.vue'
import EditProviderDialog from './settings/EditProviderDialog.vue'
import type { AiProvider, AiProviderTemplate } from '@/types/settings'

const { isOpen, activeTab, closeDrawer, setActiveTab } = useSettingsDrawer()

// 编辑对话框状态
const showEditDialog = ref(false)
const editingTemplate = ref<AiProviderTemplate | null>(null)
const editingProvider = ref<AiProvider | null>(null)
const aiModelTabRef = ref<{ refresh: () => void } | null>(null)

// 处理编辑事件
const handleEdit = (data: { template?: AiProviderTemplate; provider?: AiProvider }) => {
  editingTemplate.value = data.template || null
  editingProvider.value = data.provider || null
  showEditDialog.value = true
}

const handleEditSaved = () => {
  showEditDialog.value = false
  // 刷新 AiModelTab 数据
  aiModelTabRef.value?.refresh()
}

const tabs = [
  { key: 'ai-model', label: 'AI 模型', icon: Bot },
  { key: 'user-profile', label: '用户资料', icon: User },
  { key: 'security', label: '账户安全', icon: Shield }
] as const

// 点击遮罩关闭
const handleOverlayClick = () => {
  closeDrawer()
}

// ESC 键关闭
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    closeDrawer()
  }
}

// 监听打开状态，控制 body 滚动
watch(isOpen, (open) => {
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
        v-if="isOpen"
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
        v-if="isOpen"
        class="fixed top-0 right-0 h-full w-full max-w-2xl bg-white dark:bg-gray-900 z-50 shadow-2xl flex flex-col"
      >
        <!-- 头部 -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
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
              @click="setActiveTab(tab.key)"
            >
              <component :is="tab.icon" class="w-4 h-4" />
              {{ tab.label }}
            </button>
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
          <AiModelTab
            ref="aiModelTabRef"
            v-if="activeTab === 'ai-model'"
            @edit="handleEdit"
          />
          <UserProfileTab v-else-if="activeTab === 'user-profile'" />
          <SecurityTab v-else-if="activeTab === 'security'" />
        </div>
      </div>
    </Transition>

    <!-- 编辑对话框 (放在Transition外面，因为Transition只能有一个子元素) -->
    <EditProviderDialog
      v-model="showEditDialog"
      :template="editingTemplate"
      :provider="editingProvider"
      @saved="handleEditSaved"
    />
  </Teleport>
</template>
