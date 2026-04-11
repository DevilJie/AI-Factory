<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { X, Save, ExternalLink, CheckCircle2, XCircle, AlertTriangle, ChevronDown, ChevronUp } from 'lucide-vue-next'
import { useEditorStore } from '@/stores/editor'
import { updateChapterPlan, type ChapterPlanUpdateRequest, getChapterCharacters, type ChapterCharacter } from '@/api/chapter'
import { success, error } from '@/utils/toast'
import type { ChapterPlan } from '@/types/project'
import ForeshadowingTab from './ForeshadowingTab.vue'

const editorStore = useEditorStore()

const emit = defineEmits<{
  openCharacter: [characterId: number | string]
}>()

// Local state
const saving = ref(false)
const activeSection = ref<string>('basic')

const form = ref<{
  chapterTitle: string
  plotOutline: string
  chapterStartingScene: string
  chapterEndingScene: string
  keyEvents: string
  chapterGoal: string
  wordCountTarget: number | undefined
  chapterNotes: string
  status: string
  plotStage: string
  plannedCharacters: string
  characterArcs: string
}>({
  chapterTitle: '',
  plotOutline: '',
  chapterStartingScene: '',
  chapterEndingScene: '',
  keyEvents: '',
  chapterGoal: '',
  wordCountTarget: undefined,
  chapterNotes: '',
  status: '',
  plotStage: '',
  plannedCharacters: '',
  characterArcs: ''
})

// Computed
const visible = computed({
  get: () => editorStore.isDrawerVisible,
  set: (val) => editorStore.setDrawerVisible(val)
})

const currentChapter = computed(() => editorStore.currentChapter)
const currentChapterPlan = computed(() => editorStore.currentChapterPlan)
const projectId = computed(() => editorStore.projectId)

// Sections for navigation
const sections = [
  { key: 'basic', label: '基本信息' },
  { key: 'plot', label: '情节规划' },
  { key: 'scene', label: '场景设定' },
  { key: 'character', label: '角色规划' },
  { key: 'foreshadowing', label: '伏笔管理' }
] as const

// Status options
const statusOptions = [
  { value: 'pending', label: '待处理' },
  { value: 'in_progress', label: '进行中' },
  { value: 'completed', label: '已完成' },
  { value: 'skipped', label: '已跳过' }
]

const plotStageOptions = [
  { value: 'introduction', label: '起始阶段' },
  { value: 'development', label: '发展阶段' },
  { value: 'climax', label: '高潮阶段' },
  { value: 'conclusion', label: '结局阶段' }
]

// Editable planned characters (reactive, synced to form.plannedCharacters)
const editableCharacters = ref<any[]>([])

// Parse planned characters JSON for display
const parsedPlannedCharacters = computed(() => {
  if (form.value.plannedCharacters) {
    try {
      return JSON.parse(form.value.plannedCharacters)
    } catch { /* ignore */ }
  }
  return []
})

// Sync editableCharacters from form data
watch(() => form.value.plannedCharacters, (val) => {
  if (val) {
    try {
      editableCharacters.value = JSON.parse(val)
    } catch {
      editableCharacters.value = []
    }
  } else {
    editableCharacters.value = []
  }
}, { immediate: true })

// Sync editableCharacters back to form
const syncCharactersToForm = () => {
  form.value.plannedCharacters = JSON.stringify(editableCharacters.value)
}

const updateCharField = (idx: number, field: string, value: any) => {
  if (editableCharacters.value[idx]) {
    editableCharacters.value[idx][field] = value
    syncCharactersToForm()
  }
}

const removeCharacter = (idx: number) => {
  editableCharacters.value.splice(idx, 1)
  syncCharactersToForm()
}

const addCharacter = () => {
  editableCharacters.value.push({
    characterName: '',
    roleType: 'supporting',
    roleDescription: '',
    importance: 'medium',
    characterId: null
  })
  syncCharactersToForm()
}

// === Comparison View State (FE-02) ===
const actualCharacters = ref<ChapterCharacter[]>([])
const actualLoading = ref(false)
const showComparison = ref(true)

