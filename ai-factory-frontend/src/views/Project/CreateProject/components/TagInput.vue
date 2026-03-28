<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { X, Plus } from 'lucide-vue-next'
import { error } from '@/utils/toast'

interface Props {
  modelValue?: string[]
  maxTags?: number
  popularTags?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  maxTags: 5,
  popularTags: () => ['系统流', '无敌', '重生', '穿越', '爽文', '升级流']
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref<HTMLInputElement>()

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const addTag = () => {
  const tag = inputValue.value.trim()
  if (!tag) {
    inputVisible.value = false
    return
  }

  if (props.modelValue.length >= props.maxTags) {
    error(`最多添加${props.maxTags}个标签`)
    inputValue.value = ''
    inputVisible.value = false
    return
  }

  if (props.modelValue.includes(tag)) {
    error('标签已存在')
    inputValue.value = ''
    return
  }

  emit('update:modelValue', [...props.modelValue, tag])
  inputValue.value = ''
  inputVisible.value = false
}

const removeTag = (tag: string) => {
  emit('update:modelValue', props.modelValue.filter(t => t !== tag))
}

const addPopularTag = (tag: string) => {
  if (props.modelValue.includes(tag)) {
    error('标签已存在')
    return
  }
  if (props.modelValue.length >= props.maxTags) {
    error(`最多添加${props.maxTags}个标签`)
    return
  }
  emit('update:modelValue', [...props.modelValue, tag])
}
</script>

<template>
  <div class="space-y-3">
    <!-- 已选标签 -->
    <div v-if="modelValue.length > 0" class="flex flex-wrap gap-2">
      <span
        v-for="tag in modelValue"
        :key="tag"
        class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gradient-to-r from-blue-500 to-purple-500 text-white text-sm font-medium rounded-lg"
      >
        {{ tag }}
        <button
          type="button"
          class="w-4 h-4 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
          @click="removeTag(tag)"
        >
          <X class="w-3 h-3" />
        </button>
      </span>
    </div>

    <!-- 输入区域 -->
    <div class="flex gap-2">
      <template v-if="inputVisible">
        <input
          ref="inputRef"
          v-model="inputValue"
          type="text"
          placeholder="输入标签名"
          class="flex-1 px-3 py-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-white/50 dark:bg-gray-800/50 text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500"
          @keyup.enter="addTag"
          @blur="addTag"
        />
      </template>
      <template v-else>
        <button
          type="button"
          :disabled="modelValue.length >= maxTags"
          class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-white/50 dark:bg-gray-800/50 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700/50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          @click="showInput"
        >
          <Plus class="w-4 h-4" />
          添加标签
        </button>
      </template>
    </div>

    <!-- 热门标签 -->
    <div class="flex items-center gap-2 flex-wrap">
      <span class="text-xs text-gray-500 dark:text-gray-400">热门：</span>
      <button
        v-for="tag in popularTags"
        :key="tag"
        type="button"
        :class="[
          'px-2.5 py-1 text-xs rounded-lg border transition-colors',
          modelValue.includes(tag)
            ? 'border-blue-500 text-blue-500 bg-blue-50 dark:bg-blue-900/20'
            : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:border-gray-300 dark:hover:border-gray-600'
        ]"
        @click="addPopularTag(tag)"
      >
        {{ tag }}
      </button>
    </div>
  </div>
</template>
