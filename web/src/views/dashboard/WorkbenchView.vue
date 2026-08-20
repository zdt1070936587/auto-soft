<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button, Card, Descriptions, Empty, Space, Tag } from 'ant-design-vue'
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

onMounted(() => {
  void loadHealth()
})
</script>

<template>
  <Space direction="vertical" :size="16" style="width: 100%">
    <Card title="工作台">
      <p>欢迎，{{ auth.user?.nickname }}。</p>
      <p>当前角色：{{ auth.roleNames || '未分配' }}。</p>
    </Card>
    <Card title="已发布应用">
      <Empty v-if="!appMenus.length" description="还没有已发布且授权给你的应用。开发者可在工作室或应用建模中发布。" />
      <Space v-else wrap>
        <Card v-for="app in appMenus" :key="app.id" size="small" style="width: 240px" :title="app.name">
          <p v-for="child in app.children?.filter((item) => item.menuType === 'MENU')" :key="child.id">
            <Button type="link" @click="openMenu(child)">{{ child.name }}</Button>
          </p>
          <Button v-if="!app.children?.length" type="primary" ghost @click="openMenu(app)">打开</Button>
        </Card>
      </Space>
    </Card>
    <Card title="系统状态">
      <template #extra>
        <Button type="link" :loading="loading" @click="loadHealth">刷新</Button>
      </template>
      <Descriptions v-if="health" :column="2" bordered size="small">
        <Descriptions.Item label="应用">{{ health.appName }}</Descriptions.Item>
        <Descriptions.Item label="Profile">{{ health.profile }}</Descriptions.Item>
        <Descriptions.Item label="数据库">
          <Tag :color="health.db === 'UP' ? 'green' : 'red'">{{ health.db }}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="服务器时间">{{ health.now }}</Descriptions.Item>
      </Descriptions>
    </Card>
  </Space>
</template>
