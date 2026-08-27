<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AppstoreOutlined,
  ArrowRightOutlined,
  DatabaseOutlined,
  RocketOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import { Button, Empty, Spin } from 'ant-design-vue'
import PageShell from '@/components/layout/PageShell.vue'
import { getHealth, type HealthVO } from '@/api/health'
import type { MenuVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const health = ref<HealthVO | null>(null)

const appMenus = computed(() =>
  auth.menus.filter((menu) => menu.menuType !== 'BUTTON' && (menu.path || '').startsWith('/app/')),
)

const entityCount = computed(() =>
  appMenus.value.reduce((sum, menu) => sum + (menu.children?.filter((c) => c.menuType === 'MENU').length || 0), 0),
)

async function loadHealth() {
  loading.value = true
  try {
    health.value = await getHealth()
  } finally {
    loading.value = false
  }
}

function openMenu(menu: MenuVO) {
  const first = menu.children?.find((item) => item.path)
  if (first?.path) {
    void router.push(first.path)
    return
  }
  if (menu.path) {
    void router.push(menu.path)
  }
}

function openEntity(path?: string) {
  if (path) {
    void router.push(path)
  }
}

onMounted(() => {
  void loadHealth()
})
</script>

<template>
  <PageShell
    :title="`欢迎回来，${auth.user?.nickname || '用户'}`"
    :subtitle="`当前角色：${auth.roleNames || '未分配'} · 从这里进入已授权的应用与系统能力`"
  >
    <section class="hero-panel">
      <div class="hero-copy">
        <p class="hero-kicker"><ThunderboltOutlined /> AI 原生管理后台</p>
        <div class="hero-headline">
          <h2>把业务送上可扩展轨道</h2>
          <p>建模、发布、运行与智能体开发，统一在一个控制台完成。</p>
        </div>
        <div class="hero-actions">
          <Button type="primary" @click="router.push('/studio')">
            <RocketOutlined />
            打开工作室
          </Button>
          <Button v-if="auth.hasPermission('meta:app:manage')" @click="router.push('/meta/apps')">
            应用建模
          </Button>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat-card">
          <span class="stat-label">已发布应用</span>
          <strong class="stat-value">{{ appMenus.length }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">可访问实体</span>
          <strong class="stat-value">{{ entityCount }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">数据库</span>
          <strong class="stat-value" :class="{ up: health?.db === 'UP', down: health?.db && health.db !== 'UP' }">
            {{ health?.db || '—' }}
          </strong>
        </div>
      </div>
    </section>

    <section class="page-panel">
      <div class="section-head">
        <div>
          <h3 class="page-panel__title">已发布应用</h3>
          <p class="section-desc">点击卡片进入对应业务模块</p>
        </div>
      </div>

      <Empty
        v-if="!appMenus.length"
        description="还没有已发布且授权给你的应用。开发者可在工作室或应用建模中发布。"
      />

      <div v-else class="app-grid">
        <article v-for="app in appMenus" :key="app.id" class="app-card">
          <div class="app-card__head">
            <span class="app-icon"><AppstoreOutlined /></span>
            <div>
              <h4>{{ app.name }}</h4>
              <p>{{ app.path }}</p>
            </div>
          </div>
          <div class="app-card__links">
            <button
              v-for="child in app.children?.filter((item) => item.menuType === 'MENU')"
              :key="child.id"
              type="button"
              class="entity-link"
              @click="openEntity(child.path)"
            >
              <span>{{ child.name }}</span>
              <ArrowRightOutlined />
            </button>
            <Button
              v-if="!app.children?.length"
              type="link"
              class="open-app"
              @click="openMenu(app)"
            >
              打开应用
              <ArrowRightOutlined />
            </Button>
          </div>
        </article>
      </div>
    </section>

    <section class="page-panel status-panel">
      <div class="section-head">
        <div>
          <h3 class="page-panel__title">系统状态</h3>
          <p class="section-desc">运行时健康检查</p>
        </div>
        <Button type="link" :loading="loading" @click="loadHealth">刷新</Button>
      </div>

      <Spin :spinning="loading">
        <div v-if="health" class="status-grid">
          <div class="status-item">
            <DatabaseOutlined class="status-icon" />
            <div>
              <span class="status-label">应用</span>
              <strong>{{ health.appName }}</strong>
            </div>
          </div>
          <div class="status-item">
            <ThunderboltOutlined class="status-icon" />
            <div>
              <span class="status-label">Profile</span>
              <strong>{{ health.profile }}</strong>
            </div>
          </div>
          <div class="status-item">
            <span class="status-dot" :class="health.db === 'UP' ? 'is-up' : 'is-down'" />
            <div>
              <span class="status-label">数据库</span>
              <strong>{{ health.db }}</strong>
            </div>
          </div>
          <div class="status-item">
            <RocketOutlined class="status-icon" />
            <div>
              <span class="status-label">服务器时间</span>
              <strong class="mono">{{ health.now }}</strong>
            </div>
          </div>
        </div>
      </Spin>
    </section>
  </PageShell>
</template>

<style scoped>
.hero-panel {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background:
    radial-gradient(circle at 85% 20%, rgba(91, 140, 255, 0.14), transparent 42%),
    radial-gradient(circle at 10% 80%, rgba(34, 211, 238, 0.08), transparent 38%),
    var(--bg-surface);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 24px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: rgba(91, 140, 255, 0.1);
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.hero-headline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-headline h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 700;
  line-height: 1.25;
}

.hero-headline p {
  margin: 0;
  max-width: 520px;
  color: var(--text-2);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-stats {
  display: grid;
  gap: 12px;
  align-content: center;
}

.stat-card {
  padding: 16px 18px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--bg-elevated);
}

.stat-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-3);
  letter-spacing: 0.04em;
}

.stat-value {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
}

.stat-value.up {
  color: #34d399;
}

.stat-value.down {
  color: #f87171;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-3);
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.app-card {
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--bg-elevated);
  transition: border-color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.app-card:hover {
  border-color: rgba(91, 140, 255, 0.35);
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

.app-card__head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.app-icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(91, 140, 255, 0.14);
  color: var(--primary);
  font-size: 18px;
}

.app-card__head h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.app-card__head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-3);
  font-family: ui-monospace, monospace;
}

.app-card__links {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.entity-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.02);
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease, border-color 150ms ease;
}

.entity-link:hover {
  border-color: var(--border);
  background: var(--bg-hover);
  color: var(--text-1);
}

.open-app {
  padding-left: 0;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--bg-elevated);
}

.status-icon {
  font-size: 18px;
  color: var(--primary);
}

.status-label {
  display: block;
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 2px;
}

.status-item strong {
  font-size: 14px;
  color: var(--text-1);
}

.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px !important;
  word-break: break-all;
}

@media (max-width: 960px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }
}
</style>
