<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Loader2, Search, Trash2, UserPlus } from 'lucide-vue-next'
import {
  getFactionCharacters,
  addFactionCharacter,
  deleteFactionCharacter,
  type FactionCharacter
} from '@/api/faction'
import { getCharacterList, type Character } from '@/api/character'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  factionId: number
  projectId: string
}>()

const loading = ref(false)
const characters = ref<FactionCharacter[]>([])
const allCharacters = ref<Character[]>([])
const searchKeyword = ref('')
const selectedCharacterId = ref<string | null>(null)
const selectedRole = ref('')
const submitting = ref(false)

const presetRoles = ['掌门', '副掌门', '长老', '弟子', '护法', '执事']

const filteredCharacters = computed(() => {
  if (!searchKeyword.value.trim()) return []
  const keyword = searchKeyword.value.toLowerCase()
  return allCharacters.value.filter(c => c.name.toLowerCase().includes(keyword))
})

const associatedCharacterIds = computed(() => {
  return new Set(characters.value.map(c => c.characterId))
})

const loadData = async () => {
  loading.value = true
  try {
    const [charData, assocData] = await Promise.all([
      getCharacterList(props.projectId),
      getFactionCharacters(props.projectId, props.factionId)
    ])
    allCharacters.value = charData
    characters.value = assocData
  } catch (e: any) {
    error('加载人物关联数据失败')
  } finally {
    loading.value = false
  }
}

const handleSelectCharacter = (character: Character) => {
  selectedCharacterId.value = character.id
  searchKeyword.value = character.name
}

const clearSelection = () => {
  selectedCharacterId.value = null
  searchKeyword.value = ''
}

const handleSubmit = async () => {
  if (!selectedCharacterId.value) {
    error('请选择人物')
    return
  }
  submitting.value = true
  try {
    await addFactionCharacter(props.projectId, props.factionId, {
      characterId: Number(selectedCharacterId.value),
      role: selectedRole.value || undefined
    })
    success('关联成功')
    clearSelection()
    selectedRole.value = ''
    await loadData()
  } catch (e: any) {
    error('关联人物失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (association: FactionCharacter) => {
  const characterName = getCharacterName(association.characterId)
  if (!confirm(`移除「${characterName}」的关联？`)) return
  try {
    if (association.id) {
      await deleteFactionCharacter(props.projectId, props.factionId, association.id)
    }
    success('移除成功')
    await loadData()
  } catch (e: any) {
    error('移除关联失败')
  }
}

const getCharacterName = (id: number): string => {
  const char = allCharacters.value.find(c => String(id) === c.id)
  return char?.name || '未知人物'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div>
    <!-- 添加人物表单 -->
    <div class="mb-6">
      <div class="flex items-center gap-2 mb-2">
        <!-- 搜索输入框 -->
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索人物名称"
            class="w-full pl-9 pr-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <!-- 职位输入框 with datalist -->
        <input
          v-model="selectedRole"
          list="role-options"
          placeholder="选择或输入职位"
          class="flex-1 px-3 py-2 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        <datalist id="role-options">
          <option v-for="role in presetRoles" :key="role" :value="role" />
        </datalist>
      </div>

      <!-- 搜索结果下拉 -->
      <div
        v-if="searchKeyword.trim() && filteredCharacters.length > 0"
        class="mt-1 max-h-40 overflow-y-auto bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-sm"
      >
        <div
          v-for="char in filteredCharacters"
          :key="char.id"
          class="px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-600 cursor-pointer text-sm"
          @click="handleSelectCharacter(char)"
        >
          <span class="text-gray-800 dark:text-gray-200">{{ char.name }}</span>
          <span v-if="char.role" class="text-xs text-gray-400 ml-2">{{ char.role }}</span>
          <span v-if="associatedCharacterIds.has(Number(char.id))" class="text-xs text-gray-400 ml-2">(已关联)</span>
        </div>
      </div>

      <!-- 搜索无结果 -->
      <div
        v-else-if="searchKeyword.trim() && filteredCharacters.length === 0 && allCharacters.length > 0"
        class="mt-1 text-center py-4 text-gray-400 text-sm"
      >
        未找到匹配的人物
      </div>

      <!-- 已选人物提示 -->
      <div v-if="selectedCharacterId" class="mt-2 flex items-center gap-2">
        <span class="text-sm text-blue-600 dark:text-blue-400">已选择: {{ searchKeyword }}</span>
        <button
          @click="clearSelection"
          class="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
        >
          清除
        </button>
      </div>

      <!-- 提交按钮 -->
      <button
        @click="handleSubmit"
        :disabled="!selectedCharacterId || submitting"
        class="mt-2 flex items-center gap-2 px-4 py-2 text-sm text-white bg-blue-500 rounded-lg hover:bg-blue-600 disabled:opacity-50 transition-colors"
      >
        <UserPlus class="w-4 h-4" />
        关联人物
      </button>
    </div>

    <!-- 人物关联列表 -->
    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
    </div>

    <div v-else-if="!characters.length" class="text-center py-8 text-gray-400">
      暂无关联人物，搜索并关联角色
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="association in characters"
        :key="association.id"
        class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
      >
        <!-- 左侧：人物名 + 职位徽章 -->
        <div class="flex items-center gap-2 min-w-0 flex-1">
          <span class="text-sm font-medium text-gray-800 dark:text-gray-200">
            {{ getCharacterName(association.characterId) }}
          </span>
          <span
            v-if="association.role"
            class="text-sm text-blue-600 dark:text-blue-400"
          >
            {{ association.role }}
          </span>
        </div>
        <!-- 右侧：删除按钮 -->
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
