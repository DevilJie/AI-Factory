import request from '@/utils/request'

/** 安全解析可能是 JSON 数组或纯文本的字段 */
function safeParseArray(value: unknown): string[] {
  if (!value) return []
  if (Array.isArray(value)) return value
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed : [value]
    } catch {
      return [value]
    }
  }
  return []
}

/**
 * 人物角色类型
 */
export type CharacterRoleType = 'protagonist' | 'supporting' | 'antagonist' | 'npc'

/**
 * 人物性别
 */
export type CharacterGender = 'male' | 'female' | 'other'

/**
 * 人物信息接口
 */
export interface Character {
  id: string
  projectId: string
  name: string
  englishName?: string
  nickname?: string
  gender: CharacterGender
  age?: number
  avatar: string
  roleType: CharacterRoleType
  role: string
  personality: string[]
  appearance?: string
  background?: string
  abilities?: string[]
  tags?: string[]
  createdAt: string
  updatedAt: string
  /** 修炼境界（列表API聚合字段） */
  cultivationRealm?: string
  /** 势力信息（列表API聚合字段） */
  factionInfo?: string
}

/**
 * 后端返回的简化人物DTO
 */
export interface CharacterDto {
  id: number
  name: string
  avatar: string
  role: string
  roleType?: string
  gender?: string
  cultivationRealm?: string
  factionInfo?: string
}

/**
 * 人物创建/编辑表单
 */
export interface CharacterForm {
  name: string
  englishName?: string
  nickname?: string
  gender: CharacterGender
  age?: number
  avatar?: string
  roleType: CharacterRoleType
  role: string
  personality: string[]
  appearance?: string
  background?: string
  abilities?: string[]
  tags?: string[]
}

/**
 * 力量体系关联（人物详情中）
 */
export interface PowerSystemAssociation {
  id: number
  powerSystemId: number
  powerSystemName: string
  currentRealmId: number | null
  currentRealmName: string | null
  currentSubRealmId: number | null
  currentSubRealmName: string | null
}

/**
 * 势力关联（人物详情中）
 */
export interface FactionAssociation {
  id: number
  factionId: number
  factionName: string
  role: string
}

/**
 * 扩展人物详情（含关联数据）
 */
export interface CharacterDetail extends Character {
  powerSystemAssociations: PowerSystemAssociation[]
  factionAssociations: FactionAssociation[]
}

/**
 * 获取人物列表
 */
export const getCharacterList = async (projectId: string): Promise<Character[]> => {
  const response = await request.get<CharacterDto[]>(
    `/api/novel/${projectId}/characters`
  )
  return (response || []).map(dto => ({
    id: String(dto.id),
    projectId,
    name: dto.name,
    avatar: dto.avatar || '',
    role: dto.role || '',
    roleType: (dto.roleType as CharacterRoleType) || 'supporting',
    gender: (dto.gender as CharacterGender) || 'other',
    personality: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    cultivationRealm: dto.cultivationRealm,
    factionInfo: dto.factionInfo
  }))
}

/**
 * 获取角色详情（含关联数据）
 */
export const getCharacterDetail = async (projectId: string, characterId: string): Promise<CharacterDetail> => {
  const response = await request.get<any>(
    `/api/novel/${projectId}/characters/${characterId}`
  )
  return {
    id: String(response.id),
    projectId,
    name: response.name || '',
    avatar: response.avatar || '',
    role: response.role || '',
    roleType: (response.roleType as CharacterRoleType) || 'supporting',
    gender: (response.gender as CharacterGender) || 'other',
    personality: safeParseArray(response.personality),
    appearance: response.appearance,
    background: response.background,
    abilities: safeParseArray(response.abilities),
    tags: safeParseArray(response.tags),
    createdAt: response.createdAt || new Date().toISOString(),
    updatedAt: response.updatedAt || new Date().toISOString(),
    powerSystemAssociations: response.powerSystemAssociations || [],
    factionAssociations: response.factionAssociations || []
  }
}

/**
 * 创建人物
 */
export const createCharacter = async (projectId: string, data: CharacterForm): Promise<string> => {
  const response = await request.post<number>(
    `/api/novel/${projectId}/characters`,
    {
      name: data.name,
      avatar: data.avatar,
      gender: data.gender,
      age: data.age,
      roleType: data.roleType,
      role: data.role,
      personality: data.personality ? JSON.stringify(data.personality) : null,
      appearance: data.appearance,
      background: data.background,
      abilities: data.abilities ? JSON.stringify(data.abilities) : null,
      tags: data.tags ? JSON.stringify(data.tags) : null
    }
  )
  return String(response)
}

/**
 * 更新人物
 */
export const updateCharacter = async (projectId: string, characterId: string, data: Partial<CharacterForm>): Promise<void> => {
  await request.put(
    `/api/novel/${projectId}/characters/${characterId}`,
    {
      name: data.name,
      avatar: data.avatar,
      gender: data.gender,
      age: data.age,
      roleType: data.roleType,
      role: data.role,
      personality: data.personality ? JSON.stringify(data.personality) : null,
      appearance: data.appearance,
      background: data.background,
      abilities: data.abilities ? JSON.stringify(data.abilities) : null,
      tags: data.tags ? JSON.stringify(data.tags) : null
    }
  )
}

/**
 * 删除人物
 */
export const deleteCharacter = async (projectId: string, characterId: string): Promise<void> => {
  await request.delete(`/api/novel/${projectId}/characters/${characterId}`)
}

/**
 * 添加力量体系关联
 */
export const addPowerSystemAssociation = async (
  projectId: string, characterId: string,
  data: { powerSystemId: number; currentRealmId?: number; currentSubRealmId?: number }
): Promise<void> => {
  await request.post(`/api/novel/${projectId}/characters/${characterId}/power-systems`, data)
}

/**
 * 删除力量体系关联
 */
export const deletePowerSystemAssociation = async (
  projectId: string, characterId: string, associationId: number
): Promise<void> => {
  await request.delete(`/api/novel/${projectId}/characters/${characterId}/power-systems/${associationId}`)
}

/**
 * 添加势力关联
 */
export const addFactionAssociation = async (
  projectId: string, characterId: string,
  data: { factionId: number; role: string }
): Promise<void> => {
  await request.post(`/api/novel/${projectId}/characters/${characterId}/factions`, data)
}

/**
 * 删除势力关联
 */
export const deleteFactionAssociation = async (
  projectId: string, characterId: string, associationId: number
): Promise<void> => {
  await request.delete(`/api/novel/${projectId}/characters/${characterId}/factions/${associationId}`)
}