// The chapterId from currentChapterPlan -- only truthy when chapter is generated
const chapterId = computed(() => currentChapterPlan.value?.chapterId)

// Role type label mapping (for comparison view badges)
const roleTypeLabels: Record<string, string> = {
  protagonist: '主角',
  supporting: '配角',
  antagonist: '反派',
  minor: '路人',
  npc: 'NPC'
}

// Comparison matching logic (per D-15, D-16, D-17)
interface ComparisonResult {
  matched: Array<{ planned: any; actual: ChapterCharacter }>
  plannedOnly: any[]
  actualOnly: ChapterCharacter[]
}

const comparisonResult = computed<ComparisonResult>(() => {
  const planned = editableCharacters.value
  const actual = [...actualCharacters.value]
  const matched: Array<{ planned: any; actual: ChapterCharacter }> = []
  const plannedOnly: any[] = []

  for (const p of planned) {
    let found = false
    // ID-first matching (per D-15)
    if (p.characterId) {
      const idx = actual.findIndex(a => a.characterId === Number(p.characterId))
      if (idx !== -1) {
        matched.push({ planned: p, actual: actual.splice(idx, 1)[0] })
        found = true
      }
    }
    // Name fallback (per D-15, D-17)
    if (!found && p.characterName) {
      const idx = actual.findIndex(a => a.characterName === p.characterName)
      if (idx !== -1) {
        matched.push({ planned: p, actual: actual.splice(idx, 1)[0] })
        found = true
      }
    }
    if (!found) {
      plannedOnly.push(p)
    }
  }

  return { matched, plannedOnly, actualOnly: actual }
})

// Summary text (per UI-SPEC copywriting contract)
const comparisonSummary = computed(() => {
  const total = editableCharacters.value.length
  const matchedCount = comparisonResult.value.matched.length
  const unplannedCount = comparisonResult.value.actualOnly.length
  const parts: string[] = []
  if (total > 0) {
    parts.push(`${matchedCount}/${total} 规划角色已出场`)
  }
  if (unplannedCount > 0) {
    parts.push(`${unplannedCount} 位计划外登场`)
  }
  return parts.join(' | ') || '暂无对比数据'
})

// Load actual characters when switching to character tab and chapter is generated
watch([activeSection, chapterId], async ([section, cId]) => {
  if (section === 'character' && cId && projectId.value) {
    actualLoading.value = true
    try {
      const res = await getChapterCharacters(projectId.value, cId)
      actualCharacters.value = res || []
    } catch {
      actualCharacters.value = []
    } finally {
      actualLoading.value = false
    }
  } else {
    actualCharacters.value = []
  }
}, { immediate: true })

// Parse character arcs JSON for display
const parsedCharacterArcs = computed(() => {
  if (!form.value.characterArcs) return []
  try {
    return JSON.parse(form.value.characterArcs)
  } catch {
    return []
  }
})

// Update form from plan data
const updateFormFromPlan = (plan: ChapterPlan) => {
  form.value = {
    chapterTitle: plan.title || '',
    plotOutline: plan.plotOutline || '',
    chapterStartingScene: plan.chapterStartingScene || '',
    chapterEndingScene: plan.chapterEndingScene || '',
    keyEvents: plan.keyEvents || '',
    chapterGoal: plan.chapterGoal || '',
    wordCountTarget: plan.wordCountTarget,
    chapterNotes: plan.chapterNotes || '',
    status: plan.status || 'pending',
    plotStage: plan.plotStage || '',
    plannedCharacters: plan.plannedCharacters || '',
    characterArcs: plan.characterArcs || ''
  }
}

// Watch currentChapterPlan to update form
watch(currentChapterPlan, (plan) => {
  if (plan) {
    updateFormFromPlan(plan)
  }
}, { immediate: true })

