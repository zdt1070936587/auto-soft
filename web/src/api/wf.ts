import http, { type ApiResult } from './http'

export interface WorkflowGraph {
  version: number
  name?: string
  trigger?: {
    type?: string
    input_schema?: Record<string, string>
    app?: string
    entity?: string
    cron?: string
    enabled?: number
  }
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

export interface WorkflowNode {
  id: string
  type: string
  title?: string
  config?: Record<string, unknown>
}

export interface WorkflowEdge {
  from: string
  to: string
  when?: string
}

export interface WorkflowDefinitionVO {
  id: number
  appId: number
  code: string
  name: string
  status: string
  version: number
  grantRoles?: string
  appKind?: string
  published: boolean
  graph: WorkflowGraph
}

export interface WorkflowStepVO {
  id: number
  nodeId: string
  nodeType: string
  status: string
  inputSummary?: string
  outputSummary?: string
  errorMsg?: string
  durationMs?: number
}

export interface WorkflowRunVO {
  id: number
  definitionId: number
  definitionCode?: string
  version: number
  dryRun: boolean
  status: string
  currentNodeId?: string
  errorMsg?: string
  tokenInput?: number
  tokenOutput?: number
  createdAt?: string
  steps: WorkflowStepVO[]
}

export function getWorkflow(id: number) {
  return http.get<ApiResult<WorkflowDefinitionVO>>(`/wf/${id}`).then((res) => res.data.data)
}

export function getWorkflowByApp(appId: number) {
  return http.get<ApiResult<WorkflowDefinitionVO>>('/wf', { params: { appId } }).then((res) => res.data.data)
}

export function saveWorkflowGraph(id: number, graph: WorkflowGraph) {
  return http.put<ApiResult<void>>(`/wf/${id}/graph`, { graph }).then(() => undefined)
}

export function validateWorkflow(id: number) {
  return http.post<ApiResult<void>>(`/wf/${id}/validate`).then(() => undefined)
}

export function dryRunWorkflow(id: number, input?: Record<string, unknown>) {
  return http
    .post<ApiResult<WorkflowRunVO>>(`/wf/${id}/dry-run`, { input }, { timeout: 45000 })
    .then((res) => res.data.data)
}

export function publishWorkflow(id: number, confirm = true) {
  return http.post<ApiResult<void>>(`/wf/${id}/publish`, { confirm }).then(() => undefined)
}

export function getPublishedWorkflow(code: string) {
  return http.get<ApiResult<WorkflowDefinitionVO>>(`/wf/by-code/${code}`).then((res) => res.data.data)
}

export function runWorkflow(code: string, input?: Record<string, unknown>) {
  return http
    .post<ApiResult<WorkflowRunVO>>(`/wf/${code}/run`, { input }, { timeout: 45000 })
    .then((res) => res.data.data)
}

export function getWorkflowRun(runId: number) {
  return http.get<ApiResult<WorkflowRunVO>>(`/wf/runs/${runId}`).then((res) => res.data.data)
}

export function createWorkflowShare(id: number, permission: 'preview' | 'copy', expireDays: number) {
  return http
    .post<ApiResult<{ token: string; permission: string; expireAt?: string }>>(`/wf/${id}/share`, {
      permission,
      expireDays,
    })
    .then((res) => res.data.data)
}

export function getWorkflowShare(token: string) {
  return http.get<ApiResult<WorkflowShareVO>>(`/wf/share/${token}`).then((res) => res.data.data)
}

export function copyWorkflowShare(token: string) {
  return http.post<ApiResult<number>>(`/wf/share/${token}/copy`).then((res) => res.data.data)
}

export function setWorkflowSchedule(id: number, enabled: boolean) {
  return http.put<ApiResult<void>>(`/wf/${id}/schedule`, { enabled }).then(() => undefined)
}

export interface WorkflowShareVO {
  token: string
  permission: string
  expireAt?: string
  name?: string
  code?: string
  graph?: WorkflowGraph
}
