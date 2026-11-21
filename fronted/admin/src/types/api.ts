export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface PageQuery {
  page: number
  pageSize: number
}

export interface PageResult<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
  pages: number
}
