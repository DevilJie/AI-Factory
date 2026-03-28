import request from '@/utils/request'

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
    avatar: dto.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${dto.name}`,
    role: dto.role || '',
    roleType: (dto.roleType as CharacterRoleType) || 'supporting',
    gender: (dto.gender as CharacterGender) || 'other',
    personality: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }))
}

/**
 * 获取角色详情
 */
export const getCharacterDetail = async (projectId: string, characterId: string): Promise<Character> => {
  const response = await request.get<CharacterDto>(
    `/api/novel/${projectId}/characters/${characterId}`
  )
  return {
    id: String(response.id),
    projectId,
    name: response.name,
    avatar: response.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${response.name}`,
    role: response.role || '',
    roleType: (response.roleType as CharacterRoleType) || 'supporting',
    gender: (response.gender as CharacterGender) || 'other',
    personality: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
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
