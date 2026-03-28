<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  Plus, Eye, ArrowRight, Link, Cpu, Pencil, Trash2, Star,
  EyeOff, MoreVertical, MessageSquare, Image, Mic, Video, Layers
} from 'lucide-vue-next'
import {
  getAiProviders,
  getAiProviderTemplates,
  saveAiProvider,
  deleteAiProvider,
  testAiProviderById
} from '@/api/settings'
import { success, error } from '@/utils/toast'
import type { AiProvider, AiProviderTemplate, ProviderType } from '@/types/settings'

// Props
const props = defineProps<{
  providerType?: ProviderType
}>()

// Emit
const emit = defineEmits<{
  (e: 'edit', data: { template?: AiProviderTemplate; provider?: AiProvider }): void
}>()

// 状态
const activeType = ref<ProviderType | 'all'>('all')
const showProviders = ref(true)
const testingProviderId = ref<number | null>(null)
const loadingTemplates = ref(false)
const loadingProviders = ref(false)

// 数据
const templates = ref<AiProviderTemplate[]>([])
const providers = ref<AiProvider[]>([])

// 缺失的图标列表
const missingIcons = new Set<string>()

// 类型选项
const typeOptions: { value: ProviderType | 'all'; label: string; icon: any }[] = [
  { value: 'all', label: '全部', icon: Layers },
  { value: 'llm', label: '语言模型', icon: MessageSquare },
  { value: 'image', label: '图像生成', icon: Image },
  { value: 'tts', label: '语音合成', icon: Mic },
  { value: 'video', label: '视频生成', icon: Video }
]

// 计算属性
const filteredTemplates = computed(() => {
  if (activeType.value === 'all') return templates.value
  return templates.value.filter(t => t.providerType === activeType.value)
})

const filteredProviders = computed(() => {
  if (activeType.value === 'all') return providers.value
  return providers.value.filter(p => p.providerType === activeType.value)
})

// 方法
const handleTypeChange = (type: ProviderType | 'all') => {
  activeType.value = type
  if (type !== 'all') {
    showProviders.value = true
  }
}

const handleSelectTemplate = (template: AiProviderTemplate) => {
  emit('edit', { template })
}

const handleEdit = (provider: AiProvider) => {
  emit('edit', { provider })
}

const handleTestConnection = async (provider: AiProvider) => {
  testingProviderId.value = provider.id
  try {
    const result = await testAiProviderById(provider.id)
    if (result.success) {
      success(result.message)
    } else {
      error(result.message)
    }
  } catch (err: any) {
    error(err.message || '连接测试失败')
  } finally {
    testingProviderId.value = null
  }
}

const setDefaultProvider = async (provider: AiProvider) => {
  try {
    await saveAiProvider({
      ...provider,
      isDefault: 1
    })
    success('已设为默认')
    loadProviders()
  } catch (err: any) {
    error(err.message || '操作失败')
  }
}

const toggleProviderEnabled = async (provider: AiProvider) => {
  try {
    await saveAiProvider({
      ...provider,
      enabled: provider.enabled === 1 ? 0 : 1
    })
    success(provider.enabled === 1 ? '已禁用' : '已启用')
    loadProviders()
  } catch (err: any) {
    error(err.message || '操作失败')
  }
}

const handleDelete = async (provider: AiProvider) => {
  if (!confirm('确定要删除这个配置吗？')) return

  try {
    await deleteAiProvider(provider.id)
    success('删除成功')
    loadProviders()
  } catch (err: any) {
    error(err.message || '删除失败')
  }
}

// 图标处理
const isIconUrlMissing = (iconUrl?: string) => {
  return iconUrl && missingIcons.has(iconUrl)
}

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  if (img.src) {
    missingIcons.add(img.src)
  }
}

