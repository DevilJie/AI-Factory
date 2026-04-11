<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Pencil, Trash2, ChevronDown, ChevronUp, X } from 'lucide-vue-next'
import {
  getForeshadowingList,
  createForeshadowing,
  updateForeshadowing,
  deleteForeshadowing
} from '@/api/foreshadowing'
import type {
  Foreshadowing,
  ForeshadowingCreateRequest,
  ForeshadowingUpdateRequest,
  ForeshadowingType,
  ForeshadowingLayoutType,
  ForeshadowingStatus
} from '@/types/foreshadowing'
import { success, error } from '@/utils/toast'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'

const props = defineProps<{
  projectId: string | number
  chapterNumber: number
  volumeNumber?: number
}>()

// Data state
const loading = ref(false)
const plantForeshadowings = ref<Foreshadowing[]>([])
const callbackForeshadowings = ref<Foreshadowing[]>([])
const volumeForeshadowings = ref<Foreshadowing[]>([])

const plantSectionExpanded = ref(true)
const callbackSectionExpanded = ref(true)
const volumeSectionExpanded = ref(false) // Per D-09: default collapsed

// Modal state
const showModal = ref(false)
const editingForeshadowing = ref<Foreshadowing | null>(null)
const modalForm = ref<{
  title: string
  type: ForeshadowingType
  description: string
  layoutType: ForeshadowingLayoutType | undefined
  plantedChapter: number
  plantedVolume: number | undefined
  plannedCallbackChapter: number | undefined
  plannedCallbackVolume: number | undefined
  priority: number
  notes: string
}>({
  title: '',
  type: 'character' as ForeshadowingType,
  description: '',
  layoutType: undefined,
  plantedChapter: props.chapterNumber,
  plantedVolume: undefined,
  plannedCallbackChapter: undefined,
  plannedCallbackVolume: undefined,
  priority: 3,
  notes: ''
})
const modalSaving = ref(false)
const titleError = ref('')

// Delete state
const confirmRef = ref<InstanceType<typeof ConfirmDialog> | null>(null)
const deletingForeshadowing = ref<Foreshadowing | null>(null)

// Display mapping objects
const layoutTypeConfig: Record<string, { border: string; bg: string; text: string; label: string }> = {
  bright1: { border: 'border-l-blue-500', bg: 'bg-blue-50 dark:bg-blue-900/20', text: 'text-blue-700 dark:text-blue-300', label: '明线1' },
  bright2: { border: 'border-l-green-500', bg: 'bg-green-50 dark:bg-green-900/20', text: 'text-green-700 dark:text-green-300', label: '明线2' },
  bright3: { border: 'border-l-yellow-500', bg: 'bg-yellow-50 dark:bg-yellow-900/20', text: 'text-yellow-700 dark:text-yellow-300', label: '明线3' },
  dark: { border: 'border-l-purple-500', bg: 'bg-purple-50 dark:bg-purple-900/20', text: 'text-purple-700 dark:text-purple-300', label: '暗线' }
}

