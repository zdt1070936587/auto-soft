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

  async function login(username: string, password: string, remember: boolean) {
    const data = await loginApi(username, password)
    setToken(data.token, remember)
    token.value = data.token
    user.value = data.user
    menus.value = data.menus
    const me = await getCurrentUser()
    user.value = me.user
    menus.value = me.menus
    permissions.value = me.permissions
    ready.value = true
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
