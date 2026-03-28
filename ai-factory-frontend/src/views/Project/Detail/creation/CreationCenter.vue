<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Loader2, Sparkles } from 'lucide-vue-next'
import VolumeTree from './VolumeTree.vue'
import VolumeDetail from './VolumeDetail.vue'
import ChapterEditor from './ChapterEditor.vue'
import ChapterPlanDrawer from './ChapterPlanDrawer.vue'
import { useEditorStore } from '@/stores/editor'
import { useProjectStore } from '@/stores/project'
import type { Chapter, Volume } from '@/types/project'
import { success, error } from '@/utils/toast'
import { updateVolume, optimizeVolume } from '@/api/outline'
import { getTaskStatus, getProjectTasks } from '@/api/task'
import type { TaskDto } from '@/api/task'
import { aiPolish } from '@/api/chapter'

const route = useRoute()
const editorStore = useEditorStore()
const projectStore = useProjectStore()

// VolumeDetail 组件引用
const volumeDetailRef = ref<InstanceType<typeof VolumeDetail> | null>(null)

// Local state
const selectedChapterId = ref<string | null>(null)
const selectedVolume = ref<Volume | null>(null)
const viewMode = ref<'volume' | 'chapter'>('volume')

// AI优化状态
const optimizingVolumeId = ref<string | null>(null)
const currentTaskId = ref<string | null>(null)

// AI操作状态
const isPolishing = ref(false)

// AI修复预览弹窗
const showFixPreviewDialog = ref(false)
const fixPreviewTab = ref<'preview' | 'report'>('preview')

// AI修复类型中英文映射（与后端 prompt 模板定义保持一致）
const fixTypeMap: Record<string, string> = {
  // 模板中定义的类型
  'logic': '逻辑问题',
  'continuity': '连贯性问题',
  'character': '人物一致性问题',
  'worldview': '世界观问题',
  'timeline': '时间线问题',
  'foreshadow': '伏笔问题',
  'repetition': '重复内容',
  'setting': '设定问题',
  // 兼容旧版/其他可能的类型
  'logic_error': '逻辑错误',
  'redundancy': '冗余内容',
  'character_consistency': '人物一致性',
  'timeline_error': '时间线错误',
  'setting_conflict': '设定冲突',
  'plot_hole': '剧情漏洞',
  'dialogue_issue': '对话问题',
  'pacing_issue': '节奏问题',
  'description_issue': '描写问题',
  'grammar_error': '语法错误',
  'typo': '错别字',
  'tone_inconsistency': '语气不一致',
  'pov_shift': '视角转换',
  'foreshadowing_issue': '伏笔问题',
  'other': '其他'
}

// 获取修复类型的中文名称
const getFixTypeName = (type: string): string => {
  return fixTypeMap[type] || type
}

// Computed
const projectId = computed(() => (route.params.id as string) || projectStore.projectId || '')
const chapterIdFromRoute = computed(() => route.params.chapterId as string | null)

// 计算当前选中的分卷是否正在优化
const isCurrentVolumeOptimizing = computed(() => {
  return selectedVolume.value && optimizingVolumeId.value === selectedVolume.value.id
})

// Methods
const handleSelectChapter = async (chapter: Chapter) => {
  selectedChapterId.value = chapter.id
  selectedVolume.value = null
  viewMode.value = 'chapter'
  // 设置 projectId 到 editor store
  editorStore.setProjectId(projectId.value)

  // 设置章节规划信息（用于显示标题等）
  editorStore.setChapterPlan({
    id: chapter.id,
    volumeId: chapter.volumeId,
    projectId: chapter.projectId,
    chapterNumber: chapter.chapterNumber || 1,
    title: chapter.chapterTitle || chapter.title || '',
    summary: chapter.summary,
    keyEvents: chapter.keyEvents,
    characters: chapter.newCharacters ? chapter.newCharacters.split(',') : [],
    foreshadowing: chapter.foreshadowingSetup
  })

  // 使用 loadChapterByPlan 而不是 loadChapter，因为 chapter.id 是规划 ID
  await editorStore.loadChapterByPlan(chapter.id)
}

const handleSelectVolume = (volume: Volume) => {
  selectedVolume.value = volume
  selectedChapterId.value = null
  viewMode.value = 'volume'
}

const handleSaveVolume = async (volume: Volume) => {
  try {
    await updateVolume(projectId.value, volume.id, volume as Record<string, any>)
    success('分卷信息已保存')
    // 更新本地数据
    selectedVolume.value = volume
  } catch (e: any) {
    error(e.message || '保存失败')
  }
}

