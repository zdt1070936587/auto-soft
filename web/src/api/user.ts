import http, { type ApiResult } from './http'
import type { PageResult, UserVO } from './types'

export interface UserQuery {
  current?: number
  size?: number
  username?: string
  status?: number
}

export function pageUsers(query: UserQuery) {
  return http.get<ApiResult<PageResult<UserVO>>>('/users', { params: query }).then((res) => res.data.data)
}

export function createUser(payload: {
  username: string
  password: string
  nickname: string
  status?: number
  roleIds: number[]
}) {
  return http.post<ApiResult<number>>('/users', payload).then((res) => res.data.data)
}

export function updateUser(id: number, payload: { nickname: string }) {
  return http.put<ApiResult<void>>(`/users/${id}`, payload).then(() => undefined)
}

export function resetUserPassword(id: number, newPassword: string) {
  return http.put<ApiResult<void>>(`/users/${id}/password`, { newPassword }).then(() => undefined)
}

export function updateUserStatus(id: number, status: number) {
  return http.put<ApiResult<void>>(`/users/${id}/status`, { status }).then(() => undefined)
}

export function assignUserRoles(id: number, roleIds: number[]) {
  return http.put<ApiResult<void>>(`/users/${id}/roles`, { roleIds }).then(() => undefined)
}

export function deleteUser(id: number) {
  return http.delete<ApiResult<void>>(`/users/${id}`).then(() => undefined)
}
