import request from '@/utils/request'
import type { Project, ProjectListRequest, PageResult, DashboardStats, ProjectOverview } from '@/types'

export const getProjectList = (params: ProjectListRequest) => {
  return request.get<PageResult<Project>>('/api/project/list', { params })
}

export const getProjectDetail = (projectId: string) => {
  return request.get<Project>(`/api/project/detail/${projectId}`)
}

export const createProject = (data: any) => {
  return request.post<string>('/api/project/create', data)
}

export const updateProject = (data: any) => {
  return request.put<string>('/api/project/update', data)
}

export const deleteProject = (projectId: string) => {
  return request.delete<string>(`/api/project/delete/${projectId}`)
}

export const getDashboardStats = () => {
  return request.get<DashboardStats>('/api/project/stats')
}

export const generateProject = (data: { idea: string }) => {
  return request.post<{ name: string; description: string }>('/api/ai/generateProject', data)
}

export const getProjectOverview = (projectId: string) => {
  return request.get<ProjectOverview>(`/api/project/${projectId}/overview`)
}
