<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { Button, Drawer, Input, Space, Spin, message } from 'ant-design-vue'
import {
  chatStream,
  createSession,
  listMessages,
  type AiAssistantMessageVO,
  type AssistantStructuredPayload,
} from '@/api/assistant'
import { getToken } from '@/utils/token'
import AssistantMessageItem from './AssistantMessageItem.vue'
import AssistantMemoryDrawer from './AssistantMemoryDrawer.vue'

const SESSION_KEY = 'assistant:sessionId'

const open = defineModel<boolean>('open', { default: false })
const memoryOpen = ref(false)

interface DisplayMessage {
  role: string
  content: string
  payloadJson?: string
  structured?: AssistantStructuredPayload | null
}

const sessionId = ref<number | null>(null)
const messages = ref<DisplayMessage[]>([])
const input = ref('')
const streaming = ref(false)
const thinkingTool = ref('')
const listRef = ref<HTMLElement | null>(null)

async function ensureSession() {
  const saved = sessionStorage.getItem(SESSION_KEY)
  if (saved) {
    sessionId.value = Number(saved)
    try {
      const rows = await listMessages(sessionId.value)
      messages.value = rows
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .map(toDisplay)
      return
    } catch {
      sessionStorage.removeItem(SESSION_KEY)
    }
  }
  const id = await createSession()
  sessionId.value = id
  sessionStorage.setItem(SESSION_KEY, String(id))
  messages.value = []
}

function toDisplay(row: AiAssistantMessageVO): DisplayMessage {
  return {
    role: row.role,
    content: row.content || '',
    payloadJson: row.payloadJson,
  }
}

async function scrollBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

async function newChat() {
  const id = await createSession()
  sessionId.value = id
  sessionStorage.setItem(SESSION_KEY, String(id))
  messages.value = []
}

async function send() {
  const text = input.value.trim()
  if (!text || streaming.value) {
    return
  }
  if (!sessionId.value) {
    await ensureSession()
  }
  const sid = sessionId.value!
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  await scrollBottom()

  streaming.value = true
  thinkingTool.value = ''
  let assistantText = ''
  let structured: AssistantStructuredPayload | null = null
  messages.value.push({ role: 'assistant', content: '', structured: null })

  try {
    const token = getToken()
    if (!token) {
      throw new Error('未登录')
    }
    await chatStream(sid, { message: text }, token, (event) => {
      if (event.event === 'text') {
        const chunk = event.data.content
        if (typeof chunk === 'string') {
          assistantText += chunk
          patchLastAssistant(assistantText, structured)
        }
      } else if (event.event === 'tool_start') {
        const tool = event.data.tool
        thinkingTool.value = typeof tool === 'string' ? tool : ''
      } else if (event.event === 'structured') {
        const type = event.data.type
        if (type === 'nav_link' || type === 'oper_timeline' || type === 'action_plan') {
          structured = event.data as AssistantStructuredPayload
          patchLastAssistant(assistantText, structured)
        }
      } else if (event.event === 'error') {
        const msg = event.data.message
        message.error(typeof msg === 'string' ? msg : '对话失败')
      }
    })
    patchLastAssistant(assistantText, structured, true)
  } catch (error) {
    messages.value.pop()
    message.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    streaming.value = false
    thinkingTool.value = ''
    await scrollBottom()
  }
}

function patchLastAssistant(
  content: string,
  structured: AssistantStructuredPayload | null,
  finalize = false,
) {
  const last = messages.value[messages.value.length - 1]
  if (!last || last.role !== 'assistant') {
    return
  }
  last.content = content
  last.structured = structured
  if (finalize && structured) {
    last.payloadJson = JSON.stringify(structured)
  }
}

function toolThinkingLabel(tool: string) {
  switch (tool) {
    case 'search_menus':
      return '正在查菜单…'
    case 'query_my_operations':
    case 'get_operation_timeline':
      return '正在查操作记录…'
    case 'recall_user_memory':
      return '正在回忆…'
    case 'remember_fact':
      return '正在记住…'
    case 'query_my_page_visits':
      return '正在查浏览记录…'
    case 'search_capabilities':
      return '正在匹配功能…'
    case 'prepare_action_draft':
      return '正在准备操作…'
    case 'get_capability_schema':
      return '正在读取表单…'
    default:
      return '正在查询…'
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    void send()
  }
}

watch(open, (val) => {
  if (val) {
    void ensureSession().then(() => scrollBottom())
  }
})

onMounted(() => {
  if (open.value) {
    void ensureSession()
  }
})
</script>

<template>
  <Drawer
    v-model:open="open"
    title="AI 助手"
    placement="right"
    :width="400"
    :mask="false"
    class="assistant-drawer"
  >
    <template #extra>
      <Space>
        <Button size="small" :disabled="streaming" @click="memoryOpen = true">记忆</Button>
        <Button size="small" :disabled="streaming" @click="newChat">新对话</Button>
      </Space>
    </template>

    <div ref="listRef" class="msg-list">
      <div v-if="!messages.length" class="empty-hint">问我系统菜单在哪，或回顾你的操作记录</div>
      <AssistantMessageItem
        v-for="(msg, index) in messages"
        :key="index"
        :role="msg.role"
        :content="msg.content"
        :payload-json="msg.payloadJson"
        :structured="msg.structured"
        @navigate="open = false"
      />
      <div v-if="streaming && thinkingTool" class="thinking">
        <Spin size="small" />
        <span>{{ toolThinkingLabel(thinkingTool) }}</span>
      </div>
    </div>

    <AssistantMemoryDrawer v-model:open="memoryOpen" />

    <div class="composer">
      <Input.TextArea
        v-model:value="input"
        :rows="3"
        placeholder="输入问题，Enter 发送"
        :disabled="streaming"
        @keydown="onKeydown"
      />
      <Button type="primary" block :loading="streaming" :disabled="!input.trim()" @click="send">发送</Button>
    </div>
  </Drawer>
</template>

<style scoped>
.assistant-drawer :deep(.ant-drawer-body) {
  display: flex;
  flex-direction: column;
  padding: 12px;
  height: 100%;
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
  margin-bottom: 12px;
  padding-right: 4px;
}

.empty-hint {
  color: var(--text-3);
  font-size: 13px;
  text-align: center;
  padding: 24px 8px;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-3);
  padding: 4px 0;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--border);
  padding-top: 12px;
}
</style>
