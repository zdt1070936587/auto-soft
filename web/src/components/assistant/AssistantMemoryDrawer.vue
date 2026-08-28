<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button, Drawer, Empty, Popconfirm, Space, Spin, Tag, message } from 'ant-design-vue'
import {
  confirmMemoryFact,
  deleteMemoryFact,
  listMemoryFacts,
  type AiMemoryFactVO,
} from '@/api/assistant'

const open = defineModel<boolean>('open', { default: false })

const loading = ref(false)
const facts = ref<AiMemoryFactVO[]>([])

async function loadFacts() {
  loading.value = true
  try {
    facts.value = await listMemoryFacts()
  } catch {
    message.error('加载记忆失败')
  } finally {
    loading.value = false
  }
}

async function onConfirm(id: number) {
  try {
    await confirmMemoryFact(id)
    message.success('已确认')
    await loadFacts()
  } catch {
    message.error('确认失败')
  }
}

async function onDelete(id: number) {
  try {
    await deleteMemoryFact(id)
    message.success('已删除')
    await loadFacts()
  } catch {
    message.error('删除失败')
  }
}

function categoryLabel(category: string) {
  switch (category) {
    case 'PROFILE':
      return '画像'
    case 'PREFERENCE':
      return '偏好'
    case 'PROJECT':
      return '项目'
    default:
      return category
  }
}

function factKeyLabel(key: string) {
  switch (key) {
    case 'name':
      return '姓名'
    case 'role':
      return '职责'
    case 'team':
      return '团队'
    default:
      return key
  }
}

watch(open, (visible) => {
  if (visible) {
    void loadFacts()
  }
})
</script>

<template>
  <Drawer v-model:open="open" title="助手记忆" placement="right" :width="360" :mask="false">
    <Spin :spinning="loading">
      <Empty
        v-if="!loading && !facts.length"
        description="助手还没有记住您的额外信息"
        :image="Empty.PRESENTED_IMAGE_SIMPLE"
      />
      <div v-else class="fact-list">
        <div v-for="fact in facts" :key="fact.id" class="fact-item">
          <div class="fact-head">
            <Space size="small" wrap>
              <Tag>{{ categoryLabel(fact.category) }}</Tag>
              <Tag>{{ factKeyLabel(fact.factKey) }}</Tag>
              <Tag v-if="fact.confirmed === 1" color="green">已确认</Tag>
              <Tag v-else color="orange">待确认</Tag>
            </Space>
            <span v-if="fact.confidence != null" class="confidence">{{ Math.round(fact.confidence * 100) }}%</span>
          </div>
          <div class="fact-value">{{ fact.factValue }}</div>
          <Space class="fact-actions">
            <Button
              v-if="fact.confirmed !== 1"
              size="small"
              type="link"
              @click="onConfirm(fact.id)"
            >
              确认
            </Button>
            <Popconfirm title="确定删除这条记忆？" @confirm="onDelete(fact.id)">
              <Button size="small" type="link" danger>删除</Button>
            </Popconfirm>
          </Space>
        </div>
      </div>
    </Spin>
  </Drawer>
</template>

<style scoped>
.fact-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fact-item {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
}

.fact-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.confidence {
  font-size: 12px;
  color: var(--text-3);
  white-space: nowrap;
}

.fact-value {
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.fact-actions {
  margin-top: 6px;
}
</style>
