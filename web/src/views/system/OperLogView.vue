<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Button, Card, Input, Select, Space, Table, Tag } from 'ant-design-vue'
import { pageOperLogs, type OperLogVO } from '@/api/log'

const loading = ref(false)
const rows = ref<OperLogVO[]>([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  module: undefined as string | undefined,
  username: '',
})

async function load() {
  loading.value = true
  try {
    const page = await pageOperLogs({
      current: query.current,
      size: query.size,
      module: query.module,
      username: query.username || undefined,
    })
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <Card title="操作日志">
    <Space style="margin-bottom: 12px">
      <Select
        v-model:value="query.module"
        allow-clear
        placeholder="模块"
        style="width: 140px"
        :options="['USER', 'ROLE', 'META', 'STUDIO', 'FLOW'].map((item) => ({ value: item, label: item }))"
      />
      <Input v-model:value="query.username" placeholder="用户名" style="width: 160px" allow-clear />
      <Button type="primary" @click="load">查询</Button>
    </Space>
    <Table
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :pagination="{ current: query.current, pageSize: query.size, total, onChange: (current: number) => { query.current = current; void load() } }"
    >
      <Table.Column title="时间" data-index="createdAt" width="200" />
      <Table.Column title="用户" data-index="username" />
      <Table.Column title="模块" data-index="module" />
      <Table.Column title="动作" data-index="action" />
      <Table.Column title="业务ID" data-index="bizId" />
      <Table.Column title="结果" key="success">
        <template #default="{ record }: { record: OperLogVO }">
          <Tag :color="record.success === 1 ? 'green' : 'red'">{{ record.success === 1 ? '成功' : '失败' }}</Tag>
        </template>
      </Table.Column>
      <Table.Column title="耗时ms" data-index="costMs" />
      <Table.Column title="IP" data-index="ip" />
      <Table.Column title="详情" data-index="detailJson" ellipsis />
    </Table>
  </Card>
</template>
