// ai-factory-frontend2/src/composables/useSettingsDrawer.ts

import { ref, readonly } from 'vue'
import type { SettingsTab } from '@/types/settings'

// 全局状态
const isOpen = ref(false)
const activeTab = ref<SettingsTab>('ai-model')

/**
 * 设置抽屉状态管理 composable
 *
 * 用于在任意组件中打开/关闭设置抽屉，并控制当前激活的 Tab
 */
export function useSettingsDrawer() {
  /**
   * 打开抽屉
   * @param tab - 初始激活的 Tab，默认为 'ai-model'
   */
  const openDrawer = (tab: SettingsTab = 'ai-model') => {
    activeTab.value = tab
    isOpen.value = true
  }

  /**
   * 关闭抽屉
   */
  const closeDrawer = () => {
    isOpen.value = false
  }

  /**
   * 设置当前激活的 Tab
   */
  const setActiveTab = (tab: SettingsTab) => {
    activeTab.value = tab
  }

  return {
    isOpen: readonly(isOpen),
    activeTab: readonly(activeTab),
    openDrawer,
    closeDrawer,
    setActiveTab
  }
}
