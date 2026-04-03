<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Swords, Users, MapPin, Edit3, Save } from 'lucide-vue-next'
import type { Faction } from '@/api/faction'
import { updateFaction } from '@/api/faction'
import { getPowerSystemList } from '@/api/powerSystem'
import { success, error } from '@/utils/toast'
import FactionRelationTab from './FactionRelationTab.vue'
import FactionCharacterTab from './FactionCharacterTab.vue'
import FactionRegionTab from './FactionRegionTab.vue'

const props = defineProps<{
  modelValue: boolean
  faction: Faction | null
  projectId: string
  initialTab?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const activeTab = ref<string>('relation')
const saving = ref(false)

// Edit form state
const editName = ref('')
const editDesc = ref('')
const editType = ref('')
const editPowerSystem = ref<number | null>(null)
const powerSystemOptions = ref<{ id: number; name: string }[]>([])

const typeConfig: Record<string, { label: string; bg: string; text: string }> = {
  ally:    { label: '正派', bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  hostile: { label: '反派', bg: 'bg-red-100 dark:bg-red-900/30',    text: 'text-red-700 dark:text-red-400' },
  neutral: { label: '中立', bg: 'bg-gray-100 dark:bg-gray-700',     text: 'text-gray-600 dark:text-gray-400' },
}

const tabs = [
  { key: 'edit', label: '编辑信息', icon: Edit3 },
  { key: 'relation', label: '势力关系', icon: Swords },
  { key: 'character', label: '人物关联', icon: Users },
  { key: 'region', label: '地区关联', icon: MapPin }
] as const

const isTopLevel = () => (props.faction?.deep ?? 0) === 0

const resetEditForm = () => {
  editName.value = props.faction?.name || ''
  editDesc.value = props.faction?.description || ''
  editType.value = props.faction?.type || ''
  editPowerSystem.value = props.faction?.corePowerSystem || null
}

const loadPowerSystems = async () => {
  try {
    const list = await getPowerSystemList(props.projectId)
    powerSystemOptions.value = list.filter(ps => ps.id).map(ps => ({ id: ps.id!, name: ps.name }))
  } catch { /* ignore */ }
}

const handleSave = async () => {
  if (!props.faction?.id || !editName.value.trim()) { error('名称不能为空'); return }
  saving.value = true
  try {
    await updateFaction(props.projectId, {
      id: props.faction.id,
      name: editName.value.trim(),
      description: editDesc.value.trim() || undefined,
      parentId: props.faction.parentId,
      type: isTopLevel() ? editType.value || undefined : undefined,
      corePowerSystem: isTopLevel() ? editPowerSystem.value || undefined : undefined
    })
    success('更新成功')
    emit('saved')
  } catch (e: any) {
    error('更新失败')
  } finally {
    saving.value = false
  }
}

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
    activeTab.value = props.initialTab || 'edit'
    resetEditForm()
    if (isTopLevel()) loadPowerSystems()
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', handleKeydown)
  } else {
    document.body.style.overflow = ''
    document.removeEventListener('keydown', handleKeydown)
  }
})

watch(() => props.faction, () => {
  if (props.modelValue) resetEditForm()
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
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-center justify-between mb-3">
            <h2 class="text-base font-semibold text-gray-900 dark:text-white">
              {{ faction?.name }}
            </h2>
            <button
              class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
              @click="closeDrawer"
            >
              <X class="w-5 h-5 text-gray-500" />
            </button>
          </div>
          <div class="flex gap-1 p-1 rounded-xl bg-gray-100 dark:bg-gray-800">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              :class="[
                'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all',
                activeTab === tab.key
                  ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              ]"
              @click="activeTab = tab.key"
            >
              <component :is="tab.icon" class="w-3.5 h-3.5" />
              {{ tab.label }}
            </button>
          </div>
        </div>

        <!-- 内容区域 -->
        <div class="flex-1 overflow-y-auto p-6">
          <!-- Edit Tab -->
          <div v-if="activeTab === 'edit'" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                势力名称 <span class="text-red-500">*</span>
              </label>
              <input
                v-model="editName"
                class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="输入势力名称"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">势力描述</label>
              <textarea
                v-model="editDesc"
                rows="4"
                class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="势力描述（可选）"
              ></textarea>
            </div>

            <!-- Type & Power System (top-level only) -->
            <template v-if="isTopLevel()">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">阵营类型</label>
                <div class="flex items-center gap-2">
                  <button
                    v-for="(config, key) in typeConfig"
                    :key="key"
                    @click="editType = editType === key ? '' : key"
                    :class="[
                      'px-3 py-1.5 text-sm rounded-lg border transition-all',
                      editType === key
                        ? config.bg + ' ' + config.text + ' border-current'
                        : 'bg-gray-50 dark:bg-gray-700 text-gray-500 border-gray-200 dark:border-gray-600 hover:border-gray-300'
                    ]"
                  >
                    {{ config.label }}
                  </button>
                  <button
                    v-if="editType"
                    @click="editType = ''"
                    class="px-2 py-1.5 text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                  >
                    清除
                  </button>
                </div>
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">核心力量体系</label>
                <select
                  v-model="editPowerSystem"
                  class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option :value="null">无</option>
                  <option v-for="ps in powerSystemOptions" :key="ps.id" :value="ps.id">{{ ps.name }}</option>
                </select>
              </div>
            </template>

            <div class="flex justify-end gap-2 pt-4 border-t border-gray-200 dark:border-gray-700">
              <button
                @click="closeDrawer"
                class="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              >
                取消
              </button>
              <button
                @click="handleSave"
                :disabled="saving"
                class="flex items-center gap-1.5 px-4 py-2 text-sm text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors disabled:opacity-50"
              >
                <Save class="w-4 h-4" />
                {{ saving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>

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
