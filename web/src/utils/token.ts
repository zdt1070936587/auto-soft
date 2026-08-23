/**
 * 登录 token 仅存 sessionStorage，按浏览器标签页隔离。
 * 同一浏览器可开多个标签页分别登录不同账号；新标签页默认未登录。
 * 「记住账号」仅保存用户名（见 LoginView），不共享登录态。
 */
const TOKEN_KEY = 'autosoft.token'

export function getToken(): string {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  clearToken()
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
  // 清理旧版写入 localStorage 的 token，避免多标签串号
  localStorage.removeItem(TOKEN_KEY)
}

/** 应用启动时调用，迁移并清除跨标签共享的旧 token */
export function migrateLegacyToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
