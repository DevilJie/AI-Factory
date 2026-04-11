<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Loader2, ChevronDown, ChevronUp, Pencil, Trash2, X } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import {
  getForeshadowingList,
  createForeshadowing,
  updateForeshadowing,
  deleteForeshadowing
} from '@/api/foreshadowing'
import {
  type Foreshadowing,
  type ForeshadowingCreateRequest,
  type ForeshadowingUpdateRequest,
  type ForeshadowingType,
  type ForeshadowingLayoutType,
  type ForeshadowingStatus
} from '@/types/foreshadowing'
import { success, error } from '@/utils/toast'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'

const route = useRoute()
const projectId = computed(() => route.params.id as string)

// State
const loading = ref(false)
const foreshadowings = ref<Foreshadowing[]>([])
const searchKeyword = ref('')
const filterStatus = ref<ForeshadowingStatus | 'all'>('all')
const filterLayoutType = ref<ForeshadowingLayoutType | 'all'>('all')

// Modal state
const showModal = ref(false)
const editingItem = ref<Foreshadowing | null>(null)
const modalSaving = ref(false)
const titleError = ref('')

const modalForm = ref<{
  title: string
  type: ForeshadowingType
  description: string
  layoutType: ForeshadowingLayoutType | undefined
  plantedChapter: number | undefined
  plantedVolume: number | undefined
  plannedCallbackChapter: number | undefined
  plannedCallbackVolume: number | undefined
  priority: number
  notes: string
}>({
  title: '',
  type: 'character',
  description: '',
  layoutType: undefined,
  plantedChapter: undefined,
  plantedVolume: undefined,
  plannedCallbackChapter: undefined,
  plannedCallbackVolume: undefined,
  priority: 3,
  notes: ''
})

// Delete state
const confirmRef = ref<InstanceType<typeof ConfirmDialog> | null>(null)
const deletingItem = ref<Foreshadowing | null>(null)

// Config maps
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

const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'pending', label: '待埋设' },
  { value: 'in_progress', label: '进行中' },
  { value: 'completed', label: '已完成' }
]

// Grouped display
const groupedForeshadowings = computed(() => {
  let items = foreshadowings.value

  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    items = items.filter(f => f.title.toLowerCase().includes(kw) || (f.description && f.description.toLowerCase().includes(kw)))
  }

  if (filterStatus.value !== 'all') {
    items = items.filter(f => f.status === filterStatus.value)
  }

  if (filterLayoutType.value !== 'all') {
    items = items.filter(f => f.layoutType === filterLayoutType.value)
  }

  const groups: Record<string, Foreshadowing[]> = {
    pending: items.filter(f => f.status === 'pending'),
    in_progress: items.filter(f => f.status === 'in_progress'),
    completed: items.filter(f => f.status === 'completed')
  }

  return groups
})

const groupLabels: Record<string, string> = {
  pending: '待埋设',
  in_progress: '进行中',
  completed: '已完成'
}

// Data loading
const loadData = async () => {
  loading.value = true
  try {
    const data = await getForeshadowingList(projectId.value)
    foreshadowings.value = data || []
  } catch (e: any) {
    error('加载伏笔数据失败')
  } finally {
    loading.value = false
  }
}
onMounted(loadData)

// CRUD
const openAddModal = () => {
  editingItem.value = null
  modalForm.value = {
    title: '', type: 'character', description: '', layoutType: undefined,
    plantedChapter: undefined, plantedVolume: undefined,
    plannedCallbackChapter: undefined, plannedCallbackVolume: undefined,
    priority: 3, notes: ''
  }
  titleError.value = ''
  showModal.value = true
}

