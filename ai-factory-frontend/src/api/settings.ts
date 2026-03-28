// ai-factory-frontend2/src/api/settings.ts

import request from '@/utils/request'
import type {
  AiProvider,
  AiProviderTemplate,
  SaveAiProviderRequest,
  TestAiProviderRequest,
  TestAiProviderResponse,
  UserDetail,
  UpdateUserInfoRequest,
  ChangePasswordRequest
} from '@/types/settings'
import type { ProviderType } from '@/types/settings'

// ==================== 用户相关 ====================

/**
 * 获取用户详细信息
 */
export const getUserDetail = () => {
  return request.get<UserDetail>('/api/user/detail')
}

/**
 * 更新用户基本信息
 */
export const updateUserInfo = (data: UpdateUserInfoRequest) => {
  return request.put<string>('/api/user/info', data)
}

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordRequest) => {
  return request.put<string>('/api/user/password', data)
}

// ==================== AI 提供商相关 ====================

/**
 * 获取 AI 服务提供商配置列表
 */
export const getAiProviders = (providerType?: ProviderType) => {
  return request.get<AiProvider[]>('/api/ai-provider', {
    params: { providerType }
  })
}

/**
 * 获取 AI 服务商模板列表
 */
export const getAiProviderTemplates = () => {
  return request.get<AiProviderTemplate[]>('/api/ai-provider/templates')
}

/**
 * 保存 AI 服务提供商配置
 */
export const saveAiProvider = (data: SaveAiProviderRequest) => {
  return request.put<string>('/api/ai-provider', data)
}

/**
 * 删除 AI 服务提供商配置
 */
export const deleteAiProvider = (id: number) => {
  return request.delete<string>(`/api/ai-provider/${id}`)
}

/**
 * 测试 AI 服务提供商连接（通过配置ID）
 * 后端会查询真实的API密钥进行测试
 */
export const testAiProviderById = (id: number) => {
  return request.post<TestAiProviderResponse>(`/api/ai-provider/test/${id}`)
}
