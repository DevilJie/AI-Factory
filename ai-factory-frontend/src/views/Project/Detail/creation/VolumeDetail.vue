<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  Save,
  Loader2,
  BookOpen,
  FileText,
  Target,
  Lightbulb,
  Star,
  Flag,
  Clock,
  CheckCircle,
  Edit3,
  Sparkles,
  Lock,
  AlertCircle,
  Plus,
  X,
  ChevronDown,
  ChevronUp
} from 'lucide-vue-next'
import type { Volume } from '@/types/project'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  volume: Volume | null
  isOptimizing?: boolean  // 父组件控制的优化状态（用于页面刷新后恢复）
}>()

const emit = defineEmits<{
  save: [volume: Volume]
  optimize: [data: { targetChapterCount: number }]
}>()

// 关键事件5阶段结构
interface KeyEventsData {
  opening: string[]      // 开篇
  development: string[]  // 发展
  turning: string[]      // 转折
  climax: string[]       // 高潮
  ending: string[]       // 收尾
}

// 阶段配置
const stageConfig = [
  { key: 'opening', label: '开篇', color: 'blue', description: '故事起始，引入本卷主要人物和背景' },
  { key: 'development', label: '发展', color: 'green', description: '情节推进，矛盾逐渐展开' },
  { key: 'turning', label: '转折', color: 'yellow', description: '关键转折点，改变故事走向' },
  { key: 'climax', label: '高潮', color: 'red', description: '本卷最高潮，冲突最激烈' },
  { key: 'ending', label: '收尾', color: 'purple', description: '收束情节，为下一卷铺垫' }
] as const

// 编辑表单数据
const formData = ref({
  // 基础信息
  volumeTitle: '',
  targetChapterCount: null as number | null,
  // 核心设定
  volumeTheme: '',
  mainConflict: '',
  plotArc: '',
  coreGoal: '',
  // 关键事件（原始字符串，用于保存）
  keyEvents: '',
  // 高潮与收尾
  climax: '',
  ending: '',
  // 描述与备注
  volumeDescription: '',
  volumeNotes: '',
  // 时间线设定
  timelineSetting: '',
  // 状态
  status: 'planned' as 'planned' | 'in_progress' | 'completed',
  volumeCompleted: false
})

// 关键事件5阶段数据
const keyEventsData = ref<KeyEventsData>({
  opening: [],
  development: [],
  turning: [],
  climax: [],
  ending: []
})

// 展开状态
const expandedStages = ref<Set<string>>(new Set(['opening', 'development', 'turning', 'climax', 'ending']))

const saving = ref(false)
const localOptimizing = ref(false)

// 合并本地状态和父组件传入的状态
const optimizing = computed(() => localOptimizing.value || (props.isOptimizing ?? false))

// 锁定状态：当存在章节规划时锁定整个表单
const isLocked = computed(() => {
  return !!(props.volume && (props.volume.chapterCount ?? 0) > 0)
})

// 状态选项
const statusOptions = [
  { value: 'planned', label: '已规划' },
  { value: 'in_progress', label: '进行中' },
  { value: 'completed', label: '已完成' }
]

// 解析keyEvents JSON字符串
const parseKeyEvents = (keyEventsStr: string): KeyEventsData => {
  const defaultData: KeyEventsData = {
    opening: [],
    development: [],
    turning: [],
    climax: [],
    ending: []
  }

  if (!keyEventsStr || keyEventsStr.trim() === '') {
    return defaultData
  }

  try {
    const parsed = JSON.parse(keyEventsStr)
    // 验证并合并数据
    return {
      opening: Array.isArray(parsed.opening) ? parsed.opening : [],
      development: Array.isArray(parsed.development) ? parsed.development : [],
      turning: Array.isArray(parsed.turning) ? parsed.turning : [],
      climax: Array.isArray(parsed.climax) ? parsed.climax : [],
      ending: Array.isArray(parsed.ending) ? parsed.ending : []
    }
  } catch (e) {
    // 如果不是JSON格式，尝试按换行符分割作为开篇事件
    const lines = keyEventsStr.split('\n').filter(line => line.trim())
    if (lines.length > 0) {
      return { ...defaultData, opening: lines }
    }
    return defaultData
  }
}

// 将keyEvents对象转换为JSON字符串
const stringifyKeyEvents = (data: KeyEventsData): string => {
  return JSON.stringify(data)
}

