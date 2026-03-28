import request from '@/utils/request'

/**
 * 任务步骤DTO
 */
export interface TaskStepDto {
  id: string
  taskId: string
  stepOrder: number
  stepName: string
  stepType: string
  status: string
  progress: number
  result: any
  errorMessage: string
  createTime: string
  updateTime: string
  startedTime: string
  completedTime: string
}

/**
 * 任务DTO
 */
export interface TaskDto {
  id: string
  projectId: string
  taskType: string
  taskName: string
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'
  progress: number
  currentStep: string
  result: any
  errorMessage: string
  createdBy: string
  createTime: string
  updateTime: string
  startedTime: string
  completedTime: string
  steps: TaskStepDto[]
}

/**
 * 获取任务状态
 */
export const getTaskStatus = (taskId: string) => {
  return request.get<TaskDto>(`/api/task/${taskId}/status`)
}

/**
 * 获取项目任务列表
 */
export const getProjectTasks = (projectId: string) => {
  return request.get<TaskDto[]>(`/api/task/project/${projectId}/list`)
}

/**
 * 取消任务
 */
export const cancelTask = (taskId: string) => {
  return request.post(`/api/task/${taskId}/cancel`)
}
