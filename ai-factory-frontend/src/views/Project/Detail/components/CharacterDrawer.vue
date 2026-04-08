<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Edit3, Zap, Shield, Save, BookOpen } from 'lucide-vue-next'
import type { Character, CharacterDetail, ChapterAssociation } from '@/api/character'
import { getCharacterDetail, getCharacterChapters, updateCharacter, type CharacterForm } from '@/api/character'
import { success, error } from '@/utils/toast'
import CharacterPowerSystemTab from './CharacterPowerSystemTab.vue'
import CharacterFactionTab from './CharacterFactionTab.vue'

const props = defineProps<{
  modelValue: boolean
  character: Character | null
  projectId: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const activeTab = ref<string>('info')
const detail = ref<CharacterDetail | null>(null)
const loading = ref(false)
const saving = ref(false)

// Chapters tab state
const chapters = ref<ChapterAssociation[]>([])
const chaptersLoading = ref(false)

// Importance level labels
const importanceLevelLabels: Record<string, string> = {
  protagonist: '主角',
  supporting: '配角',
  antagonist: '反派',
  npc: 'NPC'
}

// Edit form state (info tab)
const editForm = ref<{
  name: string
  gender: string
  age: number | undefined
  roleType: string
  role: string
  appearance: string
  background: string
}>({
  name: '',
  gender: 'other',
  age: undefined,
  roleType: 'supporting',
  role: '',
  appearance: '',
  background: ''
})

const tabs = [
  { key: 'info', label: '基本信息', icon: Edit3 },
  { key: 'powerSystem', label: '修炼体系', icon: Zap },
  { key: 'faction', label: '所属势力', icon: Shield },
  { key: 'chapters', label: '出场章节', icon: BookOpen }
] as const

const genderOptions = [
  { value: 'male', label: '男' },
  { value: 'female', label: '女' },
  { value: 'other', label: '其他' }
]

const roleTypeOptions = [
  { value: 'protagonist', label: '主角' },
  { value: 'supporting', label: '配角' },
  { value: 'antagonist', label: '反派' },
  { value: 'npc', label: 'NPC' }
]

const resetEditForm = () => {
  if (!detail.value) return
  editForm.value = {
    name: detail.value.name || '',
    gender: detail.value.gender || 'other',
    age: detail.value.age,
    roleType: detail.value.roleType || 'supporting',
    role: detail.value.role || '',
    appearance: detail.value.appearance || '',
    background: detail.value.background || ''
  }
}

const loadDetail = async () => {
  if (!props.character?.id) return
  loading.value = true
  try {
    detail.value = await getCharacterDetail(props.projectId, props.character.id)
    resetEditForm()
  } catch (e: any) {
    error('加载人物详情失败')
    console.error('Failed to load character detail:', e)
  } finally {
    loading.value = false
  }

  // Load chapters data
  chaptersLoading.value = true
  getCharacterChapters(props.projectId, props.character.id)
    .then(data => {
      chapters.value = data.sort((a, b) => a.chapterNumber - b.chapterNumber)
    })
    .catch(() => {
      error('加载章节关联失败')
    })
    .finally(() => {
      chaptersLoading.value = false
    })
}

const handleSave = async () => {
  if (!props.character?.id || !editForm.value.name.trim()) {
    error('名称不能为空')
    return
  }
  saving.value = true
  try {
    await updateCharacter(props.projectId, props.character.id, {
      name: editForm.value.name.trim(),
      gender: editForm.value.gender as any,
      age: editForm.value.age,
      roleType: editForm.value.roleType as any,
      role: editForm.value.role.trim(),
      appearance: editForm.value.appearance.trim() || undefined,
      background: editForm.value.background.trim() || undefined
    })
    success('保存成功')
    emit('saved')
  } catch (e: any) {
    error('保存失败')
  } finally {
    saving.value = false
  }
}

const refreshDetail = async () => {
  if (!props.character?.id) return
  try {
    detail.value = await getCharacterDetail(props.projectId, props.character.id)
  } catch (e: any) {
    error('刷新人物详情失败')
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
    activeTab.value = 'info'
    loadDetail()
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', handleKeydown)
  } else {
    detail.value = null
    document.body.style.overflow = ''
    document.removeEventListener('keydown', handleKeydown)
  }
})
</script>

