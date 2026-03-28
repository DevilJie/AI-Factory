<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import {
  Users,
  Plus,
  Search,
  Loader2,
  User,
  Trash2,
  Edit3,
  X,
  Sparkles
} from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import {
  getCharacterList,
  createCharacter,
  deleteCharacter,
  type Character,
  type CharacterForm,
  type CharacterRoleType
} from '@/api/character'
import { success, error, warning } from '@/utils/toast'

const route = useRoute()

// State
const loading = ref(false)
const characters = ref<Character[]>([])
const searchKeyword = ref('')
const showCreateModal = ref(false)
const selectedRoleType = ref<CharacterRoleType | 'all'>('all')
const creating = ref(false)

// 表单数据
const formData = ref<CharacterForm>({
  name: '',
  gender: 'other',
  roleType: 'supporting',
  role: '',
  personality: [],
  appearance: '',
  background: '',
  abilities: [],
  tags: []
})

// 角色类型配置
const roleTypes = [
  { value: 'protagonist', label: '主角', color: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400' },
  { value: 'supporting', label: '配角', color: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400' },
  { value: 'antagonist', label: '反派', color: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400' },
  { value: 'npc', label: 'NPC', color: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300' }
]

// 性别配置
const genders = [
  { value: 'male', label: '男' },
  { value: 'female', label: '女' },
  { value: 'other', label: '其他' }
]

// 筛选后的人物列表
const filteredCharacters = computed(() => {
  let result = characters.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(c =>
      c.name.toLowerCase().includes(keyword) ||
      c.role.toLowerCase().includes(keyword)
    )
  }

  if (selectedRoleType.value !== 'all') {
    result = result.filter(c => c.roleType === selectedRoleType.value)
  }

  return result
})

// 获取项目ID
const projectId = () => route.params.id as string

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    characters.value = await getCharacterList(projectId())
  } catch (error: any) {
    if (error.response?.status !== 404) {
      error('加载人物列表失败')
      console.error('Failed to load characters:', error)
    }
  } finally {
    loading.value = false
  }
}

// 打开创建弹窗
const openCreateModal = () => {
  formData.value = {
    name: '',
    gender: 'other',
    roleType: 'supporting',
    role: '',
    personality: [],
    appearance: '',
    background: '',
    abilities: [],
    tags: []
  }
  showCreateModal.value = true
}

// 关闭创建弹窗
const closeCreateModal = () => {
  showCreateModal.value = false
}

// 创建人物
const handleCreate = async () => {
  if (!formData.value.name.trim()) {
    warning('请输入人物名称')
    return
  }

  creating.value = true
  try {
    await createCharacter(projectId(), formData.value)
    success('创建成功')
    closeCreateModal()
    loadData()
  } catch (e) {
    error('创建失败')
    console.error('Failed to create character:', e)
  } finally {
    creating.value = false
  }
}

// 删除人物
const handleDelete = async (character: Character) => {
  if (!confirm(`确定要删除人物「${character.name}」吗？`)) {
    return
  }

  try {
    await deleteCharacter(projectId(), character.id)
    success('删除成功')
    loadData()
  } catch (e) {
    error('删除失败')
    console.error('Failed to delete character:', e)
  }
}

// 获取角色类型样式
const getRoleTypeStyle = (roleType: CharacterRoleType) => {
  return roleTypes.find(r => r.value === roleType)?.color || roleTypes[3]?.color || 'bg-gray-100 text-gray-700'
}

// 获取角色类型标签
const getRoleTypeLabel = (roleType: CharacterRoleType) => {
  return roleTypes.find(r => r.value === roleType)?.label || 'NPC'
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
          <div class="w-10 h-10 rounded-xl bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center">
            <Users class="w-5 h-5 text-purple-600 dark:text-purple-400" />
          </div>
          <div>
            <h1 class="text-lg font-semibold text-gray-900 dark:text-white">人物管理</h1>
            <p class="text-sm text-gray-500 dark:text-gray-400">管理小说中的角色人物</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-900/20 rounded-lg hover:bg-purple-100 dark:hover:bg-purple-900/30 transition-colors"
          >
            <Sparkles class="w-4 h-4" />
            AI生成
          </button>
          <button
            @click="openCreateModal"
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus class="w-4 h-4" />
            新建人物
          </button>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="flex-shrink-0 px-6 py-3 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div class="flex items-center gap-4">
        <!-- Search -->
        <div class="relative flex-1 max-w-xs">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索人物..."
            class="w-full pl-10 pr-4 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <!-- Role Type Filter -->
        <div class="flex items-center gap-2">
          <button
            v-for="role in [{ value: 'all', label: '全部' }, ...roleTypes]"
            :key="role.value"
            @click="selectedRoleType = role.value as CharacterRoleType | 'all'"
            :class="[
              'px-3 py-1.5 text-sm font-medium rounded-lg transition-colors',
              selectedRoleType === role.value
                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
            ]"
          >
            {{ role.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-6">
      <!-- Loading State -->
      <div v-if="loading" class="flex items-center justify-center h-full">
        <Loader2 class="w-8 h-8 text-blue-500 animate-spin" />
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredCharacters.length === 0" class="flex flex-col items-center justify-center h-full">
        <div class="w-20 h-20 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center mb-4">
          <User class="w-10 h-10 text-gray-400" />
        </div>
        <p class="text-gray-500 dark:text-gray-400 mb-2">暂无人物</p>
        <p class="text-sm text-gray-400 dark:text-gray-500">点击"新建人物"按钮创建角色</p>
      </div>

      <!-- Character Grid -->
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
        <div
          v-for="character in filteredCharacters"
          :key="character.id"
          class="group bg-white dark:bg-gray-800 rounded-xl p-4 shadow-sm hover:shadow-md transition-all border border-gray-100 dark:border-gray-700"
        >
          <!-- Avatar & Actions -->
          <div class="relative mb-3">
            <div class="w-16 h-16 mx-auto rounded-full overflow-hidden bg-gray-100 dark:bg-gray-700">
              <img
                v-if="character.avatar"
                :src="character.avatar"
                :alt="character.name"
                class="w-full h-full object-cover"
              />
              <User v-else class="w-full h-full p-4 text-gray-400" />
            </div>
            <!-- Role Type Badge -->
            <span
              :class="[
                'absolute -bottom-1 left-1/2 -translate-x-1/2 px-2 py-0.5 text-xs font-medium rounded-full whitespace-nowrap',
                getRoleTypeStyle(character.roleType)
              ]"
            >
              {{ getRoleTypeLabel(character.roleType) }}
            </span>
            <!-- Actions -->
            <div class="absolute top-0 right-0 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                class="p-1.5 text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                title="编辑"
              >
                <Edit3 class="w-4 h-4" />
              </button>
              <button
                @click="handleDelete(character)"
                class="p-1.5 text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                title="删除"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>

          <!-- Info -->
          <div class="text-center">
            <h3 class="text-sm font-medium text-gray-900 dark:text-white truncate">{{ character.name }}</h3>
            <p class="text-xs text-gray-500 dark:text-gray-400 truncate mt-1">{{ character.role || '未设置角色' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Modal -->
    <div
      v-if="showCreateModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click.self="closeCreateModal"
    >
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
        <!-- Modal Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">新建人物</h2>
          <button
            @click="closeCreateModal"
            class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- Modal Body -->
        <div class="px-6 py-4 space-y-4 max-h-[60vh] overflow-y-auto">
          <!-- Name -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              人物名称 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.name"
              type="text"
              placeholder="请输入人物名称"
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <!-- Role Type -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              角色类型
            </label>
            <div class="grid grid-cols-4 gap-2">
              <button
                v-for="role in roleTypes"
                :key="role.value"
                @click="formData.roleType = role.value as CharacterRoleType"
                :class="[
                  'px-3 py-2 text-sm font-medium rounded-lg border transition-colors',
                  formData.roleType === role.value
                    ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
                    : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'
                ]"
              >
                {{ role.label }}
              </button>
            </div>
          </div>

          <!-- Gender & Age -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                性别
              </label>
              <select
                v-model="formData.gender"
                class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option v-for="g in genders" :key="g.value" :value="g.value">{{ g.label }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                年龄
              </label>
              <input
                v-model.number="formData.age"
                type="number"
                placeholder="年龄"
                class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          <!-- Role -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              角色身份
            </label>
            <input
              v-model="formData.role"
              type="text"
              placeholder="如：武林盟主、天才少女"
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <!-- Appearance -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              外貌描述
            </label>
            <textarea
              v-model="formData.appearance"
              rows="2"
              placeholder="描述人物的外貌特征..."
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            ></textarea>
          </div>

          <!-- Background -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              背景故事
            </label>
            <textarea
              v-model="formData.background"
              rows="2"
              placeholder="描述人物的背景故事..."
              class="w-full px-3 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            ></textarea>
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <button
            @click="closeCreateModal"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleCreate"
            :disabled="creating"
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            <Loader2 v-if="creating" class="w-4 h-4 animate-spin" />
            创建
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
