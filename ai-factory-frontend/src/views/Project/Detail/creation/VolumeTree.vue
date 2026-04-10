<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import {
  ChevronRight,
  ChevronDown,
  FolderOpen,
  FileText,
  Plus,
  Loader2,
  Sparkles,
  RefreshCw
} from 'lucide-vue-next'
import { getVolumeList, getChapterPlans } from '@/api/chapter'
import { getOutline, generateOutlineAsync, generateChapters, type GenerateOutlineRequest } from '@/api/outline'
import { getProjectTasks, getTaskStatus, type TaskDto } from '@/api/task'
import type { Volume, Chapter } from '@/types/project'
import { success, error } from '@/utils/toast'
import { useEditorStore } from '@/stores/editor'

interface VolumeWithChapters extends Volume {
  chapters: Chapter[]
}

// 任务类型
type TaskType = 'outline' | 'chapter'

// 任务状态存储键
const getTaskStorageKey = (projectId: string, taskType: TaskType) =>
  `ai-factory-task-${projectId}-${taskType}`

const props = defineProps<{
  projectId: string
  selectedChapterId?: string | null
}>()

const emit = defineEmits<{
  selectChapter: [chapter: Chapter]
  selectVolume: [volume: VolumeWithChapters]
  addVolume: []
}>()

// State
const volumes = ref<VolumeWithChapters[]>([])
const expandedVolumes = ref<Set<string>>(new Set())
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const showGenerateModal = ref(false)

// 任务相关状态
const generating = ref(false)
const generatingChapters = ref<string | null>(null)
const taskProgress = ref(0)
const taskMessage = ref('')
const currentTaskId = ref<string | null>(null)
const currentTaskType = ref<TaskType | null>(null)
let taskPollingInterval: ReturnType<typeof setInterval> | null = null

// 生成配置
const generateConfig = ref<GenerateOutlineRequest>({
  targetVolumeCount: 5,
  avgWordsPerVolume: 100000,
  additionalRequirements: ''
})

// Computed
const hasVolumes = computed(() => volumes.value.length > 0)

// Methods
const fetchData = async () => {
  if (!props.projectId) return

  loading.value = true
  errorMessage.value = null
  try {
    // 获取分卷列表
    const volumeList = await getVolumeList(props.projectId)

    // 获取章节规划列表
    const chapterPlans = await getChapterPlans(props.projectId)

    // 按分卷ID分组章节（兼容 volumeId 和 volumePlanId 两种字段名）
    const chaptersByVolume: Record<string, Chapter[]> = {}
    if (chapterPlans && Array.isArray(chapterPlans)) {
      for (const chapter of chapterPlans) {
        // 兼容不同的字段名
        const volId = (chapter as any).volumeId || (chapter as any).volumePlanId || 'default'
        if (!chaptersByVolume[volId]) {
          chaptersByVolume[volId] = []
        }
        chaptersByVolume[volId].push(chapter)
      }
    }

    // 组装分卷和章节数据
    volumes.value = (volumeList || []).map(v => ({
      ...v,
      chapters: chaptersByVolume[v.id] || []
    }))

    // 如果有分卷，默认展开第一个
    const firstVolume = volumes.value[0]
    if (firstVolume) {
      expandedVolumes.value.add(firstVolume.id)
    }
  } catch (e: any) {
    errorMessage.value = e.message || '加载数据失败'
    console.error('Failed to load data:', e)
  } finally {
    loading.value = false
  }
}

const toggleVolume = (volumeId: string, volume?: VolumeWithChapters) => {
  // 手风琴模式：展开一个时收起其他
  if (expandedVolumes.value.has(volumeId)) {
    // 如果已经展开，则收起
    expandedVolumes.value.delete(volumeId)
  } else {
    // 否则展开当前，收起其他
    expandedVolumes.value.clear()
    expandedVolumes.value.add(volumeId)
  }
  // 发送选中分卷事件
  if (volume) {
    emit('selectVolume', volume)
  }
}

const isExpanded = (volumeId: string) => expandedVolumes.value.has(volumeId)

const isSelected = (chapterId: string) => props.selectedChapterId === chapterId

