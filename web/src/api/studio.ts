import http, { type ApiResult } from './http'
import type { PageViewVO } from './meta'

export type AgentMode = 'discuss' | 'plan' | 'develop'

export interface AiSessionVO {
  id: number
  title: string
  appId?: number
  status: string
  tokenInput: number
  tokenOutput: number
  agentMode?: AgentMode
  createdAt?: string
}

export interface AiAttachmentVO {
  id: number
  fileName: string
  contentType: string
  sizeBytes: number
  kind: 'text' | 'image'
}

export interface AiMessageVO {
  id: number
  role: string
  content?: string
  toolName?: string
  createdAt?: string
  attachments?: AiAttachmentVO[]
}

export interface StudioSseEvent {
  event: string
  data: Record<string, unknown>
}

export interface ChatPayload {
  message: string
  agentMode?: AgentMode
  attachmentIds?: number[]
}

export interface PendingAttachment {
  id: number
  fileName: string
  kind: 'text' | 'image'
}

/** ask_user 事件：{ question: string } */
export function isAskUserEvent(event: StudioSseEvent): boolean {
  return event.event === 'ask_user'
}

export function extractAskUserQuestion(data: Record<string, unknown>): string {
  const question = data.question
  if (typeof question === 'string' && question.trim()) {
    return question
  }
  return '请确认以上方案，或提出修改意见。'
}

export function listSessions() {
  return http.get<ApiResult<AiSessionVO[]>>('/studio/sessions').then((res) => res.data.data)
}

export function createSession() {
  return http.post<ApiResult<number>>('/studio/sessions').then((res) => res.data.data)
}

export function deleteSession(sessionId: number) {
  return http.post<ApiResult<void>>(`/studio/sessions/${sessionId}/delete`).then(() => undefined)
}

export function listMessages(sessionId: number) {
  return http.get<ApiResult<AiMessageVO[]>>(`/studio/sessions/${sessionId}/messages`).then((res) => res.data.data)
}

export function getSessionSchema(sessionId: number) {
  return http.get<ApiResult<PageViewVO | null>>(`/studio/sessions/${sessionId}/schema`).then((res) => res.data.data)
}

export function updateSessionMode(sessionId: number, agentMode: AgentMode) {
  return http.patch<ApiResult<void>>(`/studio/sessions/${sessionId}/mode`, { agentMode }).then((res) => res.data.data)
}

export function pauseSession(sessionId: number) {
  return http.post<ApiResult<void>>(`/studio/sessions/${sessionId}/pause`).then((res) => res.data.data)
}

export function uploadAttachment(sessionId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return http
    .post<ApiResult<AiAttachmentVO>>(`/studio/sessions/${sessionId}/attachments`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data.data)
}

export async function chatStream(
  sessionId: number,
  payload: ChatPayload,
  token: string,
  onEvent: (event: StudioSseEvent) => void,
): Promise<void> {
  const controller = new AbortController()
  const response = await fetch(`/api/studio/sessions/${sessionId}/chat`, {
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

  const terminalEvents = new Set(['done', 'error', 'ask_user', 'paused'])

  const dispatch = (parsed: StudioSseEvent | null) => {
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

function parseSse(block: string): StudioSseEvent | null {
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
