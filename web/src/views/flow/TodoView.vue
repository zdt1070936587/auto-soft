<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button, Card, Input, Modal, Space, Table, Tag, message } from 'ant-design-vue'
import { completeTask, listTodo, rejectTask, type FlowTaskVO } from '@/api/flow'

const router = useRouter()
const loading = ref(false)
const rows = ref<FlowTaskVO[]>([])
const comment = ref('')

async function load() {
  loading.value = true
  try {
    rows.value = await listTodo()
  } finally {
    loading.value = false
  }
}

function pass(record: FlowTaskVO) {
  Modal.confirm({
    title: '通过该待办？',
    async onOk() {
      await completeTask(record.taskId, comment.value)
      message.success('已通过')
      comment.value = ''
      await load()
    },
  })
}

function reject(record: FlowTaskVO) {
  if (!comment.value.trim()) {
    message.warning('驳回必须填写意见')
    return
  }
  Modal.confirm({
    title: '驳回该待办？',
    content: '业务数据保留，提交人可修改后再次提交。',
    async onOk() {
      await rejectTask(record.taskId, comment.value.trim())
      message.success('已驳回')
      comment.value = ''
      await load()
    },
  })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <Card title="我的待办">
    <Space style="margin-bottom: 12px">
      <Input v-model:value="comment" placeholder="审批意见（驳回必填）" style="width: 320px" />
      <Button @click="load">刷新</Button>
    </Space>
    <Table :data-source="rows" :loading="loading" row-key="taskId" :pagination="false">
      <Table.Column title="应用" data-index="appCode" />
      <Table.Column title="实体" data-index="entityCode" />
      <Table.Column title="业务ID" data-index="bizId" />
      <Table.Column title="办理角色" data-index="roleCode" />
      <Table.Column title="操作" key="action">
        <template #default="{ record }: { record: FlowTaskVO }">
          <Space>
            <Button type="link" @click="router.push(`/app/${record.appCode}/${record.entityCode}`)">打开</Button>
            <Button type="link" @click="pass(record)">通过</Button>
            <Button type="link" danger @click="reject(record)">驳回</Button>
          </Space>
        </template>
      </Table.Column>
    </Table>
    <Tag v-if="!rows.length" style="margin-top: 12px">当前角色没有待办</Tag>
  </Card>
</template>
