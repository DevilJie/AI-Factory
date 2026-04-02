<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Plus, Loader2, ChevronRight, ChevronDown,
  Trash2, Edit3, Shield
} from 'lucide-vue-next'
import {
  getFactionTree,
  addFaction,
  updateFaction,
  deleteFaction,
  type Faction
} from '@/api/faction'
import { getPowerSystemList } from '@/api/powerSystem'
import { success, error } from '@/utils/toast'

// Type badge config: ally=green, hostile=red, neutral=gray
const typeConfig: Record<string, { label: string; bg: string; text: string }> = {
  ally:    { label: '正派', bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  hostile: { label: '反派', bg: 'bg-red-100 dark:bg-red-900/30',    text: 'text-red-700 dark:text-red-400' },
  neutral: { label: '中立', bg: 'bg-gray-100 dark:bg-gray-700',     text: 'text-gray-600 dark:text-gray-400' },
}

const props = withDefaults(defineProps<{
  projectId: string
  disabled: boolean
  nodes?: Faction[]
}>(), {
  nodes: undefined
})

const emit = defineEmits<{
  refresh: []
  delete: [node: Faction]
}>()

// Root component state (only meaningful when nodes prop is undefined)
const rootTreeData = ref<Faction[]>([])
const loading = ref(false)
const expandedNodes = ref<Set<number>>(new Set())
const editingNode = ref<Faction | null>(null)
const addingParentId = ref<number | null | undefined>(undefined)
const newNodeName = ref('')
const newNodeDesc = ref('')
const editingName = ref('')
const editingDesc = ref('')
const editingTypeValue = ref('')
const editingPowerSystemValue = ref<number | null>(null)
const showTypeSelector = ref(false)
const showPowerSystemSelector = ref(false)
const powerSystemMap = ref<Map<number, string>>(new Map())

const isRoot = computed(() => props.nodes === undefined)
const displayNodes = computed(() => props.nodes ?? rootTreeData.value)

const loadData = async () => {
  if (!isRoot.value) return
  loading.value = true
  try {
    const [tree, psList] = await Promise.all([
      getFactionTree(props.projectId),
      getPowerSystemList(props.projectId)
    ])
    rootTreeData.value = tree
    const map = new Map<number, string>()
    for (const ps of psList) {
      if (ps.id) map.set(ps.id, ps.name)
    }
    powerSystemMap.value = map
    rootTreeData.value.forEach(node => {
      if (node.id) expandedNodes.value.add(node.id)
    })
  } catch (e: any) {
    if (e.response?.status !== 404) console.error('加载势力阵营失败:', e)
  } finally {
    loading.value = false
  }
}

const refresh = () => loadData()

const toggleExpand = (id: number) => {
  expandedNodes.value.has(id) ? expandedNodes.value.delete(id) : expandedNodes.value.add(id)
}

const showAddForm = (parentId: number | null) => {
  addingParentId.value = parentId
  newNodeName.value = ''
  newNodeDesc.value = ''
}

const hideAddForm = () => { addingParentId.value = undefined }

const handleAdd = async () => {
  if (!newNodeName.value.trim()) { error('请输入势力名称'); return }
  try {
    await addFaction(props.projectId, {
      name: newNodeName.value.trim(),
      description: newNodeDesc.value.trim() || undefined,
      parentId: addingParentId.value ?? undefined
    })
    success('添加成功')
    hideAddForm()
    if (isRoot.value) {
      await loadData()
    } else {
      emit('refresh')
    }
  } catch (e: any) { error('添加失败') }
}

const startEdit = (node: Faction) => {
  editingNode.value = node
  editingName.value = node.name
  editingDesc.value = node.description || ''
  if ((node.deep ?? 0) === 0) {
    editingTypeValue.value = node.type || ''
    editingPowerSystemValue.value = node.corePowerSystem || null
    showTypeSelector.value = false
    showPowerSystemSelector.value = false
  }
}

const cancelEdit = () => {
  editingNode.value = null
  showTypeSelector.value = false
  showPowerSystemSelector.value = false
}

const handleEdit = async () => {
  if (!editingNode.value?.id || !editingName.value.trim()) return
  try {
    const isTopLevel = (editingNode.value.deep ?? 0) === 0
    await updateFaction(props.projectId, {
      id: editingNode.value.id,
      name: editingName.value.trim(),
      description: editingDesc.value.trim() || undefined,
      parentId: editingNode.value.parentId,
      type: isTopLevel ? editingTypeValue.value || undefined : undefined,
      corePowerSystem: isTopLevel ? editingPowerSystemValue.value || undefined : undefined
    })
    success('更新成功')
    cancelEdit()
    if (isRoot.value) {
      await loadData()
    } else {
      emit('refresh')
    }
  } catch (e: any) { error('更新失败') }
}

const handleDelete = async (node: Faction) => {
  if (!node.id) return
  if (!isRoot.value) { emit('delete', node); return }
  const hasChildren = node.children && node.children.length > 0
  if (!confirm(hasChildren ? `删除「${node.name}」及其所有子势力？` : `删除「${node.name}」？`)) return
  try {
    await deleteFaction(props.projectId, node.id)
    success('删除成功')
    await loadData()
  } catch (e: any) { error('删除失败') }
}

const countNodes = (nodes: Faction[]): number => {
  let c = 0
  for (const n of nodes) { c++; if (n.children) c += countNodes(n.children) }
  return c
}

const isAdding = (parentId: number | null) => addingParentId.value === parentId

const paddingLeft = (deep: number) => `${20 + deep * 20}px`

onMounted(() => { if (isRoot.value) loadData() })
defineExpose({ refresh })
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
    <!-- Header (root only) -->
    <div v-if="isRoot" class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">势力阵营</h3>
        <span v-if="rootTreeData.length" class="text-xs text-gray-400">
          {{ rootTreeData.length }} 个顶级势力，{{ countNodes(rootTreeData) }} 个节点
        </span>
      </div>
      <button
        @click="showAddForm(null)"
        :disabled="disabled"
        class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-800/30 transition-colors disabled:opacity-50"
      >
        <Plus class="w-3 h-3" />
        添加势力
      </button>
    </div>

    <!-- Add root form (root only) -->
    <div v-if="isRoot && isAdding(null)" class="mb-4 p-3 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg">
      <div class="flex items-center gap-2">
        <input v-model="newNodeName" class="flex-1 px-3 py-2 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="输入势力名称（如：紫阳宗）" @keyup.enter="handleAdd" @keyup.escape="hideAddForm" />
        <button @click="handleAdd" class="px-3 py-2 text-sm text-white bg-blue-500 rounded-lg hover:bg-blue-600">添加</button>
        <button @click="hideAddForm" class="px-3 py-2 text-sm text-gray-500 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200">取消</button>
      </div>
      <textarea v-model="newNodeDesc" class="mt-2 w-full px-3 py-2 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="势力描述（可选）"></textarea>
    </div>

    <!-- Loading (root only) -->
    <div v-if="isRoot && loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
    </div>

    <!-- Empty state (root only) -->
    <div v-else-if="isRoot && !rootTreeData.length" class="text-center py-8 text-gray-400 dark:text-gray-500">
      暂无势力阵营，点击上方「添加势力」按钮开始构建势力体系
    </div>

    <!-- Tree nodes -->
    <div v-else class="space-y-0.5">
      <template v-for="node in displayNodes" :key="node.id">
        <!-- Node row -->
        <div class="flex items-start gap-2 px-3 py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 group transition-colors">
          <!-- Chevron or spacer -->
          <button v-if="node.children?.length" @click="node.id && toggleExpand(node.id)" class="flex-shrink-0 p-0.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 mt-0.5">
            <ChevronDown v-if="node.id && expandedNodes.has(node.id)" class="w-4 h-4" />
            <ChevronRight v-else class="w-4 h-4" />
          </button>
          <span v-else class="w-5 flex-shrink-0" />

          <!-- Shield icon for top-level nodes -->
          <Shield v-if="(node.deep ?? 0) === 0" class="w-4 h-4 flex-shrink-0 mt-0.5 text-blue-500 dark:text-blue-400" />

          <!-- EDIT mode -->
          <template v-if="editingNode?.id === node.id">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <input v-model="editingName" class="flex-1 px-2 py-1 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" @keyup.enter="handleEdit" @keyup.escape="cancelEdit" />
                <button @click="handleEdit" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">保存</button>
                <button @click="cancelEdit" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded hover:bg-gray-200">取消</button>
              </div>
              <textarea v-model="editingDesc" class="mt-1 w-full px-2 py-1 text-xs bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="势力描述（可选）" @keyup.escape="cancelEdit"></textarea>

              <!-- Type selector (top-level only) -->
              <div v-if="(node.deep ?? 0) === 0" class="mt-1">
                <button @click="showTypeSelector = !showTypeSelector" class="text-xs text-blue-500 hover:text-blue-600">
                  {{ showTypeSelector ? '收起类型' : '修改类型' }}
                </button>
                <div v-if="showTypeSelector" class="flex items-center gap-2 mt-1">
                  <button v-for="(config, key) in typeConfig" :key="key" @click="editingTypeValue = key"
                    :class="['px-2 py-1 text-xs rounded', editingTypeValue === key ? config.bg + ' ' + config.text : 'bg-gray-100 dark:bg-gray-700 text-gray-500']">
                    {{ config.label }}
                  </button>
                </div>
              </div>

              <!-- Power system selector (top-level only) -->
              <div v-if="(node.deep ?? 0) === 0" class="mt-1">
                <button @click="showPowerSystemSelector = !showPowerSystemSelector" class="text-xs text-blue-500 hover:text-blue-600">
                  {{ showPowerSystemSelector ? '收起力量体系' : '修改力量体系' }}
                </button>
                <div v-if="showPowerSystemSelector" class="mt-1">
                  <select v-model="editingPowerSystemValue" class="px-2 py-1 text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500">
                    <option :value="null">无</option>
                    <option v-for="[id, name] in powerSystemMap" :key="id" :value="id">{{ name }}</option>
                  </select>
                </div>
              </div>
            </div>
          </template>

          <!-- DISPLAY mode -->
          <template v-else>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5">
                <!-- Type badge (top-level only) -->
                <span v-if="(node.deep ?? 0) === 0 && node.type && typeConfig[node.type]"
                  :class="['px-1.5 py-0.5 text-xs rounded', typeConfig[node.type].bg, typeConfig[node.type].text]">
                  {{ typeConfig[node.type].label }}
                </span>
                <!-- Name -->
                <span :class="(node.deep ?? 0) === 0 ? 'text-sm font-medium text-gray-800 dark:text-gray-200' : 'text-sm text-gray-700 dark:text-gray-300'">{{ node.name }}</span>
                <!-- Power system label (top-level only) -->
                <span v-if="(node.deep ?? 0) === 0 && node.corePowerSystem && powerSystemMap.get(node.corePowerSystem)" class="text-xs text-gray-400">
                  · {{ powerSystemMap.get(node.corePowerSystem) }}
                </span>
              </div>
              <p v-if="node.description" class="text-xs text-gray-400 dark:text-gray-500 mt-0.5 whitespace-pre-line">{{ node.description }}</p>
            </div>
            <!-- Action buttons -->
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
              <button @click="showAddForm(node.id!)" :disabled="disabled" class="p-1 text-gray-400 hover:text-blue-500 disabled:opacity-50"><Plus class="w-3.5 h-3.5" /></button>
              <button @click="startEdit(node)" :disabled="disabled" class="p-1 text-gray-400 hover:text-amber-500 disabled:opacity-50"><Edit3 class="w-3.5 h-3.5" /></button>
              <button @click="handleDelete(node)" :disabled="disabled" class="p-1 text-gray-400 hover:text-red-500 disabled:opacity-50"><Trash2 class="w-3.5 h-3.5" /></button>
            </div>
          </template>
        </div>

        <!-- Add child form for this node -->
        <div v-if="isAdding(node.id ?? -1)" class="p-2 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg" :style="{ marginLeft: paddingLeft((node.deep ?? 0) + 1) }">
          <div class="flex items-center gap-2">
            <input v-model="newNodeName" class="flex-1 px-2 py-1 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="输入势力名称" @keyup.enter="handleAdd" @keyup.escape="hideAddForm" />
            <button @click="handleAdd" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">添加</button>
            <button @click="hideAddForm" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded">取消</button>
          </div>
          <textarea v-model="newNodeDesc" class="mt-2 w-full px-2 py-1 text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="势力描述（可选）"></textarea>
        </div>

        <!-- Recursive children -->
        <div v-if="node.id && expandedNodes.has(node.id) && node.children?.length" :style="{ paddingLeft: paddingLeft((node.deep ?? 0) + 1) }" class="space-y-0.5">
          <FactionTree
            :nodes="node.children"
            :project-id="projectId"
            :disabled="disabled"
            @refresh="refresh"
            @delete="(n: Faction) => handleDelete(n)"
          />
        </div>
      </template>
    </div>
  </div>
</template>
