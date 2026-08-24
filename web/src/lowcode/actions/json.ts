import type { ActionContext } from '../types'

function readJson(text: string): unknown {
  return JSON.parse(text)
}

function writeJson(value: unknown, pretty: boolean): string {
  return pretty ? JSON.stringify(value, null, 2) : JSON.stringify(value)
}

function sortKeysDeep(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(sortKeysDeep)
  }
  if (value && typeof value === 'object') {
    const sorted: Record<string, unknown> = {}
    for (const key of Object.keys(value as Record<string, unknown>).sort()) {
      sorted[key] = sortKeysDeep((value as Record<string, unknown>)[key])
    }
    return sorted
  }
  return value
}

export function jsonFormat(ctx: ActionContext, params: Record<string, unknown>) {
  const from = String(params.from || '')
  const to = String(params.to || '')
  if (!from || !to) {
    return
  }
  try {
    const parsed = readJson(ctx.state[from] || '')
    ctx.state[to] = writeJson(parsed, true)
    ctx.message('格式化成功', 'success')
  } catch {
    ctx.message('JSON 格式无效', 'error')
  }
}

export function jsonMinify(ctx: ActionContext, params: Record<string, unknown>) {
  const from = String(params.from || '')
  const to = String(params.to || '')
  if (!from || !to) {
    return
  }
  try {
    const parsed = readJson(ctx.state[from] || '')
    ctx.state[to] = writeJson(parsed, false)
    ctx.message('压缩成功', 'success')
  } catch {
    ctx.message('JSON 格式无效', 'error')
  }
}

export function jsonSortKeys(ctx: ActionContext, params: Record<string, unknown>) {
  const from = String(params.from || '')
  const to = String(params.to || '')
  if (!from || !to) {
    return
  }
  try {
    const parsed = readJson(ctx.state[from] || '')
    ctx.state[to] = writeJson(sortKeysDeep(parsed), true)
    ctx.message('键已排序', 'success')
  } catch {
    ctx.message('JSON 格式无效', 'error')
  }
}

export function jsonValidate(ctx: ActionContext, params: Record<string, unknown>) {
  const from = String(params.from || '')
  if (!from) {
    return
  }
  try {
    readJson(ctx.state[from] || '')
    ctx.message('JSON 合法', 'success')
  } catch {
    ctx.message('JSON 格式无效', 'error')
  }
}

export function stateCopy(ctx: ActionContext, params: Record<string, unknown>) {
  const from = String(params.from || '')
  const to = String(params.to || from)
  if (!from) {
    return
  }
  ctx.state[to] = ctx.state[from] || ''
  ctx.message('已复制', 'success')
}

export function stateClear(ctx: ActionContext, params: Record<string, unknown>) {
  const target = String(params.to || params.from || '')
  if (!target) {
    return
  }
  ctx.state[target] = ''
  ctx.message('已清空', 'info')
}
