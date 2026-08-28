import type { Router, RouteLocationNormalized } from 'vue-router'
import { postPageVisits, type PageVisitItemPayload } from '@/api/telemetry'
import { getToken } from '@/utils/token'

const FLUSH_INTERVAL_MS = 5000
const MAX_QUEUE = 30
const DEDUPE_MS = 60_000

const SKIP_PATHS = new Set(['/login', '/dev/health', '/403', '/404'])
const SKIP_ROUTE_NAMES = new Set(['login', 'health', 'forbidden', 'not-found'])

let queue: PageVisitItemPayload[] = []
let flushTimer: ReturnType<typeof setInterval> | null = null
const lastSeenAt = new Map<string, number>()

function isEnabled() {
  const flag = import.meta.env.VITE_PAGE_VISIT_ENABLED
  return flag !== 'false' && flag !== '0'
}

function shouldTrack(to: RouteLocationNormalized) {
  if (!isEnabled() || !getToken()) {
    return false
  }
  if (to.path.startsWith('/h5/')) {
    return false
  }
  if (SKIP_PATHS.has(to.path) || (to.name && SKIP_ROUTE_NAMES.has(String(to.name)))) {
    return false
  }
  if (to.meta.trackVisit === false) {
    return false
  }
  return to.path.startsWith('/')
}

function enqueue(to: RouteLocationNormalized) {
  const path = to.path
  const now = Date.now()
  const last = lastSeenAt.get(path)
  if (last != null && now - last < DEDUPE_MS) {
    return
  }
  lastSeenAt.set(path, now)

  queue.push({
    path,
    routeName: to.name ? String(to.name) : undefined,
    pageTitle: to.meta.title ? String(to.meta.title) : undefined,
    visitedAt: new Date(now).toISOString(),
  })

  if (queue.length >= MAX_QUEUE) {
    void flush()
  }
}

async function flush() {
  if (!queue.length || !getToken()) {
    return
  }
  const batch = queue.splice(0, MAX_QUEUE)
  try {
    await postPageVisits({ visits: batch })
  } catch {
    // 丢弃失败批次，避免 retry 风暴
  }
}

export function setupPageVisitTracker(router: Router) {
  if (!isEnabled()) {
    return
  }

  router.afterEach((to) => {
    if (!shouldTrack(to)) {
      return
    }
    enqueue(to)
  })

  if (flushTimer == null) {
    flushTimer = setInterval(() => {
      void flush()
    }, FLUSH_INTERVAL_MS)
  }
}
