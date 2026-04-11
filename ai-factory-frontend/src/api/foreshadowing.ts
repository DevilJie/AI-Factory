import request from '@/utils/request'
import type { Foreshadowing, ForeshadowingCreateRequest, ForeshadowingUpdateRequest, ForeshadowingQueryParams } from '@/types/foreshadowing'

/**
 * 获取伏笔列表
 * GET /api/novel/{projectId}/foreshadowings
 */
export const getForeshadowingList = async (
  projectId: string | number,
  params?: ForeshadowingQueryParams
): Promise<Foreshadowing[]> => {
  return request.get<Foreshadowing[]>(
    `/api/novel/${projectId}/foreshadowings`,
    { params }
  )
}

/**
 * 创建伏笔
 * POST /api/novel/{projectId}/foreshadowings
 */
export const createForeshadowing = async (
  projectId: string | number,
  data: ForeshadowingCreateRequest
): Promise<number> => {
  return request.post<number>(
    `/api/novel/${projectId}/foreshadowings`,
    data
  )
}

/**
 * 更新伏笔
 * PUT /api/novel/{projectId}/foreshadowings/{foreshadowingId}
 */
export const updateForeshadowing = async (
  projectId: string | number,
  foreshadowingId: number,
  data: ForeshadowingUpdateRequest
): Promise<void> => {
  return request.put(
    `/api/novel/${projectId}/foreshadowings/${foreshadowingId}`,
    data
  )
}

/**
 * 删除伏笔
 * DELETE /api/novel/{projectId}/foreshadowings/{foreshadowingId}
 */
export const deleteForeshadowing = async (
  projectId: string | number,
  foreshadowingId: number
): Promise<void> => {
  return request.delete(
    `/api/novel/${projectId}/foreshadowings/${foreshadowingId}`
  )
}
