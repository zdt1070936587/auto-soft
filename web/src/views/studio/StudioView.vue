<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button, Card, Empty, Input, Modal, Select, Space, Spin, Tag, message } from 'ant-design-vue'
import SchemaRenderer from '@/components/schema/SchemaRenderer.vue'
import { publishMetaApp, type RuntimeSchemaVO } from '@/api/meta'
import {
  chatStream,
  createSession,
  getSessionSchema,
  listMessages,
  listSessions,
  type AiMessageVO,
  type AiSessionVO,
} from '@/api/studio'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const sessions = ref<AiSessionVO[]>([])
const currentId = ref<number | null>(null)
const current = computed(() => sessions.value.find((item) => item.id === currentId.value) || null)
const messages = ref<AiMessageVO[]>([])
const schema = ref<RuntimeSchemaVO | null>(null)
const input = ref('')
const sending = ref(false)
const loading = ref(false)
const tools = ref<string[]>([])

async function loadSessions() {
  sessions.value = await listSessions()
  if (currentId.value == null && sessions.value.length) {
    currentId.value = sessions.value[0].id
  }
}

async function loadDetail() {
  if (currentId.value == null) {
    messages.value = []
    schema.value = null
    return
  }
  loading.value = true
  try {
    messages.value = await listMessages(currentId.value)
    schema.value = await getSessionSchema(currentId.value)
  } finally {
    loading.value = false
  }
}

async function newSession() {
  currentId.value = await createSession()
  await loadSessions()
  await loadDetail()
}

function onSessionChange(value: unknown) {
  currentId.value = Number(value)
  void loadDetail()
}

async function send() {
  if (!input.value.trim()) {
    return
  }
  if (currentId.value == null) {
    await newSession()
  }
  const sessionId = currentId.value
  if (sessionId == null) {
    return
  }
  const text = input.value.trim()
  input.value = ''
  messages.value.push({ id: Date.now(), role: 'user', content: text })
  sending.value = true
  tools.value = []
  try {
    await chatStream(sessionId, text, auth.token || '', (event) => {
      if (event.event === 'text') {
        const content = String(event.data.content || '')
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant' && last.id < 0) {
          last.content = (last.content || '') + content
        } else {
          messages.value.push({ id: -Date.now(), role: 'assistant', content })
        }
      } else if (event.event === 'tool_start') {
        tools.value.push(`正在调用 ${String(event.data.tool || '')}…`)
      } else if (event.event === 'tool_end') {
        tools.value.push(`${String(event.data.tool || '')} ${event.data.success ? '完成' : '失败'}`)
      } else if (event.event === 'schema_updated') {
        void refreshSchema(sessionId)
      } else if (event.event === 'error') {
        message.error(String(event.data.message || '对话失败'))
      } else if (event.event === 'done') {
        void loadSessions()
      }
    })
    await loadDetail()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '对话失败')
  } finally {
    sending.value = false
  }
}

async function refreshSchema(sessionId: number) {
  schema.value = await getSessionSchema(sessionId)
}

function publish() {
  if (!current.value?.appId) {
    message.warning('当前会话还没有应用')
    return
  }
  Modal.confirm({
    title: '确认发布？',
    content: '将创建动态表并生成菜单。USER 需重新登录或刷新菜单后可见。已加列不会删除。',
    async onOk() {
      await publishMetaApp(current.value!.appId as number)
      message.success('已发布')
      await refreshSchema(current.value!.id)
    },
  })
}

onMounted(async () => {
  await loadSessions()
  await loadDetail()
})
</script>

<template>
  <div class="studio">
    <Card class="left" title="功能开发">
      <template #extra>
        <Space>
          <span v-if="current" class="tokens">input {{ current.tokenInput || 0 }} / output {{ current.tokenOutput || 0 }}</span>
          <Button size="small" @click="newSession">新会话</Button>
          <Button size="small" type="primary" :disabled="!current?.appId" @click="publish">发布</Button>
        </Space>
      </template>
      <Select
        v-if="sessions.length"
        style="width: 100%; margin-bottom: 12px"
        :value="currentId ?? undefined"
        :options="sessions.map((item) => ({ value: item.id, label: item.title }))"
        @change="onSessionChange"
      />
      <Spin :spinning="loading">
        <div class="msgs">
          <Empty v-if="!messages.length" description="描述你要的功能，例如：做请假单，字段请假天数、原因，提交后要 ADMIN 审批" />
          <div v-for="msg in messages" :key="msg.id" class="msg" :class="msg.role">
            <Tag v-if="msg.role === 'tool'">{{ msg.toolName }}</Tag>
            <pre>{{ msg.content }}</pre>
          </div>
          <div v-for="(tip, idx) in tools" :key="idx" class="tool-tip">{{ tip }}</div>
        </div>
      </Spin>
      <Input.TextArea v-model:value="input" :rows="3" placeholder="输入需求，Enter 发送 Ctrl+Enter 换行" :disabled="sending" @keydown.enter.exact.prevent="send" />
      <Button type="primary" block :loading="sending" style="margin-top: 8px" @click="send">发送</Button>
    </Card>
    <Card class="right" title="预览">
      <Empty v-if="!schema" description="右侧将显示当前草稿。确认字段后可点发布。" />
      <SchemaRenderer v-else :schema="schema" :preview="true" />
    </Card>
  </div>
</template>

<style scoped>
.studio {
  display: grid;
  grid-template-columns: 40% 1fr;
  gap: 16px;
  min-height: calc(100vh - 140px);
}
.left,
.right {
  min-height: 0;
}
.msgs {
  height: 420px;
  overflow: auto;
  margin-bottom: 12px;
  background: #fafafa;
  padding: 8px;
  border-radius: 8px;
}
.msg pre {
  white-space: pre-wrap;
  margin: 0;
}
.msg.user {
  color: #1677ff;
}
.tool-tip {
  font-size: 12px;
  color: #888;
}
.tokens {
  font-size: 12px;
  color: #888;
}
</style>