// 轮询任务状态
const pollTaskStatus = async (taskId: string, onComplete: () => void, onError: (msg: string) => void) => {
  const maxAttempts = 60 // 最多轮询60次
  const interval = 3000 // 每3秒轮询一次

  for (let i = 0; i < maxAttempts; i++) {
    try {
      const task = await getTaskStatus(taskId)

      if (task.status === 'completed') {
        onComplete()
        return
      }

      if (task.status === 'failed' || task.status === 'cancelled') {
        onError(task.errorMessage || '任务失败')
        return
      }

      // 继续等待
      await new Promise(resolve => setTimeout(resolve, interval))
    } catch (e: any) {
      onError(e.message || '查询任务状态失败')
      return
    }
  }

  onError('任务超时')
}

const handleOptimizeVolume = async (data: { targetChapterCount: number }) => {
  if (!selectedVolume.value) return

  try {
    const result = await optimizeVolume(projectId.value, selectedVolume.value.id, {
      targetChapterCount: data.targetChapterCount
    })

    if (result.taskId) {
      success('AI优化任务已创建，正在生成...')
      // 记录当前优化的分卷和任务ID
      optimizingVolumeId.value = selectedVolume.value.id
      currentTaskId.value = result.taskId
      // 开始轮询任务状态
      pollTaskStatus(result.taskId, () => {
        success('AI优化完成')
        optimizingVolumeId.value = null
        currentTaskId.value = null
        volumeDetailRef.value?.resetOptimizing()
        // 刷新分卷数据
        projectStore.fetchProject(projectId.value)
      }, (errorMsg: string) => {
        error(errorMsg)
        optimizingVolumeId.value = null
        currentTaskId.value = null
        volumeDetailRef.value?.resetOptimizing()
      })
    } else {
      // 没有taskId，立即重置
      volumeDetailRef.value?.resetOptimizing()
    }
  } catch (e: any) {
    error(e.message || '创建AI优化任务失败')
    optimizingVolumeId.value = null
    currentTaskId.value = null
    volumeDetailRef.value?.resetOptimizing()
  }
}

const handleAddVolume = () => {
  // TODO: Implement add volume
  console.log('Add Volume')
}

// AI创作
const handleGenerateChapter = async () => {
  if (!editorStore.currentChapter?.id && !editorStore.currentChapterPlan?.id) {
    error('请先选择章节')
    return
  }

  const planId = editorStore.currentChapter?.id || editorStore.currentChapterPlan?.id
  if (!planId) return

  try {
    await editorStore.generateChapterContent(planId)
    success('AI创作完成')
  } catch (e: any) {
    error(e.message || 'AI创作失败')
  }
}

// AI润色
const handlePolish = async () => {
  if (!editorStore.currentChapter?.id) return

  isPolishing.value = true
  try {
    const result = await aiPolish(projectId.value, editorStore.currentChapter.id)
    editorStore.updateContent(result.content || '')
    success('AI润色完成')
  } catch (e: any) {
    error(e.message || 'AI润色失败')
  } finally {
    isPolishing.value = false
  }
}

// AI剧情修复
const handleFix = async () => {
  if (!editorStore.currentChapter?.id) return

  try {
    await editorStore.startAiFix(editorStore.currentChapter.id)
    // 修复完成后显示预览弹窗
    if (editorStore.aiFixResult) {
      showFixPreviewDialog.value = true
      success('AI剧情修复分析完成')
    }
  } catch (e: any) {
    error(e.message || 'AI剧情修复失败')
  }
}

// 应用AI修复结果
const handleApplyFix = () => {
  editorStore.applyAiFixResult()
  showFixPreviewDialog.value = false
  success('已应用AI修复结果')
}

// 丢弃AI修复结果
const handleDiscardFix = () => {
  editorStore.discardAiFixResult()
  showFixPreviewDialog.value = false
}

// 打开详情抽屉
const handleOpenDrawer = () => {
  editorStore.setDrawerVisible(true)
}

