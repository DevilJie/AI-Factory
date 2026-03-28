import request from '@/utils/request'

// AI生成大纲请求
export interface GenerateOutlineRequest {
  projectDescription?: string
  storyTone?: string
  storyGenre?: string
  targetVolumeCount?: number
  avgWordsPerVolume?: number
  targetWordCount?: number
  additionalRequirements?: string
  startVolumeNumber?: number
  regenerate?: boolean
}

// 大纲DTO
export interface OutlineDto {
  id: number
  projectId: number
  concept?: string
  theme?: string
  volumes?: VolumePlanDto[]
  createTime?: string
  updateTime?: string
}

// 分卷规划DTO
export interface VolumePlanDto {
  id: string
  volumeNumber: number
  title: string
  theme?: string
  mainConflict?: string
  plotDirection?: string
  targetWordCount?: number
  chapterCount?: number
  chapters?: ChapterPlanDto[]
}

// 章节规划DTO
export interface ChapterPlanDto {
  id: string
  chapterNumber: number
  title: string
  summary?: string
  targetWordCount?: number
  plotPoints?: string
  generated?: boolean
}

// 获取项目大纲
export const getOutline = (projectId: string) => {
  return request.get<OutlineDto>(`/api/novel/${projectId}/outline`)
}

// AI生成大纲（异步）
export const generateOutlineAsync = (projectId: string, data: GenerateOutlineRequest) => {
  return request.post<{ taskId: string; message: string }>(`/api/novel/${projectId}/outline/generate-async`, data)
}

// AI生成章节计划
export const generateChapters = (projectId: string, volumeId: string, plotStage: string) => {
  return request.post<{ taskId: string; message: string }>(`/api/novel/${projectId}/outline/generate-chapters`, {
    volumeId,
    plotStage
  })
}

// 更新分卷信息
export const updateVolume = (projectId: string, volumeId: string, data: Record<string, any>) => {
  return request.put(`/api/novel/${projectId}/volumes/${volumeId}`, data)
}

// AI优化分卷请求
export interface OptimizeVolumeRequest {
  targetChapterCount: number
}

// AI优化分卷详情（异步）
export const optimizeVolume = (projectId: string, volumeId: string, data: OptimizeVolumeRequest) => {
  return request.post<{ taskId: string; message: string }>(`/api/novel/${projectId}/outline/volumes/${volumeId}/optimize`, data)
}
