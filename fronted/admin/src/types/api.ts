export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  list: T[]
}
