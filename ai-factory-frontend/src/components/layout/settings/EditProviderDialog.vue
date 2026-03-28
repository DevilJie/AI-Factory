<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  Eye, EyeOff, Link, Cpu, Info, Loader2, Plug, Check
} from 'lucide-vue-next'
import { saveAiProvider, testAiProviderById } from '@/api/settings'
import { success, error } from '@/utils/toast'
import type { AiProvider, AiProviderTemplate, ProviderType } from '@/types/settings'

// Props
const props = defineProps<{
  modelValue: boolean
  template?: AiProviderTemplate | null
  provider?: AiProvider | null
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

// 状态
const saving = ref(false)
const testing = ref(false)
const showApiKey = ref(false)
const isDefault = ref(false)
const isEnabled = ref(true)

// 密钥修改追踪
const isApiKeyModified = ref(false)
const originalApiKey = ref('')

// 表单数据
const formData = ref({
  apiKey: '',
  model: '',
  apiEndpoint: '',
  temperature: 0.7,
  maxTokens: 8000,
  topP: 1
})

// 缺失的图标列表
const missingIcons = new Set<string>()

// 计算属性
const isEdit = computed(() => !!props.provider)
const isCustomTemplate = computed(() => {
  const code = props.template?.templateCode || props.provider?.providerCode
  return code?.startsWith('custom_')
})

const currentTemplate = computed(() => {
  if (props.template) return props.template
  // 编辑模式下，从 provider 获取信息构造一个简单的 template 对象
  if (props.provider) {
    return {
      id: 0,
      templateCode: props.provider.providerCode,
      displayName: props.provider.providerName,
      description: getTypeLabel(props.provider.providerType),
      providerType: props.provider.providerType,
      iconUrl: props.provider.iconUrl
    } as AiProviderTemplate
  }
  return null
})

const showApiEndpoint = computed(() => {
  return isCustomTemplate.value
})

const isLlm = computed(() => {
  const type = props.template?.providerType || props.provider?.providerType
  return type === 'llm'
})

// 解析 configJson
const parseConfigJson = (json?: string): { temperature: number; maxTokens: number; topP: number } => {
  const defaults = { temperature: 0.7, maxTokens: 8000, topP: 1 }
  if (!json) return defaults
  try {
    const parsed = JSON.parse(json)
    return {
      temperature: parsed.temperature ?? defaults.temperature,
      maxTokens: parsed.maxTokens ?? defaults.maxTokens,
      topP: parsed.topP ?? defaults.topP
    }
  } catch {
    return defaults
  }
}

// 监听 modelValue
watch(() => props.modelValue, (val) => {
  if (val) {
    initForm()
  }
})

// 初始化表单
const initForm = () => {
  isApiKeyModified.value = false

  if (props.provider) {
    // 编辑模式：使用 provider 数据填充
    const config = parseConfigJson(props.provider.configJson)
    formData.value = {
      apiKey: props.provider.apiKey || '',
      model: props.provider.model || '',
      apiEndpoint: props.provider.apiEndpoint || '',
      temperature: config.temperature,
      maxTokens: config.maxTokens,
      topP: config.topP
    }
    originalApiKey.value = props.provider.apiKey || ''
    isDefault.value = props.provider.isDefault === 1
    isEnabled.value = props.provider.enabled === 1
  } else if (props.template) {
    // 新建模式：使用模板默认值填充
    formData.value = {
      apiKey: '',
      model: props.template.defaultModel || '',
      apiEndpoint: props.template.defaultEndpoint || '',
      temperature: 0.7,
      maxTokens: 8000,
      topP: 1
    }
    originalApiKey.value = ''
    isDefault.value = false
    isEnabled.value = true
  }
}

// 追踪密钥修改
const handleApiKeyInput = () => {
  isApiKeyModified.value = true
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

// 关闭对话框
const handleClose = () => {
  emit('update:modelValue', false)
}

// 测试连接
const handleTest = async () => {
  // 编辑模式：使用ID测试，后端查询真实密钥
  if (!props.provider?.id) {
    error('配置ID不存在')
    return
  }

  testing.value = true
  try {
    const result = await testAiProviderById(props.provider.id)
    if (result.success) {
      success(result.message)
    } else {
      error(result.message)
    }
  } catch (err: any) {
    error(err.message || '连接测试失败')
  } finally {
    testing.value = false
  }
}

// 保存配置
const handleSave = async () => {
  if (!formData.value.apiKey && !isEdit.value) {
    error('请输入 API Key')
    return
  }
  if (!formData.value.model) {
    error('请输入模型名称')
    return
  }
  if (showApiEndpoint.value && !formData.value.apiEndpoint) {
    error('请输入 API 端点')
    return
  }

  saving.value = true
  try {
    const request: any = {
      providerType: props.template?.providerType || props.provider?.providerType,
      providerCode: props.template?.templateCode || props.provider?.providerCode,
      providerName: props.template?.displayName || props.provider?.providerName || '',
      model: formData.value.model,
      isDefault: isDefault.value ? 1 : 0,
      enabled: isEnabled.value ? 1 : 0
    }

    // 处理 API Key：只有新建模式或密钥被修改时才发送
    if (!isEdit.value || isApiKeyModified.value) {
      request.apiKey = formData.value.apiKey
    }

    // 处理 API Endpoint
    if (showApiEndpoint.value) {
      request.apiEndpoint = formData.value.apiEndpoint
    } else if (props.template?.defaultEndpoint) {
      request.apiEndpoint = props.template.defaultEndpoint
    } else if (props.provider?.apiEndpoint) {
      request.apiEndpoint = props.provider.apiEndpoint
    }

    // 编辑模式下传递 id
    if (isEdit.value && props.provider) {
      request.id = props.provider.id
    }

    // 处理 configJson（仅 LLM 类型）
    if (isLlm.value) {
      request.configJson = JSON.stringify({
        temperature: formData.value.temperature,
        maxTokens: formData.value.maxTokens,
        topP: formData.value.topP
      })
    }

    await saveAiProvider(request)
    success('保存成功')
    emit('saved')
    handleClose()
  } catch (err: any) {
    error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 获取类型标签
const getTypeLabel = (type: ProviderType | string) => {
  const labels: Record<string, string> = {
    llm: '语言模型',
    image: '图像生成',
    tts: '语音合成',
    video: '视频生成'
  }
  return labels[type] || type
}

// 获取类型颜色
const getTypeColor = (type: ProviderType | string) => {
  const colors: Record<string, string> = {
    llm: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    image: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    tts: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
    video: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
  }
  return colors[type] || 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400'
}

// 获取模板/提供商 emoji
const getEmoji = (code: string) => {
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
    // Provider codes
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
    // Image
    'midjourney_v6': '🖼️',
    'sd_stability': '🎨',
    'dalle3': '🖌️',
    'comfyui_local': '⚙️',
    'sdwebui_local': '🛠️',
    'custom_image': '🎨',
    'midjourney': '🖼️',
    'sd': '🎨',
    'dalle': '🖌️',
    'comfyui': '⚙️',
    'sdwebui': '🛠️',
    // TTS
    'azure_tts': '🔊',
    'google_tts': '🗣️',
    'aliyun_tts': '📢',
    'tencent_tts': '🎙️',
    'elevenlabs': '🎤',
    'doubao_tts': '🫘',
    'custom_tts': '🔊',
    'azure': '🔊',
    'google': '🗣️',
    'aliyun': '📢',
    'tencent': '🎙️',
    'doubao': '🫘',
    // Video
    'runway_gen2': '🎬',
    'pika_video': '🎥',
    'custom_video': '🎬',
    'runway': '🎬',
    'pika': '🎥',
    'custom': '🔧'
  }
  return emojiMap[code] || '🤖'
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      leave-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[9999] flex items-center justify-center p-4"
      >
        <!-- 遮罩层 -->
        <div
          class="absolute inset-0 bg-black/50 backdrop-blur-sm"
          @click="handleClose"
        ></div>

        <!-- 对话框主体 -->
        <div
          class="relative w-full max-w-xl bg-white dark:bg-gray-900 rounded-2xl shadow-2xl
                 border border-gray-200 dark:border-gray-700 overflow-hidden"
        >
          <!-- 头部：模板信息 -->
          <div v-if="currentTemplate" class="p-6 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-4">
                <!-- 图标 -->
                <div class="w-14 h-14 rounded-xl bg-gradient-to-br from-blue-100 to-purple-100 dark:from-blue-900/30 dark:to-purple-900/30 flex items-center justify-center flex-shrink-0 overflow-hidden border border-gray-200 dark:border-gray-700">
                  <img
                    v-if="currentTemplate.iconUrl && !isIconUrlMissing(currentTemplate.iconUrl)"
                    :src="currentTemplate.iconUrl"
                    :alt="currentTemplate.displayName"
                    class="w-full h-full object-contain p-2"
                    @error="handleImageError"
                  />
                  <span v-else class="text-2xl">{{ getEmoji(currentTemplate.templateCode) }}</span>
                </div>
                <!-- 名称和描述 -->
                <div>
                  <h2 class="text-xl font-bold text-gray-900 dark:text-white">
                    {{ isEdit ? '编辑配置' : '添加配置' }}
                  </h2>
                  <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
                    {{ currentTemplate.displayName }} - {{ currentTemplate.description }}
                  </p>
                </div>
              </div>
              <!-- 类型标签 -->
              <span
                :class="[
                  'px-3 py-1 rounded-full text-xs font-medium',
                  getTypeColor(currentTemplate.providerType)
                ]"
              >
                {{ getTypeLabel(currentTemplate.providerType) }}
              </span>
            </div>
          </div>

          <!-- 自定义模板提示 -->
          <div v-if="isCustomTemplate" class="mx-6 mt-4 flex items-center gap-2 p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800/50">
            <Info class="w-4 h-4 text-amber-600 dark:text-amber-400 flex-shrink-0" />
            <span class="text-sm text-amber-700 dark:text-amber-300">自定义配置 - 请手动填写所有连接信息</span>
          </div>

          <!-- 表单区域 -->
          <div class="p-6 space-y-5">
            <!-- API Key -->
            <div class="space-y-2">
              <label class="flex items-center gap-1 text-sm font-medium text-gray-700 dark:text-gray-300">
                API Key
                <span class="text-red-500">*</span>
              </label>
              <div class="relative">
                <input
                  v-model="formData.apiKey"
                  :type="showApiKey ? 'text' : 'password'"
                  placeholder="sk-..."
                  class="w-full px-4 py-2.5 pr-20 rounded-lg border border-gray-300 dark:border-gray-600
                         bg-white dark:bg-gray-800 text-gray-900 dark:text-white
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent
                         placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-all"
                  @input="handleApiKeyInput"
                />
                <button
                  type="button"
                  class="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 rounded-md
                         text-gray-400 hover:text-gray-600 dark:hover:text-gray-300
                         hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  @click="showApiKey = !showApiKey"
                >
                  <Eye v-if="!showApiKey" class="w-4 h-4" />
                  <EyeOff v-else class="w-4 h-4" />
                </button>
              </div>
              <!-- 密钥脱敏提示 -->
              <p v-if="isEdit && !isApiKeyModified" class="flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
                <Info class="w-3.5 h-3.5" />
                密钥已脱敏显示，如需更新请输入新密钥
              </p>
            </div>

            <!-- 模型名称 -->
            <div class="space-y-2">
              <label class="flex items-center gap-1 text-sm font-medium text-gray-700 dark:text-gray-300">
                模型名称
                <span class="text-red-500">*</span>
              </label>
              <div class="relative">
                <Cpu class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  v-model="formData.model"
                  type="text"
                  :placeholder="template?.defaultModel || 'gpt-4-turbo'"
                  class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-gray-300 dark:border-gray-600
                         bg-white dark:bg-gray-800 text-gray-900 dark:text-white
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent
                         placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-all"
                />
              </div>
              <p v-if="template?.defaultModel && !isCustomTemplate" class="text-xs text-blue-600 dark:text-blue-400">
                默认: {{ template.defaultModel }}
              </p>
            </div>

            <!-- API 端点 (自定义模板时显示) -->
            <div v-if="showApiEndpoint" class="space-y-2">
              <label class="flex items-center gap-1 text-sm font-medium text-gray-700 dark:text-gray-300">
                API 端点
                <span class="text-red-500">*</span>
              </label>
              <div class="relative">
                <Link class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  v-model="formData.apiEndpoint"
                  type="text"
                  placeholder="https://api.example.com/v1"
                  class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-gray-300 dark:border-gray-600
                         bg-white dark:bg-gray-800 text-gray-900 dark:text-white
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent
                         placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-all"
                />
              </div>
            </div>

            <!-- API 端点 (预设模板时只读显示) -->
            <div v-else-if="template?.defaultEndpoint && !isEdit" class="space-y-2">
              <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                API 端点
              </label>
              <div class="px-4 py-2.5 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800/50">
                <span class="text-sm font-mono text-blue-700 dark:text-blue-300">{{ template.defaultEndpoint }}</span>
              </div>
            </div>

            <!-- 模型参数（仅 LLM 类型） -->
            <div v-if="isLlm" class="space-y-4 p-4 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">模型参数</h3>

              <!-- 温度 -->
              <div class="space-y-1.5">
                <div class="flex items-center justify-between">
                  <label class="text-sm text-gray-600 dark:text-gray-400">温度 (Temperature)</label>
                  <span class="text-sm font-mono text-gray-900 dark:text-white">{{ formData.temperature }}</span>
                </div>
                <input
                  v-model.number="formData.temperature"
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-500"
                />
                <div class="flex justify-between text-xs text-gray-400">
                  <span>精确 (0)</span>
                  <span>创意 (1)</span>
                </div>
                <p class="text-xs text-gray-400 dark:text-gray-500">控制输出的随机性，值越低越确定和集中，值越高越多样和创造性</p>
              </div>

              <!-- 最大 Token 数 -->
              <div class="space-y-1.5">
                <label class="text-sm text-gray-600 dark:text-gray-400">最大 Token 数 (Max Tokens)</label>
                <input
                  v-model.number="formData.maxTokens"
                  type="number"
                  min="1"
                  max="128000"
                  step="1"
                  class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600
                         bg-white dark:bg-gray-800 text-gray-900 dark:text-white
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent
                         transition-all"
                />
                <p class="text-xs text-gray-400 dark:text-gray-500">单次回复生成的最大 Token 数量，限制输出的长度</p>
              </div>

              <!-- 核采样 -->
              <div class="space-y-1.5">
                <div class="flex items-center justify-between">
                  <label class="text-sm text-gray-600 dark:text-gray-400">核采样 (Top P)</label>
                  <span class="text-sm font-mono text-gray-900 dark:text-white">{{ formData.topP }}</span>
                </div>
                <input
                  v-model.number="formData.topP"
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-500"
                />
                <div class="flex justify-between text-xs text-gray-400">
                  <span>0</span>
                  <span>1</span>
                </div>
                <p class="text-xs text-gray-400 dark:text-gray-500">控制候选词的范围，值越低只考虑概率最高的词，值越高则考虑更多可能的词</p>
              </div>
            </div>

            <!-- 开关区域 -->
            <div class="flex flex-col gap-3 pt-2">
              <!-- 设为默认 -->
              <div
                class="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                @click="isDefault = !isDefault"
              >
                <div>
                  <p class="text-sm font-medium text-gray-900 dark:text-white">设为默认</p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">同类型优先使用此模型</p>
                </div>
                <div
                  :class="[
                    'relative w-11 h-6 rounded-full transition-colors',
                    isDefault
                      ? 'bg-gradient-to-r from-blue-500 to-purple-500'
                      : 'bg-gray-300 dark:bg-gray-600'
                  ]"
                >
                  <div
                    :class="[
                      'absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform',
                      isDefault ? 'translate-x-5 left-0.5' : 'left-0.5'
                    ]"
                  ></div>
                </div>
              </div>

              <!-- 启用状态 -->
              <div
                class="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                @click="isEnabled = !isEnabled"
              >
                <div>
                  <p class="text-sm font-medium text-gray-900 dark:text-white">启用状态</p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">{{ isEnabled ? '已启用' : '已禁用' }}</p>
                </div>
                <div
                  :class="[
                    'relative w-11 h-6 rounded-full transition-colors',
                    isEnabled
                      ? 'bg-gradient-to-r from-blue-500 to-purple-500'
                      : 'bg-gray-300 dark:bg-gray-600'
                  ]"
                >
                  <div
                    :class="[
                      'absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform',
                      isEnabled ? 'translate-x-5 left-0.5' : 'left-0.5'
                    ]"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部按钮 -->
          <div class="flex items-center justify-end gap-3 p-6 bg-gray-50 dark:bg-gray-800/50 border-t border-gray-200 dark:border-gray-700">
            <button
              class="px-4 py-2 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-300
                     bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600
                     hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
              @click="handleClose"
            >
              取消
            </button>
            <button
              class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
                     text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30
                     border border-blue-200 dark:border-blue-800
                     hover:bg-blue-100 dark:hover:bg-blue-900/50 transition-colors
                     disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="testing"
              @click="handleTest"
            >
              <Loader2 v-if="testing" class="w-4 h-4 animate-spin" />
              <Plug v-else class="w-4 h-4" />
              测试连接
            </button>
            <button
              class="flex items-center gap-2 px-5 py-2 rounded-lg text-sm font-medium text-white
                     bg-gradient-to-r from-blue-500 to-purple-500
                     hover:from-blue-600 hover:to-purple-600
                     shadow-md hover:shadow-lg transition-all
                     disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="saving"
              @click="handleSave"
            >
              <Loader2 v-if="saving" class="w-4 h-4 animate-spin" />
              <Check v-else class="w-4 h-4" />
              保存配置
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