// Watch for volume changes
watch(() => props.volume, (newVolume) => {
  if (newVolume) {
    const v = newVolume as any
    formData.value = {
      volumeTitle: v.volumeTitle || v.name || '',
      targetChapterCount: v.targetChapterCount || null,
      volumeTheme: v.volumeTheme || '',
      mainConflict: v.mainConflict || '',
      plotArc: v.plotArc || '',
      coreGoal: v.coreGoal || '',
      keyEvents: v.keyEvents || '',
      climax: v.climax || '',
      ending: v.ending || '',
      volumeDescription: v.volumeDescription || '',
      volumeNotes: v.volumeNotes || '',
      timelineSetting: v.timelineSetting || '',
      status: v.status || 'planned',
      volumeCompleted: v.volumeCompleted || false
    }
    // 解析keyEvents到5阶段数据
    keyEventsData.value = parseKeyEvents(v.keyEvents || '')
  }
}, { immediate: true })

// 切换阶段展开状态
const toggleStage = (stageKey: string) => {
  if (expandedStages.value.has(stageKey)) {
    expandedStages.value.delete(stageKey)
  } else {
    expandedStages.value.add(stageKey)
  }
}

// 添加事件
const addEvent = (stageKey: keyof KeyEventsData) => {
  keyEventsData.value[stageKey].push('')
}

// 删除事件
const removeEvent = (stageKey: keyof KeyEventsData, index: number) => {
  keyEventsData.value[stageKey].splice(index, 1)
}

// 获取阶段样式
const getStageColorClass = (color: string, type: 'bg' | 'text' | 'border') => {
  const colorMap: Record<string, Record<string, string>> = {
    blue: { bg: 'bg-blue-50 dark:bg-blue-900/20', text: 'text-blue-600 dark:text-blue-400', border: 'border-blue-200 dark:border-blue-800' },
    green: { bg: 'bg-green-50 dark:bg-green-900/20', text: 'text-green-600 dark:text-green-400', border: 'border-green-200 dark:border-green-800' },
    yellow: { bg: 'bg-yellow-50 dark:bg-yellow-900/20', text: 'text-yellow-600 dark:text-yellow-400', border: 'border-yellow-200 dark:border-yellow-800' },
    red: { bg: 'bg-red-50 dark:bg-red-900/20', text: 'text-red-600 dark:text-red-400', border: 'border-red-200 dark:border-red-800' },
    purple: { bg: 'bg-purple-50 dark:bg-purple-900/20', text: 'text-purple-600 dark:text-purple-400', border: 'border-purple-200 dark:border-purple-800' }
  }
  return colorMap[color]?.[type] || ''
}

