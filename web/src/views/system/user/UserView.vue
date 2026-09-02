<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'ant-design-vue'
import {
  assignUserRoles,
  createUser,
  deleteUser,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus,
} from '@/api/user'
import { consumeActionDraft } from '@/api/assistant'
import { listRoleOptions } from '@/api/role'
import type { RoleVO, UserVO } from '@/api/types'
import { useAssistantActionStore } from '@/stores/assistantAction'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const users = ref<UserVO[]>([])
const total = ref(0)
const roles = ref<RoleVO[]>([])
const query = reactive({
  username: '',
  status: undefined as number | undefined,
  current: 1,
  size: 10,
})

const editOpen = ref(false)
const editMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const editingId = ref<number | null>(null)
const editForm = reactive({
  username: '',
  password: '',
  nickname: '',
  status: 1,
  roleIds: [] as number[],
})

const roleOpen = ref(false)
const roleUserId = ref<number | null>(null)
const roleIds = ref<number[]>([])

const resetOpen = ref(false)
const resetUserId = ref<number | null>(null)
const resetPassword = ref('')

async function loadRoles() {
  roles.value = await listRoleOptions()
}

async function loadUsers() {
  loading.value = true
  try {
    const page = await pageUsers({
      current: query.current,
      size: query.size,
      username: query.username || undefined,
      status: query.status,
    })
    users.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editMode.value = 'create'
  editingId.value = null
  editForm.username = ''
  editForm.password = ''
  editForm.nickname = ''
  editForm.status = 1
  editForm.roleIds = []
  editOpen.value = true
}

function openEdit(record: UserVO) {
  editMode.value = 'edit'
  editingId.value = record.id
  editForm.username = record.username
  editForm.password = ''
  editForm.nickname = record.nickname
  editForm.status = record.status
  editForm.roleIds = record.roles.map((role) => role.id)
  editOpen.value = true
}

async function saveUser() {
  saving.value = true
  try {
    if (editMode.value === 'create') {
      await createUser({
        username: editForm.username,
        password: editForm.password,
        nickname: editForm.nickname,
        status: editForm.status,
        roleIds: editForm.roleIds,
      })
      message.success('已创建')
    } else if (editingId.value != null) {
      await updateUser(editingId.value, { nickname: editForm.nickname })
      message.success('已保存')
    }
    editOpen.value = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

function openRoles(record: UserVO) {
  roleUserId.value = record.id
  roleIds.value = record.roles.map((role) => role.id)
  roleOpen.value = true
}

async function saveRoles() {
  if (roleUserId.value == null) {
    return
  }
  await assignUserRoles(roleUserId.value, roleIds.value)
  message.success('已分配角色')
  roleOpen.value = false
  await loadUsers()
}

function openReset(record: UserVO) {
  resetUserId.value = record.id
  resetPassword.value = ''
  resetOpen.value = true
}

async function saveReset() {
  if (resetUserId.value == null) {
    return
  }
  await resetUserPassword(resetUserId.value, resetPassword.value)
  message.success('密码已重置')
  resetOpen.value = false
}

async function toggleStatus(record: UserVO) {
  const next = record.status === 1 ? 0 : 1
  await updateUserStatus(record.id, next)
  message.success(next === 1 ? '已启用' : '已停用')
  await loadUsers()
}

async function removeUser(record: UserVO) {
  await deleteUser(record.id)
  message.success('已删除')
  await loadUsers()
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadUsers()])
  const draft = useAssistantActionStore().consume('system.user.create')
  if (!draft) {
    return
  }
  openCreate()
  if (draft.values.username != null) {
    editForm.username = String(draft.values.username)
  }
  if (draft.values.password != null) {
    editForm.password = String(draft.values.password)
  }
  if (draft.values.nickname != null) {
    editForm.nickname = String(draft.values.nickname)
  }
  if (Array.isArray(draft.values.roleIds)) {
    editForm.roleIds = draft.values.roleIds as number[]
  }
  if (draft.missing.length) {
    message.warning(`还需填写：${draft.missing.join('、')}`)
  }
  await consumeActionDraft(draft.draftId)
})
</script>