const editorStore = useEditorStore()

const handleChapterClick = async (chapter: Chapter) => {
  // 设置章节规划信息（无论是否有内容都需要设置，用于显示标题等信息）
  // 注意：title 可能来自 chapterTitle 或 title 字段
  // 兼容 ChapterPlanDto 的 hasContent 字段和 Chapter 的 content 字段
  const hasContent = (chapter as any).hasContent !== undefined
    ? (chapter as any).hasContent
    : !!chapter.content

  editorStore.setChapterPlan({
    id: chapter.id,
    volumeId: chapter.volumeId,
    volumePlanId: (chapter as any).volumePlanId,
    projectId: chapter.projectId,
    chapterNumber: chapter.chapterNumber || 1,
    title: chapter.chapterTitle || chapter.title || '',
    plotOutline: (chapter as any).plotOutline || chapter.plotOutline,
    chapterStartingScene: (chapter as any).chapterStartingScene,
    chapterEndingScene: (chapter as any).chapterEndingScene,
    keyEvents: chapter.keyEvents,
    chapterGoal: (chapter as any).chapterGoal || chapter.chapterGoal,
    wordCountTarget: (chapter as any).wordCountTarget || chapter.wordCountTarget,
    chapterNotes: (chapter as any).chapterNotes || chapter.chapterNotes,
    status: (chapter as any).status,
    plotStage: (chapter as any).plotStage,
    plannedCharacters: (chapter as any).plannedCharacters,
    characterArcs: (chapter as any).characterArcs,
    hasContent,
    chapterId: (chapter as any).chapterId,
    wordCount: (chapter as any).wordCount,
  })

  // 尝试通过规划ID加载章节（无论是否有内容都尝试加载，让 store 处理）
  await editorStore.loadChapterByPlan(chapter.id)
  emit('selectChapter', chapter)
}

// 任务轮询相关
const startTaskPolling = (taskId: string, taskType: TaskType, volumeId?: string) => {
  // 停止之前的轮询
  stopTaskPolling()

  currentTaskId.value = taskId
  currentTaskType.value = taskType
  taskProgress.value = 0
  taskMessage.value = '任务已启动...'

  // 保存任务状态到 localStorage
  localStorage.setItem(
    getTaskStorageKey(props.projectId, taskType),
    JSON.stringify({ taskId, volumeId })
  )

  // 设置生成状态
  if (taskType === 'outline') {
    generating.value = true
  } else if (taskType === 'chapter' && volumeId) {
    generatingChapters.value = volumeId
  }

  // 开始轮询
  taskPollingInterval = setInterval(async () => {
    try {
      const task = await getTaskStatus(taskId)
      taskProgress.value = task.progress || 0
      taskMessage.value = task.currentStep || '处理中...'

      if (task.status === 'completed') {
        success(`${taskType === 'outline' ? '分卷规划' : '章节规划'}生成完成！`)
        await handleTaskComplete(taskType)
      } else if (task.status === 'failed') {
        error(task.errorMessage || '任务执行失败')
        handleTaskEnd(taskType)
      } else if (task.status === 'cancelled') {
        error('任务已取消')
        handleTaskEnd(taskType)
      } else if (taskType === 'outline') {
        // 逐卷生成时实时刷新分卷列表，让用户看到分卷一个个出现
        await fetchData()
      }
    } catch (e: any) {
      console.error('Failed to poll task status:', e)
    }
  }, 3000) // 每3秒轮询一次
}

const handleTaskComplete = async (taskType: TaskType) => {
  stopTaskPolling()
  clearTaskStorage(taskType)

  // 刷新数据
  await fetchData()

  // 重置状态
  if (taskType === 'outline') {
    generating.value = false
  } else {
    generatingChapters.value = null
  }

  currentTaskId.value = null
  currentTaskType.value = null
  taskProgress.value = 100
  taskMessage.value = '完成'
}

const handleTaskEnd = (taskType: TaskType) => {
  stopTaskPolling()
  clearTaskStorage(taskType)

  if (taskType === 'outline') {
    generating.value = false
  } else {
    generatingChapters.value = null
  }

  currentTaskId.value = null
  currentTaskType.value = null
  taskProgress.value = 0
  taskMessage.value = ''
}

