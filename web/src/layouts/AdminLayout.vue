<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AppstoreOutlined,
  DashboardOutlined,
  DownOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { Breadcrumb, Dropdown, Form, Input, Layout, Menu, Modal, message } from 'ant-design-vue'
import AppLogo from '@/components/brand/AppLogo.vue'
import { updatePassword } from '@/api/auth'
import type { MenuVO } from '@/api/types'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()
const passwordOpen = ref(false)
const saving = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
})

const collapsed = computed(() => app.sidebarCollapsed)
const openMenuKeys = ref<string[]>([])

interface SideItem {
  key: string
  label: string
  icon?: () => ReturnType<typeof DashboardOutlined>
  children?: SideItem[]
}

function menuIcon(path?: string) {
  if (!path) {
    return SettingOutlined
  }
  if (path === '/dashboard') {
    return DashboardOutlined
  }
  if (path.startsWith('/app/')) {
    return AppstoreOutlined
  }
  if (path.startsWith('/page/')) {
    return AppstoreOutlined
  }
  if (path.startsWith('/system') || path.startsWith('/meta') || path.startsWith('/flow')) {
    return SettingOutlined
  }
  return DashboardOutlined
}

function toItems(menus: MenuVO[]): SideItem[] {
  return menus
    .filter((menu) => menu.menuType !== 'BUTTON')
    .map((menu) => {
      const children = menu.children?.length ? toItems(menu.children) : undefined
      const Icon = menuIcon(menu.path)
      return {
        key: menu.path || String(menu.id),
        label: menu.name,
        icon: () => h(Icon),
        children: children && children.length ? children : undefined,
      }
    })
}

const menuItems = computed(() => toItems(auth.menus))
const selectedKeys = computed(() => [route.path])

function resolveOpenKeys(path: string) {
  if (path.startsWith('/system')) {
    return ['/system']
  }
  if (path.startsWith('/flow')) {
    return ['/flow']
  }
  if (path.startsWith('/app/')) {
    const parts = path.split('/')
    return parts.length >= 3 ? [`/app/${parts[2]}`] : []
  }
  if (path.startsWith('/page/')) {
    const parts = path.split('/')
    return parts.length >= 3 ? [`/page/${parts[2]}`] : []
  }
  return []
}

openMenuKeys.value = resolveOpenKeys(route.path)

const breadcrumbs = computed(() => {
  const items: Array<{ title: string; path?: string }> = [{ title: '工作台', path: '/dashboard' }]
  const title = String(route.meta.title || '')
  if (route.path.startsWith('/app/')) {
    const parts = route.path.split('/')
    if (parts[2]) {
      items.push({ title: parts[2], path: `/app/${parts[2]}` })
    }
    if (parts[3]) {
      items.push({ title: title || parts[3] })
    }
    return items
  }
  if (route.path.startsWith('/page/')) {
    const parts = route.path.split('/')
    if (parts[2]) {
      items.push({ title: parts[2], path: `/page/${parts[2]}` })
    }
    if (parts[3]) {
      items.push({ title: title || parts[3] })
    }
    return items
  }
  if (route.path !== '/dashboard' && title) {
    items.push({ title })
  }
  return items
})

function onMenuClick(info: { key: string | number }) {
  const path = String(info.key)
  if (path.startsWith('/')) {
    void router.push(path)
  }
}

function onOpenChange(keys: string[]) {
  openMenuKeys.value = keys
}

function toggleSidebar() {
  app.toggleSidebar()
}

function logout() {
  auth.logout()
  void router.replace('/login')
}

