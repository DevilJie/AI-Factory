<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Loader2, Search, Trash2, Plus } from 'lucide-vue-next'
import {
  addFactionAssociation,
  deleteFactionAssociation,
  type FactionAssociation
} from '@/api/character'
import { getFactionList, type Faction } from '@/api/faction'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  characterId: string
  projectId: string
  associations: FactionAssociation[]
}>()

const emit = defineEmits<{
  refresh: []
}>()

const loading = ref(false)
const factions = ref<Faction[]>([])
const searchKeyword = ref('')
const selectedFactionId = ref<number | null>(null)
const selectedRole = ref('')
const submitting = ref(false)

const presetRoles = ['掌门', '副掌门', '长老', '弟子', '外门弟子', '内门弟子', '客卿', '护法', '执事']

const filteredFactions = computed(() => {
  if (!searchKeyword.value.trim()) return []
  const keyword = searchKeyword.value.toLowerCase()
  return factions.value.filter(f => f.name.toLowerCase().includes(keyword))
})

const associatedFactionIds = computed(() => {
  return new Set(props.associations.map(a => a.factionId))
})

const handleSelectFaction = (faction: Faction) => {
  if (faction.id) {
    selectedFactionId.value = faction.id
    searchKeyword.value = faction.name
  }
}

const clearSelection = () => {
  selectedFactionId.value = null
  searchKeyword.value = ''
}

const loadFactions = async () => {
  loading.value = true
  try {
    factions.value = await getFactionList(props.projectId)
  } catch (e: any) {
    error('加载势力列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  if (!selectedFactionId.value) {
    error('请选择势力')
    return
  }
  if (!selectedRole.value.trim()) {
    error('请输入职位')
    return
  }
  submitting.value = true
  try {
    await addFactionAssociation(props.projectId, props.characterId, {
      factionId: selectedFactionId.value,
      role: selectedRole.value.trim()
    })
    success('关联成功')
    clearSelection()
    selectedRole.value = ''
    emit('refresh')
  } catch (e: any) {
    error('关联势力失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (association: FactionAssociation) => {
  const label = association.factionName + (association.role ? ` - ${association.role}` : '')
  if (!confirm(`移除「${label}」的关联？`)) return
  try {
    await deleteFactionAssociation(props.projectId, props.characterId, association.id)
    success('移除成功')
    emit('refresh')
  } catch (e: any) {
    error('移除关联失败')
  }
}

onMounted(() => {
  loadFactions()
})
</script>

<template>
  <div>
    <!-- Add faction form -->
    <div class="mb-6">
      <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">添加势力</h3>

      <div class="space-y-3">
        <!-- Search faction -->
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索势力名称"
            class="w-full pl-9 pr-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <!-- Search results dropdown -->
        <div
          v-if="searchKeyword.trim() && filteredFactions.length > 0"
          class="max-h-40 overflow-y-auto bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-sm"
        >
          <div
            v-for="faction in filteredFactions"
            :key="faction.id"
            class="px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-600 cursor-pointer text-sm"
            @click="handleSelectFaction(faction)"
          >
            <span class="text-gray-800 dark:text-gray-200">{{ faction.name }}</span>
            <span v-if="faction.id && associatedFactionIds.has(faction.id)" class="text-xs text-gray-400 ml-2">(已关联)</span>
          </div>
        </div>

        <!-- No results -->
        <div
          v-else-if="searchKeyword.trim() && filteredFactions.length === 0 && factions.length > 0"
          class="text-center py-4 text-gray-400 text-sm"
        >
          未找到匹配的势力
        </div>

        <!-- Selected faction hint -->
        <div v-if="selectedFactionId" class="flex items-center gap-2">
          <span class="text-sm text-blue-600 dark:text-blue-400">已选择: {{ searchKeyword }}</span>
          <button
            @click="clearSelection"
            class="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            清除
          </button>
        </div>

        <!-- Role input with datalist -->
        <input
          v-model="selectedRole"
          list="faction-role-options"
          placeholder="选择或输入职位"
          class="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        <datalist id="faction-role-options">
          <option v-for="role in presetRoles" :key="role" :value="role" />
        </datalist>

        <button
          @click="handleAdd"
          :disabled="!selectedFactionId || !selectedRole.trim() || submitting"
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
      暂无势力关联
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="association in associations"
        :key="association.id"
        class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
      >
        <div class="flex items-center gap-2 min-w-0 flex-1">
          <span class="text-sm font-medium text-gray-800 dark:text-gray-200">
            {{ association.factionName }}
          </span>
          <span v-if="association.role" class="text-sm text-blue-600 dark:text-blue-400">
            &mdash; {{ association.role }}
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
