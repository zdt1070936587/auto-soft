import type { ActionContext } from '../types'

export function submitAction(ctx: ActionContext, _params: Record<string, unknown>) {
  if (ctx.preview) {
    ctx.message('预览模式下无法提交，请发布后在菜单中打开使用', 'info')
    return
  }
  ctx.message('当前表单页暂不支持直接提交，请使用对应 CRUD 菜单', 'info')
}
