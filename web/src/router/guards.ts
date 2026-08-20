import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/token'

const WHITE_LIST = new Set(['/login', '/dev/health'])

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()
    const token = getToken()

    if (WHITE_LIST.has(to.path)) {
      if (to.path === '/login' && token) {
        return '/dashboard'
      }
      return true
    }

    if (!token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (!auth.ready) {
      try {
        await auth.bootstrap()
      } catch {
        auth.logout()
        return '/login'
      }
    }

    if (to.path === '/') {
      return '/dashboard'
    }

    if (to.name === 'not-found' || to.path === '/403') {
      return true
    }

    if (!auth.canAccess(to.path)) {
      return '/403'
    }

    return true
  })
}
