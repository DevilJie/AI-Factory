<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import {
  Settings,
  Save,
  Loader2,
  ChevronDown,
  RefreshCw
} from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { getBasicSettings, saveBasicSettings, type BasicSettings } from '@/api/basicSettings'
import { success, error } from '@/utils/toast'

const route = useRoute()

// State
const loading = ref(false)
const saving = ref(false)
const formData = ref<BasicSettings>({})

// 选项配置
const narrativeStructures = [
  { value: 'linear', label: '线性叙事' },
  { value: 'flashback', label: '倒叙' },
  { value: 'interwoven', label: '交织叙事' },
  { value: 'multi-pov', label: '多视角叙事' },
  { value: 'parallel', label: '平行叙事' }
]

const endingTypes = [
  { value: 'happy', label: '大团圆' },
  { value: 'tragic', label: '悲剧' },
  { value: 'open', label: '开放式' },
  { value: 'bittersweet', label: '悲喜交加' },
  { value: 'twist', label: '反转' }
]

const endingTones = [
  { value: 'warm', label: '温馨治愈' },
  { value: 'sorrowful', label: '感伤' },
  { value: 'hopeful', label: '充满希望' },
  { value: 'reflective', label: '引人深思' },
  { value: 'shocking', label: '震撼' }
]

const writingStyles = [
  { value: 'simple', label: '简洁明快' },
  { value: 'detailed', label: '细腻详尽' },
  { value: 'poetic', label: '诗意抒情' },
  { value: 'humorous', label: '幽默诙谐' },
  { value: 'serious', label: '严肃庄重' }
]

const perspectives = [
  { value: 'first', label: '第一人称' },
  { value: 'third-limited', label: '第三人称限知' },
  { value: 'third-omniscient', label: '第三人称全知' }
]

const paces = [
  { value: 'slow', label: '舒缓' },
  { value: 'moderate', label: '适中' },
  { value: 'fast', label: '紧凑' },
  { value: 'variable', label: '快慢结合' }
]

const languageStyles = [
  { value: 'classical', label: '古典雅致' },
  { value: 'modern', label: '现代通俗' },
  { value: 'colloquial', label: '口语化' },
  { value: 'literary', label: '文学性' }
]

const descriptionFocuses = [
  { value: 'action', label: '动作描写' },
  { value: 'psychology', label: '心理描写' },
  { value: 'environment', label: '环境描写' },
  { value: 'dialogue', label: '对话描写' },
  { value: 'balanced', label: '均衡描写' }
]

const plotStagesOptions = [
  { value: 'three-act', label: '三幕式结构' },
  { value: 'hero-journey', label: '英雄之旅' },
  { value: 'five-act', label: '五幕式结构' },
  { value: 'custom', label: '自定义' }
]

// 获取项目ID
const projectId = () => route.params.id as string

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const data = await getBasicSettings(projectId())
    if (data) {
      formData.value = {
        ...formData.value,
        ...data
      }
    }
  } catch (error: any) {
    if (error.response?.status !== 404) {
      error('加载基础设置失败')
      console.error('Failed to load basic settings:', error)
    }
  } finally {
    loading.value = false
  }
}

// 保存数据
const handleSave = async () => {
  saving.value = true
  try {
    await saveBasicSettings(projectId(), formData.value)
    success('保存成功')
    loadData()
  } catch (e) {
    error('保存失败')
    console.error('Failed to save basic settings:', e)
  } finally {
    saving.value = false
  }
}

// Lifecycle
onMounted(() => {
  loadData()
})

// Watch route changes
watch(() => route.params.id, () => {
  if (route.params.id) {
    loadData()
  }
})
</script>

<template>
  <div class="h-full flex flex-col bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <div class="flex-shrink-0 px-6 py-4 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
            <Settings class="w-5 h-5 text-gray-600 dark:text-gray-400" />
          </div>
          <div>
            <h1 class="text-lg font-semibold text-gray-900 dark:text-white">基础设置</h1>
            <p class="text-sm text-gray-500 dark:text-gray-400">设置小说的情节结构与叙事风格</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="loadData"
            :disabled="loading"
            class="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors disabled:opacity-50"
            title="刷新"
          >
            <RefreshCw :class="['w-5 h-5', { 'animate-spin': loading }]" />
          </button>
          <button
            @click="handleSave"
            :disabled="saving"
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
      <div v-if="loading" class="flex items-center justify-center h-full">
        <Loader2 class="w-8 h-8 text-blue-500 animate-spin" />
      </div>

      <div v-else class="space-y-8">
        <!-- 情节结构 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <h3 class="text-base font-medium text-gray-900 dark:text-white mb-6 pb-3 border-b border-gray-200 dark:border-gray-700">
            情节结构
          </h3>

          <div class="space-y-6">
            <!-- 叙事结构 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                叙事结构
              </label>
              <div class="relative">
                <select
                  v-model="formData.narrativeStructure"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择叙事结构</option>
                  <option v-for="item in narrativeStructures" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <!-- 结局类型 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                结局类型
              </label>
              <div class="relative">
                <select
                  v-model="formData.endingType"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择结局类型</option>
                  <option v-for="item in endingTypes" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <!-- 结局基调 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                结局基调
              </label>
              <div class="relative">
                <select
                  v-model="formData.endingTone"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择结局基调</option>
                  <option v-for="item in endingTones" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>
          </div>
        </div>

        <!-- 叙事风格 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <h3 class="text-base font-medium text-gray-900 dark:text-white mb-6 pb-3 border-b border-gray-200 dark:border-gray-700">
            叙事风格
          </h3>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <!-- 写作风格 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                写作风格
              </label>
              <div class="relative">
                <select
                  v-model="formData.writingStyle"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择写作风格</option>
                  <option v-for="item in writingStyles" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <!-- 叙事视角 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                叙事视角
              </label>
              <div class="relative">
                <select
                  v-model="formData.writingPerspective"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择叙事视角</option>
                  <option v-for="item in perspectives" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <!-- 叙事节奏 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                叙事节奏
              </label>
              <div class="relative">
                <select
                  v-model="formData.narrativePace"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择叙事节奏</option>
                  <option v-for="item in paces" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <!-- 语言风格 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                语言风格
              </label>
              <div class="relative">
                <select
                  v-model="formData.languageStyle"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择语言风格</option>
                  <option v-for="item in languageStyles" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <!-- 描写侧重 -->
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                描写侧重
              </label>
              <div class="relative">
                <select
                  v-model="formData.descriptionFocus"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择描写侧重</option>
                  <option v-for="item in descriptionFocuses" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>
          </div>
        </div>

        <!-- 复杂结构配置 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
          <h3 class="text-base font-medium text-gray-900 dark:text-white mb-6 pb-3 border-b border-gray-200 dark:border-gray-700">
            复杂结构配置
          </h3>

          <div class="space-y-6">
            <!-- 情节阶段 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                情节阶段
              </label>
              <div class="relative">
                <select
                  v-model="formData.plotStages"
                  class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">请选择情节阶段</option>
                  <option v-for="item in plotStagesOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <!-- 单章设置 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                单章设置
              </label>
              <textarea
                v-model="formData.singleChapterSettings"
                rows="3"
                placeholder="设置单章的字数要求、结构要求等..."
                class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              ></textarea>
            </div>

            <!-- 伏笔设置 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                伏笔设置
              </label>
              <textarea
                v-model="formData.foreshadowingSettings"
                rows="3"
                placeholder="设置伏笔的埋设频率、回收周期等..."
                class="w-full px-4 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              ></textarea>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
