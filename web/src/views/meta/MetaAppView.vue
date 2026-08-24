<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CheckCircleOutlined,
  PlusOutlined,
  RocketOutlined,
} from '@ant-design/icons-vue'
import {
  Alert,
  Button,
  Empty,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Steps,
  Table,
  Tag,
  message,
} from 'ant-design-vue'
import PageShell from '@/components/layout/PageShell.vue'
import {
  addMetaField,
  createMetaApp,
  createMetaEntity,
  deleteMetaApp,
  getMetaApp,
  listMetaApps,
  publishMetaApp,
  unpublishMetaApp,
  updateMetaApp,
  type MetaAppVO,
  type MetaFieldVO,
} from '@/api/meta'

const router = useRouter()
const apps = ref<MetaAppVO[]>([])
const current = ref<MetaAppVO | null>(null)
const selectedAppId = ref<number | null>(null)
const appOpen = ref(false)
const editAppOpen = ref(false)
const entityOpen = ref(false)
const fieldOpen = ref(false)
const currentEntityId = ref<number | null>(null)
const savingApp = ref(false)
const savingEdit = ref(false)
const appForm = reactive({ code: '', name: '', grantRoles: 'USER', remark: '' })
const editForm = reactive({ code: '', name: '', grantRoles: 'USER', remark: '' })
const entityForm = reactive({ code: '', name: '', remark: '' })
const fieldForm = reactive<MetaFieldVO>({
  code: '',
  name: '',
  fieldType: 'string',
  queryable: 0,
  listed: 1,
  requiredFlag: 0,
})

const entityCount = computed(() => current.value?.entities?.length ?? 0)
const fieldCount = computed(() =>
  (current.value?.entities ?? []).reduce((sum, entity) => sum + (entity.fields?.length ?? 0), 0),
)
const allEntitiesHaveFields = computed(() => {
  if (!current.value?.entities?.length) {
    return false
  }
  return current.value.entities.every((entity) => (entity.fields?.length ?? 0) > 0)
})
const canPublish = computed(() => entityCount.value > 0 && allEntitiesHaveFields.value)
const setupStep = computed(() => {
  if (!current.value) {
    return 0
  }
  if (entityCount.value === 0) {
    return 1
  }
  if (!allEntitiesHaveFields.value) {
    return 2
  }
  if (current.value.status !== 'PUBLISHED') {
    return 3
  }
  return 4
})

const publishHint = computed(() => {
  if (!current.value) {
    return ''
  }
  if (entityCount.value === 0) {
    return '请先新增至少一个实体（如：订单、客户）'
  }
  const empty = current.value.entities.find((entity) => !(entity.fields?.length ?? 0))
  if (empty) {
    return `实体「${empty.name}」还没有字段，请先添加字段`
  }
  return ''
})

function resetAppForm() {
  appForm.code = ''
  appForm.name = ''
  appForm.grantRoles = 'USER'
  appForm.remark = ''
}

function resetEntityForm() {
  entityForm.code = ''
  entityForm.name = ''
  entityForm.remark = ''
}

function resetFieldForm() {
  fieldForm.code = ''
  fieldForm.name = ''
  fieldForm.fieldType = 'string'
  fieldForm.queryable = 0
  fieldForm.listed = 1
  fieldForm.requiredFlag = 0
}

function openCreateApp() {
  resetAppForm()
  appOpen.value = true
}

function openEditApp() {
  if (!current.value) {
    return
  }
  editForm.code = current.value.code
  editForm.name = current.value.name
  editForm.grantRoles = current.value.grantRoles || 'USER'
  editForm.remark = current.value.remark || ''
  editAppOpen.value = true
}

function confirmDeleteApp() {
  if (!current.value) {
    return
  }
  if (current.value.status === 'PUBLISHED') {
    message.warning('已发布应用请先「取消发布」再删除')
    return
  }
  Modal.confirm({
    title: `删除应用「${current.value.name}」？`,
    content: '将删除该应用及其下所有实体、字段等元数据。此操作不可恢复。',
    okText: '删除',
    okType: 'danger',
    async onOk() {
      const id = current.value!.id
      await deleteMetaApp(id)
      message.success('应用已删除')
      current.value = null
      selectedAppId.value = null
      await loadApps()
    },
  })
}

