export interface ApiResponse<T = any> {
  code: number
  ok: boolean
  msg: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
