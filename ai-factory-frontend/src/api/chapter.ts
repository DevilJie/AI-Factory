import request from '@/utils/request'
import type { Volume, Chapter } from '@/types/project'

// 获取项目的分卷列表
export const getVolumeList = (projectId: string) => {
  return request.get<Volume[]>(`/api/novel/${projectId}/volumes`)
}

// 获取项目的章节列表
export const getChapterList = (projectId: string) => {
  return request.get<Chapter[]>(`/api/novel/${projectId}/chapters`)
}

// 获取章节规划列表（带分卷信息）
export const getChapterPlans = (projectId: string) => {
  return request.get<Chapter[]>(`/api/novel/${projectId}/chapters/plans`)
}

// 获取章节详情
export const getChapterDetail = (projectId: string, chapterId: string) => {
  return request.get<Chapter>(`/api/novel/${projectId}/chapters/${chapterId}`)
}

// 根据章节规划ID获取章节详情
export const getChapterByPlanId = (projectId: string, planId: string) => {
  return request.get<Chapter>(`/api/novel/${projectId}/chapters/by-plan/${planId}`)
}

// 更新章节内容
export const updateChapter = (projectId: string, chapterId: string, data: Partial<Chapter>) => {
  return request.put<Chapter>(`/api/novel/${projectId}/chapters/update/${chapterId}`, data)
}

// 创建新章节
export const createChapter = (projectId: string, title: string, chapterNumber?: number) => {
  const params = new URLSearchParams()
  params.append('title', title)
  if (chapterNumber) {
    params.append('chapterNumber', chapterNumber.toString())
  }
  return request.post<number>(`/api/novel/${projectId}/chapters?${params.toString()}`)
}

// 删除章节
export const deleteChapter = (projectId: string, chapterId: string) => {
  return request.delete(`/api/novel/${projectId}/chapters/${chapterId}`)
}

// AI 润色
export const aiPolish = (projectId: string, chapterId: string, style: string = 'vivid', degree: string = 'medium') => {
  return request.post<{ content: string }>(`/api/novel/${projectId}/chapters/${chapterId}/ai-polish`, {
    style,
    degree
  })
}

// AI 剧情修复
export const aiFix = (projectId: string, chapterId: string, fixOptions: string[] = []) => {
  return request.post<{ content: string }>(`/api/novel/${projectId}/chapters/${chapterId}/ai-fix`, {
    fixOptions
  })
}

// AI创作章节（异步接口）
export const generateChapter = (projectId: string, planId: string) => {
  return request.post<{ taskId: string; message: string }>(`/api/novel/${projectId}/chapters/generate-async/${planId}`)
}

// AI剧情修复报告项
export interface FixReportItem {
  type: string
  original: string
  fixed: string
  reason: string
  severity: 'high' | 'medium' | 'low'
}

// AI剧情修复响应类型
export interface ChapterAiFixResponse {
  fixedContent: string
  fixReport: FixReportItem[]
  totalFixes: number
  fixSummary: string
}

// AI剧情修复（异步接口）
export const fixChapterWithAIAsync = (projectId: string, chapterId: string, fixOptions: string[] = []) => {
  return request.post<{ taskId: string; message: string }>(`/api/novel/${projectId}/chapters/${chapterId}/ai-fix-async`, {
    fixOptions
  })
}
