import request from '@/utils/request'

// 大陆区域接口
export interface ContinentRegion {
  id?: number
  parentId?: number | null
  deep?: number
  sortOrder?: number
  projectId?: number
  name: string
  description?: string
  createTime?: string
  updateTime?: string
  children?: ContinentRegion[]
}

// 获取地理区域树
export const getGeographyTree = (projectId: string) => {
  return request.get<ContinentRegion[]>(`/api/novel/${projectId}/continent-region/tree`)
}

// 获取地理区域平铺列表
export const getGeographyList = (projectId: string) => {
  return request.get<ContinentRegion[]>(`/api/novel/${projectId}/continent-region/list`)
}

// 新增区域节点
export const addRegion = (projectId: string, data: ContinentRegion) => {
  return request.post<ContinentRegion>(`/api/novel/${projectId}/continent-region/save`, data)
}

// 更新区域节点
export const updateRegion = (projectId: string, data: ContinentRegion) => {
  return request.put<ContinentRegion>(`/api/novel/${projectId}/continent-region/update`, data)
}

// 删除区域节点
export const deleteRegion = (projectId: string, id: number) => {
  return request.delete(`/api/novel/${projectId}/continent-region/${id}`)
}
