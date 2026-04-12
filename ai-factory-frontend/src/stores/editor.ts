import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Chapter, ChapterPlan } from '@/types/project'
import { getChapterDetail, getChapterByPlanId, updateChapter as updateChapterApi, generateChapter as generateChapterApi, fixChapterWithAIAsync, type ChapterAiFixResponse } from '@/api/chapter'
import { getTaskStatus } from '@/api/task'

// 任务存储键
const getChapterGenerateTaskKey = (projectId: string) =>
  `ai-factory-chapter-generate-${projectId}`

// AI剧情修复任务存储键
const getAiFixTaskKey = (projectId: string) =>
  `ai-factory-ai-fix-${projectId}`

export const useEditorStore = defineStore('editor', () => {
  // State
  const projectId = ref<string | null>(null)
  const currentChapter = ref<Chapter | null>(null)
  const content = ref('')
  const isDirty = ref(false)
  const isSaving = ref(false)
  const lastSavedAt = ref<Date | null>(null)

  // 章节规划相关状态
  const currentChapterPlan = ref<ChapterPlan | null>(null)
  const isChapterGenerated = ref<boolean>(false)
  const isGenerating = ref<boolean>(false)
  const isDrawerVisible = ref<boolean>(false)

  // 当前生成任务的章节ID
  const generatingChapterId = ref<string | null>(null)

  // AI剧情修复相关状态
  const isFixing = ref<boolean>(false)
  const fixingTaskId = ref<string | null>(null)
  const fixingChapterId = ref<string | null>(null)
  const aiFixResult = ref<ChapterAiFixResponse | null>(null)

  // Getters
  const chapterId = computed(() => currentChapter.value?.id ?? currentChapterPlan.value?.id ?? null)
  const chapterTitle = computed(() => {
    // 优先使用当前章节的标题，其次使用章节规划的标题
    return currentChapter.value?.chapterTitle || currentChapter.value?.title || currentChapterPlan.value?.title || ''
  })
  const wordCount = computed(() => {
    if (!content.value) return 0
    return content.value.replace(/\s/g, '').length
  })

  // Actions
  const setProjectId = (id: string) => {
    projectId.value = id
  }

  const loadChapter = async (id: string) => {
    if (!projectId.value) {
      console.error('Project ID not set')
      return
    }
    try {
      const chapter = await getChapterDetail(projectId.value, id)
      currentChapter.value = chapter
      content.value = chapter.content || ''
      isDirty.value = false
      // 如果章节有内容，清除章节规划状态
      if (chapter.content) {
        isChapterGenerated.value = true
        // 如果加载的章节有内容，更新 currentChapterPlan 为空（使用章节自身数据）
        currentChapterPlan.value = null
      } else {
        // 章节无内容，标记为未生成
        isChapterGenerated.value = false
      }
    } catch (e: any) {
      // 404 表示章节未生成，这是正常情况
      if (e?.response?.status === 404 || e?.message?.includes('404')) {
        console.log('Chapter not generated yet, id:', id)
        // 保持 currentChapterPlan 不变（由 setChapterPlan 设置）
        currentChapter.value = null
        content.value = ''
        isChapterGenerated.value = false
      } else {
        console.error('Failed to load chapter:', e)
        throw e
      }
    }
  }

  const updateContent = (newContent: string) => {
    content.value = newContent
    isDirty.value = true
  }

  const saveContent = async () => {
    if (!currentChapter.value || !isDirty.value || !projectId.value) return

    isSaving.value = true
    try {
      await updateChapterApi(projectId.value, currentChapter.value.id, {
        content: content.value
      })
      isDirty.value = false
      lastSavedAt.value = new Date()
    } catch (e) {
      console.error('Failed to save chapter:', e)
      throw e
    } finally {
      isSaving.value = false
    }
  }

  const clearEditor = () => {
    currentChapter.value = null
    content.value = ''
    isDirty.value = false
    lastSavedAt.value = null
  }

  // 章节规划相关方法
  const loadChapterByPlan = async (planId: string) => {
    if (!projectId.value) {
      console.error('Project ID not set')
      return
    }
    console.log('loadChapterByPlan called - projectId:', projectId.value, 'planId:', planId)
    try {
      // 使用新的 getChapterByPlanId API，根据规划ID获取章节
      const chapter = await getChapterByPlanId(projectId.value, planId)
      console.log('getChapterByPlanId response:', chapter)
      console.log('chapter.id:', chapter?.id, 'chapter.content length:', chapter?.content?.length)
      if (chapter && chapter.content) {
        // 章节已生成，加载内容
        currentChapter.value = chapter
        content.value = chapter.content
        isChapterGenerated.value = true
        console.log('Chapter loaded successfully, currentChapter.id:', currentChapter.value?.id)
      } else {
        // 章节未生成，保持 currentChapter 为 null，依赖 currentChapterPlan 显示标题
        console.log('Chapter not generated or no content')
        currentChapter.value = null
        content.value = ''
        isChapterGenerated.value = false
      }
      isDirty.value = false
    } catch (e: any) {
      // 404 表示章节未生成，这是正常情况，不需要报错
      if (e?.response?.status === 404 || e?.message?.includes('404')) {
        console.log('Chapter not generated yet, planId:', planId)
      } else {
        console.error('Failed to load chapter by plan:', e)
      }
      // 加载失败时也保持 currentChapter 为 null
      currentChapter.value = null
      content.value = ''
      isChapterGenerated.value = false
      // 不抛出错误，让流程继续
    }
  }

  const setChapterPlan = (plan: ChapterPlan | null) => {
    currentChapterPlan.value = plan
  }

  // 保存生成任务状态到 localStorage
  const saveGeneratingTask = (chapterId: string, taskId?: string) => {
    if (!projectId.value) return
    localStorage.setItem(
      getChapterGenerateTaskKey(projectId.value),
      JSON.stringify({ chapterId, taskId, startTime: Date.now() })
    )
  }

  // 清除生成任务状态
  const clearGeneratingTask = () => {
    if (!projectId.value) return
    localStorage.removeItem(getChapterGenerateTaskKey(projectId.value))
  }

  // 保存AI修复任务状态到 localStorage
  const saveFixingTask = (chapterId: string, taskId: string) => {
    if (!projectId.value) return
    localStorage.setItem(
      getAiFixTaskKey(projectId.value),
      JSON.stringify({ chapterId, taskId, startTime: Date.now() })
    )
  }

  // 清除AI修复任务状态
  const clearFixingTask = () => {
    if (!projectId.value) return
    localStorage.removeItem(getAiFixTaskKey(projectId.value))
  }

  // 轮询检查章节生成状态
  const pollChapterGeneration = async (planId: string, maxAttempts: number = 60, interval: number = 3000) => {
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise(resolve => setTimeout(resolve, interval))

      try {
        // 使用新的 getChapterByPlanId API
        const chapter = await getChapterByPlanId(projectId.value!, planId)
        console.log(`轮询第${i + 1}次，获取到章节数据:`, chapter)
        console.log('章节数据详情 - id:', chapter?.id, 'content长度:', chapter?.content?.length)
        // 检查章节是否有内容（可能是 content 或 text 字段）
        const chapterContent = chapter?.content || (chapter as any)?.text || ''
        if (chapter && chapterContent && chapterContent.trim().length > 0) {
          currentChapter.value = chapter
          content.value = chapterContent
          isChapterGenerated.value = true
          isDirty.value = false
          lastSavedAt.value = new Date()
          console.log('章节生成完成，内容长度:', chapterContent.length)
          console.log('currentChapter已设置，id:', currentChapter.value?.id)
          return true
        } else {
          console.log(`轮询第${i + 1}次，章节内容为空...`)
        }
      } catch (e) {
        // 继续轮询
        console.log(`轮询第${i + 1}次，章节尚未完成，错误:`, e)
      }
    }
    return false
  }

  // 轮询AI修复任务状态
  const pollAiFixStatus = async (taskId: string, maxAttempts: number = 60, interval: number = 3000): Promise<ChapterAiFixResponse | null> => {
    for (let i = 0; i < maxAttempts; i++) {
      try {
        const task = await getTaskStatus(taskId)

        if (task.status === 'completed' && task.result) {
          // 任务完成，返回修复结果
          return task.result as ChapterAiFixResponse
        }

        if (task.status === 'failed' || task.status === 'cancelled') {
          throw new Error(task.errorMessage || 'AI剧情修复任务失败')
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
    throw new Error('AI剧情修复超时')
  }

  const generateChapterContent = async (planId: string) => {
    if (!projectId.value) return

    isGenerating.value = true
    generatingChapterId.value = planId

    try {
      // 使用异步接口生成章节
      const result = await generateChapterApi(projectId.value, planId)
      console.log('AI创作任务已创建:', result)

      if (!result.taskId) {
        throw new Error('未返回任务ID')
      }

      // 保存任务状态（含taskId）到 localStorage，用于页面刷新恢复
      saveGeneratingTask(planId, result.taskId)

      // 轮询任务状态，等待生成完成
      await pollTaskUntilComplete(result.taskId)

      // 任务完成后，加载生成的章节内容
      const chapter = await getChapterByPlanId(projectId.value, planId)
      if (chapter && chapter.content && chapter.content.trim().length > 0) {
        currentChapter.value = chapter
        content.value = chapter.content
        isChapterGenerated.value = true
        isDirty.value = false
        lastSavedAt.value = new Date()
        console.log('章节生成完成，内容长度:', chapter.content.length)
      } else {
        throw new Error('章节生成完成但内容为空')
      }
    } catch (e) {
      console.error('Failed to generate chapter:', e)
      throw e
    } finally {
      isGenerating.value = false
      generatingChapterId.value = null
      clearGeneratingTask()
    }
  }

  // 轮询任务状态直到完成
  const pollTaskUntilComplete = async (taskId: string, maxAttempts: number = 120, interval: number = 3000) => {
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise(resolve => setTimeout(resolve, interval))

      try {
        const task = await getTaskStatus(taskId)
        console.log(`轮询任务状态第${i + 1}次，状态: ${task.status}, 进度: ${task.progress || 0}%`)

        if (task.status === 'completed') {
          console.log('任务已完成:', task.result)
          return
        }

        if (task.status === 'failed') {
          throw new Error(task.errorMessage || '章节生成失败')
        }

        if (task.status === 'cancelled') {
          throw new Error('章节生成任务已取消')
        }
        // 继续轮询 (pending/running)
      } catch (e: any) {
        // 如果是我们主动抛出的错误（failed/cancelled），直接抛出
        if (e.message?.includes('失败') || e.message?.includes('取消')) {
          throw e
        }
        // 网络错误等，继续轮询
        console.log(`轮询第${i + 1}次出错，继续等待:`, e)
      }
    }
    throw new Error('章节生成超时')
  }

  // 开始AI剧情修复
  const startAiFix = async (chapterId: string) => {
    if (!projectId.value || !content.value) return

    isFixing.value = true
    fixingChapterId.value = chapterId

    try {
      // 调用异步API
      const result = await fixChapterWithAIAsync(projectId.value, chapterId)

      console.log('AI剧情修复任务已创建:', result)

      // 保存任务状态
      fixingTaskId.value = result.taskId
      saveFixingTask(chapterId, result.taskId)

      // 轮询等待修复完成
      const fixResult = await pollAiFixStatus(result.taskId)

      if (fixResult) {
        aiFixResult.value = fixResult
        console.log('AI剧情修复完成，修改数:', fixResult.totalFixes || 0)
      }
    } catch (e: any) {
      console.error('AI剧情修复失败:', e)
      throw e
    } finally {
      isFixing.value = false
      fixingTaskId.value = null
      // 注意：不在这里清除 localStorage，因为用户可能还没确认
    }
  }

  // 应用AI修复结果
  const applyAiFixResult = () => {
    if (aiFixResult.value?.fixedContent) {
      content.value = aiFixResult.value.fixedContent
      isDirty.value = true
    }
    aiFixResult.value = null
    fixingChapterId.value = null
    clearFixingTask()
  }

  // 丢弃AI修复结果
  const discardAiFixResult = () => {
    aiFixResult.value = null
    fixingChapterId.value = null
    clearFixingTask()
  }

  // 恢复生成任务状态（页面刷新后调用）
  const restoreGeneratingState = async () => {
    if (!projectId.value) return

    try {
      const taskData = localStorage.getItem(getChapterGenerateTaskKey(projectId.value))
      if (!taskData) return

      const { chapterId: savedChapterId, taskId: savedTaskId, startTime } = JSON.parse(taskData)

      // 检查任务是否超时（超过10分钟认为已超时）
      if (Date.now() - startTime > 10 * 60 * 1000) {
        clearGeneratingTask()
        return
      }

      // 恢复生成状态
      isGenerating.value = true
      generatingChapterId.value = savedChapterId

      if (savedTaskId) {
        // 有taskId，通过任务状态轮询
        await pollTaskUntilComplete(savedTaskId)
      } else {
        // 兼容旧数据：没有taskId，回退到内容轮询
        await pollChapterGeneration(savedChapterId)
      }

      // 生成完成后加载章节内容
      const chapter = await getChapterByPlanId(projectId.value, savedChapterId)
      if (chapter && chapter.content) {
        currentChapter.value = chapter
        content.value = chapter.content
        isChapterGenerated.value = true
        isDirty.value = false
      }

      isGenerating.value = false
      generatingChapterId.value = null
      clearGeneratingTask()
    } catch (e) {
      console.error('Failed to restore generating state:', e)
      isGenerating.value = false
      generatingChapterId.value = null
      clearGeneratingTask()
    }
  }

  // 恢复AI修复状态（页面刷新后调用）
  const restoreFixingState = async () => {
    if (!projectId.value) return

    try {
      const taskData = localStorage.getItem(getAiFixTaskKey(projectId.value))
      if (!taskData) return

      const { chapterId: savedChapterId, taskId, startTime } = JSON.parse(taskData)

      // 检查任务是否超时（超过10分钟认为已超时）
      if (Date.now() - startTime > 10 * 60 * 1000) {
        clearFixingTask()
        return
      }

      // 恢复修复状态
      isFixing.value = true
      fixingChapterId.value = savedChapterId
      fixingTaskId.value = taskId

      try {
        // 继续轮询
        const fixResult = await pollAiFixStatus(taskId)

        if (fixResult) {
          aiFixResult.value = fixResult
        }
      } catch (e: any) {
        console.error('恢复AI修复状态失败:', e)
      } finally {
        isFixing.value = false
        fixingTaskId.value = null
      }
    } catch (e) {
      console.error('恢复AI修复状态失败:', e)
      isFixing.value = false
      fixingTaskId.value = null
    }
  }

  const toggleDrawer = () => {
    isDrawerVisible.value = !isDrawerVisible.value
  }

  const setDrawerVisible = (visible: boolean) => {
    isDrawerVisible.value = visible
  }

  return {
    // ... existing exports (保持不变)
    projectId,
    currentChapter,
    content,
    isDirty,
    isSaving,
    lastSavedAt,
    chapterId,
    chapterTitle,
    wordCount,
    setProjectId,
    loadChapter,
    updateContent,
    saveContent,
    clearEditor,
    // 新增导出
    currentChapterPlan,
    isChapterGenerated,
    isGenerating,
    generatingChapterId,
    isDrawerVisible,
    loadChapterByPlan,
    setChapterPlan,
    generateChapterContent,
    restoreGeneratingState,
    toggleDrawer,
    setDrawerVisible,
    // AI剧情修复相关
    isFixing,
    fixingTaskId,
    fixingChapterId,
    aiFixResult,
    startAiFix,
    applyAiFixResult,
    discardAiFixResult,
    restoreFixingState,
  }
})
