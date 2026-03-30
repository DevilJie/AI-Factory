import request from '@/utils/request'

import type { PowerSystemLevelStep } from './powerSystem'
// 力量体系类型
export interface PowerSystemLevelStep {
  id?: number
  level?: number
  levelName: string
}

export interface PowerSystemLevel {
  id?: number
  level?: number
  levelName: string
  description?: string
  breakthroughCondition?: string
  lifespan?: string
  powerRange?: string
  landmarkAbility?: string
  steps?: PowerSystemLevelStep[]
}

export interface PowerSystem {
  id?: number
  projectId?: number
  name: string
  sourceFrom?: string
  coreResource?: string
  cultivationMethod?: string
  description?: string
  createTime?: string
  updateTime?: string
  levels?: PowerSystemLevel[]
}

export const getPowerSystemList = (projectId: string) => {
  return request.get<PowerSystem[]>(`/api/novel/${projectId}/power-system/list`)
}

export const getPowerSystemDetail = (projectId: string, id: number) => {
  return request.get<PowerSystem>(`/api/novel/${projectId}/power-system/${id}`)
}

export const savePowerSystem = (projectId: string, data: PowerSystem) => {
  return request.post<PowerSystem>(`/api/novel/${projectId}/power-system/save`, data)
}

export const deletePowerSystem = (projectId: string, id: number) => {
  return request.delete(`/api/novel/${projectId}/power-system/${id}`)
}