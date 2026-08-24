export interface LowCodeBlock {
  type: string
  id?: string
  label?: string
  bind?: string
  rows?: number
  readonly?: boolean
  placeholder?: string
  action?: string
  from?: string
  to?: string
  items?: LowCodeToolbarItem[]
  title?: string
  blocks?: LowCodeBlock[]
}

export interface LowCodeToolbarItem {
  label: string
  action: string
  from?: string
  to?: string
}

export interface LowCodeSchema {
  version: number
  title?: string
  layout?: string
  state?: Record<string, string>
  blocks: LowCodeBlock[]
}

export interface ActionContext {
  state: Record<string, string>
  message: (text: string, type?: 'success' | 'error' | 'info') => void
}

export type LowCodeAction = (ctx: ActionContext, params: Record<string, unknown>) => void
