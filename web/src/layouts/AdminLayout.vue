<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Dropdown, Form, Input, Layout, Menu, Modal, message } from 'ant-design-vue'
import { updatePassword } from '@/api/auth'
import type { MenuVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const passwordOpen = ref(false)
const saving = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
})

interface SideItem {
  key: string
  label: string
  children?: SideItem[]
}

function toItems(menus: MenuVO[]): SideItem[] {
  return menus
    .filter((menu) => menu.menuType !== 'BUTTON')
    .map((menu) => {
      const children = menu.children?.length ? toItems(menu.children) : undefined
      return {
        key: menu.path || String(menu.id),
        label: menu.name,
        children: children && children.length ? children : undefined,
      }
    })
}

const menuItems = computed(() => toItems(auth.menus))
const selectedKeys = computed(() => [route.path])
const openKeys = computed(() => {
  if (route.path.startsWith('/system')) {
    return ['/system']
  }
  if (route.path.startsWith('/flow')) {
    return ['/flow']
  }
  if (route.path.startsWith('/app/')) {
    const parts = route.path.split('/')
    return parts.length >= 3 ? [`/app/${parts[2]}`] : []
  }
  return []
})

function onMenuClick(info: { key: string | number }) {
  const path = String(info.key)
  if (path.startsWith('/')) {
    void router.push(path)
  }
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
    <Layout.Sider width="220" theme="dark">
      <div class="logo">auto-soft</div>
      <Menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        :open-keys="openKeys"
        :items="menuItems"
        @click="onMenuClick"
      />
    </Layout.Sider>
    <Layout>
      <Layout.Header class="header">
        <div class="title">{{ route.meta.title || '工作台' }}</div>
        <div class="user">
          <span>{{ auth.user?.nickname }}（{{ auth.roleNames || '未分配角色' }}）</span>
          <Dropdown>
            <a class="link">账号</a>
            <template #overlay>
              <Menu>
                <Menu.Item key="password" @click="passwordOpen = true">修改密码</Menu.Item>
                <Menu.Item key="logout" @click="logout">退出</Menu.Item>
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
  min-height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.title {
  font-weight: 600;
}

.user {
  display: flex;
  gap: 16px;
  align-items: center;
}

.link {
  color: #1677ff;
}

.content {
  margin: 16px;
  padding: 16px;
  background: #fff;
  min-height: calc(100vh - 96px);
}
</style>
