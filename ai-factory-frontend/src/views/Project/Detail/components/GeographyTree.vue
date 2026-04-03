<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  Plus, Loader2, ChevronRight, ChevronDown, MapPin,
  Trash2, Edit3, Globe2, Mountain, Trees, Sparkles
} from 'lucide-vue-next'
import {
  getGeographyTree as getGeographyTreeApi,
  addRegion,
  updateRegion,
  deleteRegion,
  type ContinentRegion
} from '@/api/continentRegion'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  projectId: string
  disabled: boolean
  generatingSelf?: boolean
}>()

const emit = defineEmits<{
  generate: []
}>()

const treeData = ref<ContinentRegion[]>([])
const loading = ref(false)
const expandedNodes = ref<Set<number>>(new Set())
const editingNode = ref<ContinentRegion | null>(null)
const addingParentId = ref<number | null | undefined>(undefined)
const newNodeName = ref('')
const newNodeDesc = ref('')
const editingName = ref('')
const editingDesc = ref('')

const loadData = async () => {
  loading.value = true
  try {
    treeData.value = await getGeographyTreeApi(props.projectId)
  } catch (e: any) {
    if (e.response?.status !== 404) console.error('加载地理区域失败:', e)
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
  if (!newNodeName.value.trim()) { error('请输入区域名称'); return }
  try {
    await addRegion(props.projectId, {
      name: newNodeName.value.trim(),
      description: newNodeDesc.value.trim() || undefined,
      parentId: addingParentId.value ?? undefined
    })
    success('添加成功')
    hideAddForm()
    await loadData()
  } catch (e: any) { error('添加失败') }
}

const startEdit = (node: ContinentRegion) => {
  editingNode.value = node
  editingName.value = node.name
  editingDesc.value = node.description || ''
}

const cancelEdit = () => { editingNode.value = null }

const handleEdit = async () => {
  if (!editingNode.value?.id || !editingName.value.trim()) return
  try {
    await updateRegion(props.projectId, {
      id: editingNode.value.id,
      name: editingName.value.trim(),
      description: editingDesc.value.trim() || undefined,
      parentId: editingNode.value.parentId
    })
    success('更新成功')
    cancelEdit()
    await loadData()
  } catch (e: any) { error('更新失败') }
}

const handleDelete = async (node: ContinentRegion) => {
  if (!node.id) return
  const hasChildren = node.children && node.children.length > 0
  if (!confirm(hasChildren ? `删除「${node.name}」及其所有子区域？` : `删除「${node.name}」？`)) return
  try {
    await deleteRegion(props.projectId, node.id)
    success('删除成功')
    await loadData()
  } catch (e: any) { error('删除失败') }
}

const getLevelIcon = (deep: number) => {
  if (deep === 0) return Globe2
  if (deep === 1) return Mountain
  if (deep === 2) return Trees
  return MapPin
}

const getLevelColor = (deep: number) => {
  if (deep === 0) return 'text-blue-500 dark:text-blue-400'
  if (deep === 1) return 'text-green-500 dark:text-green-400'
  if (deep === 2) return 'text-amber-500 dark:text-amber-400'
  return 'text-gray-400 dark:text-gray-500'
}

const countNodes = (nodes: ContinentRegion[]): number => {
  let c = 0
  for (const n of nodes) { c++; if (n.children) c += countNodes(n.children) }
  return c
}

const isAdding = (parentId: number | null) => addingParentId.value === parentId

const paddingLeft = (deep: number) => `${20 + deep * 20}px`

onMounted(() => loadData())
defineExpose({ refresh })
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">地理环境</h3>
        <span v-if="treeData.length" class="text-xs text-gray-400">
          {{ treeData.length }} 个顶级区域，{{ countNodes(treeData) }} 个节点
        </span>
      </div>
      <div class="flex items-center gap-2">
        <!-- AI Generate Button -->
        <button
          @click="emit('generate')"
          :disabled="disabled"
          class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-gradient-to-r from-green-500 to-teal-500 rounded-lg hover:from-green-600 hover:to-teal-600 transition-colors disabled:opacity-50"
        >
          <Loader2 v-if="generatingSelf" class="w-3 h-3 animate-spin" />
          <Sparkles v-else class="w-3 h-3" />
          {{ generatingSelf ? '生成中...' : 'AI生成' }}
        </button>
        <!-- Add Button -->
        <button
          @click="showAddForm(null)"
          :disabled="disabled"
          class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-800/30 transition-colors disabled:opacity-50"
        >
          <Plus class="w-3 h-3" />
          添加区域
        </button>
      </div>
    </div>

    <!-- Add root form -->
    <div v-if="isAdding(null)" class="mb-4 p-3 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg">
      <div class="flex items-center gap-2">
        <input v-model="newNodeName" class="flex-1 px-3 py-2 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="输入区域名称（如：东玄大陆）" @keyup.enter="handleAdd" @keyup.escape="hideAddForm" />
        <button @click="handleAdd" class="px-3 py-2 text-sm text-white bg-blue-500 rounded-lg hover:bg-blue-600">添加</button>
        <button @click="hideAddForm" class="px-3 py-2 text-sm text-gray-500 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200">取消</button>
      </div>
      <textarea v-model="newNodeDesc" class="mt-2 w-full px-3 py-2 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）"></textarea>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
    </div>

    <!-- Empty state -->
    <div v-else-if="!treeData.length" class="text-center py-8 text-gray-400 dark:text-gray-500">
      暂无地理区域，点击上方「添加区域」按钮开始构建世界地理
    </div>

    <!-- Recursive Tree -->
    <div v-else class="space-y-0.5">
      <template v-for="node in treeData" :key="node.id">
        <!-- Region Node (recursive template) -->
        <!-- Level 0 -->
        <div class="flex items-start gap-2 px-3 py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 group transition-colors">
          <button v-if="node.children?.length" @click="node.id && toggleExpand(node.id)" class="flex-shrink-0 p-0.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 mt-0.5">
            <ChevronDown v-if="node.id && expandedNodes.has(node.id)" class="w-4 h-4" />
            <ChevronRight v-else class="w-4 h-4" />
          </button>
          <span v-else class="w-5 flex-shrink-0" />
          <component :is="getLevelIcon(node.deep || 0)" :class="['w-4 h-4 flex-shrink-0 mt-0.5', getLevelColor(node.deep || 0)]" />

          <template v-if="editingNode?.id === node.id">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <input v-model="editingName" class="flex-1 px-2 py-1 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" @keyup.enter="handleEdit" @keyup.escape="cancelEdit" />
                <button @click="handleEdit" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">保存</button>
                <button @click="cancelEdit" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded hover:bg-gray-200">取消</button>
              </div>
              <textarea v-model="editingDesc" class="mt-1 w-full px-2 py-1 text-xs bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）" @keyup.escape="cancelEdit"></textarea>
            </div>
          </template>
          <template v-else>
            <div class="flex-1 min-w-0">
              <span class="text-sm font-medium text-gray-800 dark:text-gray-200">{{ node.name }}</span>
              <span v-if="node.children?.length" class="text-xs text-gray-400 ml-1">({{ node.children.length }})</span>
              <p v-if="node.description" class="text-xs text-gray-400 dark:text-gray-500 mt-0.5 whitespace-pre-line">{{ node.description }}</p>
            </div>
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
              <button @click="showAddForm(node.id!)" :disabled="disabled" class="p-1 text-gray-400 hover:text-blue-500 disabled:opacity-50"><Plus class="w-3.5 h-3.5" /></button>
              <button @click="startEdit(node)" :disabled="disabled" class="p-1 text-gray-400 hover:text-amber-500 disabled:opacity-50"><Edit3 class="w-3.5 h-3.5" /></button>
              <button @click="handleDelete(node)" :disabled="disabled" class="p-1 text-gray-400 hover:text-red-500 disabled:opacity-50"><Trash2 class="w-3.5 h-3.5" /></button>
            </div>
          </template>
        </div>

        <!-- Add child form for this node -->
        <div v-if="isAdding(node.id ?? -1)" class="p-2 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg" :style="{ marginLeft: '40px' }">
          <div class="flex items-center gap-2">
            <input v-model="newNodeName" class="flex-1 px-2 py-1 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="输入区域名称" @keyup.enter="handleAdd" @keyup.escape="hideAddForm" />
            <button @click="handleAdd" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">添加</button>
            <button @click="hideAddForm" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded">取消</button>
          </div>
          <textarea v-model="newNodeDesc" class="mt-2 w-full px-2 py-1 text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）"></textarea>
        </div>

        <!-- Children (recursive via explicit nesting for Vue template compatibility) -->
        <div v-if="node.id && expandedNodes.has(node.id) && node.children?.length" class="space-y-0.5">
          <template v-for="child in node.children" :key="child.id">
            <div class="flex items-start gap-2 px-3 py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 group transition-colors" :style="{ paddingLeft: '40px' }">
              <button v-if="child.children?.length" @click="child.id && toggleExpand(child.id)" class="flex-shrink-0 p-0.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 mt-0.5">
                <ChevronDown v-if="child.id && expandedNodes.has(child.id)" class="w-4 h-4" />
                <ChevronRight v-else class="w-4 h-4" />
              </button>
              <span v-else class="w-5 flex-shrink-0" />
              <component :is="getLevelIcon(child.deep || 0)" :class="['w-4 h-4 flex-shrink-0 mt-0.5', getLevelColor(child.deep || 0)]" />

              <template v-if="editingNode?.id === child.id">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2">
                    <input v-model="editingName" class="flex-1 px-2 py-1 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" @keyup.enter="handleEdit" @keyup.escape="cancelEdit" />
                    <button @click="handleEdit" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">保存</button>
                    <button @click="cancelEdit" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded hover:bg-gray-200">取消</button>
                  </div>
                  <textarea v-model="editingDesc" class="mt-1 w-full px-2 py-1 text-xs bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）" @keyup.escape="cancelEdit"></textarea>
                </div>
              </template>
              <template v-else>
                <div class="flex-1 min-w-0">
                  <span class="text-sm text-gray-700 dark:text-gray-300">{{ child.name }}</span>
                  <span v-if="child.children?.length" class="text-xs text-gray-400 ml-1">({{ child.children.length }})</span>
                  <p v-if="child.description" class="text-xs text-gray-400 dark:text-gray-500 mt-0.5 whitespace-pre-line">{{ child.description }}</p>
                </div>
                <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
                  <button @click="showAddForm(child.id!)" :disabled="disabled" class="p-1 text-gray-400 hover:text-blue-500 disabled:opacity-50"><Plus class="w-3.5 h-3.5" /></button>
                  <button @click="startEdit(child)" :disabled="disabled" class="p-1 text-gray-400 hover:text-amber-500 disabled:opacity-50"><Edit3 class="w-3.5 h-3.5" /></button>
                  <button @click="handleDelete(child)" :disabled="disabled" class="p-1 text-gray-400 hover:text-red-500 disabled:opacity-50"><Trash2 class="w-3.5 h-3.5" /></button>
                </div>
              </template>
            </div>

            <!-- Add child form -->
            <div v-if="isAdding(child.id ?? -1)" class="p-2 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg" :style="{ marginLeft: '60px' }">
              <div class="flex items-center gap-2">
                <input v-model="newNodeName" class="flex-1 px-2 py-1 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="输入区域名称" @keyup.enter="handleAdd" @keyup.escape="hideAddForm" />
                <button @click="handleAdd" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">添加</button>
                <button @click="hideAddForm" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded">取消</button>
              </div>
              <textarea v-model="newNodeDesc" class="mt-2 w-full px-2 py-1 text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）"></textarea>
            </div>

            <!-- Level 2 children -->
            <div v-if="child.id && expandedNodes.has(child.id) && child.children?.length" class="space-y-0.5">
              <template v-for="gc in child.children" :key="gc.id">
                <div class="flex items-start gap-2 px-3 py-1.5 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 group transition-colors" :style="{ paddingLeft: '60px' }">
                  <button v-if="gc.children?.length" @click="gc.id && toggleExpand(gc.id)" class="flex-shrink-0 p-0.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 mt-0.5">
                    <ChevronDown v-if="gc.id && expandedNodes.has(gc.id)" class="w-3.5 h-3.5" />
                    <ChevronRight v-else class="w-3.5 h-3.5" />
                  </button>
                  <span v-else class="w-4.5 flex-shrink-0" />
                  <component :is="getLevelIcon(gc.deep || 0)" :class="['w-3.5 h-3.5 flex-shrink-0 mt-0.5', getLevelColor(gc.deep || 0)]" />

                  <template v-if="editingNode?.id === gc.id">
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2">
                        <input v-model="editingName" class="flex-1 px-2 py-1 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" @keyup.enter="handleEdit" @keyup.escape="cancelEdit" />
                        <button @click="handleEdit" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">保存</button>
                        <button @click="cancelEdit" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded hover:bg-gray-200">取消</button>
                      </div>
                      <textarea v-model="editingDesc" class="mt-1 w-full px-2 py-1 text-xs bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）" @keyup.escape="cancelEdit"></textarea>
                    </div>
                  </template>
                  <template v-else>
                    <div class="flex-1 min-w-0">
                      <span class="text-sm text-gray-600 dark:text-gray-400">{{ gc.name }}</span>
                      <p v-if="gc.description" class="text-xs text-gray-400 dark:text-gray-500 mt-0.5 whitespace-pre-line">{{ gc.description }}</p>
                    </div>
                    <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
                      <button @click="showAddForm(gc.id!)" :disabled="disabled" class="p-1 text-gray-400 hover:text-blue-500 disabled:opacity-50"><Plus class="w-3 h-3" /></button>
                      <button @click="startEdit(gc)" :disabled="disabled" class="p-1 text-gray-400 hover:text-amber-500 disabled:opacity-50"><Edit3 class="w-3 h-3" /></button>
                      <button @click="handleDelete(gc)" :disabled="disabled" class="p-1 text-gray-400 hover:text-red-500 disabled:opacity-50"><Trash2 class="w-3 h-3" /></button>
                    </div>
                  </template>
                </div>

                <!-- Add child form -->
                <div v-if="isAdding(gc.id ?? -1)" class="p-2 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg" :style="{ marginLeft: '80px' }">
                  <div class="flex items-center gap-2">
                    <input v-model="newNodeName" class="flex-1 px-2 py-1 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="输入区域名称" @keyup.enter="handleAdd" @keyup.escape="hideAddForm" />
                    <button @click="handleAdd" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">添加</button>
                    <button @click="hideAddForm" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded">取消</button>
                  </div>
                  <textarea v-model="newNodeDesc" class="mt-2 w-full px-2 py-1 text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）"></textarea>
                </div>

                <!-- Level 3+ children -->
                <div v-if="gc.id && expandedNodes.has(gc.id) && gc.children?.length" class="space-y-0.5">
                  <template v-for="deep in gc.children" :key="deep.id">
                    <div class="flex items-start gap-2 px-3 py-1 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 group transition-colors" :style="{ paddingLeft: '80px' }">
                      <MapPin class="w-3 h-3 flex-shrink-0 text-gray-400 mt-0.5" />
                      <template v-if="editingNode?.id === deep.id">
                        <div class="flex-1 min-w-0">
                          <div class="flex items-center gap-2">
                            <input v-model="editingName" class="flex-1 px-2 py-1 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" @keyup.enter="handleEdit" @keyup.escape="cancelEdit" />
                            <button @click="handleEdit" class="px-2 py-1 text-xs text-white bg-blue-500 rounded">保存</button>
                            <button @click="cancelEdit" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded">取消</button>
                          </div>
                          <textarea v-model="editingDesc" class="mt-1 w-full px-2 py-1 text-xs bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）" @keyup.escape="cancelEdit"></textarea>
                        </div>
                      </template>
                      <template v-else>
                        <div class="flex-1 min-w-0">
                          <span class="text-xs text-gray-500 dark:text-gray-400">{{ deep.name }}</span>
                          <p v-if="deep.description" class="text-xs text-gray-400 dark:text-gray-500 mt-0.5 whitespace-pre-line">{{ deep.description }}</p>
                        </div>
                        <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
                          <button @click="startEdit(deep)" :disabled="disabled" class="p-0.5 text-gray-400 hover:text-amber-500 disabled:opacity-50"><Edit3 class="w-3 h-3" /></button>
                          <button @click="handleDelete(deep)" :disabled="disabled" class="p-0.5 text-gray-400 hover:text-red-500 disabled:opacity-50"><Trash2 class="w-3 h-3" /></button>
                        </div>
                      </template>
                    </div>

                    <!-- Add child form for deep nodes -->
                    <div v-if="isAdding(deep.id ?? -1)" class="p-2 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg" :style="{ marginLeft: '100px' }">
                      <div class="flex items-center gap-2">
                        <input v-model="newNodeName" class="flex-1 px-2 py-1 text-sm bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="输入区域名称" @keyup.enter="handleAdd" @keyup.escape="hideAddForm" />
                        <button @click="handleAdd" class="px-2 py-1 text-xs text-white bg-blue-500 rounded hover:bg-blue-600">添加</button>
                        <button @click="hideAddForm" class="px-2 py-1 text-xs text-gray-500 bg-gray-100 dark:bg-gray-700 rounded">取消</button>
                      </div>
                      <textarea v-model="newNodeDesc" class="mt-2 w-full px-2 py-1 text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded resize-none focus:outline-none focus:ring-1 focus:ring-blue-500" rows="2" placeholder="区域描述（可选）"></textarea>
                    </div>
                  </template>
                </div>
              </template>
            </div>
          </template>
        </div>
      </template>
    </div>
  </div>
</template>
