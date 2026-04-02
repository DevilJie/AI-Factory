<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Loader2, Trash2, ChevronRight, ChevronDown, MapPin } from 'lucide-vue-next'
import {
  getFactionRegions,
  addFactionRegion,
  deleteFactionRegion,
  type FactionRegion
} from '@/api/faction'
import { getGeographyTree, type ContinentRegion } from '@/api/continentRegion'
import { success, error } from '@/utils/toast'

const props = defineProps<{
  factionId: number
  projectId: string
}>()

const loading = ref(false)
const regions = ref<FactionRegion[]>([])
const regionTree = ref<ContinentRegion[]>([])
const selectedRegionIds = ref<number[]>([])
const expandedNodes = ref<Set<number>>(new Set())
const submitting = ref(false)

const associatedRegionIds = computed(() => {
  return new Set(regions.value.map(r => r.regionId))
})

const flattenTree = (nodes: ContinentRegion[]): ContinentRegion[] => {
  const result: ContinentRegion[] = []
  const walk = (items: ContinentRegion[]) => {
    for (const item of items) {
      result.push(item)
      if (item.children?.length) {
        walk(item.children)
      }
    }
  }
  walk(nodes)
  return result
}

const flatNodes = computed(() => flattenTree(regionTree.value))

const regionNameMap = computed(() => {
  const map = new Map<number, string>()
  for (const node of flatNodes.value) {
    if (node.id) map.set(node.id, node.name)
  }
  return map
})

const regionParentMap = computed(() => {
  const map = new Map<number, number | null>()
  for (const node of flatNodes.value) {
    if (node.id) map.set(node.id, node.parentId ?? null)
  }
  return map
})

const getRegionPath = (regionId: number): string => {
  const parts: string[] = []
  let currentId: number | null | undefined = regionId
  while (currentId != null) {
    const name = regionNameMap.value.get(currentId)
    if (name) parts.unshift(name)
    currentId = regionParentMap.value.get(currentId)
  }
  return parts.join(' > ')
}

const visibleNodes = computed(() => {
  const result: ContinentRegion[] = []
  const walk = (nodes: ContinentRegion[], parentExpanded: boolean) => {
    for (const node of nodes) {
      if (parentExpanded) result.push(node)
      if (node.children?.length && expandedNodes.value.has(node.id!)) {
        walk(node.children, parentExpanded && expandedNodes.value.has(node.id!))
      }
    }
  }
  walk(regionTree.value, true)
  return result
})

const loadData = async () => {
  loading.value = true
  try {
    const [treeData, assocData] = await Promise.all([
      getGeographyTree(props.projectId),
      getFactionRegions(props.projectId, props.factionId)
    ])
    regionTree.value = treeData
    regions.value = assocData
    // Auto-expand first-level nodes
    const newExpanded = new Set<number>()
    for (const node of treeData) {
      if (node.id) newExpanded.add(node.id)
    }
    expandedNodes.value = newExpanded
  } catch (e: any) {
    error('加载地区关联数据失败')
  } finally {
    loading.value = false
  }
}

const toggleExpand = (id: number) => {
  const newSet = new Set(expandedNodes.value)
  if (newSet.has(id)) {
    newSet.delete(id)
  } else {
    newSet.add(id)
  }
  expandedNodes.value = newSet
}

const handleSubmit = async () => {
  if (selectedRegionIds.value.length === 0) {
    error('请至少选择一个地区')
    return
  }
  submitting.value = true
  try {
    const idsToAdd = selectedRegionIds.value.filter(id => !associatedRegionIds.value.has(id))
    let addedCount = 0
    for (const id of idsToAdd) {
      await addFactionRegion(props.projectId, props.factionId, { regionId: id })
      addedCount++
    }
    success(`已添加 ${addedCount} 个地区`)
    selectedRegionIds.value = []
    await loadData()
  } catch (e: any) {
    error('添加地区失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (association: FactionRegion) => {
  const regionName = regionNameMap.value.get(association.regionId) || '未知地区'
  if (!confirm(`移除地区「${regionName}」的关联？`)) return
  try {
    if (association.id) {
      await deleteFactionRegion(props.projectId, props.factionId, association.id)
    }
    success('移除成功')
    await loadData()
  } catch (e: any) {
    error('移除地区关联失败')
  }
}

const hasChildren = (node: ContinentRegion): boolean => {
  return !!(node.children && node.children.length > 0)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div>
    <!-- 地区树选择器 -->
    <div class="mb-6">
      <div class="border border-gray-200 dark:border-gray-600 rounded-lg p-3 max-h-60 overflow-y-auto">
        <div
          v-for="node in visibleNodes"
          :key="node.id"
          class="flex items-center gap-2 py-1"
          :style="{ paddingLeft: `${(node.deep ?? 0) * 16 + 8}px` }"
        >
          <!-- 展开/折叠按钮 -->
          <button
            v-if="hasChildren(node)"
            @click="node.id && toggleExpand(node.id)"
            class="p-0 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 flex-shrink-0"
          >
            <ChevronDown v-if="node.id && expandedNodes.has(node.id)" class="w-4 h-4" />
            <ChevronRight v-else class="w-4 h-4" />
          </button>
          <!-- 占位符：没有子节点时保持对齐 -->
          <span v-else class="w-4 flex-shrink-0" />

          <!-- 复选框 -->
          <input
            type="checkbox"
            :value="node.id"
            :checked="associatedRegionIds.has(node.id!) || selectedRegionIds.includes(node.id!)"
            :disabled="associatedRegionIds.has(node.id!)"
            @change="(e: Event) => {
              const checked = (e.target as HTMLInputElement).checked
              if (checked && node.id && !selectedRegionIds.includes(node.id)) {
                selectedRegionIds.push(node.id)
              } else if (!checked && node.id) {
                const idx = selectedRegionIds.indexOf(node.id)
                if (idx > -1) selectedRegionIds.splice(idx, 1)
              }
            }"
            class="flex-shrink-0"
          />

          <!-- 节点名称 -->
          <span
            :class="[
              'text-sm',
              associatedRegionIds.has(node.id!) ? 'text-gray-400' : 'text-gray-800 dark:text-gray-200'
            ]"
          >
            {{ node.name }}
          </span>
        </div>

        <!-- 空树 -->
        <div v-if="regionTree.length === 0 && !loading" class="text-center py-4 text-gray-400 text-sm">
          暂无地区数据，请先创建地区
        </div>
      </div>

      <!-- 提交按钮 -->
      <button
        @click="handleSubmit"
        :disabled="selectedRegionIds.length === 0 || submitting"
        class="mt-2 flex items-center gap-2 px-4 py-2 text-sm text-white bg-blue-500 rounded-lg hover:bg-blue-600 disabled:opacity-50 transition-colors"
      >
        <MapPin class="w-4 h-4" />
        添加地区
      </button>
    </div>

    <!-- 地区关联列表 -->
    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="w-6 h-6 text-blue-500 animate-spin" />
    </div>

    <div v-else-if="!regions.length" class="text-center py-8 text-gray-400">
      暂无关联地区，选择地区后添加
    </div>

    <div v-else class="space-y-2 mt-4">
      <div
        v-for="association in regions"
        :key="association.id"
        class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
      >
        <!-- 左侧：地区名称（含层级路径） -->
        <div class="min-w-0 flex-1">
          <span class="text-sm font-medium text-gray-800 dark:text-gray-200">
            {{ getRegionPath(association.regionId) }}
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