<template>
  <div>
    <Form layout="inline" class="toolbar" @finish="loadUsers">
      <Form.Item label="用户名">
        <Input v-model:value="query.username" allow-clear placeholder="模糊查询" />
      </Form.Item>
      <Form.Item label="状态">
        <Select
          v-model:value="query.status"
          allow-clear
          style="width: 120px"
          :options="[
            { label: '启用', value: 1 },
            { label: '停用', value: 0 },
          ]"
        />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button type="primary" html-type="submit">查询</Button>
          <Button v-if="auth.hasPermission('system:user:create')" type="primary" @click="openCreate">新建</Button>
        </Space>
      </Form.Item>
    </Form>
    <Table
      row-key="id"
      :loading="loading"
      :data-source="users"
      :pagination="{ current: query.current, pageSize: query.size, total, showSizeChanger: true }"
      @change="
        (pag) => {
          query.current = pag.current || 1
          query.size = pag.pageSize || 10
          void loadUsers()
        }
      "
    >
      <Table.Column title="用户名" data-index="username" />
      <Table.Column title="昵称" data-index="nickname" />
      <Table.Column title="状态">
        <template #default="{ record }">
          <Tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '停用' }}</Tag>
        </template>
      </Table.Column>
      <Table.Column title="角色">
        <template #default="{ record }">
          {{ record.roles.map((role: RoleVO) => role.name).join('、') }}
        </template>
      </Table.Column>
      <Table.Column title="最后登录" data-index="lastLoginAt" />
      <Table.Column title="操作" width="320">
        <template #default="{ record }">
          <Space>
            <Button v-if="auth.hasPermission('system:user:update')" type="link" @click="openEdit(record)">编辑</Button>
            <Button v-if="auth.hasPermission('system:user:update')" type="link" @click="openRoles(record)">角色</Button>
            <Button v-if="auth.hasPermission('system:user:update')" type="link" @click="openReset(record)">重置密码</Button>
            <Button v-if="auth.hasPermission('system:user:update')" type="link" @click="toggleStatus(record)">
              {{ record.status === 1 ? '停用' : '启用' }}
            </Button>
            <Popconfirm
              v-if="auth.hasPermission('system:user:delete')"
              title="确认删除该用户？"
              @confirm="removeUser(record)"
            >
              <Button type="link" danger>删除</Button>
            </Popconfirm>
          </Space>
        </template>
      </Table.Column>
    </Table>

    <Modal
      v-model:open="editOpen"
      :title="editMode === 'create' ? '新建用户' : '编辑用户'"
      :confirm-loading="saving"
      @ok="saveUser"
    >
      <Form layout="vertical">
        <Form.Item label="用户名">
          <Input v-model:value="editForm.username" :disabled="editMode === 'edit'" />
        </Form.Item>
        <Form.Item v-if="editMode === 'create'" label="密码">
          <Input.Password v-model:value="editForm.password" placeholder="8-32 位，含字母和数字" />
        </Form.Item>
        <Form.Item label="昵称">
          <Input v-model:value="editForm.nickname" />
        </Form.Item>
        <Form.Item v-if="editMode === 'create'" label="角色">
          <Select
            v-model:value="editForm.roleIds"
            mode="multiple"
            :options="roles.map((role) => ({ label: `${role.name} (${role.code})`, value: role.id }))"
          />
        </Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="roleOpen" title="分配角色" @ok="saveRoles">
      <Select
        v-model:value="roleIds"
        mode="multiple"
        style="width: 100%"
        :options="roles.map((role) => ({ label: `${role.name} (${role.code})`, value: role.id }))"
      />
    </Modal>

    <Modal v-model:open="resetOpen" title="重置密码" @ok="saveReset">
      <Input.Password v-model:value="resetPassword" placeholder="8-32 位，含字母和数字" />
    </Modal>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
