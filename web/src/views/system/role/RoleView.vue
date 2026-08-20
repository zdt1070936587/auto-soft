<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Space, Table, Tag, Tree, message } from 'ant-design-vue'
import type { DataNode } from 'ant-design-vue/es/tree'
import { createRole, deleteRole, grantRoleMenus, listRoleMenus, pageRoles, updateRole } from '@/api/role'
import { listMenuTree } from '@/api/menu'
import type { MenuVO, RoleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const roles = ref<RoleVO[]>([])
const total = ref(0)
const query = reactive({
  name: '',
  current: 1,
  size: 10,
})

const editOpen = ref(false)
const editMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const editingId = ref<number | null>(null)
const editForm = reactive({
  code: '',
  name: '',
  remark: '',
  sort: 0,
  status: 1,
})

const grantOpen = ref(false)
const grantRoleId = ref<number | null>(null)
const treeData = ref<DataNode[]>([])
const checkedKeys = ref<number[]>([])
const halfCheckedKeys = ref<number[]>([])

function toTree(menus: MenuVO[]): DataNode[] {
  return menus.map((menu) => ({
    key: menu.id,
    title: menu.menuType === 'BUTTON' ? `${menu.name}（${menu.permission}）` : menu.name,
    children: menu.children?.length ? toTree(menu.children) : undefined,
  }))
}

async function loadRoles() {
  loading.value = true
  try {
    const page = await pageRoles({
      current: query.current,
      size: query.size,
      name: query.name || undefined,
    })
    roles.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editMode.value = 'create'
  editingId.value = null
  editForm.code = ''
  editForm.name = ''
  editForm.remark = ''
  editForm.sort = 0
  editForm.status = 1
  editOpen.value = true
}

function openEdit(record: RoleVO) {
  editMode.value = 'edit'
  editingId.value = record.id
  editForm.code = record.code
  editForm.name = record.name
  editForm.remark = record.remark || ''
  editForm.sort = record.sort
  editForm.status = record.status
  editOpen.value = true
}

async function saveRole() {
  saving.value = true
  try {
    if (editMode.value === 'create') {
      await createRole({
        code: editForm.code,
        name: editForm.name,
        remark: editForm.remark,
        sort: editForm.sort,
        status: editForm.status,
      })
      message.success('已创建')
    } else if (editingId.value != null) {
      await updateRole(editingId.value, {
        name: editForm.name,
        remark: editForm.remark,
        sort: editForm.sort,
        status: editForm.status,
      })
      message.success('已保存')
    }
    editOpen.value = false
    await loadRoles()
  } finally {
    saving.value = false
  }
}

async function openGrant(record: RoleVO) {
  grantRoleId.value = record.id
  const [tree, ids] = await Promise.all([listMenuTree(), listRoleMenus(record.id)])
  treeData.value = toTree(tree)
  checkedKeys.value = ids
  halfCheckedKeys.value = []
  grantOpen.value = true
}

function onTreeCheck(
  keys: (string | number)[] | { checked: (string | number)[]; halfChecked: (string | number)[] },
  info: { halfCheckedKeys?: (string | number)[] },
) {
  const checked = Array.isArray(keys) ? keys : keys.checked
  checkedKeys.value = checked.map(Number)
  halfCheckedKeys.value = (info.halfCheckedKeys || []).map(Number)
}

async function saveGrant() {
  if (grantRoleId.value == null) {
    return
  }
  const menuIds = Array.from(new Set([...checkedKeys.value, ...halfCheckedKeys.value]))
  await grantRoleMenus(grantRoleId.value, menuIds)
  message.success('已保存菜单授权')
  grantOpen.value = false
}

async function removeRole(record: RoleVO) {
  await deleteRole(record.id)
  message.success('已删除')
  await loadRoles()
}

onMounted(() => {
  void loadRoles()
})
</script>

<template>
  <div>
    <Form layout="inline" class="toolbar" @finish="loadRoles">
      <Form.Item label="角色名">
        <Input v-model:value="query.name" allow-clear />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button type="primary" html-type="submit">查询</Button>
          <Button v-if="auth.hasPermission('system:role:create')" type="primary" @click="openCreate">新建</Button>
        </Space>
      </Form.Item>
    </Form>
    <Table
      row-key="id"
      :loading="loading"
      :data-source="roles"
      :pagination="{ current: query.current, pageSize: query.size, total, showSizeChanger: true }"
      @change="
        (pag) => {
          query.current = pag.current || 1
          query.size = pag.pageSize || 10
          void loadRoles()
        }
      "
    >
      <Table.Column title="编码" data-index="code" />
      <Table.Column title="名称" data-index="name" />
      <Table.Column title="备注" data-index="remark" />
      <Table.Column title="排序" data-index="sort" />
      <Table.Column title="内置">
        <template #default="{ record }">
          <Tag v-if="record.builtin === 1">是</Tag>
          <span v-else>否</span>
        </template>
      </Table.Column>
      <Table.Column title="操作" width="240">
        <template #default="{ record }">
          <Space>
            <Button v-if="auth.hasPermission('system:role:update')" type="link" @click="openEdit(record)">编辑</Button>
            <Button v-if="auth.hasPermission('system:role:grant')" type="link" @click="openGrant(record)">分配菜单</Button>
            <Popconfirm
              v-if="auth.hasPermission('system:role:delete') && record.builtin !== 1"
              title="确认删除该角色？"
              @confirm="removeRole(record)"
            >
              <Button type="link" danger>删除</Button>
            </Popconfirm>
          </Space>
        </template>
      </Table.Column>
    </Table>

    <Modal
      v-model:open="editOpen"
      :title="editMode === 'create' ? '新建角色' : '编辑角色'"
      :confirm-loading="saving"
      @ok="saveRole"
    >
      <Form layout="vertical">
        <Form.Item label="编码">
          <Input v-model:value="editForm.code" :disabled="editMode === 'edit'" placeholder="大写字母开头，如 OPS" />
        </Form.Item>
        <Form.Item label="名称">
          <Input v-model:value="editForm.name" />
        </Form.Item>
        <Form.Item label="备注">
          <Input v-model:value="editForm.remark" />
        </Form.Item>
        <Form.Item label="排序">
          <InputNumber v-model:value="editForm.sort" :min="0" />
        </Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="grantOpen" title="分配菜单" @ok="saveGrant">
      <Tree
        checkable
        default-expand-all
        :tree-data="treeData"
        :checked-keys="checkedKeys"
        @check="onTreeCheck"
      />
    </Modal>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
