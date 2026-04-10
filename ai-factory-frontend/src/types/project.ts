export type ProjectType = 'video' | 'novel'
export type ProjectStatus = 'draft' | 'in_progress' | 'completed' | 'archived'

export interface Project {
  id: string
  name: string
  description: string
  projectType: ProjectType
  status: ProjectStatus
  coverUrl?: string
  chapterCount: number
  characterCount: number
  totalWordCount: number
  createTime: string
  updateTime: string
  progress: number
}

export interface ProjectListRequest {
  page?: number
  pageSize?: number
  sortBy?: 'createTime' | 'updateTime' | 'likeCount' | 'favoriteCount' | 'commentCount'
  sortOrder?: 'asc' | 'desc'
  keyword?: string
}

export interface DashboardStats {
  projectCount: number
  chapterCount: number
  characterCount: number
  totalWordCount: number  // 新增
}

// 小说类型
export type NovelType = 'fantasy' | 'urban' | 'scifi' | 'history' | 'military' | 'mystery' | 'romance' | 'gaming'

// 故事基调
export type StoryTone = 'relaxed' | 'serious' | 'suspense' | 'adventure'

// 目标长度
export type TargetLength = 'short' | 'medium' | 'long'

// 创建项目请求
export interface CreateProjectRequest {
  name: string
  description: string
  projectType: 'novel'
  storyTone: StoryTone
  novelType: NovelType
  tags: string
  targetLength: TargetLength
}

// AI生成请求
export interface AIGenerateRequest {
  idea: string
}

// AI生成响应
export interface AIGenerateResponse {
  name: string
  description: string
}

// 分卷状态
export type VolumeStatus = 'planned' | 'in_progress' | 'completed'

// 分卷
export interface Volume {
  id: string
  projectId: string
  outlineId?: string
  volumeNumber?: number
  // 标题相关
  name: string
  title?: string
  volumeTitle?: string
  // 基础信息
  description?: string
  volumeDescription?: string
  estimatedWordCount?: number
  targetChapterCount?: number
  startChapter?: number
  endChapter?: number
  // 核心设定
  volumeTheme?: string
  mainConflict?: string
  plotArc?: string
  coreGoal?: string
  // 关键事件
  keyEvents?: string
  // 高潮与收尾
  climax?: string
  ending?: string
  // 备注
  volumeNotes?: string
  // 时间线
  timelineSetting?: string
  // 人物与伏笔
  newCharacters?: string[]
  stageForeshadowings?: string[]
  // 状态
  status?: VolumeStatus
  volumeCompleted?: boolean
  // 排序
  sortOrder?: number
  chapterCount: number
  createTime?: string
  updateTime?: string
}

// 章节
export interface Chapter {
  id: string
  volumeId?: string
  volumePlanId?: string
  projectId: string
  chapterNumber?: number
  title: string
  chapterTitle?: string
  content: string
  summary?: string
  plotOutline?: string
  keyEvents?: string
  chapterGoal?: string
  wordCount: number
  wordCountTarget?: number
  order: number
  status: 'draft' | 'published'
  newCharacters?: string
  plantingForeshadowings?: string
  chapterNotes?: string
  createTime: string
  updateTime: string
}

// 章节规划（用于编辑器状态管理）
export interface ChapterPlan {
  id: string
  volumeId?: string
  volumePlanId?: string
  projectId: string
  chapterNumber: number
  title: string
  plotOutline?: string
  chapterStartingScene?: string
  chapterEndingScene?: string
  keyEvents?: string
  chapterGoal?: string
  wordCountTarget?: number
  chapterNotes?: string
  status?: string
  plotStage?: string
  plannedCharacters?: string
  characterArcs?: string
  // 扩展字段（来自 ChapterPlanDto）
  volumeTitle?: string
  volumeNumber?: number
  hasContent?: boolean
  chapterId?: string
  wordCount?: number
}

// AI润色请求
export interface PolishRequest {
  style?: 'vivid' | 'fast' | 'literary'
  degree?: 'light' | 'medium' | 'heavy'
  customRequirements?: string
}

// AI润色响应
export interface PolishResponse {
  content: string
  polishReport?: {
    totalOptimizations: number
  }
  polishSummary?: string
}

// AI剧情修复请求
export interface FixRequest {
  fixOptions?: string[]
  customRequirements?: string
}

// AI剧情修复响应
export interface FixResponse {
  content: string
  fixReport?: Array<{
    type: string
    severity: 'high' | 'medium' | 'low'
    original: string
    fixed: string
    reason: string
  }>
  totalFixes?: number
  fixSummary?: string
}

// 角色基础信息
export interface Character {
  id: string
  projectId: string
  name: string
  role: 'protagonist' | 'antagonist' | 'supporting' | 'minor'
  description: string
  avatar?: string
  createTime: string
  updateTime: string
}

// 世界观设定分类
export type WorldSettingCategory = 'geography' | 'history' | 'culture' | 'magic' | 'technology' | 'other'

// 世界观设定
export interface WorldSetting {
  id: string
  projectId: string
  category: WorldSettingCategory
  title: string
  content: string
  createTime: string
  updateTime: string
}

// 项目概览数据
export interface ProjectOverview {
  id: string
  name: string
  description: string
  coverUrl?: string
  status: string
  tags: string[]
  chapterCount: number
  characterCount: number
  totalWordCount: number
  targetChapterCount: number
  progress: number
}
