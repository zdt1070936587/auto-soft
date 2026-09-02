import http, { type ApiResult } from './http'

export interface AiAssistantSessionVO {
  id: number
  title: string
  status: string
  tokenInput: number
  tokenOutput: number
  createdAt?: string
}

export interface AiAssistantMessageVO {
  id: number
  role: string
  content?: string
  payloadJson?: string
  toolName?: string
  createdAt?: string
}

export interface AssistantSseEvent {
  event: string
  data: Record<string, unknown>
}

export interface NavLinkItem {
  name: string
  path: string
  permission?: string
  parentName?: string
}

export interface OperLogItem {
  id: number
  module: string
  action: string
  bizId?: string
  success?: number
  createdAt?: string
}

export interface NavLinkPayload {
  type: 'nav_link'
  items: NavLinkItem[]
}

export interface OperTimelinePayload {
  type: 'oper_timeline'
  items: OperLogItem[]
  label?: string
}

export interface ActionPlanFieldItem {
  label: string
  display: string
}

export interface ActionPlanPayload {
  type: 'action_plan'
  draftId: string
  capabilityId: string
  label?: string
  targetPath: string
  targetType: 'system_modal' | 'runtime_form'
  modalKey?: string
  summary?: string
  fields?: ActionPlanFieldItem[]
  missing?: string[]
  unknown?: string[]
  canConfirm?: boolean
  values?: Record<string, unknown>
  displayValues?: Record<string, unknown>
}

export interface ActionDraftVO {
  draftId: string
  capabilityId: string
  status: string
  label?: string
  targetPath: string
  targetType: 'system_modal' | 'runtime_form'
  modalKey?: string
  values?: Record<string, unknown>
  displayValues?: Record<string, unknown>
  missing?: string[]
  unknown?: string[]
}

export type AssistantStructuredPayload = NavLinkPayload | OperTimelinePayload | ActionPlanPayload

export interface ChatPayload {
  message: string
}

export interface AiMemoryFactVO {
  id: number
  category: string
  factKey: string
  factValue: string
  confidence?: number
  confirmed?: number
  lastUsedAt?: string
  updatedAt?: string
}

export interface AiMemoryEpisodeVO {
  id: number
  contentSummary: string
  occurredAt?: string
  importance?: number
  score?: number
}

export function listSessions() {
  return http.get<ApiResult<AiAssistantSessionVO[]>>('/assistant/sessions').then((res) => res.data.data)
}

export function createSession() {
  return http.post<ApiResult<number>>('/assistant/sessions').then((res) => res.data.data)
}

export function deleteSession(sessionId: number) {
  return http.delete<ApiResult<void>>(`/assistant/sessions/${sessionId}`).then(() => undefined)
}

export function listMessages(sessionId: number) {
  return http
    .get<ApiResult<AiAssistantMessageVO[]>>(`/assistant/sessions/${sessionId}/messages`)
    .then((res) => res.data.data)
}

export function listMemoryFacts() {
  return http.get<ApiResult<AiMemoryFactVO[]>>('/assistant/memory/facts').then((res) => res.data.data)
}

export function deleteMemoryFact(factId: number) {
  return http.delete<ApiResult<void>>(`/assistant/memory/facts/${factId}`).then(() => undefined)
}

export function confirmMemoryFact(factId: number) {
  return http.post<ApiResult<void>>(`/assistant/memory/facts/${factId}/confirm`).then(() => undefined)
}

export function listMemoryEpisodes(limit = 20) {
  return http
    .get<ApiResult<AiMemoryEpisodeVO[]>>('/assistant/memory/episodes', { params: { limit } })
    .then((res) => res.data.data)
}

export function getActionDraft(draftId: string) {
  return http.get<ApiResult<ActionDraftVO>>(`/assistant/action-drafts/${draftId}`).then((res) => res.data.data)
}

export function consumeActionDraft(draftId: string) {
  return http.post<ApiResult<{ consumed: boolean }>>(`/assistant/action-drafts/${draftId}/consume`).then(() => undefined)
}

export function cancelActionDraft(draftId: string) {
  return http.post<ApiResult<{ cancelled: boolean }>>(`/assistant/action-drafts/${draftId}/cancel`).then(() => undefined)
}

export async function chatStream(
  sessionId: number,
  payload: ChatPayload,
  token: string,
  onEvent: (event: AssistantSseEvent) => void,
): Promise<void> {
  const controller = new AbortController()
  const response = await fetch(`/api/assistant/sessions/${sessionId}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
    signal: controller.signal,
  })
  if (!response.ok || !response.body) {
    const text = await response.text()
    throw new Error(text || '对话失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let terminal = false
  const terminalEvents = new Set(['done', 'error'])

  const dispatch = (parsed: AssistantSseEvent | null) => {
    if (!parsed) {
      return
    }
    onEvent(parsed)
    if (terminalEvents.has(parsed.event)) {
      terminal = true
      controller.abort()
    }
  }

  try {
    while (!terminal) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        dispatch(parseSse(part))
        if (terminal) {
          break
        }
      }
    }
    if (!terminal && buffer.trim()) {
      dispatch(parseSse(buffer))
    }
  } catch (error) {
    if (terminal) {
      return
    }
    if (error instanceof DOMException && error.name === 'AbortError') {
      return
    }
    throw error
  } finally {
    try {
      await reader.cancel()
    } catch {
      // ignore
    }
  }
}

function parseSse(block: string): AssistantSseEvent | null {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }
  if (!dataLines.length) {
    return null
  }
  const raw = dataLines.join('\n')
  try {
    return { event, data: JSON.parse(raw) as Record<string, unknown> }
  } catch {
    return { event, data: { content: raw } }
  }
}

export function parsePayloadJson(payloadJson?: string): AssistantStructuredPayload | null {
  if (!payloadJson) {
    return null
  }
  try {
    return JSON.parse(payloadJson) as AssistantStructuredPayload
  } catch {
    return null
  }
}