const getTemplateEmoji = (templateCode: string) => {
  const emojiMap: Record<string, string> = {
    // LLM
    'openai_gpt4': '🧠',
    'claude_opus': '🤖',
    'deepseek_chat': '💬',
    'zhipu_glm4': '🎯',
    'baichuan_turbo': '⚡',
    'tongyi_qwen': '💭',
    'wenxin_ernie': '📝',
    'kimi_moonshot': '🌙',
    'lingyi_yi': '🎨',
    'minimax_abab': '🔮',
    'tiange_chat': '✨',
    'gemini_pro': '💎',
    'custom_llm': '⚡',
    // Image
    'midjourney_v6': '🖼️',
    'sd_stability': '🎨',
    'dalle3': '🖌️',
    'comfyui_local': '⚙️',
    'sdwebui_local': '🛠️',
    'custom_image': '🎨',
    // TTS
    'azure_tts': '🔊',
    'google_tts': '🗣️',
    'aliyun_tts': '📢',
    'tencent_tts': '🎙️',
    'elevenlabs': '🎤',
    'doubao_tts': '🫘',
    'custom_tts': '🔊',
    // Video
    'runway_gen2': '🎬',
    'pika_video': '🎥',
    'custom_video': '🎬'
  }
  return emojiMap[templateCode] || '🤖'
}

const getProviderEmoji = (providerCode: string) => {
  const emojiMap: Record<string, string> = {
    'openai': '🧠',
    'claude': '🤖',
    'deepseek': '💬',
    'zhipu': '🎯',
    'baichuan': '⚡',
    'tongyi': '💭',
    'wenxin': '📝',
    'kimi': '🌙',
    'lingyi': '🎨',
    'minimax': '🔮',
    'tiange': '✨',
    'gemini': '💎',
    'midjourney': '🖼️',
    'sd': '🎨',
    'dalle': '🖌️',
    'comfyui': '⚙️',
    'sdwebui': '🛠️',
    'azure': '🔊',
    'google': '🗣️',
    'aliyun': '📢',
    'tencent': '🎙️',
    'elevenlabs': '🎤',
    'doubao': '🫘',
    'runway': '🎬',
    'pika': '🎥',
    'custom': '🔧'
  }
  return emojiMap[providerCode] || '🤖'
}

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    llm: '语言模型',
    image: '图像生成',
    tts: '语音合成',
    video: '视频生成'
  }
  return labels[type] || type
}

const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    llm: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    image: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    tts: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
    video: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
  }
  return colors[type] || 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400'
}

// 数据加载
const loadTemplates = async () => {
  loadingTemplates.value = true
  try {
    templates.value = await getAiProviderTemplates()
  } catch (err) {
    console.error('Failed to load templates:', err)
  } finally {
    loadingTemplates.value = false
  }
}

const loadProviders = async () => {
  loadingProviders.value = true
  try {
    const providersData = await getAiProviders(activeType.value === 'all' ? undefined : activeType.value)

    // 为每个 provider 添加 iconUrl（从模板中获取）
    providers.value = providersData.map((provider) => {
      const template = templates.value.find(t => t.templateCode === provider.providerCode)
      return {
        ...provider,
        iconUrl: template?.iconUrl || provider.iconUrl || ''
      }
    })
  } catch (err) {
    console.error('Failed to load providers:', err)
  } finally {
    loadingProviders.value = false
  }
}

// 监听 providerType prop 变化
watch(() => props.providerType, (newType) => {
  if (newType) {
    activeType.value = newType
    showProviders.value = true
  } else {
    activeType.value = 'all'
    showProviders.value = true
  }
}, { immediate: true })

onMounted(() => {
  // 如果传入了 providerType，使用它；否则使用 'all'
  if (props.providerType) {
    activeType.value = props.providerType
    showProviders.value = true
  } else {
    activeType.value = 'all'
    showProviders.value = true
  }
  loadTemplates()
  loadProviders()
})

