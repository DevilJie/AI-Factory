<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { X, Save, Plus, ArrowUp, ArrowDown, Trash2, ChevronDown, ChevronUp } from 'lucide-vue-next'
import type { PowerSystem, PowerSystemLevel, PowerSystemLevelStep } from '@/api/powerSystem'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  system: PowerSystem | null
  projectId: string
}>()

const emit = defineEmits<{
  save: [system: PowerSystem]
  close: []
}>()

interface LevelFormData {
  id?: number
  level?: number
  levelName: string
  description: string
  breakthroughCondition: string
  lifespan: string
  powerRange: string
  landmarkAbility: string
  steps: StepFormData[]
}

interface StepFormData {
  id?: number
  level?: number
  levelName: string
}

const formData = reactive<{
  id?: number
  name: string
  sourceFrom: string
  coreResource: string
  cultivationMethod: string
  description: string
  levels: LevelFormData[]
}>({
  name: '',
  sourceFrom: '',
  coreResource: '',
  cultivationMethod: '',
  description: '',
  levels: []
})

const levelIndex = ref(-1)

watch(() => props.system, (val) => {
  if (val) {
    formData.id = val.id
    formData.name = val.name || ''
    formData.sourceFrom = val.sourceFrom || ''
    formData.coreResource = val.coreResource || ''
    formData.cultivationMethod = val.cultivationMethod || ''
    formData.description = val.description || ''
    formData.levels = val.levels?.map(l => ({
      id: l.id,
      level: l.level,
      levelName: l.levelName || '',
      description: l.description || '',
      breakthroughCondition: l.breakthroughCondition || '',
      lifespan: l.lifespan || '',
      powerRange: l.powerRange || '',
      landmarkAbility: l.landmarkAbility || '',
      steps: l.steps?.map(s => ({
        id: s.id,
        level: s.level,
        levelName: s.levelName || ''
      })) || []
    })) || []
  } else {
    formData.id = undefined
    formData.name = ''
    formData.sourceFrom = ''
    formData.coreResource = ''
    formData.cultivationMethod = ''
    formData.description = ''
    formData.levels = []
  }
}, { immediate: true })

const addLevel = () => {
  formData.levels.push({
    levelName: '',
    description: '',
    breakthroughCondition: '',
    lifespan: '',
    powerRange: '',
    landmarkAbility: '',
    steps: []
  })
  levelIndex.value = formData.levels.length - 1
}

const removeLevel = (index: number) => {
  formData.levels.splice(index, 1)
  if (levelIndex.value === index) {
    levelIndex.value = -1
  } else if (levelIndex.value > index) {
    levelIndex.value--
  }
}

const moveLevelUp = (index: number) => {
  if (index <= 0) return
  const level = formData.levels.splice(index, 1)[0]
  formData.levels.splice(index - 1, 0, level)
  if (levelIndex.value === index) {
    levelIndex.value = index - 1
  } else if (levelIndex.value === index - 1) {
    levelIndex.value = index
  }
}

const moveLevelDown = (index: number) => {
  if (index >= formData.levels.length - 1) return
  const level = formData.levels.splice(index, 1)[0]
  formData.levels.splice(index + 1, 0, level)
  if (levelIndex.value === index) {
    levelIndex.value = index + 1
  } else if (levelIndex.value === index + 1) {
    levelIndex.value = index
  }
}

const addStep = (levelIdx: number) => {
  if (!formData.levels[levelIdx]) return
  formData.levels[levelIdx].steps.push({ levelName: '' })
}

const removeStep = (levelIdx: number, stepIdx: number) => {
  if (!formData.levels[levelIdx]?.steps) return
  formData.levels[levelIdx].steps.splice(stepIdx, 1)
}

