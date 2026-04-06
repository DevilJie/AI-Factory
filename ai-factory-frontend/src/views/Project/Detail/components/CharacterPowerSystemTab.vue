<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Loader2, Search, Trash2, Plus } from 'lucide-vue-next'
import {
  addPowerSystemAssociation,
  deletePowerSystemAssociation,
  type PowerSystemAssociation
} from '@/api/character'
import { getPowerSystemList, type PowerSystem } from '@/api/powerSystem'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  characterId: string
  projectId: string
  associations: PowerSystemAssociation[]
}>()

const emit = defineEmits<{
  refresh: []
}>()

const loading = ref(false)
const powerSystems = ref<PowerSystem[]>([])
const selectedSystemId = ref<number | null>(null)
const selectedRealmId = ref<number | null>(null)
const selectedSubRealmId = ref<number | null>(null)
const submitting = ref(false)

// Get the currently selected power system object
const selectedSystem = computed(() => {
  if (!selectedSystemId.value) return null
  return powerSystems.value.find(ps => ps.id === selectedSystemId.value) || null
})

// Realm levels for the selected power system
const realmLevels = computed(() => {
  return selectedSystem.value?.levels || []
})

// Sub-realm steps for the selected realm level
const subRealmSteps = computed(() => {
  if (!selectedRealmId.value) return []
  const level = realmLevels.value.find(l => l.id === selectedRealmId.value)
  return level?.steps || []
})

// Already-associated power system IDs
const associatedSystemIds = computed(() => {
  return new Set(props.associations.map(a => a.powerSystemId))
})

// Filtered power systems (exclude already-associated)
const availablePowerSystems = computed(() => {
  return powerSystems.value.filter(ps => ps.id && !associatedSystemIds.value.has(ps.id))
})

const loadPowerSystems = async () => {
  loading.value = true
  try {
    powerSystems.value = await getPowerSystemList(props.projectId)
  } catch (e: any) {
    error('加载力量体系列表失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  selectedSystemId.value = null
  selectedRealmId.value = null
  selectedSubRealmId.value = null
}

const handleAdd = async () => {
  if (!selectedSystemId.value) {
    error('请选择力量体系')
    return
  }
  submitting.value = true
  try {
    await addPowerSystemAssociation(props.projectId, props.characterId, {
      powerSystemId: selectedSystemId.value,
      currentRealmId: selectedRealmId.value || undefined,
      currentSubRealmId: selectedSubRealmId.value || undefined
    })
    success('关联成功')
    resetForm()
    emit('refresh')
  } catch (e: any) {
    error('关联力量体系失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (association: PowerSystemAssociation) => {
  const label = association.powerSystemName
  if (!confirm(`移除「${label}」的关联？`)) return
  try {
    await deletePowerSystemAssociation(props.projectId, props.characterId, association.id)
    success('移除成功')
    emit('refresh')
  } catch (e: any) {
    error('移除关联失败')
  }
}

onMounted(() => {
  loadPowerSystems()
})
</script>

<template>
  <div>
    <!-- Add power system form -->
    <div class="mb-6">
      <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">添加修炼体系</h3>

      <!-- Power system select -->
      <div class="space-y-3">
        <select
          v-model="selectedSystemId"
          class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option :value="null">选择力量体系</option>
          <option v-for="ps in availablePowerSystems" :key="ps.id" :value="ps.id">{{ ps.name }}</option>
        </select>

        <!-- Realm level select -->
        <select
          v-if="realmLevels.length > 0"
          v-model="selectedRealmId"
          class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option :value="null">选择境界</option>
          <option v-for="level in realmLevels" :key="level.id" :value="level.id">{{ level.levelName }}</option>
        </select>

        <!-- Sub-realm select -->
        <select
          v-if="subRealmSteps.length > 0"
          v-model="selectedSubRealmId"
          class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option :value="null">选择阶段</option>
          <option v-for="step in subRealmSteps" :key="step.id" :value="step.id">{{ step.levelName }}</option>
        </select>

        <button
          @click="handleAdd"
          :disabled="!selectedSystemId || submitting"
          class="flex items-center gap-2 px-4 py-2 text-sm text-white bg-blue-500 rounded-lg hover:bg-blue-600 disabled:opacity-50 transition-colors"
        >
          <Plus class="w-4 h-4" />
          添加
        </button>
      </div>
    </div>

    <!-- Current associations list -->
    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
    </div>

    <div v-else-if="!associations.length" class="text-center py-8 text-gray-400">
      暂无修炼体系关联
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="association in associations"
        :key="association.id"
        class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
      >
        <div class="flex items-center gap-2 min-w-0 flex-1">
          <span class="text-sm font-medium text-gray-800 dark:text-gray-200">
            {{ association.powerSystemName }}
          </span>
          <span v-if="association.currentRealmName" class="text-sm text-blue-600 dark:text-blue-400">
            &mdash; {{ association.currentRealmName }}
          </span>
          <span v-if="association.currentSubRealmName" class="text-xs text-gray-400">
            ({{ association.currentSubRealmName }})
          </span>
        </div>
        <button
          @click="handleDelete(association)"
          class="p-1 text-gray-400 hover:text-red-500 transition-colors flex-shrink-0 ml-2"
        >
          <Trash2 class="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>
</template>
