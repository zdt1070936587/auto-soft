import http, { type ApiResult } from './http'
import type { PageResult } from './types'

export interface MetaFieldVO {
  id?: number
  entityId?: number
  code: string
  name: string
  fieldType: string
  length?: number
  nullableFlag?: number
  defaultValue?: string
  optionsJson?: string
  refApp?: string
  refEntity?: string
  sort?: number
  queryable?: number
  listed?: number
  requiredFlag?: number
}

export interface MetaEntityVO {
  id: number
  appId: number
  code: string
  name: string
  remark?: string
  fields: MetaFieldVO[]
}

export interface MetaAppVO {
  id: number
  code: string
  name: string
  status: string
  version: number
  grantRoles: string
  remark?: string
  entities: MetaEntityVO[]
}

export interface RuntimeSchemaVO {
  appCode: string
  appName: string
  entityCode: string
  entityName: string
  published: boolean
  flowBound: boolean
  fields: MetaFieldVO[]
}

export function listMetaApps() {
  return http.get<ApiResult<MetaAppVO[]>>('/meta/apps').then((res) => res.data.data)
}

export function getMetaApp(id: number) {
  return http.get<ApiResult<MetaAppVO>>(`/meta/apps/${id}`).then((res) => res.data.data)
}

export function createMetaApp(payload: { code: string; name: string; grantRoles?: string; remark?: string }) {
  return http.post<ApiResult<number>>('/meta/apps', payload).then((res) => res.data.data)
}

export function updateMetaApp(id: number, payload: { code: string; name: string; grantRoles?: string; remark?: string }) {
  return http.put<ApiResult<void>>(`/meta/apps/${id}`, payload).then(() => undefined)
}

export function createMetaEntity(appId: number, payload: { code: string; name: string; remark?: string }) {
  return http.post<ApiResult<number>>(`/meta/apps/${appId}/entities`, payload).then((res) => res.data.data)
}

export function addMetaField(entityId: number, payload: MetaFieldVO) {
  return http.post<ApiResult<number>>(`/meta/entities/${entityId}/fields`, payload).then((res) => res.data.data)
}

export function updateMetaField(id: number, payload: MetaFieldVO) {
  return http.put<ApiResult<void>>(`/meta/fields/${id}`, payload).then(() => undefined)
}

export function deleteMetaField(id: number) {
  return http.delete<ApiResult<void>>(`/meta/fields/${id}`).then(() => undefined)
}

export function publishMetaApp(id: number, grantRoles?: string) {
  return http.post<ApiResult<void>>(`/meta/apps/${id}/publish`, { grantRoles }).then(() => undefined)
}

export function unpublishMetaApp(id: number) {
  return http.post<ApiResult<void>>(`/meta/apps/${id}/unpublish`).then(() => undefined)
}

export function getRuntimeSchema(app: string, entity: string, preview = false) {
  return http
    .get<ApiResult<RuntimeSchemaVO>>(`/runtime/${app}/${entity}/schema`, { params: { preview } })
    .then((res) => res.data.data)
}

export function pageRuntime(app: string, entity: string, params: Record<string, unknown>, preview = false) {
  return http
    .get<ApiResult<PageResult<Record<string, unknown>>>>(`/runtime/${app}/${entity}/page`, {
      params: { ...params, preview },
    })
    .then((res) => res.data.data)
}

export function createRuntime(app: string, entity: string, body: Record<string, unknown>) {
  return http.post<ApiResult<number>>(`/runtime/${app}/${entity}`, body).then((res) => res.data.data)
}

export function updateRuntime(app: string, entity: string, id: number, body: Record<string, unknown>) {
  return http.put<ApiResult<void>>(`/runtime/${app}/${entity}/${id}`, body).then(() => undefined)
}

export function deleteRuntime(app: string, entity: string, id: number) {
  return http.delete<ApiResult<void>>(`/runtime/${app}/${entity}/${id}`).then(() => undefined)
}

export function submitRuntime(app: string, entity: string, id: number) {
  return http.post<ApiResult<void>>(`/runtime/${app}/${entity}/${id}/submit`).then(() => undefined)
}
