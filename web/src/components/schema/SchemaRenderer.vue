<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Button, DatePicker, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import type { MetaFieldVO, RuntimeSchemaVO } from '@/api/meta'
import { createRuntime, deleteRuntime, pageRuntime, submitRuntime, updateRuntime } from '@/api/meta'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  schema: RuntimeSchemaVO
  preview?: boolean
}>()

const auth = useAuthStore()
const loading = ref(false)
const records = ref<Record<string, unknown>[]>([])
const total = ref(0)
const query = reactive<Record<string, string | number | undefined>>({ current: 1, size: 10 })
const editOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<Record<string, any>>({})

const listedFields = computed(() => props.schema.fields.filter((f) => f.listed !== 0))
const queryFields = computed(() => props.schema.fields.filter((f) => f.queryable === 1))

function perm(action: string) {
  return `app:${props.schema.appCode}:${props.schema.entityCode}:${action}`
}

function can(action: string) {
  return auth.hasPermission(perm(action))
}

function flowStatus(row: Record<string, unknown>) {
  return String(row.flow_status || 'none')
}

function readonlyRow(row: Record<string, unknown>) {
  if (!props.schema.flowBound) {
    return false
  }
  const status = flowStatus(row)
  return status === 'processing' || status === 'approved'
}

function parseOptions(field: MetaFieldVO) {
  if (!field.optionsJson) {
    return []
  }
  try {
    return JSON.parse(field.optionsJson) as Array<{ label: string; value: string }>
  } catch {
    return []
  }
}

async function load() {
  if (props.preview && !props.schema.published) {
    records.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const page = await pageRuntime(props.schema.appCode, props.schema.entityCode, query, Boolean(props.preview))
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  for (const key of Object.keys(form)) {
    delete form[key]
  }
  editOpen.value = true
}

function openCreateWithDraft(values: Record<string, unknown>) {
  openCreate()
  for (const field of props.schema.fields) {
    if (values[field.code] !== undefined) {
      form[field.code] = values[field.code]
    }
  }
}

defineExpose({ openCreateWithDraft })

function openEdit(row: Record<string, unknown>) {
  editingId.value = Number(row.id)
  for (const key of Object.keys(form)) {
    delete form[key]
  }
  for (const field of props.schema.fields) {
    form[field.code] = row[field.code]
  }
  editOpen.value = true
}

function toPayload() {
  const body: Record<string, unknown> = {}
  for (const field of props.schema.fields) {
    let value = form[field.code]
    if (dayjs.isDayjs(value)) {
      value = field.fieldType === 'date' ? (value as Dayjs).format('YYYY-MM-DD') : (value as Dayjs).toISOString()
    }
    body[field.code] = value
  }
  return body
}

async function save() {
  saving.value = true
  try {
    if (editingId.value == null) {
      await createRuntime(props.schema.appCode, props.schema.entityCode, toPayload())
    } else {
      await updateRuntime(props.schema.appCode, props.schema.entityCode, editingId.value, toPayload())
    }
    message.success('已保存')
    editOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Record<string, unknown>) {
  await deleteRuntime(props.schema.appCode, props.schema.entityCode, Number(row.id))
  message.success('已删除')
  await load()
}

async function submit(row: Record<string, unknown>) {
  await submitRuntime(props.schema.appCode, props.schema.entityCode, Number(row.id))
  message.success('已提交审批')
  await load()
}

watch(
  () => props.schema.entityCode,
  () => {
    query.current = 1
    void load()
  },
  { immediate: true },
)
</script>

<template>
  <div class="schema-renderer">
    <Form layout="inline" class="page-toolbar" @finish="load">
      <Form.Item v-for="field in queryFields" :key="field.code" :label="field.name">
        <Input v-model:value="query[field.code]" allow-clear />
      </Form.Item>
      <Form.Item v-if="schema.flowBound" label="流程状态">
        <Select
          v-model:value="query.flow_status"
          allow-clear
          style="width: 140px"
          :options="['none', 'draft', 'processing', 'approved', 'rejected'].map((v) => ({ label: v, value: v }))"
        />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button type="primary" html-type="submit">查询</Button>
          <Button v-if="can('create')" type="primary" @click="openCreate">新建</Button>
        </Space>
      </Form.Item>
    </Form>
    <Table
      class="schema-table"
      row-key="id"
      :loading="loading"
      :data-source="records"
      :pagination="{ current: Number(query.current), pageSize: Number(query.size), total }"
      @change="
        (pag) => {
          query.current = pag.current || 1
          query.size = pag.pageSize || 10
          void load()
        }
      "
    >
      <Table.Column v-for="field in listedFields" :key="field.code" :title="field.name" :data-index="field.code" />
      <Table.Column v-if="schema.flowBound" title="流程状态" data-index="flow_status" />
      <Table.Column title="操作" width="240">
        <template #default="{ record }">
          <Space>
            <Button v-if="can('update') && !readonlyRow(record)" type="link" @click="openEdit(record)">编辑</Button>
            <Button
              v-if="schema.flowBound && can('submit') && (flowStatus(record) === 'draft' || flowStatus(record) === 'rejected')"
              type="link"
              @click="submit(record)"
            >
              提交
            </Button>
            <Button v-if="can('delete') && !readonlyRow(record)" type="link" danger @click="remove(record)">删除</Button>
          </Space>
        </template>
      </Table.Column>
    </Table>

    <Modal v-model:open="editOpen" :title="editingId ? '编辑' : '新建'" :confirm-loading="saving" @ok="save">
      <Form layout="vertical">
        <Form.Item v-for="field in schema.fields" :key="field.code" :label="field.name" :required="field.requiredFlag === 1">
          <InputNumber v-if="field.fieldType === 'int' || field.fieldType === 'long' || field.fieldType === 'decimal'" v-model:value="form[field.code]" style="width: 100%" />
          <Input.TextArea v-else-if="field.fieldType === 'text'" v-model:value="form[field.code]" :rows="3" />
          <Switch v-else-if="field.fieldType === 'bool'" :checked="form[field.code] === 1 || form[field.code] === true" @change="(c) => (form[field.code] = c ? 1 : 0)" />
          <DatePicker v-else-if="field.fieldType === 'date' || field.fieldType === 'datetime'" v-model:value="form[field.code]" style="width: 100%" :show-time="field.fieldType === 'datetime'" />
          <Select v-else-if="field.fieldType === 'dict'" v-model:value="form[field.code]" :options="parseOptions(field)" />
          <Input v-else v-model:value="form[field.code]" />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.schema-renderer :deep(.ant-form-item-label > label) {
  color: var(--text-2);
}

.schema-table :deep(.ant-table-cell) {
  font-size: 13px;
}
</style>
