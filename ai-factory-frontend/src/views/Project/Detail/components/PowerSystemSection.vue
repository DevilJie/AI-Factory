<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Loader2, Sparkles } from 'lucide-vue-next'
import {
  getPowerSystemList,
  savePowerSystem,
  deletePowerSystem,
  type PowerSystem
} from '@/api/powerSystem'
import { success, error } from '@/utils/toast'
import PowerSystemCard from './PowerSystemCard.vue'
import PowerSystemForm from './PowerSystemForm.vue'

const props = defineProps<{
  projectId: string
  disabled: boolean
  generatingSelf?: boolean
}>()

const emit = defineEmits<{
  generate: []
}>()

const systems = ref<PowerSystem[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingSystem = ref<PowerSystem | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    systems.value = await getPowerSystemList(props.projectId)
  } catch (e: any) {
    console.error('加载力量体系失败:', e)
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  loadData()
}

const handleAdd = () => {
  editingSystem.value = null
  showForm.value = true
}

const handleEdit = (system: PowerSystem) => {
  editingSystem.value = system
  showForm.value = true
}

const handleDelete = async (id: number) => {
  try {
    await deletePowerSystem(props.projectId, id)
    systems.value = systems.value.filter(s => s.id !== id)
    success('删除成功')
  } catch (e: any) {
    error('删除失败')
  }
}

const handleFormSave = async (data: PowerSystem) => {
  try {
    await savePowerSystem(props.projectId, data)
    await loadData()
    showForm.value = false
    editingSystem.value = null
    success('保存成功')
  } catch (e: any) {
    error('保存失败')
  }
}

onMounted(() => {
  loadData()
})

defineExpose({ refresh })
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm space-y-4">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">力量体系</h3>
      <div class="flex items-center gap-2">
        <button
          @click="emit('generate')"
          :disabled="disabled"
          class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-white bg-gradient-to-r from-green-500 to-teal-500 rounded-lg hover:from-green-600 hover:to-teal-600 transition-colors disabled:opacity-50"
        >
          <Loader2 v-if="generatingSelf" class="w-3 h-3 animate-spin" />
          <Sparkles v-else class="w-3 h-3" />
          {{ generatingSelf ? '生成中...' : 'AI生成' }}
        </button>
        <button
          @click="handleAdd"
          :disabled="disabled"
          class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-800/30 transition-colors disabled:opacity-50"
        >
          <Plus class="w-3 h-3" />
          添加力量体系
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
    </div>

    <!-- 卡片列表 -->
    <div v-else-if="systems.length > 0" class="space-y-3">
      <PowerSystemCard
        v-for="system in systems"
        :key="system.id"
        :system="system"
        :disabled="disabled"
        @edit="handleEdit(system)"
        @delete="handleDelete(system.id!)"
      />
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-8 text-gray-400 dark:text-gray-500">
      暂无力量体系设定，点击上方按钮添加
    </div>

    <!-- 编辑表单弹窗 -->
    <PowerSystemForm
      v-if="showForm"
      :system="editingSystem"
      :project-id="projectId"
      @save="handleFormSave"
      @close="showForm = false"
    />
  </div>
</template>
