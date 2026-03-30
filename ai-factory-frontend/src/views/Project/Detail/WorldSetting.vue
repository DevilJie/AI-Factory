<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import {
  Globe,
  Sparkles,
  RefreshCw,
  Save,
  Loader2,
  ChevronDown
} from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { getWorldview, saveWorldview, generateWorldviewAsync, type Worldview } from '@/api/worldview'
import { getTaskStatus } from '@/api/task'
import { success, error } from '@/utils/toast'
import PowerSystemSection from './components/PowerSystemSection.vue'

const route = useRoute()

// State
const loading = ref(false)
const saving = ref(false)
const generating = ref(false)
const formData = ref<Worldview>({
  worldType: '',
  worldBackground: '',
  geography: '',
  forces: '',
  timeline: '',
  rules: ''
})

const powerSystemRef = ref()

// 任务存储键
const getGenerateTaskKey = (projectId: string) =>
  `ai-factory-worldview-generate-${projectId}`

// 表单配置
const worldTypes = [
  { value: 'modern', label: '现代都市' },
  { value: 'ancient', label: '古代历史' },
  { value: 'fantasy', label: '奇幻玄幻' },
  { value: 'scifi', label: '科幻未来' },
  { value: 'wuxia', label: '武侠江湖' },
  { value: 'xianxia', label: '仙侠修真' },
  { value: 'other', label: '其他' }
]

// 获取项目ID
const projectId = () => route.params.id as string

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const data = await getWorldview(projectId())
    if (data) {
      formData.value = {
        ...formData.value,
        ...data
      }
    }
  } catch (e: any) {
    if (e.response?.status !== 404) {
      error('加载世界观设定失败')
      console.error('Failed to load worldview:', e)
    }
  } finally {
    loading.value = false
  }
}

// 保存数据
const handleSave = async () => {
  saving.value = true
  try {
    const data = {
      ...formData.value,
      projectId: Number(projectId())
    }
    await saveWorldview(projectId(), data)
    success('保存成功')
    loadData()
  } catch (e) {
    error('保存失败')
    console.error('Failed to save worldview:', e)
  } finally {
    saving.value = false
  }
}

// 轮询任务状态
const pollTaskStatus = async (taskId: string, maxAttempts: number = 60, interval: number = 3000) => {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const task = await getTaskStatus(taskId)

      if (task.status === 'completed') {
        return true
      }

      if (task.status === 'failed' || task.status === 'cancelled') {
        throw new Error(task.errorMessage || 'AI生成世界观失败')
      }

      // 继续等待
      await new Promise(resolve => setTimeout(resolve, interval))
    } catch (e: any) {
      if (e.message?.includes('失败')) {
        throw e
      }
      // 网络错误等，继续轮询
      console.log(`轮询第${i + 1}次出错，继续等待:`, e)
      await new Promise(resolve => setTimeout(resolve, interval))
    }
  }
  throw new Error('AI生成世界观超时')
}

// 保存生成任务状态到 localStorage
const saveGeneratingTask = (taskId: string) => {
  localStorage.setItem(
    getGenerateTaskKey(projectId()),
    JSON.stringify({ taskId, startTime: Date.now() })
  )
}

// 清除生成任务状态
const clearGeneratingTask = () => {
  localStorage.removeItem(getGenerateTaskKey(projectId()))
}

// AI生成
const handleGenerate = async () => {
  generating.value = true
  try {
    const result = await generateWorldviewAsync(projectId())
    console.log('AI生成世界观任务已创建:', result)

    // 保存任务状态
    saveGeneratingTask(result.taskId)

    // 轮询等待完成
    await pollTaskStatus(result.taskId)

    // 清除任务状态
    clearGeneratingTask()

    // 刷新数据
    await loadData()
    powerSystemRef.value?.refresh()
    success('AI生成世界观成功')
  } catch (e: any) {
    error(e.message || 'AI生成失败')
    console.error('Failed to generate worldview:', e)
  } finally {
    generating.value = false
    clearGeneratingTask()
  }
}

// 恢复生成任务状态（页面刷新后调用）
const restoreGeneratingState = async () => {
  try {
    const taskData = localStorage.getItem(getGenerateTaskKey(projectId()))
    if (!taskData) return

    const { taskId, startTime } = JSON.parse(taskData)

    // 检查任务是否超时（超过10分钟认为已超时）
    if (Date.now() - startTime > 10 * 60 * 1000) {
      clearGeneratingTask()
      return
    }

    // 恢复生成状态
    generating.value = true

    try {
      // 继续轮询
      await pollTaskStatus(taskId)

      // 生成完成，刷新数据
      await loadData()
      powerSystemRef.value?.refresh()
      success('AI生成世界观完成')
    } catch (e: any) {
      console.error('恢复生成状态失败:', e)
      error(e.message || 'AI生成失败')
    } finally {
      generating.value = false
      clearGeneratingTask()
    }
  } catch (e) {
    console.error('恢复生成状态失败:', e)
    generating.value = false
    clearGeneratingTask()
  }
}

