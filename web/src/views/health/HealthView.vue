<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Button, Card, Descriptions, Space, Tag } from 'ant-design-vue'
import { getHealth, type HealthVO } from '@/api/health'
import { useAppStore } from '@/stores/app'

const loading = ref(false)
const health = ref<HealthVO | null>(null)
const appStore = useAppStore()

async function loadHealth() {
  loading.value = true
  try {
    const data = await getHealth()
    health.value = data
    appStore.appName = data.appName
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadHealth()
})
</script>

<template>
  <div class="page">
    <Card title="开发健康检查" :loading="loading">
      <template v-if="health">
        <Descriptions :column="1" bordered>
          <Descriptions.Item label="应用">{{ health.appName }}</Descriptions.Item>
          <Descriptions.Item label="Profile">{{ health.profile }}</Descriptions.Item>
          <Descriptions.Item label="数据库">
            <Tag :color="health.db === 'UP' ? 'green' : 'red'">{{ health.db }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="服务器时间">{{ health.now }}</Descriptions.Item>
        </Descriptions>
      </template>
      <template v-else-if="!loading">
        <p>尚未获取到健康信息。</p>
      </template>
      <template #extra>
        <Space>
          <Button type="primary" :loading="loading" @click="loadHealth">重新检查</Button>
        </Space>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 48px 24px;
  background: #f5f5f5;
  display: flex;
  justify-content: center;
}

.page :deep(.ant-card) {
  width: 640px;
}
</style>
