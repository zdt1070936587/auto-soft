import http, { type ApiResult } from './http'

export interface FlowTaskVO {
  taskId: number
  appCode: string
  entityCode: string
  bizId: number
  roleCode?: string
  status?: string
  comment?: string
  createdAt?: string
  startUserId?: number
}

export function listTodo() {
  return http.get<ApiResult<FlowTaskVO[]>>('/flow/todo').then((res) => res.data.data)
}

export function listDone() {
  return http.get<ApiResult<FlowTaskVO[]>>('/flow/done').then((res) => res.data.data)
}

export function completeTask(taskId: number, comment?: string) {
  return http.post<ApiResult<void>>(`/flow/todo/${taskId}/complete`, { comment }).then(() => undefined)
}

export function rejectTask(taskId: number, comment: string) {
  return http.post<ApiResult<void>>(`/flow/todo/${taskId}/reject`, { comment }).then(() => undefined)
}
