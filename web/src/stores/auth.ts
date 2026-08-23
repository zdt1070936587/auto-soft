import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, login as loginApi } from '@/api/auth'
import type { MenuVO, UserVO } from '@/api/types'
import { clearToken, getToken, setToken } from '@/utils/token'

function flattenMenuPaths(menus: MenuVO[]): string[] {
  const paths: string[] = []
  for (const menu of menus) {
    if (menu.menuType === 'MENU' && menu.path) {
      paths.push(menu.path)
    }
    if (menu.children?.length) {
      paths.push(...flattenMenuPaths(menu.children))
    }
  }
  return paths
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref<UserVO | null>(null)
  const menus = ref<MenuVO[]>([])
  const permissions = ref<string[]>([])
  const ready = ref(false)

  const loggedIn = computed(() => Boolean(token.value))
  const roleNames = computed(() => (user.value?.roles || []).map((role) => role.name).join('、'))
  const isSuperAdmin = computed(() => (user.value?.roles || []).some((role) => role.code === 'SUPER_ADMIN'))
  const accessiblePaths = computed(() => flattenMenuPaths(menus.value))

  function hasPermission(code: string) {
    if (isSuperAdmin.value) {
      return true
    }
    return permissions.value.includes(code)
  }

  function canAccess(path: string) {
    if (path === '/dashboard' || path === '/dev/health' || path === '/403' || path === '/404') {
      return true
    }
    return accessiblePaths.value.includes(path)
  }

  async function bootstrap() {
    if (!getToken()) {
      ready.value = true
      return
    }
    const me = await getCurrentUser()
    user.value = me.user
    menus.value = me.menus
    permissions.value = me.permissions
    token.value = getToken()
    ready.value = true
  }

  function collectPermissions(menuList: MenuVO[]): string[] {
    const codes: string[] = []
    for (const menu of menuList) {
      if (menu.permission) {
        codes.push(menu.permission)
      }
      if (menu.children?.length) {
        codes.push(...collectPermissions(menu.children))
      }
    }
    return codes
  }

  async function login(username: string, password: string, _remember: boolean) {
    const data = await loginApi(username, password)
    if (!data?.token) {
      throw new Error('登录响应无效')
    }
    setToken(data.token)
    token.value = data.token
    user.value = data.user
    menus.value = data.menus || []
    permissions.value = collectPermissions(menus.value)
    ready.value = true
    try {
      const me = await getCurrentUser()
      user.value = me.user
      menus.value = me.menus
      permissions.value = me.permissions
    } catch {
      // /me 失败时拦截器可能清掉 token，这里恢复登录态，避免卡在登录页
      setToken(data.token)
      token.value = data.token
      user.value = data.user
      menus.value = data.menus || []
      permissions.value = collectPermissions(menus.value)
      ready.value = true
    }
  }

  function logout() {
    clearToken()
    token.value = ''
    user.value = null
    menus.value = []
    permissions.value = []
    ready.value = true
  }

  return {
    token,
    user,
    menus,
    permissions,
    ready,
    loggedIn,
    roleNames,
    isSuperAdmin,
    hasPermission,
    canAccess,
    bootstrap,
    login,
    logout,
  }
})
