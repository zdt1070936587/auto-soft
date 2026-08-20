import http, { type ApiResult } from './http'

export interface HealthVO {
  appName: string
  profile: string
  db: string
  now: string
}

export function getHealth() {
  return http.get<ApiResult<HealthVO>>('/health').then((res) => res.data.data)
}
