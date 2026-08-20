import http, { type ApiResult } from './http'
import type { MenuVO } from './types'

export function listMyMenus() {
  return http.get<ApiResult<MenuVO[]>>('/menus/mine').then((res) => res.data.data)
}

export function listMenuTree() {
  return http.get<ApiResult<MenuVO[]>>('/menus/tree').then((res) => res.data.data)
}