// Watch currentChapter to merge any additional data from chapter
watch(currentChapter, (chapter) => {
  if (chapter && !currentChapterPlan.value) {
    // Fallback: populate from chapter if no plan
    form.value = {
      chapterTitle: chapter.chapterTitle || chapter.title || '',
      plotOutline: chapter.plotOutline || '',
      chapterStartingScene: '',
      chapterEndingScene: '',
      keyEvents: chapter.keyEvents || '',
      chapterGoal: chapter.chapterGoal || '',
      wordCountTarget: chapter.wordCountTarget,
      chapterNotes: chapter.chapterNotes || '',
      status: '',
      plotStage: '',
      plannedCharacters: '',
      characterArcs: ''
    }
  }
}, { immediate: true })

// Methods
const handleClose = () => {
  visible.value = false
}

const handleSave = async () => {
  if (!currentChapterPlan.value?.id || !projectId.value) return

  saving.value = true
  try {
    const updateData: ChapterPlanUpdateRequest = {
      chapterTitle: form.value.chapterTitle,
      plotOutline: form.value.plotOutline,
      chapterStartingScene: form.value.chapterStartingScene,
      chapterEndingScene: form.value.chapterEndingScene,
      keyEvents: form.value.keyEvents,
      chapterGoal: form.value.chapterGoal,
      wordCountTarget: form.value.wordCountTarget,
      chapterNotes: form.value.chapterNotes,
      status: form.value.status || undefined,
      plotStage: form.value.plotStage || undefined,
      plannedCharacters: form.value.plannedCharacters,
      characterArcs: form.value.characterArcs
    }

    await updateChapterPlan(projectId.value, currentChapterPlan.value.id, updateData)
    success('章节规划已保存')
    handleClose()
  } catch (e: any) {
    error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// Chapter number display
const chapterNumber = computed(() => {
  return currentChapterPlan.value?.chapterNumber || currentChapter.value?.chapterNumber || 0
})
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="visible" class="fixed inset-0 z-50">
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/30"
          @click="handleClose"
        />

        <!-- Drawer - wider to accommodate all fields -->
        <div class="absolute top-0 right-0 h-full w-[640px] bg-white dark:bg-gray-800 shadow-xl flex flex-col">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700 shrink-0">
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                章节规划详情
              </h3>
              <p v-if="chapterNumber" class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
                第 {{ chapterNumber }} 章
              </p>
            </div>
            <button
              class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded"
              @click="handleClose"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Section Tabs -->
          <div class="flex border-b border-gray-200 dark:border-gray-700 px-6 shrink-0">
            <button
              v-for="section in sections"
              :key="section.key"
              class="px-4 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px"
              :class="activeSection === section.key
                ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
              @click="activeSection = section.key"
            >
              {{ section.label }}
            </button>
          </div>

          <!-- Content -->
          <div class="flex-1 overflow-y-auto p-6">
            <!-- Basic Info Section -->
            <div v-show="activeSection === 'basic'" class="space-y-5">
              <!-- Chapter Title -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  章节标题
                </label>
                <input
                  v-model="form.chapterTitle"
                  type="text"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="输入章节标题"
                />
              </div>

              <!-- Chapter Goal -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  章节目标
                </label>
                <textarea
                  v-model="form.chapterGoal"
                  rows="2"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                  placeholder="本章要达成的叙事目的"
                />
              </div>

              <!-- Chapter Notes -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  章节备注
                </label>
                <textarea
                  v-model="form.chapterNotes"
                  rows="2"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                  placeholder="补充说明信息"
                />
              </div>
            </div>

            <!-- Plot Section -->
            <div v-show="activeSection === 'plot'" class="space-y-5">
              <!-- Plot Outline -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  情节大纲
                </label>
                <textarea
                  v-model="form.plotOutline"
                  rows="6"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-y"
                  placeholder="描述本章主要剧情发展..."
                />
              </div>

              <!-- Key Events -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  关键事件
                </label>
                <textarea
                  v-model="form.keyEvents"
                  rows="4"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-y"
                  placeholder="用分号分隔多个关键事件"
                />
              </div>
            </div>

            <!-- Scene Section -->
            <div v-show="activeSection === 'scene'" class="space-y-5">
              <!-- Starting Scene -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  章节起点场景
                </label>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-2">
                  描述章节开始时的地点、时间、状态
                </p>
                <textarea
                  v-model="form.chapterStartingScene"
                  rows="4"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-y"
                  placeholder="地点：江南城醉仙楼；时间：傍晚；状态：主角刚抵达城中"
                />
              </div>

              <!-- Ending Scene -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  章节终点场景
                </label>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-2">
                  描述章节结束时的地点、时间、状态
                </p>
                <textarea
                  v-model="form.chapterEndingScene"
                  rows="4"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-y"
                  placeholder="地点：城外树林；时间：深夜；状态：主角与神秘人对话"
                />
              </div>
            </div>

            <!-- Character Section -->
            <div v-show="activeSection === 'character'" class="space-y-5">
              <!-- Comparison Region (FE-02) -->
              <div v-if="chapterId && editableCharacters.length > 0" class="mb-4">
                <!-- Summary bar (collapsible header) -->
                <div
                  class="flex items-center justify-between px-3 py-2 bg-blue-50 dark:bg-blue-900/20 rounded-lg cursor-pointer select-none"
                  @click="showComparison = !showComparison"
                >
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-medium text-blue-700 dark:text-blue-300">
                      {{ comparisonSummary }}
                    </span>
                  </div>
                  <component :is="showComparison ? ChevronUp : ChevronDown" class="w-4 h-4 text-blue-500" />
                </div>

                <!-- Comparison detail grid (collapsible) -->
                <div v-if="showComparison" class="mt-2 grid grid-cols-2 gap-3">
                  <!-- Left: Planned characters -->
                  <div class="space-y-1">
                    <div class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">规划角色</div>
                    <!-- Matched (green check) -->
                    <div
                      v-for="item in comparisonResult.matched"
                      :key="'m-' + item.planned.characterName"
                      class="flex items-center gap-1.5 px-2 py-1 text-sm"
                    >
                      <CheckCircle2 class="w-3.5 h-3.5 text-green-500 shrink-0" />
                      <span class="text-gray-900 dark:text-white truncate">{{ item.planned.characterName }}</span>
                      <span v-if="item.planned.roleType" class="text-xs text-gray-400">
                        {{ roleTypeLabels[item.planned.roleType] || item.planned.roleType }}
                      </span>
                    </div>
                    <!-- Planned-only (red cross) -->
                    <div
                      v-for="item in comparisonResult.plannedOnly"
                      :key="'p-' + item.characterName"
                      class="flex items-center gap-1.5 px-2 py-1 text-sm"
                    >
                      <XCircle class="w-3.5 h-3.5 text-red-500 shrink-0" />
                      <span class="text-gray-500 dark:text-gray-400 truncate">{{ item.characterName || '未命名' }}</span>
                      <span v-if="item.roleType" class="text-xs text-gray-400">
                        {{ roleTypeLabels[item.roleType] || item.roleType }}
                      </span>
                    </div>
                  </div>

                  <!-- Right: Actual characters -->
                  <div class="space-y-1">
                    <div class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">实际登场</div>
                    <!-- Matched (green check) -->
                    <div
                      v-for="item in comparisonResult.matched"
                      :key="'ma-' + item.actual.characterId"
                      class="flex items-center gap-1.5 px-2 py-1 text-sm"
                    >
                      <CheckCircle2 class="w-3.5 h-3.5 text-green-500 shrink-0" />
                      <span class="text-gray-900 dark:text-white truncate">{{ item.actual.characterName }}</span>
                      <span v-if="item.actual.roleType" class="text-xs text-gray-400">
                        {{ roleTypeLabels[item.actual.roleType] || item.actual.roleType }}
                      </span>
                    </div>
                    <!-- Actual-only (amber warning) -->
                    <div
                      v-for="item in comparisonResult.actualOnly"
                      :key="'a-' + item.characterId"
                      class="flex items-center gap-1.5 px-2 py-1 text-sm"
                    >
                      <AlertTriangle class="w-3.5 h-3.5 text-amber-500 shrink-0" />
                      <span class="text-gray-900 dark:text-white truncate">{{ item.characterName }}</span>
                      <span v-if="item.roleType" class="text-xs text-gray-400">
                        {{ roleTypeLabels[item.roleType] || item.roleType }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Planned Characters -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  规划角色列表
                </label>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-2">
                  本章计划登场的角色信息
                </p>

                <!-- Editable character list -->
                <div v-if="editableCharacters.length > 0" class="mb-3 space-y-3">
                  <div
                    v-for="(char, idx) in editableCharacters"
                    :key="idx"
                    class="p-3 rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700/50"
                  >
                    <!-- Row 1: name + role + importance + delete -->
                    <div class="flex items-center gap-2 mb-2">
                      <input
                        :value="char.characterName || ''"
                        @input="updateCharField(idx, 'characterName', ($event.target as HTMLInputElement).value)"
                        type="text"
                        class="flex-1 px-2 py-1 text-sm font-medium border border-gray-300 dark:border-gray-500 rounded bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:ring-1 focus:ring-blue-500"
                        placeholder="角色名称"
                      />
                      <button
                        v-if="char.characterId"
                        class="p-1 text-blue-500 hover:text-blue-600 dark:text-blue-400"
                        title="查看角色详情"
                        @click.stop="emit('openCharacter', char.characterId)"
                      >
                        <ExternalLink class="w-3.5 h-3.5" />
                      </button>
                      <select
                        :value="char.roleType || 'supporting'"
                        @change="updateCharField(idx, 'roleType', ($event.target as HTMLSelectElement).value)"
                        class="px-2 py-1 text-xs border border-gray-300 dark:border-gray-500 rounded bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:ring-1 focus:ring-blue-500"
                      >
                        <option value="protagonist">主角</option>
                        <option value="antagonist">反派</option>
                        <option value="supporting">配角</option>
                        <option value="minor">路人</option>
                      </select>
                      <select
                        :value="char.importance || 'medium'"
                        @change="updateCharField(idx, 'importance', ($event.target as HTMLSelectElement).value)"
                        class="px-2 py-1 text-xs border border-gray-300 dark:border-gray-500 rounded bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:ring-1 focus:ring-blue-500"
                      >
                        <option value="high">重要</option>
                        <option value="medium">一般</option>
                        <option value="low">次要</option>
                      </select>
                      <button
                        @click="removeCharacter(idx)"
                        class="p-1 text-gray-400 hover:text-red-500 dark:hover:text-red-400 rounded"
                        title="删除角色"
                      >
                        <X class="w-4 h-4" />
                      </button>
                    </div>
                    <!-- Row 2: description -->
                    <textarea
                      :value="char.roleDescription || ''"
                      @input="updateCharField(idx, 'roleDescription', ($event.target as HTMLTextAreaElement).value)"
                      rows="2"
                      class="w-full px-2 py-1 text-sm border border-gray-300 dark:border-gray-500 rounded bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:ring-1 focus:ring-blue-500 resize-none"
                      placeholder="角色在本章的戏份描述..."
                    />
                  </div>
                </div>

                <!-- Add character button -->
                <button
                  @click="addCharacter"
                  class="w-full py-2 text-sm text-blue-600 dark:text-blue-400 border border-dashed border-gray-300 dark:border-gray-600 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors"
                >
                  + 添加角色
                </button>
              </div>
            </div>

            <!-- Foreshadowing Section -->
            <ForeshadowingTab
              v-if="activeSection === 'foreshadowing'"
              :project-id="projectId"
              :chapter-number="chapterNumber"
              :volume-number="currentChapterPlan?.volumeNumber"
            />
          </div>

          <!-- Footer -->
          <div class="shrink-0 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
            <div class="flex justify-end gap-3">
              <button
                class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                @click="handleClose"
              >
                取消
              </button>
              <button
                class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-500 rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="saving"
                @click="handleSave"
              >
                <Save v-if="saving" class="w-4 h-4 animate-spin" />
                <Save v-else class="w-4 h-4" />
                {{ saving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.2s ease;
}

.drawer-enter-active .absolute.top-0.right-0,
.drawer-leave-active .absolute.top-0.right-0 {
  transition: transform 0.2s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .absolute.top-0.right-0,
.drawer-leave-to .absolute.top-0.right-0 {
  transform: translateX(100%);
}
</style>