// 检查正在进行的AI优化任务
const checkRunningOptimizeTasks = async () => {
  if (!projectId.value) return

  try {
    const tasks = await getProjectTasks(projectId.value)
    // 查找正在进行的 volume_optimize 任务
    const runningTask = tasks.find((task: TaskDto) =>
      task.taskType === 'volume_optimize' &&
      (task.status === 'pending' || task.status === 'running')
    )

    if (runningTask) {
      // 从任务配置中获取 volumeId
      const config = runningTask.configJson ? JSON.parse(runningTask.configJson as unknown as string) : {}
      const volumeId = config.volumeId

      if (volumeId) {
        optimizingVolumeId.value = String(volumeId)
        currentTaskId.value = runningTask.id

        // 继续轮询任务状态
        pollTaskStatus(runningTask.id, () => {
          success('AI优化完成')
          optimizingVolumeId.value = null
          currentTaskId.value = null
          volumeDetailRef.value?.resetOptimizing()
          projectStore.fetchProject(projectId.value)
        }, (errorMsg: string) => {
          error(errorMsg)
          optimizingVolumeId.value = null
          currentTaskId.value = null
          volumeDetailRef.value?.resetOptimizing()
        })
      }
    }
  } catch (e) {
    console.error('检查运行中任务失败:', e)
  }
}

// 监听 projectId 变化，设置到 editor store
watch(projectId, (newProjectId) => {
  if (newProjectId) {
    editorStore.setProjectId(newProjectId)
  }
}, { immediate: true })

// Watch for route changes to load chapter
watch(chapterIdFromRoute, async (newChapterId) => {
  if (newChapterId && newChapterId !== selectedChapterId.value) {
    selectedChapterId.value = newChapterId
    viewMode.value = 'chapter'
    await editorStore.loadChapter(newChapterId)
  }
}, { immediate: true })

// Cleanup on unmount
onMounted(async () => {
  // 设置 projectId
  if (projectId.value) {
    editorStore.setProjectId(projectId.value)
  }
  // If there's a chapter ID in route, load it
  if (chapterIdFromRoute.value && !selectedChapterId.value) {
    selectedChapterId.value = chapterIdFromRoute.value
    viewMode.value = 'chapter'
    editorStore.loadChapter(chapterIdFromRoute.value)
  }
  // 检查是否有正在进行的AI优化任务
  await checkRunningOptimizeTasks()
  // 恢复章节生成任务状态
  await editorStore.restoreGeneratingState()
  // 恢复AI修复任务状态
  await editorStore.restoreFixingState()
})
</script>