async function submitPassword() {
  saving.value = true
  try {
    await updatePassword(passwordForm.oldPassword, passwordForm.newPassword)
    message.success('密码已更新，请重新登录')
    passwordOpen.value = false
    logout()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Layout class="admin-root">
    <Layout.Sider
      :collapsed="app.sidebarCollapsed"
      class="sider"
      :width="232"
      :collapsed-width="72"
      :trigger="null"
      collapsible
      @update:collapsed="app.setSidebarCollapsed"
    >
      <div class="logo-bar" :class="{ collapsed }">
        <RouterLink to="/dashboard" class="logo-link">
          <AppLogo size="md" theme="dark" :collapsed="collapsed" />
        </RouterLink>
        <button
          v-if="!collapsed"
          class="collapse-btn"
          type="button"
          title="收起菜单"
          @click="toggleSidebar"
        >
          <MenuFoldOutlined />
        </button>
      </div>
      <Menu
        theme="dark"
        mode="inline"
        class="side-menu"
        :inline-collapsed="collapsed"
        :selected-keys="selectedKeys"
        :open-keys="collapsed ? [] : openMenuKeys"
        :items="menuItems"
        @click="onMenuClick"
        @open-change="onOpenChange"
      />
      <div v-if="!collapsed" class="sider-foot">
        <span class="sider-foot__label">AI 管理后台</span>
        <span class="sider-foot__ver">v0.1</span>
      </div>
    </Layout.Sider>
    <Layout class="main-layout">
      <Layout.Header class="header">
        <div class="header-left">
          <button
            v-if="collapsed"
            class="header-collapse-btn"
            type="button"
            title="展开菜单"
            @click="toggleSidebar"
          >
            <MenuUnfoldOutlined />
          </button>
          <Breadcrumb class="breadcrumb">
            <Breadcrumb.Item v-for="(item, index) in breadcrumbs" :key="`${item.title}-${index}`">
              <a v-if="item.path && index < breadcrumbs.length - 1" @click.prevent="router.push(item.path)">
                {{ item.title }}
              </a>
              <span v-else>{{ item.title }}</span>
            </Breadcrumb.Item>
          </Breadcrumb>
        </div>
        <div class="header-right">
          <div class="user-chip">
            <span class="user-avatar"><UserOutlined /></span>
            <div class="user-meta">
              <span class="user-name">{{ auth.user?.nickname }}</span>
              <span class="user-role">{{ auth.roleNames || '未分配角色' }}</span>
            </div>
          </div>
          <Dropdown placement="bottomRight">
            <button class="account-btn" type="button">
              账号
              <DownOutlined />
            </button>
            <template #overlay>
              <Menu>
                <Menu.Item key="password" @click="passwordOpen = true">修改密码</Menu.Item>
                <Menu.Item key="logout" @click="logout">退出登录</Menu.Item>
              </Menu>
            </template>
          </Dropdown>
        </div>
      </Layout.Header>
      <Layout.Content class="content">
        <RouterView />
      </Layout.Content>
    </Layout>
    <Modal v-model:open="passwordOpen" title="修改密码" :confirm-loading="saving" @ok="submitPassword">
      <Form layout="vertical">
        <Form.Item label="原密码">
          <Input.Password v-model:value="passwordForm.oldPassword" />
        </Form.Item>
        <Form.Item label="新密码">
          <Input.Password v-model:value="passwordForm.newPassword" placeholder="8-32 位，含字母和数字" />
        </Form.Item>
      </Form>
    </Modal>
  </Layout>
</template>

<style scoped>
.admin-root {
  --admin-topbar-height: 56px;
  min-height: 100vh;
  background: var(--bg-base);
}

.sider {
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid var(--border);
  background: linear-gradient(180deg, #121826 0%, #0f1522 100%) !important;
}

.sider :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo-bar {
  height: var(--admin-topbar-height);
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 0 10px 0 14px;
  border-bottom: 1px solid var(--border);
}

.logo-bar.collapsed {
  justify-content: center;
  padding: 0 6px;
  height: var(--admin-topbar-height);
}

.logo-link {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  text-decoration: none;
}

.collapse-btn,
.header-collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin: 0;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-2);
  cursor: pointer;
  transition: border-color 150ms ease, color 150ms ease, background 150ms ease;
  flex-shrink: 0;
}

.collapse-btn:hover,
.header-collapse-btn:hover {
  border-color: rgba(91, 140, 255, 0.35);
  color: var(--primary);
  background: rgba(91, 140, 255, 0.08);
}

.side-menu {
  padding: 12px 0 64px;
  background: transparent !important;
  border-inline-end: none !important;
}

.sider-foot {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  font-size: 12px;
  color: var(--text-3);
}

.sider-foot__ver {
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
  color: var(--text-2);
}

.main-layout {
  background: var(--bg-base);
}

.header {
  position: sticky;
  top: 0;
  z-index: 10;
  height: var(--admin-topbar-height);
  box-sizing: border-box;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--border);
  background: rgba(18, 24, 38, 0.85) !important;
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.breadcrumb :deep(.ant-breadcrumb-link),
.breadcrumb :deep(.ant-breadcrumb-separator) {
  color: var(--text-3);
}

.breadcrumb :deep(a) {
  color: var(--text-2);
}

.breadcrumb :deep(a:hover) {
  color: var(--primary);
}

.breadcrumb :deep(li:last-child) {
  color: var(--text-1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  max-width: min(100%, 320px);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-elevated);
  min-width: 0;
  max-width: 200px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(91, 140, 255, 0.18);
  color: var(--primary);
  font-size: 14px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  min-width: 0;
  overflow: hidden;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: 11px;
  color: var(--text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-2);
  font-size: 13px;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition: border-color 150ms ease, color 150ms ease;
  flex-shrink: 0;
}

.account-btn:hover {
  border-color: var(--border-strong);
  color: var(--text-1);
}

.content {
  padding: 24px;
  min-height: calc(100vh - var(--admin-topbar-height));
  background: var(--bg-base);
}
</style>
