<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  FileText,
  BookOpen,
  Palette,
  Tag,
  Sparkles,
  Check
} from 'lucide-vue-next'
import Btn from '@/components/ui/Btn.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import FormSection from './components/FormSection.vue'
import ToneSelector from './components/ToneSelector.vue'
import LengthSelector from './components/LengthSelector.vue'
import TagInput from './components/TagInput.vue'
import AIGenerateDialog from './components/AIGenerateDialog.vue'
import { createProject, generateProject } from '@/api/project'
import { success, error } from '@/utils/toast'
import type { NovelType, StoryTone, TargetLength } from '@/types/project'

const router = useRouter()

// 小说类型选项
const novelTypeOptions = [
  { value: 'fantasy', label: '玄幻' },
  { value: 'urban', label: '都市' },
  { value: 'scifi', label: '科幻' },
  { value: 'history', label: '历史' },
  { value: 'military', label: '军事' },
  { value: 'mystery', label: '悬疑' },
  { value: 'romance', label: '言情' },
  { value: 'gaming', label: '游戏' }
]

// 表单数据
const formData = reactive({
  name: '',
  description: '',
  novelType: '' as NovelType | '',
  storyTone: 'relaxed' as StoryTone,
  targetLength: 'medium' as TargetLength,
  tags: [] as string[]
})

// 表单验证错误
const errors = reactive({
  name: '',
  description: '',
  novelType: ''
})

// UI 状态
const submitting = ref(false)
const aiDialogVisible = ref(false)
const aiGenerating = ref(false)

// 验证表单
const validateForm = (): boolean => {
  let isValid = true

  // 重置错误
  errors.name = ''
  errors.description = ''
  errors.novelType = ''

  // 验证项目名称
  if (!formData.name.trim()) {
    errors.name = '请输入项目名称'
    isValid = false
  } else if (formData.name.length < 2 || formData.name.length > 50) {
    errors.name = '项目名称长度在 2 到 50 个字符'
    isValid = false
  }

  // 验证作品简介
  if (!formData.description.trim()) {
    errors.description = '请输入作品简介'
    isValid = false
  } else if (formData.description.length < 10 || formData.description.length > 500) {
    errors.description = '简介长度在 10 到 500 个字符'
    isValid = false
  }

  // 验证作品类型
  if (!formData.novelType) {
    errors.novelType = '请选择作品类型'
    isValid = false
  }

  return isValid
}

// 返回
const goBack = () => {
  router.back()
}