const handleSave = async () => {
  if (!formData.name.trim()) {
    error('体系名称不能为空')
    return
  }
  const payload: PowerSystem = {
    id: formData.id,
    name: formData.name,
    sourceFrom: formData.sourceFrom || undefined,
    coreResource: formData.coreResource || undefined,
    cultivationMethod: formData.cultivationMethod || undefined,
    description: formData.description || undefined,
    levels: formData.levels.map((l, idx) => ({
      id: l.id,
      level: idx,
      levelName: l.levelName,
      description: l.description || undefined,
      breakthroughCondition: l.breakthroughCondition || undefined,
      lifespan: l.lifespan || undefined,
      powerRange: l.powerRange || undefined,
      landmarkAbility: l.landmarkAbility || undefined,
      steps: l.steps.map((s, sIdx) => ({
        id: s.id,
        level: sIdx,
        levelName: s.levelName
      }))
    }))
  }
  emit('save', payload)
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" @click.self="emit('close')">
    <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto">
      <div class="p-6 space-y-4">
        <!-- 标题 -->
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            {{ props.system ? '编辑力量体系' : '添加力量体系' }}
          </h3>
          <button
            @click="emit('close')"
            class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded transition-colors"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- 体系名称 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            体系名称 <span class="text-red-500">*</span>
          </label>
          <input
            v-model="formData.name"
            class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="如：修仙"
          />
        </div>

        <!-- 能量来源 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">能量来源</label>
          <input
            v-model="formData.sourceFrom"
            class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="如：天地灵气"
          />
        </div>

        <!-- 核心资源 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">核心资源</label>
          <input
            v-model="formData.coreResource"
            class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="如：灵石"
          />
        </div>

        <!-- 修炼方式 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">修炼方式</label>
          <input
            v-model="formData.cultivationMethod"
            class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="如：打坐冥想"
          />
        </div>

        <!-- 描述 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">体系描述</label>
          <textarea
            v-model="formData.description"
            rows="3"
            class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white resize-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="体系整体描述..."
          ></textarea>
        </div>

        <!-- 等级划分 -->
        <div class="border-t border-gray-200 dark:border-gray-700 pt-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300">等级划分</h4>
            <button
              @click="addLevel"
              class="flex items-center gap-1 px-2 py-1 text-xs text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 rounded hover:bg-blue-100 dark:hover:bg-blue-800/30 transition-colors"
            >
              <Plus class="w-3 h-3" />
              添加等级
            </button>
          </div>

          <div class="space-y-3">
            <div
              v-for="(level, lIdx) in formData.levels"
              :key="lIdx"
              class="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden"
            >
              <!-- 等级头部 -->
              <div
                class="flex items-center gap-2 px-3 py-2 bg-gray-50 dark:bg-gray-700 cursor-pointer"
                @click="levelIndex = levelIndex === lIdx ? -1 : lIdx"
              >
                <span class="text-sm font-medium text-gray-800 dark:text-gray-200 flex-1">
                  等级 {{ lIdx + 1 }}: {{ level.levelName || '未命名' }}
                </span>
                <div class="flex items-center gap-1">
                  <button
                    @click.stop="moveLevelUp(lIdx)"
                    :disabled="lIdx === 0"
                    class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-30"
                  >
                    <ArrowUp class="w-3 h-3" />
                  </button>
                  <button
                    @click.stop="moveLevelDown(lIdx)"
                    :disabled="lIdx === formData.levels.length - 1"
                    class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-30"
                  >
                    <ArrowDown class="w-3 h-3" />
                  </button>
                  <button
                    @click.stop="removeLevel(lIdx)"
                    class="p-1 text-red-400 hover:text-red-600"
                  >
                    <Trash2 class="w-3 h-3" />
                  </button>
                  <component :is="levelIndex === lIdx ? ChevronUp : ChevronDown" class="w-3 h-3 text-gray-400" />
                </div>
              </div>

              <!-- 等级编辑区 -->
              <div v-if="levelIndex === lIdx" class="p-3 space-y-2">
                <div class="grid grid-cols-2 gap-2">
                  <div>
                    <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">等级名称</label>
                    <input
                      v-model="level.levelName"
                      class="w-full px-2 py-1 text-sm bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded focus:ring-1 focus:ring-blue-500"
                      placeholder="如: 练气期"
                    />
                  </div>
                  <div>
                    <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">标志性能力</label>
                    <input
                      v-model="level.landmarkAbility"
                      class="w-full px-2 py-1 text-sm bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded focus:ring-1 focus:ring-blue-500"
                      placeholder="如: 灵气外放"
                    />
                  </div>
                  <div>
                    <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">寿命范围</label>
                    <input
                      v-model="level.lifespan"
                      class="w-full px-2 py-1 text-sm bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded focus:ring-1 focus:ring-blue-500"
                      placeholder="如: 约150年"
                    />
                  </div>
                  <div>
                    <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">突破条件</label>
                    <input
                      v-model="level.breakthroughCondition"
                      class="w-full px-2 py-1 text-sm bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded focus:ring-1 focus:ring-blue-500"
                      placeholder="突破条件"
                    />
                  </div>
                </div>
                <div>
                  <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">等级描述</label>
                  <input
                    v-model="level.description"
                    class="w-full px-2 py-1 text-sm bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded focus:ring-1 focus:ring-blue-500"
                    placeholder="该等级描述"
                  />
                </div>
                <div>
                  <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">战力描述</label>
                  <input
                    v-model="level.powerRange"
                    class="w-full px-2 py-1 text-sm bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded focus:ring-1 focus:ring-blue-500"
                    placeholder="战力描述"
                  />
                </div>

                <!-- 境界划分 -->
                <div class="mt-2">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium text-gray-600 dark:text-gray-400">境界划分</span>
                    <button
                      @click="addStep(lIdx)"
                      class="flex items-center gap-1 px-2 py-0.5 text-xs text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 rounded hover:bg-blue-100 dark:hover:bg-blue-800/30 transition-colors"
                    >
                      <Plus class="w-3 h-3" />
                      添加境界
                    </button>
                  </div>
                  <div class="flex flex-wrap gap-1">
                    <div v-for="(step, sIdx) in level.steps" :key="sIdx" class="flex items-center gap-1">
                      <input
                        v-model="step.levelName"
                        class="px-2 py-1 text-xs bg-white dark:bg-gray-600 border border-gray-200 dark:border-gray-500 rounded w-24 focus:ring-1 focus:ring-blue-500"
                        placeholder="境界名称"
                      />
                      <button @click="removeStep(lIdx, sIdx)" class="p-1 text-red-400 hover:text-red-600">
                        <X class="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex justify-end gap-2 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button
            @click="emit('close')"
            class="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="handleSave"
            class="flex items-center gap-1 px-4 py-2 text-sm text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
          >
            <Save class="w-4 h-4" />
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