// Lifecycle
onMounted(() => {
  loadData()
  // 恢复生成任务状态
  restoreGeneratingState()
})

// Watch route changes
watch(() => route.params.id, (newId) => {
  if (newId) {
    loadData()
  }
})

// 清理
onUnmounted(() => {
  // 不清除 localStorage，以便恢复
})
</script>

<template>
  <div class="h-full flex flex-col bg-gray-50 dark:bg-gray-900 relative">
    <!-- AI生成遮罩层 -->
    <div
      v-if="generating"
      class="absolute inset-0 z-50 bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm flex flex-col items-center justify-center"
    >
      <div class="w-16 h-16 rounded-2xl bg-gradient-to-r from-green-500 to-teal-500 flex items-center justify-center mb-4 shadow-lg">
        <Loader2 class="w-8 h-8 text-white animate-spin" />
      </div>
      <p class="text-lg font-medium text-gray-900 dark:text-white">AI 正在生成世界观设定...</p>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">请稍候，这可能需要一些时间</p>
    </div>

    <!-- Header -->
    <div class="flex-shrink-0 px-6 py-4 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
            <Globe class="w-5 h-5 text-green-600 dark:text-green-400" />
          </div>
          <div>
            <h1 class="text-lg font-semibold text-gray-900 dark:text-white">世界观设定</h1>
            <p class="text-sm text-gray-500 dark:text-gray-400">构建小说世界的背景与规则</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="handleGenerate"
            :disabled="generating"
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-gradient-to-r from-green-500 to-teal-500 rounded-lg hover:from-green-600 hover:to-teal-600 transition-colors disabled:opacity-50"
          >
            <Loader2 v-if="generating" class="w-4 h-4 animate-spin" />
            <Sparkles v-else class="w-4 h-4" />
            AI生成
          </button>
          <button
            @click="loadData"
            :disabled="loading || generating"
            class="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors disabled:opacity-50"
            title="刷新"
          >
            <RefreshCw :class="['w-5 h-5', { 'animate-spin': loading }]" />
          </button>
          <button
            @click="handleSave"
            :disabled="saving || generating"
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            <Save v-if="!saving" class="w-4 h-4" />
            <Loader2 v-else class="w-4 h-4 animate-spin" />
            保存
          </button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-6">
      <div v-if="loading && !generating" class="flex items-center justify-center h-full">
        <Loader2 class="w-8 h-8 text-blue-500 animate-spin" />
      </div>

      <div v-else class="space-y-6">
        <!-- 世界类型 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            世界类型
          </label>
          <div class="relative">
            <select
              v-model="formData.worldType"
              :disabled="generating"
              class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
            >
              <option value="">请选择世界类型</option>
              <option v-for="type in worldTypes" :key="type.value" :value="type.value">
                {{ type.label }}
              </option>
            </select>
            <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
          </div>
        </div>

        <!-- 世界背景 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            世界背景
          </label>
          <textarea
            v-model="formData.worldBackground"
            rows="4"
            :disabled="generating"
            placeholder="描述这个世界的基本背景设定，包括时代背景、社会环境等..."
            class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
          ></textarea>
        </div>

        <!-- 力量体系 -->
        <PowerSystemSection ref="powerSystemRef" :project-id="projectId()" :disabled="generating" />

        <!-- 地理环境 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            地理环境
          </label>
          <textarea
            v-model="formData.geography"
            rows="4"
            :disabled="generating"
            placeholder="描述世界的地理环境、重要地点、区域划分等..."
            class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
          ></textarea>
        </div>

        <!-- 势力阵营 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            势力阵营
          </label>
          <textarea
            v-model="formData.forces"
            rows="4"
            :disabled="generating"
            placeholder="描述世界中的主要势力、组织、阵营及其相互关系..."
            class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
          ></textarea>
        </div>

        <!-- 时间线 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            时间线
          </label>
          <textarea
            v-model="formData.timeline"
            rows="4"
            :disabled="generating"
            placeholder="描述世界的重要历史事件和时间节点..."
            class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
          ></textarea>
        </div>

        <!-- 世界规则 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            世界规则
          </label>
          <textarea
            v-model="formData.rules"
            rows="4"
            :disabled="generating"
            placeholder="描述世界的运行规则、物理法则、禁忌等..."
            class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
          ></textarea>
        </div>
      </div>
    </div>
  </div>
</template>
