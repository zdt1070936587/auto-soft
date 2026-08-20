<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Button, Form, Input, Modal, Select, Space, Table, Tag, message } from 'ant-design-vue'
import {
  addMetaField,
  createMetaApp,
  createMetaEntity,
  getMetaApp,
  listMetaApps,
  publishMetaApp,
  unpublishMetaApp,
  type MetaAppVO,
  type MetaFieldVO,
} from '@/api/meta'

const apps = ref<MetaAppVO[]>([])
const current = ref<MetaAppVO | null>(null)
const appOpen = ref(false)
const entityOpen = ref(false)
const fieldOpen = ref(false)
const currentEntityId = ref<number | null>(null)
const appForm = reactive({ code: '', name: '', grantRoles: 'USER', remark: '' })
const entityForm = reactive({ code: '', name: '', remark: '' })
const fieldForm = reactive<MetaFieldVO>({
  code: '',
  name: '',
  fieldType: 'string',
  queryable: 0,
  listed: 1,
  requiredFlag: 0,
})

async function loadApps() {
  apps.value = await listMetaApps()
}

async function openApp(id: number) {
  current.value = await getMetaApp(id)
}

async function saveApp() {
  await createMetaApp(appForm)
  message.success('已创建')
  appOpen.value = false
  await loadApps()
}

async function saveEntity() {
  if (!current.value) {
    return
  }
  await createMetaEntity(current.value.id, entityForm)
  message.success('已创建实体')
  entityOpen.value = false
  await openApp(current.value.id)
}

async function saveField() {
  if (currentEntityId.value == null) {
    return
  }
  await addMetaField(currentEntityId.value, fieldForm)
  message.success('已添加字段')
  fieldOpen.value = false
  if (current.value) {
    await openApp(current.value.id)
  }
}

async function publish() {
  if (!current.value) {
    return
  }
  await publishMetaApp(current.value.id, current.value.grantRoles || 'USER')
  message.success('已发布，请重新登录或刷新菜单')
  await openApp(current.value.id)
}

async function unpublish() {
  if (!current.value) {
    return
  }
  await unpublishMetaApp(current.value.id)
  message.success('已取消发布')
  await openApp(current.value.id)
}

onMounted(() => {
  void loadApps()
})
</script>

<template>
  <div class="wrap">
    <div class="left">
      <Space style="margin-bottom: 12px">
        <Button type="primary" @click="appOpen = true">新建应用</Button>
      </Space>
      <Table
        row-key="id"
        size="small"
        :data-source="apps"
        :pagination="false"
        :custom-row="(record: MetaAppVO) => ({ onClick: () => openApp(record.id) })"
      >
        <Table.Column title="应用" data-index="name" />
        <Table.Column title="编码" data-index="code" />
        <Table.Column title="状态">
          <template #default="{ record }">
            <Tag :color="record.status === 'PUBLISHED' ? 'green' : 'orange'">{{ record.status }}</Tag>
          </template>
        </Table.Column>
      </Table>
    </div>
    <div class="right" v-if="current">
      <Space style="margin-bottom: 12px">
        <Button @click="entityOpen = true">新增实体</Button>
        <Button type="primary" @click="publish">发布</Button>
        <Button v-if="current.status === 'PUBLISHED'" @click="unpublish">取消发布</Button>
      </Space>
      <p>{{ current.name }}（{{ current.code }}）授权角色：{{ current.grantRoles }} 版本：{{ current.version }}</p>
      <div v-for="entity in current.entities" :key="entity.id" class="entity">
        <Space>
          <strong>{{ entity.name }} / {{ entity.code }}</strong>
          <Button type="link" @click="currentEntityId = entity.id; fieldOpen = true">加字段</Button>
        </Space>
        <Table row-key="id" size="small" :pagination="false" :data-source="entity.fields">
          <Table.Column title="编码" data-index="code" />
          <Table.Column title="名称" data-index="name" />
          <Table.Column title="类型" data-index="fieldType" />
          <Table.Column title="查询" data-index="queryable" />
          <Table.Column title="列表" data-index="listed" />
          <Table.Column title="必填" data-index="requiredFlag" />
        </Table>
      </div>
    </div>
  </div>

  <Modal v-model:open="appOpen" title="新建应用" @ok="saveApp">
    <Form layout="vertical">
      <Form.Item label="编码"><Input v-model:value="appForm.code" placeholder="demo" /></Form.Item>
      <Form.Item label="名称"><Input v-model:value="appForm.name" /></Form.Item>
      <Form.Item label="授权角色"><Input v-model:value="appForm.grantRoles" placeholder="USER" /></Form.Item>
    </Form>
  </Modal>
  <Modal v-model:open="entityOpen" title="新增实体" @ok="saveEntity">
    <Form layout="vertical">
      <Form.Item label="编码"><Input v-model:value="entityForm.code" /></Form.Item>
      <Form.Item label="名称"><Input v-model:value="entityForm.name" /></Form.Item>
    </Form>
  </Modal>
  <Modal v-model:open="fieldOpen" title="新增字段" @ok="saveField">
    <Form layout="vertical">
      <Form.Item label="编码"><Input v-model:value="fieldForm.code" /></Form.Item>
      <Form.Item label="名称"><Input v-model:value="fieldForm.name" /></Form.Item>
      <Form.Item label="类型">
        <Select
          v-model:value="fieldForm.fieldType"
          :options="['string', 'text', 'int', 'long', 'decimal', 'bool', 'date', 'datetime', 'dict', 'ref'].map((v) => ({ label: v, value: v }))"
        />
      </Form.Item>
      <Form.Item label="可查询">
        <Select v-model:value="fieldForm.queryable" :options="[{ label: '否', value: 0 }, { label: '是', value: 1 }]" />
      </Form.Item>
      <Form.Item label="必填">
        <Select v-model:value="fieldForm.requiredFlag" :options="[{ label: '否', value: 0 }, { label: '是', value: 1 }]" />
      </Form.Item>
    </Form>
  </Modal>
</template>

<style scoped>
.wrap {
  display: flex;
  gap: 16px;
}
.left {
  width: 360px;
}
.right {
  flex: 1;
}
.entity {
  margin-bottom: 16px;
}
</style>
