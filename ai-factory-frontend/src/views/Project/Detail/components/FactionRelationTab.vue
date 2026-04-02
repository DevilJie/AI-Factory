<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Loader2, Trash2 } from 'lucide-vue-next'
import {
  getFactionList,
  getFactionRelations,
  addFactionRelation,
  deleteFactionRelation,
  type Faction,
  type FactionRelation
} from '@/api/faction'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  factionId: number
  projectId: string
}>()

const loading = ref(false)
const relations = ref<FactionRelation[]>([])
const factionList = ref<Faction[]>([])
const selectedTargetId = ref<number | null>(null)
const relationType = ref<string>('')
const description = ref('')
const submitting = ref(false)

const relationTypeConfig: Record<string, { label: string; bg: string; text: string }> = {
  ally:    { label: '盟友', bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  hostile: { label: '敌对', bg: 'bg-red-100 dark:bg-red-900/30',    text: 'text-red-700 dark:text-red-400' },
  neutral: { label: '中立', bg: 'bg-gray-100 dark:bg-gray-700',     text: 'text-gray-600 dark:text-gray-400' },
}

const factionNameMap = computed(() => {
  const map = new Map<number, string>()
  for (const f of factionList.value) {
    if (f.id) map.set(f.id, f.name)
  }
  return map
})

const availableTargets = computed(() => {
  return factionList.value.filter(f => f.id !== props.factionId)
})

const loadRelations = async () => {
  loading.value = true
  try {
    const data = await getFactionRelations(props.projectId, props.factionId)
    relations.value = data
  } catch (e: any) {
    error('加载势力关系失败')
  } finally {
    loading.value = false
  }
}

const loadFactionList = async () => {
  try {
    const data = await getFactionList(props.projectId)
    factionList.value = data
  } catch (e: any) {
    console.error('加载势力列表失败:', e)
  }
}

const handleAdd = async () => {
  if (!selectedTargetId.value || !relationType.value) {
    error('请选择目标势力和关系类型')
    return
  }
  submitting.value = true
  try {
    // 创建正向关系 (A -> B)
    await addFactionRelation(props.projectId, props.factionId, {
      targetFactionId: selectedTargetId.value,
      relationType: relationType.value,
      description: description.value || undefined
    })
    // 创建反向关系 (B -> A)
    try {
      await addFactionRelation(props.projectId, selectedTargetId.value, {
        targetFactionId: props.factionId,
        relationType: relationType.value,
        description: description.value || undefined
      })
    } catch (e: any) {
      // 反向创建失败不影响正向，仅提示
      console.error('反向关系创建失败:', e)
    }
    success('添加成功')
    selectedTargetId.value = null
    relationType.value = ''
    description.value = ''
    await loadRelations()
  } catch (e: any) {
    error('添加关系失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (relation: FactionRelation) => {
  const targetName = factionNameMap.value.get(relation.targetFactionId!) || '未知势力'
  const typeLabel = relationTypeConfig[relation.relationType]?.label || relation.relationType
  if (!confirm(`删除与「${targetName}」的${typeLabel}关系？`)) return
  try {
    // 删除正向关系
    if (relation.id) {
      await deleteFactionRelation(props.projectId, props.factionId, relation.id)
    }
    // 查找并删除反向关系
    if (relation.targetFactionId) {
      try {
        const reverseList = await getFactionRelations(props.projectId, relation.targetFactionId)
        const reverse = reverseList.find(
          (r) => r.targetFactionId === props.factionId && r.relationType === relation.relationType
        )
        if (reverse?.id) {
          await deleteFactionRelation(props.projectId, relation.targetFactionId, reverse.id)
        }
      } catch (e: any) {
        console.error('反向关系删除失败:', e)
      }
    }
    success('删除成功')
    await loadRelations()
  } catch (e: any) {
    error('删除关系失败')
  }
}

onMounted(() => {
  loadFactionList()
  loadRelations()
})
</script>

<template>
  <div>
    <!-- 添加关系表单 -->
    <div class="mb-6">
      <div class="flex items-center gap-2 mb-2">
        <!-- 目标势力选择器 -->
        <select
          v-model="selectedTargetId"
          class="flex-1 px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option :value="null" disabled>选择目标势力</option>
          <option v-for="f in availableTargets" :key="f.id" :value="f.id">{{ f.name }}</option>
        </select>

        <!-- 关系类型按钮 -->
        <div class="flex items-center gap-1">
          <button
            v-for="(config, key) in relationTypeConfig"
            :key="key"
            @click="relationType = key"
            :class="[
              'px-3 py-2 text-sm rounded-lg transition-colors',
              relationType === key
                ? config.bg + ' ' + config.text + ' font-medium'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-500 hover:bg-gray-200 dark:hover:bg-gray-600'
            ]"
          >
            {{ config.label }}
          </button>
        </div>
      </div>

      <!-- 描述输入 -->
      <div class="flex items-center gap-2">
        <textarea
          v-model="description"
          class="flex-1 px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg resize-none focus:outline-none focus:ring-1 focus:ring-blue-500"
          rows="1"
          placeholder="关系描述（可选）"
        />
        <button
          @click="handleAdd"
          :disabled="submitting"
          class="px-4 py-2 text-sm text-white bg-blue-500 rounded-lg hover:bg-blue-600 disabled:opacity-50 transition-colors"
        >
          添加
        </button>
      </div>
    </div>

    <!-- 关系列表 -->
    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
    </div>

    <div v-else-if="!relations.length" class="text-center py-8 text-gray-400">
      暂无势力关系，选择目标势力后添加
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="relation in relations"
        :key="relation.id"
        class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
      >
        <!-- 左侧：目标势力名 + 类型徽章 + 描述 -->
        <div class="flex items-center gap-2 min-w-0 flex-1">
          <span class="text-sm font-medium text-gray-800 dark:text-gray-200">
            {{ factionNameMap.get(relation.targetFactionId!) || '未知势力' }}
          </span>
          <span
            v-if="relationTypeConfig[relation.relationType]"
            :class="['px-2 py-0.5 text-xs rounded', relationTypeConfig[relation.relationType].bg, relationTypeConfig[relation.relationType].text]"
          >
            {{ relationTypeConfig[relation.relationType].label }}
          </span>
          <span v-if="relation.description" class="text-xs text-gray-400 ml-2 truncate">
            {{ relation.description }}
          </span>
        </div>
        <!-- 右侧：删除按钮 -->
        <button
          @click="handleDelete(relation)"
          class="p-1 text-gray-400 hover:text-red-500 transition-colors flex-shrink-0 ml-2"
        >
          <Trash2 class="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>
</template>
