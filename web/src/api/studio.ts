import http, { type ApiResult } from './http'
import type { RuntimeSchemaVO } from './meta'

export interface AiSessionVO {
  id: number
  title: string
  appId?: number
  status: string
  tokenInput: number
  tokenOutput: number
  createdAt?: string
}

export interface AiMessageVO {
  id: number
  role: string
  content?: string
  toolName?: string
  createdAt?: string
}

export interface StudioSseEvent {
  event: string
  data: Record<string, unknown>
}

export function listSessions() {
  return http.get<ApiResult<AiSessionVO[]>>('/studio/sessions').then((res) => res.data.data)
}

export function createSession() {
  return http.post<ApiResult<number>>('/studio/sessions').then((res) => res.data.data)
}

export function listMessages(sessionId: number) {
  return http.get<ApiResult<AiMessageVO[]>>(`/studio/sessions/${sessionId}/messages`).then((res) => res.data.data)
}

export function getSessionSchema(sessionId: number) {
  return http.get<ApiResult<RuntimeSchemaVO | null>>(`/studio/sessions/${sessionId}/schema`).then((res) => res.data.data)
}

export async function chatStream(
  sessionId: number,
  message: string,
  token: string,
  onEvent: (event: StudioSseEvent) => void,
): Promise<void> {
  const response = await fetch(`/api/studio/sessions/${sessionId}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ message }),
  })
  if (!response.ok || !response.body) {
    const text = await response.text()
    throw new Error(text || '对话失败')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    for (const part of parts) {
      const parsed = parseSse(part)
      if (parsed) {
        onEvent(parsed)
      }
    }
  }
  if (buffer.trim()) {
    const parsed = parseSse(buffer)
    if (parsed) {
      onEvent(parsed)
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
