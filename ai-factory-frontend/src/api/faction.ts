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

// 势力关系接口
export interface FactionRelation {
  id?: number
  factionId?: number
  targetFactionId?: number
  relationType: string
  description?: string
}

// 势力-人物关联接口
export interface FactionCharacter {
  id?: number
  factionId?: number
  characterId: number
  role?: string
}

// 势力-地区关联接口
export interface FactionRegion {
  id?: number
  factionId?: number
  regionId: number
}

// 获取势力平铺列表（用于目标势力选择器）
export const getFactionList = (projectId: string) => {
  return request.get<Faction[]>(`/api/novel/${projectId}/faction/list`)
}

// 势力关系 API
export const getFactionRelations = (projectId: string, factionId: number) => {
  return request.get<FactionRelation[]>(`/api/novel/${projectId}/faction/${factionId}/relations`)
}

export const addFactionRelation = (projectId: string, factionId: number, data: Partial<FactionRelation>) => {
  return request.post<FactionRelation>(`/api/novel/${projectId}/faction/${factionId}/relations`, data)
}

export const deleteFactionRelation = (projectId: string, factionId: number, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${factionId}/relations/${id}`)
}

// 势力-人物关联 API
export const getFactionCharacters = (projectId: string, factionId: number) => {
  return request.get<FactionCharacter[]>(`/api/novel/${projectId}/faction/${factionId}/characters`)
}

export const addFactionCharacter = (projectId: string, factionId: number, data: Partial<FactionCharacter>) => {
  return request.post<FactionCharacter>(`/api/novel/${projectId}/faction/${factionId}/characters`, data)
}

export const deleteFactionCharacter = (projectId: string, factionId: number, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${factionId}/characters/${id}`)
}

// 势力-地区关联 API
export const getFactionRegions = (projectId: string, factionId: number) => {
  return request.get<FactionRegion[]>(`/api/novel/${projectId}/faction/${factionId}/regions`)
}

export const addFactionRegion = (projectId: string, factionId: number, data: Partial<FactionRegion>) => {
  return request.post<FactionRegion>(`/api/novel/${projectId}/faction/${factionId}/regions`, data)
}

export const deleteFactionRegion = (projectId: string, factionId: number, id: number) => {
  return request.delete(`/api/novel/${projectId}/faction/${factionId}/regions/${id}`)
}