// 暴露刷新方法供父组件调用
defineExpose({
  refresh: loadProviders
})
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- 操作栏 -->
    <div class="flex items-center justify-between mb-6 p-4 rounded-xl bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
      <div class="flex-1">
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
          {{ showProviders ? '我的配置' : '模型模板市场' }}
        </h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
          {{ showProviders ? `已添加的 ${getTypeLabel(activeType === 'all' ? 'llm' : activeType)} 服务配置` : '选择模板快速添加 AI 服务配置' }}
        </p>
      </div>
      <button
        v-if="showProviders"
        class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
               bg-gradient-to-r from-blue-500 to-purple-500 text-white
               hover:from-blue-600 hover:to-purple-600 transition-all shadow-md hover:shadow-lg"
        @click="showProviders = false"
      >
        <Plus class="w-4 h-4" />
        添加模型
      </button>
      <button
        v-else-if="providers.length > 0"
        class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
               bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400
               border border-blue-200 dark:border-blue-800
               hover:bg-blue-100 dark:hover:bg-blue-900/50 transition-colors"
        @click="showProviders = true"
      >
        <Eye class="w-4 h-4" />
        查看已配置
      </button>
    </div>

    <!-- 类型筛选按钮组 -->
    <div class="flex flex-wrap gap-2 mb-6">
      <button
        v-for="option in typeOptions"
        :key="option.value"
        :class="[
          'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all',
          activeType === option.value
            ? 'bg-blue-500 text-white shadow-sm'
            : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700'
        ]"
        @click="handleTypeChange(option.value)"
      >
        <component :is="option.icon" class="w-4 h-4" />
        {{ option.label }}
      </button>
    </div>

    <!-- 模板列表视图 -->
    <div v-if="!showProviders" class="flex-1 overflow-y-auto">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="template in filteredTemplates"
          :key="template.id"
          class="group relative bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700
                 hover:border-blue-300 dark:hover:border-blue-600 cursor-pointer transition-all
                 hover:shadow-lg hover:-translate-y-1 overflow-hidden"
          @click="handleSelectTemplate(template)"
        >
          <!-- 悬停光效 -->
          <div class="absolute inset-0 bg-gradient-to-br from-blue-500/5 to-purple-500/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>

          <div class="relative flex items-center gap-3 p-4">
            <!-- 图标 -->
            <div class="w-12 h-12 rounded-lg bg-gradient-to-br from-blue-100 to-purple-100 dark:from-blue-900/30 dark:to-purple-900/30 flex items-center justify-center flex-shrink-0 overflow-hidden">
              <img
                v-if="template.iconUrl && !isIconUrlMissing(template.iconUrl)"
                :src="template.iconUrl"
                :alt="template.displayName"
                class="w-full h-full object-contain p-2"
                @error="handleImageError"
              />
              <span v-else class="text-2xl">{{ getTemplateEmoji(template.templateCode) }}</span>
            </div>

            <!-- 信息 -->
            <div class="flex-1 min-w-0">
              <h3 class="font-semibold text-gray-900 dark:text-white truncate">
                {{ template.displayName }}
              </h3>
              <p class="text-sm text-gray-500 dark:text-gray-400 truncate mt-0.5">
                {{ template.description }}
              </p>
            </div>

            <!-- 箭头 -->
            <ArrowRight class="w-5 h-5 text-blue-500 opacity-0 group-hover:opacity-100 transition-opacity transform group-hover:translate-x-1" />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredTemplates.length === 0 && !loadingTemplates" class="flex flex-col items-center justify-center py-16 text-gray-500 dark:text-gray-400">
        <Layers class="w-12 h-12 mb-4 opacity-50" />
        <p>暂无可用的模板</p>
      </div>
    </div>

    <!-- 已配置列表视图 -->
    <div v-else class="flex-1 overflow-y-auto">
      <div class="flex flex-col gap-3">
        <div
          v-for="provider in filteredProviders"
          :key="provider.id"
          :class="[
            'flex items-center justify-between gap-4 p-4 rounded-xl border transition-all',
            provider.isDefault === 1
              ? 'border-green-300 dark:border-green-700 bg-green-50/50 dark:bg-green-900/10'
              : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-blue-300 dark:hover:border-blue-600',
            provider.enabled === 0 ? 'opacity-60' : ''
          ]"
        >
          <!-- 左侧信息 -->
          <div class="flex items-center gap-3 flex-1 min-w-0">
            <!-- 图标和状态 -->
            <div class="relative flex-shrink-0">
              <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-100 to-purple-100 dark:from-blue-900/30 dark:to-purple-900/30 flex items-center justify-center overflow-hidden border border-gray-200 dark:border-gray-700">
                <img
                  v-if="provider.iconUrl && !isIconUrlMissing(provider.iconUrl)"
                  :src="provider.iconUrl"
                  :alt="provider.providerName"
                  class="w-full h-full object-contain p-1.5"
                  @error="handleImageError"
                />
                <span v-else class="text-xl">{{ getProviderEmoji(provider.providerCode) }}</span>
              </div>
              <!-- 状态点 -->
              <div
                :class="[
                  'absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-white dark:border-gray-800',
                  provider.enabled === 1 ? 'bg-green-500' : 'bg-red-500'
                ]"
              ></div>
            </div>

            <!-- 详细信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="font-semibold text-gray-900 dark:text-white">
                  {{ provider.providerName }}
                </h3>
                <span
                  v-if="provider.isDefault === 1"
                  class="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                >
                  默认
                </span>
                <span
                  v-if="provider.enabled === 0"
                  class="px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400"
                >
                  已禁用
                </span>
                <span
                  :class="[
                    'px-2 py-0.5 rounded text-xs font-medium',
                    getTypeColor(provider.providerType)
                  ]"
                >
                  {{ getTypeLabel(provider.providerType) }}
                </span>
              </div>
              <div class="flex items-center gap-4 mt-1 text-sm text-gray-500 dark:text-gray-400">
                <span class="flex items-center gap-1">
                  <Link class="w-3.5 h-3.5" />
                  <span class="truncate max-w-[150px]">{{ provider.apiEndpoint || '未设置' }}</span>
                </span>
                <span class="flex items-center gap-1">
                  <Cpu class="w-3.5 h-3.5" />
                  <span class="truncate max-w-[100px]">{{ provider.model || '未设置' }}</span>
                </span>
              </div>
            </div>
          </div>

          <!-- 右侧操作 -->
          <div class="flex items-center gap-2 flex-shrink-0">
            <button
              class="p-2 rounded-lg text-gray-500 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors"
              title="编辑"
              @click="handleEdit(provider)"
            >
              <Pencil class="w-4 h-4" />
            </button>
            <button
              class="p-2 rounded-lg text-gray-500 hover:text-green-600 hover:bg-green-50 dark:hover:bg-green-900/30 transition-colors disabled:opacity-50"
              title="测试连接"
              :disabled="testingProviderId === provider.id"
              @click="handleTestConnection(provider)"
            >
              <svg v-if="testingProviderId === provider.id" class="w-4 h-4 animate-spin" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
            </button>

            <!-- 更多操作下拉 -->
            <div class="relative group">
              <button
                class="p-2 rounded-lg text-gray-500 hover:text-purple-600 hover:bg-purple-50 dark:hover:bg-purple-900/30 transition-colors"
                title="更多操作"
              >
                <MoreVertical class="w-4 h-4" />
              </button>

              <!-- 下拉菜单 -->
              <div class="absolute right-0 top-full mt-1 w-36 py-1 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-10">
                <button
                  v-if="provider.isDefault !== 1"
                  class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                  @click="setDefaultProvider(provider)"
                >
                  <Star class="w-4 h-4" />
                  设为默认
                </button>
                <button
                  class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                  @click="toggleProviderEnabled(provider)"
                >
                  <EyeOff v-if="provider.enabled === 1" class="w-4 h-4" />
                  <Eye v-else class="w-4 h-4" />
                  {{ provider.enabled === 1 ? '禁用' : '启用' }}
                </button>
                <div class="my-1 border-t border-gray-200 dark:border-gray-700"></div>
                <button
                  class="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                  @click="handleDelete(provider)"
                >
                  <Trash2 class="w-4 h-4" />
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredProviders.length === 0 && !loadingProviders" class="flex flex-col items-center justify-center py-16 text-gray-500 dark:text-gray-400">
        <Layers class="w-12 h-12 mb-4 opacity-50" />
        <p>暂无配置，点击上方"添加模型"开始</p>
      </div>
    </div>
  </div>
</template>
