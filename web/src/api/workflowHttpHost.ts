import http, { type ApiResult } from './http'

export interface WorkflowHttpHostVO {
  id: number
  host: string
  remark?: string
  createdAt?: string
}

export function listWorkflowHttpHosts() {
  return http.get<ApiResult<WorkflowHttpHostVO[]>>('/system/workflow-http-hosts').then((res) => res.data.data)
}

export function createWorkflowHttpHost(payload: { host: string; remark?: string }) {
  return http.post<ApiResult<number>>('/system/workflow-http-hosts', payload).then((res) => res.data.data)
}

export function deleteWorkflowHttpHost(id: number) {
  return http.delete<ApiResult<void>>(`/system/workflow-http-hosts/${id}`).then(() => undefined)
}