const openEditModal = (f: Foreshadowing) => {
  editingItem.value = f
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

const closeModal = () => {
  showModal.value = false
  editingItem.value = null
}

const saveItem = async () => {
  if (!modalForm.value.title.trim()) {
    titleError.value = '请输入伏笔标题'
    return
  }
  titleError.value = ''
  modalSaving.value = true

  try {
    if (editingItem.value) {
      const updateData: ForeshadowingUpdateRequest = {
        title: modalForm.value.title,
        type: modalForm.value.type,
        description: modalForm.value.description,
        layoutType: modalForm.value.layoutType,
        plannedCallbackChapter: modalForm.value.plannedCallbackChapter,
        plannedCallbackVolume: modalForm.value.plannedCallbackVolume,
        priority: modalForm.value.priority,
        notes: modalForm.value.notes || undefined
      }
      await updateForeshadowing(projectId.value, editingItem.value.id, updateData)
      success('伏笔已更新')
    } else {
      const createData: ForeshadowingCreateRequest = {
        title: modalForm.value.title,
        type: modalForm.value.type,
        description: modalForm.value.description,
        layoutType: modalForm.value.layoutType,
        plantedChapter: modalForm.value.plantedChapter || 1,
        plantedVolume: modalForm.value.plantedVolume,
        plannedCallbackChapter: modalForm.value.plannedCallbackChapter,
        plannedCallbackVolume: modalForm.value.plannedCallbackVolume,
        priority: modalForm.value.priority,
        notes: modalForm.value.notes || undefined
      }
      await createForeshadowing(projectId.value, createData)
      success('伏笔已创建')
    }
    closeModal()
    loadData()
  } catch (e: any) {
    error(e?.message || '操作失败')
  } finally {
    modalSaving.value = false
  }
}

const confirmDelete = (f: Foreshadowing) => {
  deletingItem.value = f
  confirmRef.value?.show()
}

const handleDelete = async () => {
  if (!deletingItem.value) return
  try {
    await deleteForeshadowing(projectId.value, deletingItem.value.id)
    success('伏笔已删除')
    loadData()
  } catch (e: any) {
    error(e?.message || '删除失败')
  } finally {
    deletingItem.value = null
  }
}

const getChapterRef = (f: Foreshadowing): string => {
  if (f.status === 'completed' && f.actualCallbackChapter) {
    return `第${f.plantedChapter}章埋设 -> 第${f.actualCallbackChapter}章回收`
  }
  if (f.plannedCallbackChapter) {
    return `第${f.plantedChapter}章埋设 -> 第${f.plannedCallbackChapter}章回收`
  }
  return `第${f.plantedChapter}章埋设`
}
</script>

<template>
  <div class="p-6 max-w-5xl mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">伏笔管理</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">管理小说中的伏笔埋设与回收</p>
      </div>
      <button
        class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium"
        @click="openAddModal"
      >
        <Plus class="w-4 h-4" />
        添加伏笔
      </button>
    </div>

    <!-- Filters -->
    <div class="flex items-center gap-3 mb-6">
      <div class="relative flex-1 max-w-xs">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          v-model="searchKeyword"
          placeholder="搜索伏笔..."
          class="w-full pl-9 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>
      <select
        v-model="filterStatus"
        class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
      >
        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <select
        v-model="filterLayoutType"
        class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
      >
        <option value="all">全部布局线</option>
        <option v-for="opt in layoutTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <Loader2 class="w-6 h-6 animate-spin text-blue-500" />
    </div>

    <!-- Empty -->
    <div v-else-if="foreshadowings.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
      <p class="text-lg mb-2">暂无伏笔</p>
      <p class="text-sm">点击"添加伏笔"开始管理小说中的伏笔线索</p>
    </div>

    <!-- Content grouped by status -->
    <div v-else class="space-y-8">
      <div v-for="(group, status) in groupedForeshadowings" :key="status">
        <div class="flex items-center gap-2 mb-3">
          <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-300">{{ groupLabels[status] }}</h2>
          <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 px-1.5 py-0.5 rounded-full">{{ group.length }}</span>
        </div>

        <div v-if="group.length === 0" class="text-sm text-gray-400 dark:text-gray-500 pl-2">
          无匹配伏笔
        </div>

        <div v-else class="grid gap-3">
          <div
            v-for="f in group"
            :key="f.id"
            :class="[
              'rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden border-l-[3px]',
              layoutTypeConfig[f.layoutType || 'bright1']?.border || 'border-l-gray-400',
              f.status === 'completed' ? 'opacity-75' : ''
            ]"
          >
            <div class="p-4">
              <div class="flex items-start justify-between gap-2">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-base font-medium text-gray-900 dark:text-white">{{ f.title }}</span>
                    <span :class="['text-xs px-1.5 py-0.5 rounded-full', statusConfig[f.status]?.bg, statusConfig[f.status]?.text]">
                      {{ statusConfig[f.status]?.label }}
                    </span>
                  </div>
                  <div class="flex items-center gap-2 mb-2">
                    <span :class="['text-xs px-1.5 py-0.5 rounded-full', typeConfig[f.type]?.bg, typeConfig[f.type]?.text]">
                      {{ typeConfig[f.type]?.label }}
                    </span>
                    <span v-if="f.layoutType" :class="['text-xs px-1.5 py-0.5 rounded-full', layoutTypeConfig[f.layoutType]?.bg, layoutTypeConfig[f.layoutType]?.text]">
                      {{ layoutTypeConfig[f.layoutType]?.label }}
                    </span>
                  </div>
                  <p v-if="f.description" class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-1">{{ f.description }}</p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">{{ getChapterRef(f) }}</p>
                </div>
                <div class="flex items-center gap-1">
                  <button
                    class="p-1.5 text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors rounded"
                    @click="openEditModal(f)"
                  >
                    <Pencil class="w-4 h-4" />
                  </button>
                  <button
                    class="p-1.5 text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors rounded"
                    @click="confirmDelete(f)"
                  >
                    <Trash2 class="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center" @click.self="closeModal">
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md mx-4">
          <!-- Header -->
          <div class="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">{{ editingItem ? '编辑伏笔' : '添加伏笔' }}</h3>
            <button class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" @click="closeModal">
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Body -->
          <div class="p-4 space-y-4 max-h-[70vh] overflow-y-auto">
            <!-- Title -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">标题 <span class="text-red-500">*</span></label>
              <input
                v-model="modalForm.title"
                placeholder="伏笔标题"
                class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
              />
              <p v-if="titleError" class="text-xs text-red-500 mt-1">{{ titleError }}</p>
            </div>

            <!-- Type + LayoutType -->
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">类型</label>
                <select
                  v-model="modalForm.type"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
                >
                  <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">布局线</label>
                <select
                  v-model="modalForm.layoutType"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
                >
                  <option :value="undefined">未设定</option>
                  <option v-for="opt in layoutTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
            </div>

            <!-- Description -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">描述</label>
              <textarea
                v-model="modalForm.description"
                rows="4"
                placeholder="伏笔描述"
                class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm resize-none"
              />
            </div>

            <!-- Planted chapter/volume -->
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">埋设章节</label>
                <input
                  v-model.number="modalForm.plantedChapter"
                  type="number"
                  min="1"
                  :disabled="!!editingItem"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">埋设分卷</label>
                <input
                  v-model.number="modalForm.plantedVolume"
                  type="number"
                  min="1"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <!-- Callback chapter/volume -->
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">回收分卷</label>
                <input
                  v-model.number="modalForm.plannedCallbackVolume"
                  type="number"
                  min="1"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">回收章节</label>
                <input
                  v-model.number="modalForm.plannedCallbackChapter"
                  type="number"
                  min="1"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <!-- Priority + Notes -->
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">优先级</label>
                <input
                  v-model.number="modalForm.priority"
                  type="number"
                  min="1"
                  max="5"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">备注</label>
                <input
                  v-model="modalForm.notes"
                  placeholder="备注"
                  class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="flex items-center justify-end gap-2 p-4 border-t border-gray-200 dark:border-gray-700">
            <button
              class="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              @click="closeModal"
            >
              取消
            </button>
            <button
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
              :disabled="modalSaving"
              @click="saveItem"
            >
              {{ modalSaving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <ConfirmDialog ref="confirmRef" @confirm="handleDelete" />
  </div>
</template>