const stopTaskPolling = () => {
  if (taskPollingInterval) {
    clearInterval(taskPollingInterval)
    taskPollingInterval = null
  }
}

const clearTaskStorage = (taskType: TaskType) => {
  localStorage.removeItem(getTaskStorageKey(props.projectId, taskType))
}

// 恢复任务状态（页面刷新后）
const restoreTaskState = async () => {
  try {
    // 检查是否有运行中的大纲任务
    const outlineTaskData = localStorage.getItem(getTaskStorageKey(props.projectId, 'outline'))
    if (outlineTaskData) {
      const { taskId } = JSON.parse(outlineTaskData)
      const task = await getTaskStatus(taskId)
      if (task.status === 'running' || task.status === 'pending') {
        startTaskPolling(taskId, 'outline')
        return
      } else if (task.status === 'completed') {
        clearTaskStorage('outline')
      }
    }

    // 检查是否有运行中的章节任务
    const chapterTaskData = localStorage.getItem(getTaskStorageKey(props.projectId, 'chapter'))
    if (chapterTaskData) {
      const { taskId, volumeId } = JSON.parse(chapterTaskData)
      const task = await getTaskStatus(taskId)
      if (task.status === 'running' || task.status === 'pending') {
        if (volumeId) {
          startTaskPolling(taskId, 'chapter', volumeId)
        }
        return
      } else if (task.status === 'completed') {
        clearTaskStorage('chapter')
      }
    }
  } catch (e) {
    console.error('Failed to restore task state:', e)
  }
}

// AI生成分卷规划
const handleGenerateOutline = async () => {
  if (!props.projectId) return

  generating.value = true
  showGenerateModal.value = false
  try {
    const result = await generateOutlineAsync(props.projectId, generateConfig.value)
    success(result.message || 'AI生成任务已启动，正在生成中...')

    // 开始任务轮询
    if (result.taskId) {
      startTaskPolling(result.taskId, 'outline')
    }
  } catch (e: any) {
    error(e.message || 'AI生成失败')
    console.error('Failed to generate outline:', e)
    generating.value = false
  }
}

// AI生成章节规划
const handleGenerateChapters = async (volume: VolumeWithChapters) => {
  if (!props.projectId) return

  generatingChapters.value = volume.id
  try {
    const result = await generateChapters(props.projectId, volume.id, 'introduction')
    success(result.message || 'AI生成章节规划任务已启动，正在生成中...')

    // 开始任务轮询
    if (result.taskId) {
      startTaskPolling(result.taskId, 'chapter', volume.id)
    }
  } catch (e: any) {
    error(e.message || 'AI生成章节规划失败')
    console.error('Failed to generate chapters:', e)
    generatingChapters.value = null
  }
}

// Lifecycle
onMounted(async () => {
  await fetchData()
  // 恢复任务状态
  await restoreTaskState()
})

onUnmounted(() => {
  stopTaskPolling()
})

// Watch for project changes
watch(() => props.projectId, async () => {
  stopTaskPolling()
  await fetchData()
  await restoreTaskState()
})
</script>