// 保存
const handleSave = async () => {
  if (!props.volume) return

  saving.value = true
  try {
    // 将5阶段数据转换为JSON字符串
    const keyEventsStr = stringifyKeyEvents(keyEventsData.value)

    // 构建更新数据
    const updateData = {
      ...formData.value,
      keyEvents: keyEventsStr,
      id: props.volume.id,
      projectId: props.volume.projectId
    }
    emit('save', updateData as Volume)
  } catch (e: any) {
    error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// AI优化
const handleOptimize = async () => {
  if (!props.volume) return
  if (!formData.value.targetChapterCount || formData.value.targetChapterCount < 1) {
    error('请先设置目标章节数')
    return
  }

  localOptimizing.value = true
  emit('optimize', { targetChapterCount: formData.value.targetChapterCount })
}

// 重置优化状态（供父组件调用）
const resetOptimizing = () => {
  localOptimizing.value = false
}

// 暴露方法供父组件调用
defineExpose({
  resetOptimizing
})

// 获取状态样式
const getStatusStyle = (status: string) => {
  const styles: Record<string, string> = {
    planned: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    in_progress: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
    completed: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
  }
  return styles[status] || styles.planned
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    planned: '已规划',
    in_progress: '进行中',
    completed: '已完成'
  }
  return labels[status] || status
}
</script>

<template>
  <div v-if="volume" class="h-full flex flex-col bg-white dark:bg-gray-800 relative">
    <!-- AI优化遮罩层 -->
    <div
      v-if="optimizing"
      class="absolute inset-0 z-50 bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm flex flex-col items-center justify-center"
    >
      <div class="w-16 h-16 rounded-2xl bg-gradient-to-r from-purple-600 to-cyan-600 flex items-center justify-center mb-4 shadow-lg">
        <Loader2 class="w-8 h-8 text-white animate-spin" />
      </div>
      <p class="text-lg font-medium text-gray-900 dark:text-white">AI 正在优化分卷内容...</p>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">请稍候，这可能需要一些时间</p>
    </div>

    <!-- Header -->
    <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-purple-50 to-cyan-50 dark:from-purple-900/20 dark:to-cyan-900/20">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center">
          <BookOpen class="w-5 h-5 text-yellow-600 dark:text-yellow-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
            第 {{ (volume as any).volumeNumber || '?' }} 卷
          </h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">
            {{ formData.volumeTitle || '未命名分卷' }}
          </p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <!-- 状态标签 -->
        <span :class="['px-3 py-1 text-xs font-medium rounded-full', getStatusStyle(formData.status)]">
          {{ getStatusLabel(formData.status) }}
        </span>
        <!-- 完成状态 -->
        <span v-if="formData.volumeCompleted" class="flex items-center gap-1 px-3 py-1 text-xs font-medium text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20 rounded-full">
          <CheckCircle class="w-3.5 h-3.5" />
          已完成
        </span>
        <!-- 锁定状态标签 -->
        <span v-if="isLocked" class="flex items-center gap-1 px-3 py-1 text-xs font-medium text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 rounded-full">
          <Lock class="w-3.5 h-3.5" />
          已锁定
        </span>
        <!-- AI优化按钮 -->
        <button
          v-if="!isLocked"
          @click="handleOptimize"
          :disabled="optimizing || !formData.targetChapterCount"
          class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-gradient-to-r from-purple-600 to-cyan-600 rounded-lg hover:from-purple-700 hover:to-cyan-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Loader2 v-if="optimizing" class="w-4 h-4 animate-spin" />
          <Sparkles v-else class="w-4 h-4" />
          AI优化
        </button>
        <!-- 保存按钮 -->
        <button
          @click="handleSave"
          :disabled="saving || isLocked"
          class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Loader2 v-if="saving" class="w-4 h-4 animate-spin" />
          <Save v-else class="w-4 h-4" />
          保存
        </button>
      </div>
    </div>

    <!-- 锁定提示 -->
    <div v-if="isLocked" class="mx-6 mt-4 px-4 py-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg flex items-center gap-3">
      <AlertCircle class="w-5 h-5 text-amber-500 flex-shrink-0" />
      <div>
        <p class="text-sm font-medium text-amber-700 dark:text-amber-400">分卷已锁定</p>
        <p class="text-xs text-amber-600 dark:text-amber-500">该分卷已有章节规划，无法编辑。如需修改，请先删除相关章节规划。</p>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto">
      <div class="p-6 space-y-8">
        <!-- 基础信息 -->
        <div class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
            <Edit3 class="w-4 h-4 text-purple-500" />
            基础信息
          </h3>
          <div class="grid grid-cols-2 gap-4">
            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">分卷标题</label>
              <input
                v-model="formData.volumeTitle"
                type="text"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="请输入分卷标题"
              />
            </div>
            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">目标章节数</label>
              <input
                v-model.number="formData.targetChapterCount"
                type="number"
                min="1"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="目标章节数"
              />
            </div>
          </div>
        </div>

        <!-- 核心设定 -->
        <div class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
            <Target class="w-4 h-4 text-purple-500" />
            核心设定
          </h3>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">本卷主旨</label>
              <textarea
                v-model="formData.volumeTheme"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="本卷的核心主题和主旨"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">主要冲突</label>
              <textarea
                v-model="formData.mainConflict"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="本卷的主要矛盾和冲突"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">情节走向</label>
              <textarea
                v-model="formData.plotArc"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="情节发展的整体走向"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">核心目标</label>
              <textarea
                v-model="formData.coreGoal"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="本卷要达成的核心目标"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 关键事件（5阶段） -->
        <div class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
            <Star class="w-4 h-4 text-purple-500" />
            关键事件
          </h3>

          <!-- 5阶段折叠面板 -->
          <div class="space-y-3">
            <div
              v-for="stage in stageConfig"
              :key="stage.key"
              class="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden"
            >
              <!-- 阶段头部 -->
              <button
                type="button"
                @click="toggleStage(stage.key)"
                :class="[
                  'w-full flex items-center justify-between px-4 py-3 text-left transition-colors',
                  getStageColorClass(stage.color, 'bg')
                ]"
              >
                <div class="flex items-center gap-3">
                  <span :class="['w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold', getStageColorClass(stage.color, 'bg'), getStageColorClass(stage.color, 'text')]">
                    {{ ['开', '发', '转', '高', '收'][stageConfig.findIndex(s => s.key === stage.key)] }}
                  </span>
                  <div>
                    <span :class="['font-medium', getStageColorClass(stage.color, 'text')]">{{ stage.label }}</span>
                    <span class="ml-2 text-xs text-gray-500 dark:text-gray-400">({{ keyEventsData[stage.key].length }} 个事件)</span>
                  </div>
                </div>
                <ChevronDown
                  v-if="!expandedStages.has(stage.key)"
                  :class="['w-4 h-4 text-gray-400 transition-transform']"
                />
                <ChevronUp
                  v-else
                  :class="['w-4 h-4 text-gray-400 transition-transform']"
                />
              </button>

              <!-- 阶段内容 -->
              <div
                v-show="expandedStages.has(stage.key)"
                class="p-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800"
              >
                <!-- 阶段描述 -->
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-3">{{ stage.description }}</p>

                <!-- 事件列表 -->
                <div class="space-y-2">
                  <div
                    v-for="(event, index) in keyEventsData[stage.key]"
                    :key="index"
                    class="flex items-start gap-2"
                  >
                    <span class="mt-2.5 w-5 h-5 rounded bg-gray-100 dark:bg-gray-700 flex items-center justify-center text-xs text-gray-500 flex-shrink-0">
                      {{ index + 1 }}
                    </span>
                    <input
                      v-model="keyEventsData[stage.key][index]"
                      type="text"
                      :disabled="isLocked"
                      class="flex-1 px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                      placeholder="输入事件描述..."
                    />
                    <button
                      v-if="!isLocked"
                      type="button"
                      @click="removeEvent(stage.key, index)"
                      class="mt-1.5 p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
                    >
                      <X class="w-4 h-4" />
                    </button>
                  </div>

                  <!-- 空状态 -->
                  <div
                    v-if="keyEventsData[stage.key].length === 0"
                    class="text-center py-4 text-sm text-gray-400 dark:text-gray-500"
                  >
                    暂无事件
                  </div>
                </div>

                <!-- 添加按钮 -->
                <button
                  v-if="!isLocked"
                  type="button"
                  @click="addEvent(stage.key)"
                  class="mt-3 flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
                >
                  <Plus class="w-3.5 h-3.5" />
                  添加事件
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 高潮与收尾 -->
        <div class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
            <Flag class="w-4 h-4 text-purple-500" />
            高潮与收尾
          </h3>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">高潮事件</label>
              <textarea
                v-model="formData.climax"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="本卷的高潮事件描述"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">收尾描述</label>
              <textarea
                v-model="formData.ending"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="本卷的收尾方式"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 描述与备注 -->
        <div class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
            <Lightbulb class="w-4 h-4 text-purple-500" />
            描述与备注
          </h3>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">分卷描述</label>
              <textarea
                v-model="formData.volumeDescription"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="分卷的详细描述"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">分卷备注</label>
              <textarea
                v-model="formData.volumeNotes"
                rows="3"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
                placeholder="分卷的备注信息"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 时间线设定 -->
        <div class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
            <Clock class="w-4 h-4 text-purple-500" />
            时间线设定
          </h3>
          <div>
            <textarea
              v-model="formData.timelineSetting"
              rows="3"
              :disabled="isLocked"
              class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
              placeholder="时间线的具体设定"
            ></textarea>
          </div>
        </div>

        <!-- 状态 -->
        <div class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wide">
            <CheckCircle class="w-4 h-4 text-purple-500" />
            状态
          </h3>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">分卷状态</label>
              <select
                v-model="formData.status"
                :disabled="isLocked"
                class="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed"
              >
                <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">是否完成</label>
              <div class="flex items-center h-10">
                <button
                  @click="formData.volumeCompleted = !formData.volumeCompleted"
                  :disabled="isLocked"
                  :class="[
                    'relative inline-flex h-6 w-11 items-center rounded-full transition-colors disabled:opacity-60 disabled:cursor-not-allowed',
                    formData.volumeCompleted ? 'bg-green-500' : 'bg-gray-300 dark:bg-gray-600'
                  ]"
                >
                  <span
                    :class="[
                      'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
                      formData.volumeCompleted ? 'translate-x-6' : 'translate-x-1'
                    ]"
                  />
                </button>
                <span class="ml-3 text-sm text-gray-600 dark:text-gray-400">
                  {{ formData.volumeCompleted ? '已完成' : '进行中' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
