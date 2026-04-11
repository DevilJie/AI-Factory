/**
 * 伏笔相关类型定义
 * Maps from backend ForeshadowingDto, ForeshadowingCreateDto, ForeshadowingUpdateDto, ForeshadowingQueryDto
 */

/**
 * 伏笔类型
 */
export type ForeshadowingType = 'character' | 'item' | 'event' | 'secret'

/**
 * 布局线类型
 */
export type ForeshadowingLayoutType = 'bright1' | 'bright2' | 'bright3' | 'dark'

/**
 * 伏笔状态
 */
export type ForeshadowingStatus = 'pending' | 'in_progress' | 'completed'

/**
 * 伏笔信息 (maps from ForeshadowingDto)
 */
export interface Foreshadowing {
  id: number
  projectId: number
  title: string
  type: ForeshadowingType
  description: string
  layoutType: ForeshadowingLayoutType | null
  plantedChapter: number
  plantedVolume: number | null
  plannedCallbackChapter: number | null
  plannedCallbackVolume: number | null
  actualCallbackChapter: number | null
  status: ForeshadowingStatus
  priority: number
  notes: string | null
  createTime: string
  updateTime: string
}

/**
 * 伏笔创建请求 (maps from ForeshadowingCreateDto)
 * Per D-05: plantedChapter is required for creation
 */
export interface ForeshadowingCreateRequest {
  title: string
  type: ForeshadowingType
  description: string
  layoutType?: ForeshadowingLayoutType
  plantedChapter: number
  plantedVolume?: number
  plannedCallbackChapter?: number
  plannedCallbackVolume?: number
  priority?: number
  notes?: string
}

/**
 * 伏笔更新请求 (maps from ForeshadowingUpdateDto)
 * Per D-05: plantedChapter is NOT included (immutable after creation)
 */
export interface ForeshadowingUpdateRequest {
  title: string
  type: ForeshadowingType
  description: string
  layoutType?: ForeshadowingLayoutType
  plannedCallbackChapter?: number
  plannedCallbackVolume?: number
  actualCallbackChapter?: number
  status?: ForeshadowingStatus
  priority?: number
  notes?: string
}

/**
 * 伏笔查询参数 (maps from ForeshadowingQueryDto)
 */
export interface ForeshadowingQueryParams {
  type?: ForeshadowingType
  layoutType?: ForeshadowingLayoutType
  status?: ForeshadowingStatus
  currentChapter?: number
  plantedChapter?: number
  plannedCallbackChapter?: number
  plantedVolume?: number
  plannedCallbackVolume?: number
}
