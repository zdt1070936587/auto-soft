import http, { type ApiResult } from './http'

export interface PageVisitItemPayload {
  path: string
  routeName?: string
  pageTitle?: string
  visitedAt?: string
}

export interface PageVisitBatchPayload {
  visits: PageVisitItemPayload[]
}

export function postPageVisits(payload: PageVisitBatchPayload) {
  return http
    .post<ApiResult<{ inserted: number }>>('/telemetry/page-visits', payload)
    .then((res) => res.data.data)
}
