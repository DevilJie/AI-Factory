import request from '@/utils/request'
import type { WorldSetting } from '@/types/project'

// 获取项目的世界观设定列表
export const getWorldSettingList = (projectId: string) => {
  return request.get<WorldSetting[]>(`/api/project/${projectId}/world-settings`)
}

// 获取设定详情
export const getWorldSettingDetail = (settingId: string) => {
  return request.get<WorldSetting>(`/api/world-setting/${settingId}`)
}

// 创建设定
export const createWorldSetting = (projectId: string, data: Partial<WorldSetting>) => {
  return request.post<WorldSetting>(`/api/project/${projectId}/world-setting`, data)
}

// 更新设定
export const updateWorldSetting = (settingId: string, data: Partial<WorldSetting>) => {
  return request.put<WorldSetting>(`/api/world-setting/${settingId}`, data)
}

// 删除设定
export const deleteWorldSetting = (settingId: string) => {
  return request.delete(`/api/world-setting/${settingId}`)
}