function openCreateEntity() {
  if (!current.value) {
    message.info('请先在左侧选择一个应用')
    return
  }
  resetEntityForm()
  entityOpen.value = true
}

function openCreateField(entityId: number) {
  currentEntityId.value = entityId
  resetFieldForm()
  fieldOpen.value = true
}

async function loadApps() {
  apps.value = await listMetaApps()
}

async function openApp(id: number) {
  selectedAppId.value = id
  current.value = await getMetaApp(id)
}

async function saveEditApp() {
  if (!current.value) {
    return Promise.reject()
  }
  const name = editForm.name.trim()
  if (!name) {
    message.warning('请填写应用名称')
    return Promise.reject()
  }
  savingEdit.value = true
  try {
    await updateMetaApp(current.value.id, {
      code: current.value.code,
      name,
      grantRoles: editForm.grantRoles.trim() || 'USER',
      remark: editForm.remark.trim(),
    })
    message.success('应用已更新')
    editAppOpen.value = false
    await loadApps()
    await openApp(current.value.id)
  } catch (error) {
    return Promise.reject(error)
  } finally {
    savingEdit.value = false
  }
}

async function saveApp() {
  const code = appForm.code.trim()
  const name = appForm.name.trim()
  if (!code || !name) {
    message.warning('请填写应用编码和名称')
    return Promise.reject()
  }
  savingApp.value = true
  try {
    const id = await createMetaApp({ ...appForm, code, name })
    message.success('应用已创建，请继续新增实体')
    appOpen.value = false
    await loadApps()
    await openApp(id)
    entityOpen.value = true
  } catch (error) {
    return Promise.reject(error)
  } finally {
    savingApp.value = false
  }
}

async function saveEntity() {
  if (!current.value) {
    return Promise.reject()
  }
  const code = entityForm.code.trim()
  const name = entityForm.name.trim()
  if (!code || !name) {
    message.warning('请填写实体编码和名称')
    return Promise.reject()
  }
  const entityId = await createMetaEntity(current.value.id, { ...entityForm, code, name })
  message.success('实体已创建，请继续添加字段')
  entityOpen.value = false
  await openApp(current.value.id)
  openCreateField(entityId)
}

async function saveField() {
  if (currentEntityId.value == null) {
    return Promise.reject()
  }
  const code = fieldForm.code.trim()
  const name = fieldForm.name.trim()
  if (!code || !name) {
    message.warning('请填写字段编码和名称')
    return Promise.reject()
  }
  await addMetaField(currentEntityId.value, { ...fieldForm, code, name })
  message.success('字段已添加')
  fieldOpen.value = false
  if (current.value) {
    await openApp(current.value.id)
  }
}

function confirmPublish() {
  if (!current.value) {
    return
  }
  if (!canPublish.value) {
    message.warning(publishHint.value || '尚未满足发布条件')
    return
  }
  Modal.confirm({
    title: '确认发布应用？',
    content:
      '发布后将创建数据库表并生成菜单。授权角色需重新登录或刷新菜单后才能在「工作台」看到该应用。',
    okText: '发布',
    async onOk() {
      await publishMetaApp(current.value!.id, current.value!.grantRoles || 'USER')
      message.success('已发布，请重新登录或刷新菜单')
      await openApp(current.value!.id)
    },
  })
}

async function unpublish() {
  if (!current.value) {
    return
  }
  await unpublishMetaApp(current.value.id)
  message.success('已取消发布')
  await openApp(current.value.id)
}

onMounted(async () => {
  await loadApps()
  if (apps.value.length === 1) {
    await openApp(apps.value[0].id)
  }
})
</script>

