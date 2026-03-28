// ai-factory-frontend2/src/types/settings.ts

/**
 * AI 服务提供商类型
 */
export type ProviderType = 'llm' | 'image' | 'tts' | 'video'

/**
 * AI 提供商模板
 */
export interface AiProviderTemplate {
  id: number
  templateCode: string
  displayName: string
  description: string
  providerType: ProviderType
  iconUrl?: string
  defaultModel?: string
  defaultEndpoint?: string
}

/**
 * AI 服务提供商配置
 */
export interface AiProvider {
  id: number
  providerCode: string
  providerName: string
  providerType: ProviderType
  apiKey?: string
  apiEndpoint?: string
  model?: string
  isDefault: number
  enabled: number
  iconUrl?: string
  configJson?: string
}

/**
 * 保存 AI 提供商配置请求
 */
export interface SaveAiProviderRequest {
  id?: number
  providerType: ProviderType
  providerCode: string
  providerName: string
  apiKey?: string
  apiEndpoint?: string
  model?: string
  isDefault?: number
  enabled?: number
  configJson?: string
}

/**
 * 测试连接请求
 */
export interface TestAiProviderRequest {
  providerType: ProviderType
  providerCode: string
  apiKey?: string
  apiEndpoint?: string
  model?: string
}

/**
 * 测试连接响应
 */
export interface TestAiProviderResponse {
  success: boolean
  message: string
}

/**
 * 用户详细信息（扩展）
 */
export interface UserDetail {
  userId: number
  loginName: string
  actualName: string
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
  bio?: string
}

/**
 * 更新用户信息请求
 */
export interface UpdateUserInfoRequest {
  nickname?: string
  phone?: string
  email?: string
  bio?: string
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/**
 * 设置抽屉 Tab 类型
 */
export type SettingsTab = 'ai-model' | 'user-profile' | 'security'
