import request from '@/utils/request'

// 势力接口
export interface Faction {
  id?: number
  parentId?: number | null
  deep?: number
  sortOrder?: number
  projectId?: number
  name: string
  type?: string          // ally/hostile/neutral, top-level only
  corePowerSystem?: number | null  // power system ID, top-level only
  description?: string
  createTime?: string
  updateTime?: string
  children?: Faction[]
}

// 获取势力树
export const getFactionTree = (projectId: string) => {
  return request.get<Faction[]>(`/api/novel/${projectId}/faction/tree`)
}

// 新增势力节点
export const addFaction = (projectId: string, data: Faction) => {
  return request.post<Faction>(`/api/novel/${projectId}/faction/save`, data)
}

// 更新势力节点
export const updateFaction = (projectId: string, data: Faction) => {
  return request.put<Faction>(`/api/novel/${projectId}/faction/update`, data)
}

// 删除势力节点
export const deleteFaction = (projectId: string, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${id}`)
}
