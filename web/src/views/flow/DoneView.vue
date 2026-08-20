<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button, Card, Table, Tag } from 'ant-design-vue'
import { listDone, type FlowTaskVO } from '@/api/flow'

const router = useRouter()
const loading = ref(false)
const rows = ref<FlowTaskVO[]>([])

async function load() {
  loading.value = true
  try {
    rows.value = await listDone()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <Card title="我已办">
    <Button style="margin-bottom: 12px" @click="load">刷新</Button>
    <Table :data-source="rows" :loading="loading" row-key="taskId" :pagination="false">
      <Table.Column title="应用" data-index="appCode" />
      <Table.Column title="实体" data-index="entityCode" />
      <Table.Column title="业务ID" data-index="bizId" />
      <Table.Column title="结果" key="status">
        <template #default="{ record }: { record: FlowTaskVO }">
          <Tag :color="record.status === 'done' ? 'green' : 'orange'">{{ record.status }}</Tag>
        </template>
      </Table.Column>
      <Table.Column title="意见" data-index="comment" />
      <Table.Column title="操作" key="action">
        <template #default="{ record }: { record: FlowTaskVO }">
          <Button type="link" @click="router.push(`/app/${record.appCode}/${record.entityCode}`)">打开</Button>
        </template>
      </Table.Column>
    </Table>
  </Card>
</template>