<template>
  <div class="h-full flex flex-col bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700">
    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700">
      <div class="flex items-center gap-2">
        <h3 class="text-sm font-medium text-gray-900 dark:text-white">章节目录</h3>
        <span v-if="generating || generatingChapters" class="whitespace-nowrap flex items-center gap-1 px-2 py-0.5 text-xs font-medium text-purple-600 dark:text-purple-400 bg-purple-100 dark:bg-purple-900/30 rounded-full">
          <span class="w-1.5 h-1.5 bg-purple-500 rounded-full animate-pulse"></span>
          生成中 {{ taskProgress > 0 ? `${taskProgress}%` : '' }}
        </span>
      </div>
      <div class="flex items-center gap-1">
        <button
          class="p-1 text-purple-500 hover:text-purple-700 dark:text-purple-400 dark:hover:text-purple-300 rounded hover:bg-purple-50 dark:hover:bg-purple-900/20"
          :class="{ 'animate-pulse': generating }"
          @click="showGenerateModal = true"
          :disabled="generating"
          title="AI生成分卷"
        >
          <Sparkles class="w-4 h-4" />
        </button>
        <button
          class="p-1 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded hover:bg-gray-100 dark:hover:bg-gray-700"
          @click="fetchData"
          :disabled="loading"
          title="刷新"
        >
          <RefreshCw :class="['w-4 h-4', { 'animate-spin': loading }]" />
        </button>
        <button
          class="p-1 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded hover:bg-gray-100 dark:hover:bg-gray-700"
          @click="emit('addVolume')"
          title="新增分卷"
        >
          <Plus class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto">
      <!-- Loading State -->
      <div v-if="loading" class="flex items-center justify-center py-8">
        <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
      </div>

      <!-- Error State -->
      <div v-else-if="errorMessage" class="px-4 py-8 text-center text-sm text-red-500">
        {{ errorMessage }}
      </div>

      <!-- Generating State (only when no volumes yet) -->
      <div v-else-if="generating && !hasVolumes" class="px-4 py-8 text-center">
        <div class="relative w-16 h-16 mx-auto mb-4">
          <!-- 外圈旋转动画 -->
          <div class="absolute inset-0 rounded-full border-2 border-purple-200 dark:border-purple-900/50"></div>
          <div class="absolute inset-0 rounded-full border-2 border-transparent border-t-purple-500 animate-spin"></div>
          <!-- 中心图标 -->
          <div class="absolute inset-0 flex items-center justify-center">
            <Sparkles class="w-6 h-6 text-purple-500 animate-pulse" />
          </div>
        </div>
        <p class="text-sm font-medium text-purple-600 dark:text-purple-400 mb-1">AI正在生成分卷规划</p>
        <p class="text-xs text-gray-400 dark:text-gray-500 mb-2">{{ taskMessage || '请稍候，这可能需要一些时间...' }}</p>
        <!-- 进度条 -->
        <div v-if="taskProgress > 0" class="w-32 mx-auto">
          <div class="h-1.5 bg-purple-100 dark:bg-purple-900/30 rounded-full overflow-hidden">
            <div
              class="h-full bg-purple-500 rounded-full transition-all duration-300"
              :style="{ width: `${taskProgress}%` }"
            ></div>
          </div>
          <p class="text-xs text-purple-500 mt-1">{{ taskProgress }}%</p>
        </div>
        <!-- 进度指示点 -->
        <div v-else class="flex items-center justify-center gap-1 mt-4">
          <span class="w-1.5 h-1.5 bg-purple-500 rounded-full animate-bounce" style="animation-delay: 0ms"></span>
          <span class="w-1.5 h-1.5 bg-purple-500 rounded-full animate-bounce" style="animation-delay: 150ms"></span>
          <span class="w-1.5 h-1.5 bg-purple-500 rounded-full animate-bounce" style="animation-delay: 300ms"></span>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!hasVolumes" class="px-4 py-8 text-center">
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-3">暂无分卷规划</p>
        <button
          @click="showGenerateModal = true"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-900/20 rounded-lg hover:bg-purple-100 dark:hover:bg-purple-900/30 transition-colors"
        >
          <Sparkles class="w-3.5 h-3.5" />
          AI生成分卷
        </button>
      </div>

      <!-- Volume List -->
      <div v-else class="py-2">
        <div
          v-for="volume in volumes"
          :key="volume.id"
          class="select-none"
        >
          <!-- Volume Header -->
          <div
            class="flex items-center px-4 py-2 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700"
            @click="toggleVolume(volume.id, volume)"
          >
            <ChevronRight
              v-if="!isExpanded(volume.id)"
              class="w-4 h-4 text-gray-400 mr-1 flex-shrink-0"
            />
            <ChevronDown
              v-else
              class="w-4 h-4 text-gray-400 mr-1 flex-shrink-0"
            />
            <FolderOpen class="w-4 h-4 text-yellow-500 mr-2 flex-shrink-0" />
            <span class="text-sm text-gray-900 dark:text-white truncate">{{ volume.volumeTitle || volume.name || volume.title || '未命名分卷' }}</span>
            <span class="ml-auto text-xs text-gray-400">{{ volume.chapterCount }}</span>
          </div>

          <!-- Chapter List -->
          <div v-show="isExpanded(volume.id)" class="ml-4">
            <!-- 章节生成中状态（顶部提示） -->
            <div v-if="generatingChapters === volume.id" class="px-4 py-2 bg-purple-50 dark:bg-purple-900/20 border-b border-purple-100 dark:border-purple-800">
              <div class="flex items-center gap-2">
                <Loader2 class="w-3.5 h-3.5 text-purple-500 animate-spin" />
                <span class="text-xs text-purple-600 dark:text-purple-400 font-medium">
                  AI正在生成章节规划 {{ taskProgress > 0 ? `${taskProgress}%` : '' }}
                </span>
              </div>
              <p class="text-xs text-gray-400 mt-1">{{ taskMessage || '请稍候...' }}</p>
              <!-- 进度条 -->
              <div v-if="taskProgress > 0" class="mt-2 h-1 bg-purple-100 dark:bg-purple-900/30 rounded-full overflow-hidden">
                <div
                  class="h-full bg-purple-500 rounded-full transition-all duration-300"
                  :style="{ width: `${taskProgress}%` }"
                ></div>
              </div>
            </div>
            <!-- 章节列表（包括生成中和已有章节） -->
            <div
              v-for="chapter in volume.chapters"
              :key="chapter.id"
              class="flex items-center px-4 py-2 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700"
              :class="{
                'bg-blue-50 dark:bg-blue-900/20': isSelected(chapter.id)
              }"
              @click="handleChapterClick(chapter)"
            >
              <FileText class="w-4 h-4 text-gray-400 mr-2 flex-shrink-0" />
              <span
                class="text-sm truncate"
                :class="isSelected(chapter.id) ? 'text-blue-600 dark:text-blue-400 font-medium' : 'text-gray-700 dark:text-gray-300'"
              >
                第{{chapter.chapterNumber}}章：{{ chapter.chapterTitle || chapter.title || '未命名章节' }}
              </span>
            </div>
            <!-- 暂无章节 -->
            <div v-if="volume.chapters.length === 0 && generatingChapters !== volume.id" class="px-4 py-3">
              <p class="text-xs text-gray-400 mb-2">暂无章节</p>
              <button
                @click.stop="handleGenerateChapters(volume)"
                :disabled="generatingChapters !== null"
                class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-900/20 rounded-lg hover:bg-purple-100 dark:hover:bg-purple-900/30 transition-colors disabled:opacity-50"
              >
                <Loader2 v-if="generatingChapters === volume.id" class="w-3.5 h-3.5 animate-spin" />
                <Sparkles v-else class="w-3.5 h-3.5" />
                AI生成章节规划
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI Generate Modal -->
    <div
      v-if="showGenerateModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click.self="showGenerateModal = false"
    >
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
        <div class="flex items-center justify-between px-5 py-4 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-base font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <Sparkles class="w-5 h-5 text-purple-500" />
            AI生成分卷规划
          </h3>
        </div>

        <div class="px-5 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              目标分卷数
            </label>
            <input
              v-model.number="generateConfig.targetVolumeCount"
              type="number"
              min="1"
              max="20"
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              每卷平均字数
            </label>
            <input
              v-model.number="generateConfig.avgWordsPerVolume"
              type="number"
              min="10000"
              step="10000"
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              额外要求
            </label>
            <textarea
              v-model="generateConfig.additionalRequirements"
              rows="3"
              placeholder="如有特殊要求，请在此描述..."
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            ></textarea>
          </div>
        </div>

        <div class="flex items-center justify-end gap-3 px-5 py-4 border-t border-gray-200 dark:border-gray-700">
          <button
            @click="showGenerateModal = false"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleGenerateOutline"
            :disabled="generating"
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-purple-600 rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50"
          >
            <Loader2 v-if="generating" class="w-4 h-4 animate-spin" />
            <Sparkles v-else class="w-4 h-4" />
            开始生成
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