<template>
  <div class="h-full flex">
    <!-- Left: Volume Tree -->
    <div class="w-64 flex-shrink-0">
      <VolumeTree
        :project-id="projectId"
        :selected-chapter-id="selectedChapterId"
        @select-chapter="handleSelectChapter"
        @select-volume="handleSelectVolume"
        @add-volume="handleAddVolume"
      />
    </div>

    <!-- Right: Main Content Area -->
    <div class="flex-1 min-w-0 flex flex-col relative">
      <!-- AI创作遮罩层 -->
      <div
        v-if="editorStore.isGenerating"
        class="absolute inset-0 z-50 bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm flex flex-col items-center justify-center"
      >
        <div class="w-16 h-16 rounded-2xl bg-gradient-to-r from-purple-500 to-blue-500 flex items-center justify-center mb-4 shadow-lg">
          <Loader2 class="w-8 h-8 text-white animate-spin" />
        </div>
        <p class="text-lg font-medium text-gray-900 dark:text-white">AI 正在创作章节内容...</p>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">请稍候，这可能需要一些时间</p>
      </div>

      <!-- AI剧情修复遮罩层 -->
      <div
        v-if="editorStore.isFixing"
        class="absolute inset-0 z-50 bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm flex flex-col items-center justify-center"
      >
        <div class="w-16 h-16 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 flex items-center justify-center mb-4 shadow-lg">
          <Loader2 class="w-8 h-8 text-white animate-spin" />
        </div>
        <p class="text-lg font-medium text-gray-900 dark:text-white">AI 正在分析剧情问题...</p>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">请稍候，这可能需要一些时间</p>
      </div>

      <!-- Volume Detail View -->
      <template v-if="viewMode === 'volume' && selectedVolume">
        <VolumeDetail
          ref="volumeDetailRef"
          :volume="selectedVolume"
          :is-optimizing="isCurrentVolumeOptimizing"
          @save="handleSaveVolume"
          @optimize="handleOptimizeVolume"
        />
      </template>

      <!-- Chapter Editor View -->
      <template v-else-if="viewMode === 'chapter'">
        <!-- Chapter Info Bar -->
        <div v-if="editorStore.currentChapter || editorStore.currentChapterPlan" class="flex-shrink-0 bg-gray-50 dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 px-4 py-2">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-4 text-sm">
              <span class="text-gray-600 dark:text-gray-400">
                字数: <span class="font-medium text-gray-900 dark:text-white">{{ editorStore.wordCount.toLocaleString() }}</span>
              </span>
              <span class="text-gray-600 dark:text-gray-400" v-if="editorStore.currentChapter">
                状态: <span :class="editorStore.currentChapter.status === 'published' ? 'text-green-500' : 'text-orange-500'">
                  {{ editorStore.currentChapter.status === 'published' ? '已发布' : '草稿' }}
                </span>
              </span>
              <span v-if="editorStore.lastSavedAt" class="text-green-600 dark:text-green-400">
                已保存 {{ editorStore.lastSavedAt.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}
              </span>
            </div>
            <div class="flex items-center gap-2">
              <!-- 详情按钮 -->
              <button
                @click="handleOpenDrawer"
                class="flex items-center gap-1 px-3 py-1 text-xs font-medium text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-800 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
              >
                详情
              </button>
              <!-- AI创作按钮 - 始终显示，渐变色 -->
              <button
                @click="handleGenerateChapter"
                class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-gradient-to-r from-purple-500 to-blue-500 rounded-lg hover:from-purple-600 hover:to-blue-600 transition-all shadow-sm hover:shadow"
              >
                <Sparkles class="w-3.5 h-3.5" />
                AI创作
              </button>
              <!-- AI润色按钮 - 仅章节已生成时显示 -->
              <button
                v-if="editorStore.currentChapter?.id"
                @click="handlePolish"
                :disabled="isPolishing"
                class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-gradient-to-r from-cyan-500 to-blue-500 rounded-lg hover:from-cyan-600 hover:to-blue-600 transition-all shadow-sm hover:shadow disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Loader2 v-if="isPolishing" class="w-3.5 h-3.5 animate-spin" />
                <svg v-else class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
                </svg>
                AI润色
              </button>
              <!-- AI剧情修复按钮 - 仅章节已生成时显示 -->
              <button
                v-if="editorStore.currentChapter?.id"
                @click="handleFix"
                :disabled="editorStore.isFixing"
                class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-gradient-to-r from-amber-500 to-orange-500 rounded-lg hover:from-amber-600 hover:to-orange-600 transition-all shadow-sm hover:shadow disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Loader2 v-if="editorStore.isFixing" class="w-3.5 h-3.5 animate-spin" />
                <svg v-else class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                AI剧情修复
              </button>
            </div>
          </div>
        </div>

        <!-- Chapter Editor -->
        <div class="flex-1 min-h-0">
          <ChapterEditor />
        </div>
      </template>

      <!-- Empty State -->
      <template v-else>
        <div class="h-full flex flex-col items-center justify-center text-gray-500 dark:text-gray-400">
          <div class="w-20 h-20 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center mb-4">
            <svg class="w-10 h-10 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <p class="text-lg font-medium mb-2">开始创作</p>
          <p class="text-sm">从左侧选择分卷查看详情，或选择章节开始编辑</p>
        </div>
      </template>
    </div>

    <!-- Chapter Plan Drawer -->
    <ChapterPlanDrawer />

    <!-- AI剧情修复预览弹窗 -->
    <div
      v-if="showFixPreviewDialog"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50"
      @click.self="showFixPreviewDialog = false"
    >
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-[900px] max-h-[80vh] overflow-hidden">
        <!-- 标题栏 -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">AI剧情修复预览</h3>
          <button
            @click="showFixPreviewDialog = false"
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Tab切换 -->
        <div class="flex border-b border-gray-200 dark:border-gray-700">
          <button
            @click="fixPreviewTab = 'preview'"
            :class="[
              'px-6 py-3 text-sm font-medium border-b-2 transition-colors',
              fixPreviewTab === 'preview'
                ? 'border-amber-500 text-amber-600 dark:text-amber-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
            ]"
          >
            修复预览
          </button>
          <button
            @click="fixPreviewTab = 'report'"
            :class="[
              'px-6 py-3 text-sm font-medium border-b-2 transition-colors',
              fixPreviewTab === 'report'
                ? 'border-amber-500 text-amber-600 dark:text-amber-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
            ]"
          >
            修复报告
            <span v-if="editorStore.aiFixResult?.fixReport?.length" class="ml-1 px-1.5 py-0.5 text-xs bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400 rounded">
              {{ editorStore.aiFixResult.fixReport.length }}
            </span>
          </button>
        </div>

        <!-- 内容区域 -->
        <div class="p-6 overflow-auto" style="max-height: calc(80vh - 180px);">
          <!-- 修复预览Tab -->
          <div v-if="fixPreviewTab === 'preview'">
            <!-- 左右对比 -->
            <div class="flex gap-4 mb-4">
              <!-- 左侧：原文 -->
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span class="text-sm font-medium text-gray-700 dark:text-gray-300">原文</span>
                  <span class="text-xs text-gray-500">{{ editorStore.content.length }} 字</span>
                </div>
                <div class="h-56 overflow-auto border border-gray-200 dark:border-gray-700 rounded-lg p-3 bg-gray-50 dark:bg-gray-900 text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                  {{ editorStore.content }}
                </div>
              </div>
              <!-- 右侧：修复后 -->
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span class="text-sm font-medium text-gray-700 dark:text-gray-300">修复后</span>
                  <span class="px-2 py-0.5 text-xs font-medium text-white bg-green-500 rounded-full">
                    {{ editorStore.aiFixResult?.totalFixes || 0 }} 处修改
                  </span>
                </div>
                <div class="h-56 overflow-auto border border-amber-200 dark:border-amber-700 rounded-lg p-3 bg-amber-50 dark:bg-amber-900/20 text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                  {{ editorStore.aiFixResult?.fixedContent }}
                </div>
              </div>
            </div>

            <!-- 修复摘要 -->
            <div v-if="editorStore.aiFixResult?.fixSummary" class="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
              <div class="flex items-start gap-2">
                <svg class="w-5 h-5 text-blue-500 mt-0.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p class="text-sm text-blue-700 dark:text-blue-300">{{ editorStore.aiFixResult.fixSummary }}</p>
              </div>
            </div>
          </div>

          <!-- 修复报告Tab -->
          <div v-else-if="fixPreviewTab === 'report'">
            <div v-if="editorStore.aiFixResult?.fixReport?.length" class="space-y-3">
              <div
                v-for="(item, index) in editorStore.aiFixResult.fixReport"
                :key="index"
                class="border rounded-lg overflow-hidden"
                :class="{
                  'border-red-200 dark:border-red-800': item.severity === 'high',
                  'border-yellow-200 dark:border-yellow-800': item.severity === 'medium',
                  'border-blue-200 dark:border-blue-800': item.severity === 'low'
                }"
              >
                <!-- 修复项头部 -->
                <div class="flex items-center justify-between px-4 py-2"
                  :class="{
                    'bg-red-50 dark:bg-red-900/20': item.severity === 'high',
                    'bg-yellow-50 dark:bg-yellow-900/20': item.severity === 'medium',
                    'bg-blue-50 dark:bg-blue-900/20': item.severity === 'low'
                  }"
                >
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ getFixTypeName(item.type) }}</span>
                    <span
                      class="px-2 py-0.5 text-xs font-medium rounded"
                      :class="{
                        'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300': item.severity === 'high',
                        'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-300': item.severity === 'medium',
                        'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300': item.severity === 'low'
                      }"
                    >
                      {{ item.severity === 'high' ? '高' : item.severity === 'medium' ? '中' : '低' }}
                    </span>
                  </div>
                  <span class="text-xs text-gray-500 dark:text-gray-400">#{{ index + 1 }}</span>
                </div>
                <!-- 修复项内容 -->
                <div class="p-4 bg-white dark:bg-gray-800 space-y-3">
                  <!-- 原文 -->
                  <div>
                    <span class="text-xs font-medium text-red-500 dark:text-red-400">原文：</span>
                    <p class="mt-1 text-sm text-gray-600 dark:text-gray-400 bg-red-50 dark:bg-red-900/10 rounded p-2">{{ item.original }}</p>
                  </div>
                  <!-- 修复后 -->
                  <div>
                    <span class="text-xs font-medium text-green-500 dark:text-green-400">修复后：</span>
                    <p class="mt-1 text-sm text-gray-600 dark:text-gray-400 bg-green-50 dark:bg-green-900/10 rounded p-2">{{ item.fixed }}</p>
                  </div>
                  <!-- 修复原因 -->
                  <div>
                    <span class="text-xs font-medium text-gray-500 dark:text-gray-400">原因：</span>
                    <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">{{ item.reason }}</p>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="flex flex-col items-center justify-center py-12 text-gray-500 dark:text-gray-400">
              <svg class="w-12 h-12 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p class="text-sm">暂无详细修复报告</p>
            </div>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
          <button
            @click="handleDiscardFix"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          >
            丢弃
          </button>
          <button
            @click="handleApplyFix"
            class="px-4 py-2 text-sm font-medium text-white bg-gradient-to-r from-amber-500 to-orange-500 rounded-lg hover:from-amber-600 hover:to-orange-600 transition-all shadow-sm"
          >
            应用修复
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
