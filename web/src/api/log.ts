import http, { type ApiResult } from './http'
import type { PageResult } from './types'

export interface OperLogVO {
  id: number
  userId?: number
  username?: string
  module: string
  action: string
  bizId?: string
  success: number
  ip?: string
  costMs: number
  detailJson?: string
  createdAt?: string
}

export function pageOperLogs(params: { current: number; size: number; module?: string; username?: string }) {
  return http.get<ApiResult<PageResult<OperLogVO>>>('/system/logs', { params }).then((res) => res.data.data)
}