// AI 生成
const handleAIGenerate = async (idea: string) => {
  try {
    aiGenerating.value = true
    const result = await generateProject({ idea })
    if (result) {
      formData.name = result.name
      formData.description = result.description
      success('AI 生成成功')
      aiDialogVisible.value = false
    }
  } catch (err: any) {
    error(err.message || 'AI 生成失败，请稍后重试')
  } finally {
    aiGenerating.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) return

  try {
    submitting.value = true
    const projectId = await createProject({
      name: formData.name,
      description: formData.description,
      projectType: 'novel',
      storyTone: formData.storyTone,
      novelType: formData.novelType as NovelType,
      tags: formData.tags.length > 0 ? JSON.stringify(formData.tags) : '',
      targetLength: formData.targetLength
    })
    success('创建成功')
    router.push(`/project/${projectId}/overview`)
  } catch (err: any) {
    error(err.message || '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-purple-50 dark:from-gray-900 dark:via-slate-900 dark:to-gray-900 relative overflow-x-hidden">
    <!-- 装饰光晕 -->
    <div class="absolute top-0 right-0 w-[500px] h-[500px] bg-blue-500/20 dark:bg-blue-500/10 rounded-full blur-[128px] pointer-events-none" />
    <div class="absolute bottom-0 left-0 w-[400px] h-[400px] bg-purple-500/20 dark:bg-purple-500/10 rounded-full blur-[128px] pointer-events-none" />
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[300px] h-[300px] bg-pink-500/10 dark:bg-pink-500/5 rounded-full blur-[128px] pointer-events-none" />

    <!-- 顶部栏 -->
    <div class="sticky top-0 z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b border-gray-200/50 dark:border-gray-700/50">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 py-4">
            <button
              type="button"
              class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-gray-100/80 dark:bg-gray-800/80 text-gray-600 dark:text-gray-300 text-sm font-medium hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
              @click="goBack"
            >
              <ArrowLeft class="w-4 h-4" />
              返回工作台
            </button>
          </div>
        </div>

    <!-- 主内容 -->
    <main class="max-w-5xl mx-auto px-4 sm:px-6 py-8 space-y-6">
      <!-- 页面标题 -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 text-white text-sm font-medium mb-4">
          <Sparkles class="w-4 h-4" />
          开始创作
        </div>
        <h1 class="text-3xl sm:text-4xl font-bold bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
          创建你的小说项目
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-2">
          填写项目信息，AI 将辅助你完成创作
        </p>
      </div>

      <!-- 项目名称 -->
      <FormSection title="项目名称" description="为你的作品起一个响亮的名字" :icon="FileText" required>
        <div class="flex gap-3">
          <div class="flex-1">
            <Input
              v-model="formData.name"
              placeholder="例如：修仙从代码开始"
              :error="errors.name"
            />
            <p class="mt-1.5 text-xs text-gray-400 text-right">{{ formData.name.length }}/50</p>
          </div>
          <Btn
            variant="secondary"
            size="sm"
            class="flex-shrink-0"
            @click="aiDialogVisible = true"
          >
            <Sparkles class="w-3.5 h-3.5" />
            AI生成
          </Btn>
        </div>
      </FormSection>

      <!-- 作品简介 -->
      <FormSection title="作品简介" description="描述你的故事构思和核心设定" :icon="BookOpen" required>
        <textarea
          v-model="formData.description"
          rows="5"
          placeholder="例如：一个现代程序员穿越到修仙世界，发现可以用编程的方式修炼功法..."
          :class="[
            'w-full px-4 py-3 rounded-xl border bg-white/50 dark:bg-gray-800/50 text-gray-900 dark:text-gray-100 placeholder-gray-400 resize-none transition-all focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500',
            errors.description
              ? 'border-red-500'
              : 'border-gray-200 dark:border-gray-700'
          ]"
        />
        <p v-if="errors.description" class="mt-1 text-sm text-red-500">{{ errors.description }}</p>
        <p class="mt-1.5 text-xs text-gray-400 text-right">{{ formData.description.length }}/500</p>
      </FormSection>

      <!-- 作品类型 -->
      <FormSection title="作品类型" description="选择小说的主要题材" :icon="Palette" required>
        <Select
          v-model="formData.novelType"
          :options="novelTypeOptions"
          placeholder="选择类型"
        />
        <p v-if="errors.novelType" class="mt-1 text-sm text-red-500">{{ errors.novelType }}</p>
      </FormSection>

      <!-- 故事基调 -->
      <FormSection title="故事基调" description="选择作品的情感色彩和风格">
        <ToneSelector v-model="formData.storyTone" />
      </FormSection>

      <!-- 预计字数 -->
      <FormSection title="预计字数" description="规划作品的篇幅大小">
        <LengthSelector v-model="formData.targetLength" />
      </FormSection>

      <!-- 作品标签 -->
      <FormSection title="作品标签" description="添加标签让作品更容易被发现" :icon="Tag">
        <TagInput v-model="formData.tags" />
      </FormSection>

      <!-- 操作按钮 -->
      <div class="sticky bottom-0 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-t border-gray-200/50 dark:border-gray-700/50 -mx-4 sm:-mx-6 px-4 sm:px-6 py-4 mt-8">
        <div class="flex gap-3">
          <Btn variant="secondary" block size="lg" @click="goBack">
            取消
          </Btn>
          <Btn block size="lg" :loading="submitting" @click="handleSubmit">
            <Check class="w-5 h-5" />
            创建项目
          </Btn>
        </div>
      </div>
    </main>

    <!-- AI 生成对话框 -->
    <AIGenerateDialog
      v-model:visible="aiDialogVisible"
      :loading="aiGenerating"
      @generate="handleAIGenerate"
    />
  </div>
</template>
