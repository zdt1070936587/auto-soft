import type { LowCodeAction } from '../types'
import {
  jsonFormat,
  jsonMinify,
  jsonSortKeys,
  jsonValidate,
  stateClear,
  stateCopy,
} from './json'

const registry: Record<string, LowCodeAction> = {
  'json.format': jsonFormat,
  'json.minify': jsonMinify,
  'json.sortKeys': jsonSortKeys,
  'json.validate': jsonValidate,
  'state.copy': stateCopy,
  'state.clear': stateClear,
}

export function runLowCodeAction(action: string, ctx: Parameters<LowCodeAction>[0], params: Record<string, unknown>) {
  const handler = registry[action]
  if (!handler) {
    ctx.message(`未知动作: ${action}`, 'error')
    return
  }
  handler(ctx, params)
}

export { registry as lowCodeActions }
