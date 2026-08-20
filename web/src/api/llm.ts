import http, { type ApiResult } from './http'

export interface LlmConfigVO {
  defaultModel: string
  allowedModelsJson?: string
  keyConfigured: boolean
  keyMask?: string
}

export interface LlmModelVO {
  id: string
}

export function getLlmConfig() {
  return http.get<ApiResult<LlmConfigVO>>('/system/llm-config').then((res) => res.data.data)
}

export function saveLlmConfig(payload: { apiKey?: string; defaultModel?: string; allowedModelsJson?: string }) {
  return http.put<ApiResult<void>>('/system/llm-config', payload).then(() => undefined)
}

export function listLlmModels() {
  return http.get<ApiResult<LlmModelVO[]>>('/system/llm-models', { timeout: 30000 }).then((res) => res.data.data)
}
