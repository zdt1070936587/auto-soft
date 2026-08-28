<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Button, Card, Form, Input, Modal, Space, Table, Typography, message } from 'ant-design-vue'
import {
  createWorkflowHttpHost,
  deleteWorkflowHttpHost,
  listWorkflowHttpHosts,
  type WorkflowHttpHostVO,
} from '@/api/workflowHttpHost'

const loading = ref(false)
const rows = ref<WorkflowHttpHostVO[]>([])
const addOpen = ref(false)
const form = reactive({
  host: '',
  remark: '',
})

async function load() {
  loading.value = true
  try {
    rows.value = await listWorkflowHttpHosts()
  } finally {
    loading.value = false
  }
}

function openAdd() {
  form.host = ''
  form.remark = ''
  addOpen.value = true
}

async function submitAdd() {
  const host = form.host.trim()
  if (!host) {
    message.warning('请填写域名')
    return
  }
  await createWorkflowHttpHost({ host, remark: form.remark.trim() || undefined })
  message.success('已添加')
  addOpen.value = false
  await load()
}

function confirmDelete(record: WorkflowHttpHostVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定从白名单移除 ${record.host}？`,
    onOk: async () => {
      await deleteWorkflowHttpHost(record.id)
      message.success('已删除')
      await load()
    },
  })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <Card title="HTTP 出站白名单">
    <Typography.Paragraph>
      工作流 HTTP 节点只能访问此列表中的域名（与 application.yml 中
      <Typography.Text code>autosoft.workflow.http.allowed-hosts</Typography.Text>
      合并生效）。localhost、内网及元数据地址仍会被系统拒绝。
    </Typography.Paragraph>
    <Space style="margin-bottom: 12px">
      <Button type="primary" @click="openAdd">添加域名</Button>
      <Button :loading="loading" @click="load">刷新</Button>
    </Space>
    <Table :data-source="rows" :loading="loading" row-key="id" :pagination="false">
      <Table.Column title="域名" data-index="host" />
      <Table.Column title="备注" data-index="remark" />
      <Table.Column title="添加时间" data-index="createdAt" width="200" />
      <Table.Column title="操作" key="action" width="100">
        <template #default="{ record }: { record: WorkflowHttpHostVO }">
          <Button type="link" danger size="small" @click="confirmDelete(record)">删除</Button>
        </template>
      </Table.Column>
    </Table>

    <Modal v-model:open="addOpen" title="添加出站域名" ok-text="添加" @ok="submitAdd">
      <Form layout="vertical">
        <Form.Item label="域名" required>
          <Input v-model:value="form.host" placeholder="如 www.baidu.com（不要带 http:// 或端口）" />
        </Form.Item>
        <Form.Item label="备注">
          <Input v-model:value="form.remark" placeholder="可选，如「百度 API」" />
        </Form.Item>
      </Form>
    </Modal>
  </Card>
</template>
