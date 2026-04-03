import request from '@/utils/request'

// 世界观设定接口
export interface Worldview {
  id?: number
  projectId?: number
  outlineId?: number
  worldType?: string
  worldBackground?: string
  forces?: string
  timeline?: string
  rules?: string
  createTime?: string
  updateTime?: string
}

// 获取项目的世界观设定
export const getWorldview = (projectId: string) => {
  return request.get<Worldview>(`/api/novel/${projectId}/worldview`)
}

// 保存世界观设定
export const saveWorldview = (projectId: string, data: Worldview) => {
  return request.post<Worldview>(`/api/novel/${projectId}/worldview/save`, data)
}

// 删除世界观设定
export const deleteWorldview = (projectId: string) => {
  return request.delete(`/api/novel/${projectId}/worldview`)
}

// AI异步生成世界观设定
export const generateWorldviewAsync = (projectId: string) => {
  return request.post<{ taskId: string; message: string }>(`/api/novel/${projectId}/worldview/generate-async`, {})
}

// AI异步生成地理环境
export const generateGeographyAsync = (projectId: string) => {
  return request.post<{ taskId: string; message: string }>(
    `/api/novel/${projectId}/worldview/generate-geography`, {}
  )
}

// AI异步生成力量体系
export const generatePowerSystemAsync = (projectId: string) => {
  return request.post<{ taskId: string; message: string }>(
    `/api/novel/${projectId}/worldview/generate-power-system`, {}
  )
}

// AI异步生成势力阵营
export const generateFactionAsync = (projectId: string) => {
  return request.post<{ taskId: string; message: string }>(
    `/api/novel/${projectId}/worldview/generate-faction`, {}
  )
}
