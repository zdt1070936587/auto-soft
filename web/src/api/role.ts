import http, { type ApiResult } from './http'
import type { PageResult, RoleVO } from './types'

export interface RoleQuery {
  current?: number
  size?: number
  name?: string
}

export function pageRoles(query: RoleQuery) {
  return http.get<ApiResult<PageResult<RoleVO>>>('/roles', { params: query }).then((res) => res.data.data)
}

export function listRoleOptions() {
  return http.get<ApiResult<RoleVO[]>>('/roles/options').then((res) => res.data.data)
}

export function createRole(payload: {
  code: string
  name: string
  remark?: string
  sort?: number
  status?: number
}) {
  return http.post<ApiResult<number>>('/roles', payload).then((res) => res.data.data)
}

export function updateRole(id: number, payload: { name: string; remark?: string; sort?: number; status?: number }) {
  return http.put<ApiResult<void>>(`/roles/${id}`, payload).then(() => undefined)
}

export function deleteRole(id: number) {
  return http.delete<ApiResult<void>>(`/roles/${id}`).then(() => undefined)
}

export function listRoleMenus(id: number) {
  return http.get<ApiResult<number[]>>(`/roles/${id}/menus`).then((res) => res.data.data)
}

export function grantRoleMenus(id: number, menuIds: number[]) {
  return http.put<ApiResult<void>>(`/roles/${id}/menus`, { menuIds }).then(() => undefined)
}