<template>
  <Teleport to="body">
    <!-- Overlay -->
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

    <!-- Drawer panel -->
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
        <!-- Header -->
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-center justify-between mb-3">
            <h2 class="text-base font-semibold text-gray-900 dark:text-white">
              {{ character?.name }}
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

        <!-- Content area -->
        <div class="flex-1 overflow-y-auto p-6">
          <!-- Loading -->
          <div v-if="loading" class="flex items-center justify-center py-12">
            <svg class="animate-spin h-8 w-8 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>

          <!-- Info Tab -->
          <div v-if="!loading && activeTab === 'info'" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                人物名称 <span class="text-red-500">*</span>
              </label>
              <input
                v-model="editForm.name"
                class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="输入人物名称"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">性别</label>
                <select
                  v-model="editForm.gender"
                  class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option v-for="g in genderOptions" :key="g.value" :value="g.value">{{ g.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">年龄</label>
                <input
                  v-model.number="editForm.age"
                  type="number"
                  placeholder="年龄"
                  class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">角色类型</label>
              <div class="grid grid-cols-4 gap-2">
                <button
                  v-for="rt in roleTypeOptions"
                  :key="rt.value"
                  @click="editForm.roleType = rt.value"
                  :class="[
                    'px-3 py-2 text-sm font-medium rounded-lg border transition-colors',
                    editForm.roleType === rt.value
                      ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
                      : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'
                  ]"
                >
                  {{ rt.label }}
                </button>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">角色身份</label>
              <input
                v-model="editForm.role"
                type="text"
                placeholder="如：武林盟主、天才少女"
                class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">外貌描述</label>
              <textarea
                v-model="editForm.appearance"
                rows="2"
                class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="描述人物外貌特征..."
              ></textarea>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">背景故事</label>
              <textarea
                v-model="editForm.background"
                rows="3"
                class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="描述人物背景故事..."
              ></textarea>
            </div>

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

          <!-- Power System Tab -->
          <CharacterPowerSystemTab
            v-if="!loading && activeTab === 'powerSystem' && detail"
            :character-id="character!.id"
            :project-id="projectId"
            :associations="detail.powerSystemAssociations"
            @refresh="refreshDetail"
          />

          <!-- Faction Tab -->
          <CharacterFactionTab
            v-if="!loading && activeTab === 'faction' && detail"
            :character-id="character!.id"
            :project-id="projectId"
            :associations="detail.factionAssociations"
            @refresh="refreshDetail"
          />

          <!-- Chapters Tab -->
          <div v-if="!loading && activeTab === 'chapters'">
            <div v-if="chaptersLoading" class="flex items-center justify-center py-12">
              <svg class="animate-spin h-8 w-8 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </div>

            <div v-else-if="chapters.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-500 dark:text-gray-400">
              <BookOpen class="w-10 h-10 mb-2 text-gray-300 dark:text-gray-600" />
              <p>暂无章节关联数据</p>
            </div>

            <div v-else class="divide-y divide-gray-100 dark:divide-gray-700">
              <div
                v-for="chapter in chapters"
                :key="chapter.id"
                class="py-4"
              >
                <!-- Chapter header -->
                <div class="flex items-center gap-2 mb-3">
                  <h4 class="text-sm font-medium text-gray-900 dark:text-white">
                    第{{ chapter.chapterNumber }}章 {{ chapter.chapterTitle }}
                  </h4>
                  <span
                    v-if="chapter.isFirstAppearance"
                    class="px-1.5 py-0.5 text-xs font-medium rounded bg-teal-100 text-teal-700 dark:bg-teal-900/30 dark:text-teal-400"
                  >
                    首次出场
                  </span>
                </div>

                <!-- Detail fields grid -->
                <div class="space-y-2">
                  <div v-if="chapter.importanceLevel" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">重要程度</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ importanceLevelLabels[chapter.importanceLevel] || chapter.importanceLevel }}</span>
                  </div>
                  <div v-if="chapter.statusInChapter" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">状态变化</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.statusInChapter }}</span>
                  </div>
                  <div v-if="chapter.emotionChange" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">情绪变化</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.emotionChange }}</span>
                  </div>
                  <div v-if="chapter.keyBehavior" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">关键行为</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.keyBehavior }}</span>
                  </div>
                  <div v-if="chapter.appearanceChange" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">外貌变化</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.appearanceChange }}</span>
                  </div>
                  <div v-if="chapter.personalityReveal" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">性格展现</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.personalityReveal }}</span>
                  </div>
                  <div v-if="chapter.abilityShown" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">能力展现</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.abilityShown }}</span>
                  </div>
                  <div v-if="chapter.characterDevelopment" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">角色成长</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.characterDevelopment }}</span>
                  </div>
                  <div v-if="chapter.dialogueSummary" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">对话摘要</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.dialogueSummary }}</span>
                  </div>
                  <div v-if="chapter.cultivationLevel" class="flex items-start gap-2">
                    <span class="text-xs text-gray-500 dark:text-gray-400 w-20 shrink-0">修为境界</span>
                    <span class="text-sm text-gray-900 dark:text-white">{{ chapter.cultivationLevel }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
