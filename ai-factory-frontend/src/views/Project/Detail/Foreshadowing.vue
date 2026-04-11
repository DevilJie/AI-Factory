<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Plus, Search, Loader2, Pencil, Trash2, X, BookOpen } from 'lucide-vue-next'
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
const layoutTypeConfig: Record<string, { border: string; bg: string; text: string; label: string; avatar: string }> = {
  bright1: { border: 'border-l-blue-500', bg: 'bg-blue-50 dark:bg-blue-900/20', text: 'text-blue-700 dark:text-blue-300', label: '明线1', avatar: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400' },
  bright2: { border: 'border-l-green-500', bg: 'bg-green-50 dark:bg-green-900/20', text: 'text-green-700 dark:text-green-300', label: '明线2', avatar: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
  bright3: { border: 'border-l-yellow-500', bg: 'bg-yellow-50 dark:bg-yellow-900/20', text: 'text-yellow-700 dark:text-yellow-300', label: '明线3', avatar: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' },
  dark: { border: 'border-l-purple-500', bg: 'bg-purple-50 dark:bg-purple-900/20', text: 'text-purple-700 dark:text-purple-300', label: '暗线', avatar: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400' }
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

const statusTabs = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '待埋设' },
  { value: 'in_progress', label: '进行中' },
  { value: 'completed', label: '已完成' }
]

// Filtered list
const filteredForeshadowings = computed(() => {
  let items = foreshadowings.value

  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    items = items.filter(f => f.title.toLowerCase().includes(kw) || (f.description && f.description.toLowerCase().includes(kw)))
  }

  if (filterStatus.value !== 'all') {
    items = items.filter(f => f.status === filterStatus.value)
  }

  return items
})

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

watch(() => route.params.id, () => {
  if (route.params.id) loadData()
})

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
</script>

<template>
  <div class="h-full flex flex-col bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <div class="flex-shrink-0 px-6 py-4 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-indigo-100 dark:bg-indigo-900/30 flex items-center justify-center">
            <BookOpen class="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
          </div>
          <div>
            <h1 class="text-lg font-semibold text-gray-900 dark:text-white">伏笔管理</h1>
            <p class="text-sm text-gray-500 dark:text-gray-400">管理小说中的伏笔埋设与回收</p>
          </div>
        </div>
        <button
          @click="openAddModal"
          class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus class="w-4 h-4" />
          添加伏笔
        </button>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="flex-shrink-0 px-6 py-3 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div class="flex items-center gap-4">
        <!-- Search -->
        <div class="relative flex-1 max-w-xs">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索伏笔..."
            class="w-full pl-10 pr-4 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <!-- Status Tabs -->
        <div class="flex items-center gap-2">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            @click="filterStatus = tab.value as ForeshadowingStatus | 'all'"
            :class="[
              'px-3 py-1.5 text-sm font-medium rounded-lg transition-colors',
              filterStatus === tab.value
                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
            ]"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-6">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center h-full">
        <Loader2 class="w-8 h-8 text-blue-500 animate-spin" />
      </div>

      <!-- Empty -->
      <div v-else-if="filteredForeshadowings.length === 0" class="flex flex-col items-center justify-center h-full">
        <div class="w-20 h-20 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center mb-4">
          <BookOpen class="w-10 h-10 text-gray-400" />
        </div>
        <p class="text-gray-500 dark:text-gray-400 mb-2">暂无伏笔</p>
        <p class="text-sm text-gray-400 dark:text-gray-500">点击"添加伏笔"按钮创建伏笔</p>
      </div>

      <!-- Card Grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <div
          v-for="f in filteredForeshadowings"
          :key="f.id"
          class="group bg-white dark:bg-gray-800 rounded-xl shadow-sm hover:shadow-md transition-all border border-gray-100 dark:border-gray-700 overflow-hidden border-l-[3px]"
          :class="[
            layoutTypeConfig[f.layoutType || 'bright1']?.border || 'border-l-gray-400',
            f.status === 'completed' ? 'opacity-70' : ''
          ]"
        >
          <!-- Card Header: Layout Icon + Badges -->
          <div class="relative px-4 pt-4 pb-2">
            <!-- Actions -->
            <div class="absolute top-3 right-3 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                @click="openEditModal(f)"
                class="p-1.5 text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                title="编辑"
              >
                <Pencil class="w-4 h-4" />
              </button>
              <button
                @click="confirmDelete(f)"
                class="p-1.5 text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                title="删除"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            </div>

            <div class="flex items-center gap-2 flex-wrap">
              <span :class="['text-xs font-medium px-2 py-0.5 rounded-full', statusConfig[f.status]?.bg, statusConfig[f.status]?.text]">
                {{ statusConfig[f.status]?.label }}
              </span>
              <span :class="['text-xs px-1.5 py-0.5 rounded-full', typeConfig[f.type]?.bg, typeConfig[f.type]?.text]">
                {{ typeConfig[f.type]?.label }}
              </span>
              <span v-if="f.layoutType" :class="['text-xs px-1.5 py-0.5 rounded-full', layoutTypeConfig[f.layoutType]?.bg, layoutTypeConfig[f.layoutType]?.text]">
                {{ layoutTypeConfig[f.layoutType]?.label }}
              </span>
            </div>
          </div>

          <!-- Card Body -->
          <div class="px-4 pb-4">
            <h3 class="text-sm font-medium text-gray-900 dark:text-white mb-1 line-clamp-1">{{ f.title }}</h3>
            <p v-if="f.description" class="text-xs text-gray-500 dark:text-gray-400 line-clamp-2 mb-2">{{ f.description }}</p>
            <p class="text-xs text-gray-400 dark:text-gray-500">{{ getChapterRef(f) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click.self="closeModal"
    >
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
        <!-- Modal Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">{{ editingItem ? '编辑伏笔' : '添加伏笔' }}</h2>
          <button
            @click="closeModal"
            class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- Modal Body -->
        <div class="px-6 py-4 space-y-4 max-h-[60vh] overflow-y-auto">
          <!-- Title -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              标题 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="modalForm.title"
              type="text"
              placeholder="请输入伏笔标题"
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p v-if="titleError" class="text-xs text-red-500 mt-1">{{ titleError }}</p>
          </div>

          <!-- Type -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">类型</label>
            <div class="grid grid-cols-4 gap-2">
              <button
                v-for="opt in typeOptions"
                :key="opt.value"
                @click="modalForm.type = opt.value as ForeshadowingType"
                :class="[
                  'px-3 py-2 text-sm font-medium rounded-lg border transition-colors',
                  modalForm.type === opt.value
                    ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
                    : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'
                ]"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- Layout Line -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">布局线</label>
            <div class="grid grid-cols-4 gap-2">
              <button
                v-for="opt in layoutTypeOptions"
                :key="opt.value"
                @click="modalForm.layoutType = opt.value as ForeshadowingLayoutType"
                :class="[
                  'px-3 py-2 text-sm font-medium rounded-lg border transition-colors',
                  modalForm.layoutType === opt.value
                    ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
                    : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'
                ]"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- Description -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">描述</label>
            <textarea
              v-model="modalForm.description"
              rows="3"
              placeholder="描述伏笔内容和埋设方式..."
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            ></textarea>
          </div>

          <!-- Planted Volume + Chapter -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">埋设分卷</label>
              <input
                v-model.number="modalForm.plantedVolume"
                type="number"
                min="1"
                placeholder="分卷号"
                class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">埋设章节</label>
              <input
                v-model.number="modalForm.plantedChapter"
                type="number"
                min="1"
                placeholder="章节号"
                :disabled="!!editingItem"
                class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </div>
          </div>

          <!-- Callback Volume + Chapter -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">回收分卷</label>
              <input
                v-model.number="modalForm.plannedCallbackVolume"
                type="number"
                min="1"
                placeholder="分卷号"
                class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">回收章节</label>
              <input
                v-model.number="modalForm.plannedCallbackChapter"
                type="number"
                min="1"
                placeholder="章节号"
                class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          <!-- Priority -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">优先级</label>
            <div class="grid grid-cols-5 gap-2">
              <button
                v-for="n in 5"
                :key="n"
                @click="modalForm.priority = n"
                :class="[
                  'px-3 py-2 text-sm font-medium rounded-lg border transition-colors',
                  modalForm.priority === n
                    ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
                    : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'
                ]"
              >
                {{ n }}
              </button>
            </div>
          </div>

          <!-- Notes -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">备注</label>
            <textarea
              v-model="modalForm.notes"
              rows="2"
              placeholder="备注信息..."
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            ></textarea>
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <button
            @click="closeModal"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            取消
          </button>
          <button
            @click="saveItem"
            :disabled="modalSaving"
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            <Loader2 v-if="modalSaving" class="w-4 h-4 animate-spin" />
            {{ editingItem ? '保存' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <ConfirmDialog ref="confirmRef" @confirm="handleDelete" />
  </div>
</template>
