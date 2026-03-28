import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Project } from '@/types/project'
import { getProjectDetail, updateProject as updateProjectApi } from '@/api/project'

export const useProjectStore = defineStore('project', () => {
  // State
  const currentProject = ref<Project | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const projectId = computed(() => currentProject.value?.id ?? null)
  const projectName = computed(() => currentProject.value?.name ?? '')
  const isLoading = computed(() => loading.value)

  // Actions
  const fetchProject = async (id: string) => {
    loading.value = true
    error.value = null
    try {
      const data = await getProjectDetail(id)
      currentProject.value = data
    } catch (e: any) {
      error.value = e.message || '加载项目失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  const updateProject = async (data: Partial<Project>) => {
    if (!currentProject.value) return
    loading.value = true
    error.value = null
    try {
      await updateProjectApi({ ...currentProject.value, ...data })
      currentProject.value = { ...currentProject.value, ...data }
    } catch (e: any) {
      error.value = e.message || '更新项目失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  const clearProject = () => {
    currentProject.value = null
    error.value = null
  }

  return {
    // State
    currentProject,
    loading,
    error,
    // Getters
    projectId,
    projectName,
    isLoading,
    // Actions
    fetchProject,
    updateProject,
    clearProject
  }
})