<template>
  <PageShell
    title="应用建模"
    subtitle="手动维护元数据：创建应用容器 → 定义实体与字段 → 发布后在「工作台」可用。也可使用「功能开发」由 AI 自动生成。"
  >
    <template #actions>
      <Button @click="router.push('/studio')">
        <RocketOutlined />
        用 AI 开发
      </Button>
      <Button type="primary" @click="openCreateApp">
        <PlusOutlined />
        新建应用
      </Button>
    </template>

    <div class="wrap">
      <aside class="left panel">
        <div class="panel-head">
          <strong>应用列表</strong>
          <span class="panel-hint">点击选择</span>
        </div>
        <Table
          row-key="id"
          size="small"
          :data-source="apps"
          :pagination="false"
          :row-class-name="(record: MetaAppVO) => (record.id === selectedAppId ? 'row-active' : '')"
          :custom-row="(record: MetaAppVO) => ({ onClick: () => openApp(record.id) })"
        >
          <Table.Column title="应用" data-index="name" />
          <Table.Column title="编码" data-index="code" />
          <Table.Column title="状态" width="88">
            <template #default="{ record }">
              <Tag :color="record.status === 'PUBLISHED' ? 'green' : 'orange'">
                {{ record.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </Tag>
            </template>
          </Table.Column>
        </Table>
        <Empty v-if="!apps.length" class="list-empty" description="还没有应用，点击右上角新建">
          <Button type="primary" size="small" @click="openCreateApp">新建应用</Button>
        </Empty>
      </aside>

      <section class="right panel">
        <Empty v-if="!current" class="detail-empty" description="请从左侧选择一个应用，或新建应用开始建模">
          <Space>
            <Button type="primary" @click="openCreateApp">新建应用</Button>
            <Button @click="router.push('/studio')">去功能开发</Button>
          </Space>
        </Empty>

        <template v-else>
          <Steps :current="setupStep" size="small" class="setup-steps">
            <Steps.Step title="创建应用" />
            <Steps.Step title="新增实体" />
            <Steps.Step title="添加字段" />
            <Steps.Step title="发布上线" />
          </Steps>

          <Alert
            v-if="current.status !== 'PUBLISHED'"
            type="info"
            show-icon
            class="setup-alert"
            :message="setupStep < 3 ? '应用尚未就绪' : '可以发布了'"
            :description="
              setupStep < 3
                ? '当前只是元数据草稿，不会出现在工作台。请按上方步骤完成实体、字段配置后再发布。'
                : '实体与字段已配置完成，发布后将在工作台生成菜单并创建数据表。'
            "
          />
          <Alert
            v-else
            type="success"
            show-icon
            class="setup-alert"
            message="应用已发布"
            description="用户重新登录或刷新菜单后，可在「工作台」进入该应用。"
          />

          <div class="detail-head">
            <div>
              <h2 class="app-title">{{ current.name }}</h2>
              <p class="app-meta">
                编码 {{ current.code }} · 授权 {{ current.grantRoles || 'USER' }} · 版本
                {{ current.version ?? 0 }} · {{ entityCount }} 个实体 · {{ fieldCount }} 个字段
              </p>
            </div>
            <Space wrap>
              <Button @click="openEditApp">编辑</Button>
              <Button danger @click="confirmDeleteApp">删除</Button>
              <Button @click="openCreateEntity">新增实体</Button>
              <Button
                type="primary"
                :disabled="!canPublish"
                :title="publishHint"
                @click="confirmPublish"
              >
                发布
              </Button>
              <Button v-if="current.status === 'PUBLISHED'" @click="unpublish">取消发布</Button>
            </Space>
          </div>

          <div v-if="!entityCount" class="entity-empty">
            <Empty description="还没有实体，应用无法发布">
              <p class="empty-tip">实体代表一张业务表，例如：请假单、客户、订单。</p>
              <Button type="primary" @click="openCreateEntity">新增第一个实体</Button>
            </Empty>
          </div>

          <div v-for="entity in current.entities" :key="entity.id" class="entity-card">
            <div class="entity-head">
              <div>
                <strong>{{ entity.name }}</strong>
                <span class="entity-code">{{ entity.code }}</span>
              </div>
              <Button type="link" @click="openCreateField(entity.id)">加字段</Button>
            </div>

            <Empty
              v-if="!(entity.fields?.length ?? 0)"
              class="field-empty"
              image-style="{ height: 48px }"
              description="该实体还没有字段，无法发布"
            >
              <Button size="small" type="primary" @click="openCreateField(entity.id)">添加第一个字段</Button>
            </Empty>

            <Table
              v-else
              row-key="id"
              size="small"
              :pagination="false"
              :data-source="entity.fields"
            >
              <Table.Column title="编码" data-index="code" />
              <Table.Column title="名称" data-index="name" />
              <Table.Column title="类型" data-index="fieldType" />
              <Table.Column title="查询" data-index="queryable" />
              <Table.Column title="列表" data-index="listed" />
              <Table.Column title="必填" data-index="requiredFlag" />
            </Table>
          </div>

          <div v-if="canPublish && current.status !== 'PUBLISHED'" class="publish-banner">
            <CheckCircleOutlined />
            <span>配置已完成，点击「发布」即可在工作台使用该应用。</span>
            <Button type="primary" size="small" @click="confirmPublish">立即发布</Button>
          </div>
        </template>
      </section>
    </div>
  </PageShell>

  <Modal
    v-model:open="appOpen"
    title="新建应用"
    ok-text="创建并继续"
    :confirm-loading="savingApp"
    @ok="saveApp"
  >
    <Alert
      type="info"
      show-icon
      class="modal-tip"
      message="这一步只创建应用容器"
      description="创建后会引导你继续「新增实体」和「添加字段」，全部完成并发布后才可在工作台使用。"
    />
    <Form layout="vertical">
      <Form.Item label="编码" required>
        <Input v-model:value="appForm.code" placeholder="小写英文开头，如 leave_request" />
      </Form.Item>
      <Form.Item label="名称" required>
        <Input v-model:value="appForm.name" placeholder="如：请假管理" />
      </Form.Item>
      <Form.Item label="发布后授权角色">
        <Input v-model:value="appForm.grantRoles" placeholder="USER" />
      </Form.Item>
    </Form>
  </Modal>

  <Modal
    v-model:open="editAppOpen"
    title="编辑应用"
    ok-text="保存"
    :confirm-loading="savingEdit"
    @ok="saveEditApp"
  >
    <Form layout="vertical">
      <Form.Item label="编码">
        <Input v-model:value="editForm.code" disabled />
      </Form.Item>
      <Form.Item label="名称" required>
        <Input v-model:value="editForm.name" />
      </Form.Item>
      <Form.Item label="授权角色">
        <Input v-model:value="editForm.grantRoles" placeholder="USER" />
      </Form.Item>
      <Form.Item label="备注">
        <Input.TextArea v-model:value="editForm.remark" :rows="2" />
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="entityOpen" title="新增实体" ok-text="创建并添加字段" @ok="saveEntity">
    <Alert
      type="info"
      show-icon
      class="modal-tip"
      message="实体 = 一张业务数据表"
      description="例如客户管理可建实体 customer，再在下一步添加姓名、电话等字段。"
    />
    <Form layout="vertical">
      <Form.Item label="编码" required>
        <Input v-model:value="entityForm.code" placeholder="customer" />
      </Form.Item>
      <Form.Item label="名称" required>
        <Input v-model:value="entityForm.name" placeholder="客户" />
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="fieldOpen" title="新增字段" ok-text="添加" @ok="saveField">
    <Form layout="vertical">
      <Form.Item label="编码" required>
        <Input v-model:value="fieldForm.code" placeholder="customer_name" />
      </Form.Item>
      <Form.Item label="名称" required>
        <Input v-model:value="fieldForm.name" placeholder="客户名称" />
      </Form.Item>
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
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}

.panel {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-elevated);
  padding: 16px;
  min-height: 520px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-hint {
  font-size: 12px;
  color: var(--text-3);
}

.left :deep(.row-active > td) {
  background: rgba(91, 140, 255, 0.12) !important;
}

.list-empty,
.detail-empty {
  margin-top: 48px;
}

.setup-steps {
  margin-bottom: 16px;
}

.setup-alert {
  margin-bottom: 16px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-1);
}

.app-meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-3);
}

.entity-empty {
  padding: 24px 0;
}

.empty-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--text-3);
}

.entity-card {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-surface);
}

.entity-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.entity-code {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-3);
}

.field-empty {
  margin: 8px 0;
}

.publish-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(34, 197, 94, 0.25);
  background: rgba(34, 197, 94, 0.08);
  color: var(--text-2);
  font-size: 13px;
}

.modal-tip {
  margin-bottom: 16px;
}

@media (max-width: 960px) {
  .wrap {
    grid-template-columns: 1fr;
  }
}
</style>
