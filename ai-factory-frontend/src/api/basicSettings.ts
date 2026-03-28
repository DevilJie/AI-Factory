import request from '@/utils/request'

// 基础设置接口
export interface BasicSettings {
  id?: number
  projectId?: number
  // 情节结构属性
  narrativeStructure?: string
  endingType?: string
  endingTone?: string
  // 叙事风格属性
  writingStyle?: string
  writingPerspective?: string
  narrativePace?: string
  languageStyle?: string
  descriptionFocus?: string
  // 复杂结构配置
  plotStages?: string
  singleChapterSettings?: string
  foreshadowingSettings?: string
  createTime?: string
  updateTime?: string
}

// 设置状态
export interface SetupStatus {
  currentStage: string
  canAccessCreation: boolean
  worldviewLocked: boolean
  basicSettingsLocked: boolean
}

// 获取项目基础设置
export const getBasicSettings = (projectId: string) => {
  return request.get<BasicSettings>(`/api/project/${projectId}/basic-settings`)
}

// 保存项目基础设置
export const saveBasicSettings = (projectId: string, data: BasicSettings) => {
  return request.post<void>(`/api/project/${projectId}/basic-settings`, data)
}

// 获取项目设置状态
export const getSetupStatus = (projectId: string) => {
  return request.get<SetupStatus>(`/api/project/${projectId}/setup-status`)
}