const statusConfig: Record<string, { bg: string; text: string; label: string }> = {
  pending: { bg: 'bg-gray-100 dark:bg-gray-700', text: 'text-gray-600 dark:text-gray-400', label: '待埋设' },
  in_progress: { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-600 dark:text-blue-400', label: '进行中' },
  completed: { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-600 dark:text-green-400', label: '已完成' }
}

const typeConfig: Record<string, { bg: string; text: string; label: string }> = {
  character: { bg: 'bg-indigo-100 dark:bg-indigo-900/30', text: 'text-indigo-600 dark:text-indigo-400', label: '人物' },
  item: { bg: 'bg-teal-100 dark:bg-teal-900/30', text: 'text-teal-600 dark:text-teal-400', label: '物品' },
  event: { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-600 dark:text-orange-400', label: '事件' },
  secret: { bg: 'bg-rose-100 dark:bg-rose-900/30', text: 'text-rose-600 dark:text-rose-400', label: '秘密' }
}

const typeOptions = [
  { value: 'character', label: '人物' },
  { value: 'item', label: '物品' },
  { value: 'event', label: '事件' },
  { value: 'secret', label: '秘密' }
]

const layoutTypeOptions = [
  { value: 'bright1', label: '明线1' },
  { value: 'bright2', label: '明线2' },
  { value: 'bright3', label: '明线3' },
  { value: 'dark', label: '暗线' }
]

// Chapter reference line helper
const getChapterRef = (f: Foreshadowing): string => {
  const plantedVol = f.plantedVolume ? `第${f.plantedVolume}卷` : ''
  if (f.status === 'completed' && f.actualCallbackChapter) {
    const cbVol = f.plannedCallbackVolume && f.plannedCallbackVolume !== f.plantedVolume ? `第${f.plannedCallbackVolume}卷` : plantedVol
    return `${plantedVol}第${f.plantedChapter}章埋设 -> ${cbVol}第${f.actualCallbackChapter}章回收`
  }
  if (f.plannedCallbackChapter) {
    const cbVol = f.plannedCallbackVolume && f.plannedCallbackVolume !== f.plantedVolume ? `第${f.plannedCallbackVolume}卷` : plantedVol
    return `${plantedVol}第${f.plantedChapter}章埋设 -> ${cbVol}第${f.plannedCallbackChapter}章回收`
  }
  return `${plantedVol}第${f.plantedChapter}章埋设`
}

// Data loading
const loadData = async () => {
  loading.value = true
  try {
    // Query 1: foreshadowing planted in this chapter
    const plantedParams: Record<string, any> = { plantedChapter: props.chapterNumber }
    if (props.volumeNumber) plantedParams.plantedVolume = props.volumeNumber
    const planted = await getForeshadowingList(props.projectId, plantedParams)
    plantForeshadowings.value = planted || []

    // Query 2: foreshadowing planned for callback in this chapter
    const callbackParams: Record<string, any> = { plannedCallbackChapter: props.chapterNumber }
    if (props.volumeNumber) callbackParams.plannedCallbackVolume = props.volumeNumber
    const callbacks = await getForeshadowingList(props.projectId, callbackParams)
    callbackForeshadowings.value = callbacks || []

    // Query 3: volume active foreshadowing
    if (props.volumeNumber) {
      const allVolume = await getForeshadowingList(props.projectId, {
        plantedVolume: props.volumeNumber
      })
      const chapterIds = new Set([
        ...plantForeshadowings.value.map(f => f.id),
        ...callbackForeshadowings.value.map(f => f.id)
      ])
      volumeForeshadowings.value = (allVolume || []).filter(f => !chapterIds.has(f.id))
    }
  } catch (e: any) {
    error('加载伏笔数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
onMounted(loadData)

// CRUD functions
const openAddModal = () => {
  editingForeshadowing.value = null
  modalForm.value = {
    title: '',
    type: 'character' as ForeshadowingType,
    description: '',
    layoutType: undefined,
    plantedChapter: props.chapterNumber,
    plantedVolume: props.volumeNumber,
    plannedCallbackChapter: undefined,
    plannedCallbackVolume: undefined,
    priority: 3,
    notes: ''
  }
  titleError.value = ''
  showModal.value = true
}

const openEditModal = (f: Foreshadowing) => {
  editingForeshadowing.value = f
  modalForm.value = {
    title: f.title,
    type: f.type,
    description: f.description,
    layoutType: f.layoutType || undefined,
    plantedChapter: f.plantedChapter,
    plantedVolume: f.plantedVolume || undefined,
    plannedCallbackChapter: f.plannedCallbackChapter || undefined,
    plannedCallbackVolume: f.plannedCallbackVolume || undefined,
    priority: f.priority,
    notes: f.notes || ''
  }
  titleError.value = ''
  showModal.value = true
}

const saveForeshadowing = async () => {
  if (!modalForm.value.title.trim()) {
    titleError.value = '请输入伏笔标题'
    return
  }
  titleError.value = ''
  modalSaving.value = true
  try {
    if (editingForeshadowing.value) {
      // Update mode - ForeshadowingUpdateRequest has NO plantedChapter (per D-05)
      const updateData: ForeshadowingUpdateRequest = {
        title: modalForm.value.title,
        type: modalForm.value.type,
        description: modalForm.value.description,
        layoutType: modalForm.value.layoutType,
        plannedCallbackChapter: modalForm.value.plannedCallbackChapter,
        plannedCallbackVolume: modalForm.value.plannedCallbackVolume,
        priority: modalForm.value.priority,
        notes: modalForm.value.notes
      }
      await updateForeshadowing(props.projectId, editingForeshadowing.value.id, updateData)
      success('伏笔已更新')
    } else {
      // Create mode
      const createData: ForeshadowingCreateRequest = {
        title: modalForm.value.title,
        type: modalForm.value.type,
        description: modalForm.value.description,
        layoutType: modalForm.value.layoutType,
        plantedChapter: modalForm.value.plantedChapter,
        plantedVolume: modalForm.value.plantedVolume,
        plannedCallbackChapter: modalForm.value.plannedCallbackChapter,
        plannedCallbackVolume: modalForm.value.plannedCallbackVolume,
        priority: modalForm.value.priority,
        notes: modalForm.value.notes
      }
      await createForeshadowing(props.projectId, createData)
      success('伏笔已创建')
    }
    showModal.value = false
    await loadData()
  } catch (e: any) {
    error(e.message || '操作失败')
  } finally {
    modalSaving.value = false
  }
}

const confirmDelete = (f: Foreshadowing) => {
  deletingForeshadowing.value = f
  confirmRef.value?.show()
}

const handleDelete = async () => {
  if (!deletingForeshadowing.value) return
  try {
    await deleteForeshadowing(props.projectId, deletingForeshadowing.value.id)
    success('伏笔已删除')
    await loadData()
  } catch (e: any) {
    error(e.message || '删除失败')
  } finally {
    deletingForeshadowing.value = null
  }
}
</script>

<template>
  <div class="space-y-4">
    <!-- Loading skeleton -->
    <div v-if="loading" class="space-y-3">
      <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-lg h-20" />
      <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-lg h-20" />
      <div class="animate-pulse bg-gray-200 dark:bg-gray-700 rounded-lg h-20" />
    </div>

    <template v-else>
      <!-- Add button -->
      <button
        class="w-full py-2.5 text-sm text-blue-600 dark:text-blue-400 border border-dashed border-gray-300 dark:border-gray-600 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors flex items-center justify-center gap-1.5"
        @click="openAddModal"
      >
        <Plus class="w-4 h-4" />
        添加伏笔
      </button>

      <!-- Section: 待埋设伏笔 -->
      <div>
        <div
          class="flex items-center cursor-pointer py-2"
          @click="plantSectionExpanded = !plantSectionExpanded"
        >
          <component :is="plantSectionExpanded ? ChevronUp : ChevronDown" class="w-4 h-4 text-gray-400 mr-1" />
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">待埋设伏笔</span>
          <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 px-1.5 py-0.5 rounded-full ml-2">
            {{ plantForeshadowings.length }}
          </span>
        </div>
        <div v-if="plantSectionExpanded" class="space-y-2">
          <p v-if="plantForeshadowings.length === 0" class="text-sm text-gray-400 dark:text-gray-500 py-2 pl-5">
            暂无待埋设伏笔
          </p>
          <div
            v-for="f in plantForeshadowings"
            :key="f.id"
            class="rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden border-l-[3px]"
            :class="[
              layoutTypeConfig[f.layoutType || '']?.border || 'border-l-gray-400',
              f.status === 'completed' ? 'opacity-75' : ''
            ]"
          >
            <div class="p-3">
              <!-- Row 1: Title + Status -->
              <div class="flex items-center justify-between mb-1.5">
                <span class="text-base font-medium text-gray-900 dark:text-white truncate">
                  {{ f.title }}
                </span>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="[statusConfig[f.status]?.bg, statusConfig[f.status]?.text]"
                >
                  {{ statusConfig[f.status]?.label }}
                </span>
              </div>
              <!-- Row 2: Type + LayoutType + Actions -->
              <div class="flex items-center justify-between mb-1.5">
                <div class="flex items-center gap-1.5">
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="[typeConfig[f.type]?.bg, typeConfig[f.type]?.text]"
                  >
                    {{ typeConfig[f.type]?.label }}
                  </span>
                  <span
                    v-if="f.layoutType"
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="[layoutTypeConfig[f.layoutType]?.bg, layoutTypeConfig[f.layoutType]?.text]"
                  >
                    {{ layoutTypeConfig[f.layoutType]?.label }}
                  </span>
                </div>
                <div class="flex items-center gap-1">
                  <button
                    class="p-1 text-gray-400 hover:text-blue-500 dark:hover:text-blue-400 rounded"
                    title="编辑"
                    @click.stop="openEditModal(f)"
                  >
                    <Pencil class="w-3.5 h-3.5" />
                  </button>
                  <button
                    class="p-1 text-gray-400 hover:text-red-500 dark:hover:text-red-400 rounded"
                    title="删除"
                    @click.stop="confirmDelete(f)"
                  >
                    <Trash2 class="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
              <!-- Row 3: Description -->
              <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-1">
                {{ f.description }}
              </p>
              <!-- Row 4: Chapter reference -->
              <p class="text-xs text-gray-500 dark:text-gray-400">
                {{ getChapterRef(f) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Section: 待回收伏笔 -->
      <div>
        <div
          class="flex items-center cursor-pointer py-2"
          @click="callbackSectionExpanded = !callbackSectionExpanded"
        >
          <component :is="callbackSectionExpanded ? ChevronUp : ChevronDown" class="w-4 h-4 text-gray-400 mr-1" />
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">待回收伏笔</span>
          <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 px-1.5 py-0.5 rounded-full ml-2">
            {{ callbackForeshadowings.length }}
          </span>
        </div>
        <div v-if="callbackSectionExpanded" class="space-y-2">
          <p v-if="callbackForeshadowings.length === 0" class="text-sm text-gray-400 dark:text-gray-500 py-2 pl-5">
            暂无待回收伏笔
          </p>
          <div
            v-for="f in callbackForeshadowings"
            :key="f.id"
            class="rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden border-l-[3px]"
            :class="[
              layoutTypeConfig[f.layoutType || '']?.border || 'border-l-gray-400',
              f.status === 'completed' ? 'opacity-75' : ''
            ]"
          >
            <div class="p-3">
              <div class="flex items-center justify-between mb-1.5">
                <span class="text-base font-medium text-gray-900 dark:text-white truncate">
                  {{ f.title }}
                </span>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="[statusConfig[f.status]?.bg, statusConfig[f.status]?.text]"
                >
                  {{ statusConfig[f.status]?.label }}
                </span>
              </div>
              <div class="flex items-center justify-between mb-1.5">
                <div class="flex items-center gap-1.5">
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="[typeConfig[f.type]?.bg, typeConfig[f.type]?.text]"
                  >
                    {{ typeConfig[f.type]?.label }}
                  </span>
                  <span
                    v-if="f.layoutType"
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="[layoutTypeConfig[f.layoutType]?.bg, layoutTypeConfig[f.layoutType]?.text]"
                  >
                    {{ layoutTypeConfig[f.layoutType]?.label }}
                  </span>
                </div>
                <div class="flex items-center gap-1">
                  <button
                    class="p-1 text-gray-400 hover:text-blue-500 dark:hover:text-blue-400 rounded"
                    title="编辑"
                    @click.stop="openEditModal(f)"
                  >
                    <Pencil class="w-3.5 h-3.5" />
                  </button>
                  <button
                    class="p-1 text-gray-400 hover:text-red-500 dark:hover:text-red-400 rounded"
                    title="删除"
                    @click.stop="confirmDelete(f)"
                  >
                    <Trash2 class="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
              <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-1">
                {{ f.description }}
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-400">
                {{ getChapterRef(f) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Section: 分卷伏笔参考 (only if volumeNumber provided, default collapsed per D-09) -->
      <div v-if="volumeNumber">
        <div
          class="flex items-center cursor-pointer py-2"
          @click="volumeSectionExpanded = !volumeSectionExpanded"
        >
          <component :is="volumeSectionExpanded ? ChevronUp : ChevronDown" class="w-4 h-4 text-gray-400 mr-1" />
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">分卷伏笔参考</span>
          <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 px-1.5 py-0.5 rounded-full ml-2">
            {{ volumeForeshadowings.length }}
          </span>
        </div>
        <div v-if="volumeSectionExpanded" class="space-y-2">
          <p v-if="volumeForeshadowings.length === 0" class="text-sm text-gray-400 dark:text-gray-500 py-2 pl-5">
            暂无分卷伏笔
          </p>
          <div
            v-for="f in volumeForeshadowings"
            :key="f.id"
            class="rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden border-l-[3px]"
            :class="[
              layoutTypeConfig[f.layoutType || '']?.border || 'border-l-gray-400',
              f.status === 'completed' ? 'opacity-75' : ''
            ]"
          >
            <div class="p-3">
              <div class="flex items-center justify-between mb-1.5">
                <span class="text-base font-medium text-gray-900 dark:text-white truncate">
                  {{ f.title }}
                </span>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="[statusConfig[f.status]?.bg, statusConfig[f.status]?.text]"
                >
                  {{ statusConfig[f.status]?.label }}
                </span>
              </div>
              <div class="flex items-center justify-between mb-1.5">
                <div class="flex items-center gap-1.5">
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="[typeConfig[f.type]?.bg, typeConfig[f.type]?.text]"
                  >
                    {{ typeConfig[f.type]?.label }}
                  </span>
                  <span
                    v-if="f.layoutType"
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="[layoutTypeConfig[f.layoutType]?.bg, layoutTypeConfig[f.layoutType]?.text]"
                  >
                    {{ layoutTypeConfig[f.layoutType]?.label }}
                  </span>
                </div>
                <div class="flex items-center gap-1">
                  <button
                    class="p-1 text-gray-400 hover:text-blue-500 dark:hover:text-blue-400 rounded"
                    title="编辑"
                    @click.stop="openEditModal(f)"
                  >
                    <Pencil class="w-3.5 h-3.5" />
                  </button>
                  <button
                    class="p-1 text-gray-400 hover:text-red-500 dark:hover:text-red-400 rounded"
                    title="删除"
                    @click.stop="confirmDelete(f)"
                  >
                    <Trash2 class="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
              <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-1">
                {{ f.description }}
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-400">
                {{ getChapterRef(f) }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Delete confirmation dialog -->
    <ConfirmDialog
      ref="confirmRef"
      :title="'删除伏笔'"
      :message="deletingForeshadowing ? `确定要删除伏笔「${deletingForeshadowing.title}」吗？` : ''"
      confirm-text="删除"
      cancel-text="取消"
      variant="danger"
      @confirm="handleDelete"
    />

    <!-- Add/Edit Modal -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition ease-out duration-200"
        enter-from-class="opacity-0"
        leave-active-class="transition ease-in duration-150"
        leave-to-class="opacity-0"
      >
        <div
          v-if="showModal"
          class="fixed inset-0 z-[60] flex items-center justify-center bg-black/50"
          @click.self="showModal = false"
        >
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
            <!-- Modal Header -->
            <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                {{ editingForeshadowing ? '编辑伏笔' : '添加伏笔' }}
              </h2>
              <button
                class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
                @click="showModal = false"
              >
                <X class="w-5 h-5" />
              </button>
            </div>

            <!-- Modal Body -->
            <div class="px-6 py-4 space-y-4 max-h-[60vh] overflow-y-auto">
              <!-- Title -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  伏笔标题 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="modalForm.title"
                  type="text"
                  placeholder="请输入伏笔标题"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p v-if="titleError" class="text-xs text-red-500 mt-1">{{ titleError }}</p>
              </div>

              <!-- Type + LayoutType side by side -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    伏笔类型
                  </label>
                  <select
                    v-model="modalForm.type"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    布局线类型
                  </label>
                  <select
                    v-model="modalForm.layoutType"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option :value="undefined">无</option>
                    <option v-for="opt in layoutTypeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                </div>
              </div>

              <!-- Description -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  描述
                </label>
                <textarea
                  v-model="modalForm.description"
                  rows="4"
                  placeholder="描述伏笔的内容..."
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                />
              </div>

              <!-- PlantedChapter + plannedCallbackVolume side by side -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    埋设章节
                  </label>
                  <input
                    v-model.number="modalForm.plantedChapter"
                    type="number"
                    :disabled="!!editingForeshadowing"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    计划回收分卷
                  </label>
                  <input
                    v-model.number="modalForm.plannedCallbackVolume"
                    type="number"
                    placeholder="选填"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>

              <!-- plannedCallbackChapter + Priority side by side -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    计划回收章节
                  </label>
                  <input
                    v-model.number="modalForm.plannedCallbackChapter"
                    type="number"
                    placeholder="选填"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    优先级
                  </label>
                  <input
                    v-model.number="modalForm.priority"
                    type="number"
                    min="1"
                    max="5"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>

              <!-- Notes -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  备注
                </label>
                <textarea
                  v-model="modalForm.notes"
                  rows="2"
                  placeholder="补充说明..."
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                />
              </div>
            </div>

            <!-- Modal Footer -->
            <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700">
              <button
                class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
                @click="showModal = false"
              >
                取消
              </button>
              <button
                class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-500 rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="modalSaving"
                @click="saveForeshadowing"
              >
                {{ modalSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
