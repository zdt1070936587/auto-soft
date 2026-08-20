import http, { type ApiResult } from './http'
import type { CurrentUserVO, LoginVO } from './types'

export function login(username: string, password: string) {
  return http
    .post<ApiResult<LoginVO>>('/auth/login', { username, password })
    .then((res) => res.data.data)
}

export function getCurrentUser() {
  return http.get<ApiResult<CurrentUserVO>>('/auth/me').then((res) => res.data.data)
}

export function updatePassword(oldPassword: string, newPassword: string) {
  return http.put<ApiResult<void>>('/auth/password', { oldPassword, newPassword }).then(() => undefined)
}
