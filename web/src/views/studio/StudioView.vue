<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { Button, Card, Empty, Input, Modal, Select, Space, Spin, message } from 'ant-design-vue'
import SchemaRenderer from '@/components/schema/SchemaRenderer.vue'
import { publishMetaApp, type RuntimeSchemaVO } from '@/api/meta'
import {
  chatStream,
  createSession,
  extractAskUserQuestion,
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
const inputRef = ref<{ focus?: () => void; resizableTextArea?: { textArea?: HTMLTextAreaElement } } | null>(null)
const msgsEl = ref<HTMLElement | null>(null)

/** 本轮思考过程时间线 */
const thinkingSteps = ref<string[]>([])
const thinkingOpen = ref(true)
const thinkingDone = ref(false)

const visibleMessages = computed(() => {
  const list = messages.value
  return list.filter((msg, index) => {
    if (msg.role === 'tool' && msg.toolName !== 'ask_user') {
      return false
    }
    // 新后端会紧跟持久化 assistant(ask_user)，避免与 tool 结果重复出两张卡
    if (msg.role === 'tool' && msg.toolName === 'ask_user') {
      const next = list[index + 1]
      if (next?.role === 'assistant' && next.toolName === 'ask_user') {
        return false
      }
    }
    return true
  })
})

const pendingConfirm = computed(() => {
  for (let i = visibleMessages.value.length - 1; i >= 0; i -= 1) {
    const msg = visibleMessages.value[i]
    if (isConfirmMessage(msg)) {
      return msg
    }
    if (msg.role === 'user') {
      break
    }
  }
  return null
})

function isConfirmMessage(msg: AiMessageVO): boolean {
  if (msg.role === 'assistant' && msg.toolName === 'ask_user') {
    return true
  }
  if (msg.role === 'tool' && msg.toolName === 'ask_user') {
    return true
  }
  return false
}

function confirmQuestion(msg: AiMessageVO): string {
  const raw = msg.content || ''
  if (msg.role === 'assistant' && msg.toolName === 'ask_user') {
    return raw
  }
  try {
    const parsed = JSON.parse(raw) as { question?: string }
    if (parsed.question) {
      return parsed.question
    }
  } catch {
    // ignore
  }
  const match = raw.match(/"question"\s*:\s*"((?:\\.|[^"\\])*)"/)
  if (match?.[1]) {
    return match[1].replace(/\\n/g, '\n').replace(/\\"/g, '"').replace(/\\\\/g, '\\')
  }
  return raw
}

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
  thinkingSteps.value = []
  thinkingDone.value = false
}

function onSessionChange(value: unknown) {
  currentId.value = Number(value)
  thinkingSteps.value = []
  thinkingDone.value = false
  void loadDetail()
}

async function scrollToBottom() {
  await nextTick()
  if (msgsEl.value) {
    msgsEl.value.scrollTop = msgsEl.value.scrollHeight
  }
}

async function send(overrideText?: string) {
  const text = (overrideText ?? input.value).trim()
  if (!text) {
    return
  }
  if (sending.value) {
    return
  }
  if (currentId.value == null) {
    await newSession()
  }
  const sessionId = currentId.value
  if (sessionId == null) {
    return
  }
  if (!overrideText) {
    input.value = ''
  }
  messages.value.push({ id: Date.now(), role: 'user', content: text })
  sending.value = true
  thinkingSteps.value = ['正在理解需求…']
  thinkingOpen.value = true
  thinkingDone.value = false
  await scrollToBottom()
  try {
    await chatStream(sessionId, text, auth.token || '', (event) => {
      if (event.event === 'text') {
        const content = String(event.data.content || '')
        thinkingDone.value = true
        thinkingOpen.value = false
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant' && last.id < 0 && last.toolName !== 'ask_user') {
          last.content = (last.content || '') + content
        } else {
          messages.value.push({ id: -Date.now(), role: 'assistant', content })
        }
        void scrollToBottom()
      } else if (event.event === 'ask_user') {
        const question = extractAskUserQuestion(event.data)
        thinkingDone.value = true
        thinkingOpen.value = false
        sending.value = false
        messages.value.push({
          id: -Date.now(),
          role: 'assistant',
          content: question,
          toolName: 'ask_user',
        })
        void scrollToBottom()
      } else if (event.event === 'tool_start') {
        const tool = String(event.data.tool || '')
        thinkingSteps.value.push(`正在调用 ${tool}…`)
        void scrollToBottom()
      } else if (event.event === 'tool_end') {
        const tool = String(event.data.tool || '')
        thinkingSteps.value.push(`${tool} ${event.data.success ? '完成' : '失败'}`)
        void scrollToBottom()
      } else if (event.event === 'schema_updated') {
        void refreshSchema(sessionId)
      } else if (event.event === 'error') {
        message.error(String(event.data.message || '对话失败'))
      } else if (event.event === 'done') {
        void loadSessions()
      }
    })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '对话失败')
  } finally {
    sending.value = false
    thinkingDone.value = true
    void loadDetail().then(() => scrollToBottom())
  }
}

function confirmAskUser() {
  void send('确认')
}

function reviseAskUser() {
  input.value = '我有修改意见：'
  void nextTick(() => {
    const el = inputRef.value?.resizableTextArea?.textArea
    if (el) {
      el.focus()
      el.setSelectionRange(el.value.length, el.value.length)
      return
    }
    inputRef.value?.focus?.()
  })
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
    <Card class="left chat-card" :bordered="false">
      <template #title>
        <div class="chat-header">
          <span class="chat-title">功能开发</span>
          <span v-if="current" class="tokens">input {{ current.tokenInput || 0 }} / output {{ current.tokenOutput || 0 }}</span>
        </div>
      </template>
      <template #extra>
        <Space>
          <Button size="small" @click="newSession">新会话</Button>
          <Button size="small" type="primary" :disabled="!current?.appId" @click="publish">发布</Button>
        </Space>
      </template>

      <div class="chat-body">
        <Select
          v-if="sessions.length"
          class="session-select"
          :value="currentId ?? undefined"
          :options="sessions.map((item) => ({ value: item.id, label: item.title }))"
          @change="onSessionChange"
        />

        <Spin :spinning="loading">
          <div ref="msgsEl" class="msgs">
            <Empty
              v-if="!visibleMessages.length && !sending"
              description="描述你要的功能，例如：做请假单，字段请假天数、原因，提交后要 ADMIN 审批"
            />

            <div
              v-for="msg in visibleMessages"
              :key="msg.id"
              class="msg-row"
              :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
            >
              <div v-if="isConfirmMessage(msg)" class="confirm-card">
                <div class="confirm-title">需要确认</div>
                <pre class="confirm-body">{{ confirmQuestion(msg) }}</pre>
                <div v-if="pendingConfirm?.id === msg.id && !sending" class="confirm-actions">
                  <Button type="primary" size="small" @click="confirmAskUser">确认</Button>
                  <Button size="small" @click="reviseAskUser">提出修改</Button>
                </div>
              </div>
              <div v-else class="bubble" :class="msg.role">
                <pre>{{ msg.content }}</pre>
              </div>
            </div>

            <div v-if="sending || thinkingSteps.length" class="thinking">
              <button type="button" class="thinking-toggle" @click="thinkingOpen = !thinkingOpen">
                <span class="thinking-label">
                  <span v-if="sending && !thinkingDone" class="dots" aria-hidden="true">
                    <i /><i /><i />
                  </span>
                  {{ sending && !thinkingDone ? '思考中' : '已思考' }}
                </span>
                <span class="thinking-chevron">{{ thinkingOpen ? '收起' : '展开' }}</span>
              </button>
              <ul v-show="thinkingOpen" class="thinking-steps">
                <li v-for="(step, idx) in thinkingSteps" :key="idx">{{ step }}</li>
              </ul>
            </div>
          </div>
        </Spin>

        <div class="composer">
          <Input.TextArea
            ref="inputRef"
            v-model:value="input"
            :rows="3"
            :disabled="sending"
            placeholder="输入需求，Enter 发送 Ctrl+Enter 换行"
            @keydown.enter.exact.prevent="send()"
          />
          <Button type="primary" class="send-btn" :loading="sending" :disabled="sending || !input.trim()" @click="send()">
            发送
          </Button>
        </div>
      </div>
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
  grid-template-columns: minmax(360px, 42%) 1fr;
  gap: 16px;
  min-height: calc(100vh - 140px);
  align-items: stretch;
}

.left,
.right {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-top: 12px;
}

.chat-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.chat-title {
  font-weight: 600;
}

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  gap: 12px;
}

.session-select {
  width: 100%;
}

.msgs {
  flex: 1;
  min-height: 320px;
  max-height: calc(100vh - 320px);
  overflow: auto;
  padding: 16px 12px;
  border-radius: 16px;
  background:
    radial-gradient(1200px 400px at 10% -10%, rgba(91, 140, 255, 0.1), transparent 60%),
    linear-gradient(180deg, var(--bg-elevated) 0%, var(--bg-surface) 100%);
  border: 1px solid var(--border);
}

.msg-row {
  display: flex;
  margin-bottom: 14px;
}

.msg-row.is-user {
  justify-content: flex-end;
}

.msg-row.is-assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 88%;
  padding: 10px 14px;
  border-radius: 16px;
  border: 1px solid var(--border);
}

.bubble pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

.bubble.user {
  background: rgba(91, 140, 255, 0.18);
  color: var(--text-1);
  border-color: rgba(91, 140, 255, 0.28);
  border-bottom-right-radius: 6px;
}

.bubble.assistant {
  background: var(--bg-elevated);
  color: var(--text-1);
  border-bottom-left-radius: 6px;
}

.confirm-card {
  max-width: 92%;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px 16px;
}

.confirm-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  margin-bottom: 8px;
}

.confirm-body {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.65;
  color: var(--text-1);
  background: var(--bg-surface);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid var(--border);
}

.confirm-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.thinking {
  margin: 4px 0 12px;
  max-width: 92%;
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  overflow: hidden;
}

.thinking-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 0;
  background: transparent;
  padding: 10px 12px;
  cursor: pointer;
  color: var(--text-3);
  font-size: 13px;
}

.thinking-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.thinking-chevron {
  font-size: 12px;
  color: var(--text-3);
}

.thinking-steps {
  margin: 0;
  padding: 0 12px 12px 28px;
  color: var(--text-3);
  font-size: 12px;
  line-height: 1.7;
}

.dots {
  display: inline-flex;
  gap: 3px;
}

.dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--text-3);
  display: inline-block;
  animation: pulse 1.2s infinite ease-in-out;
}

.dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes pulse {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.composer {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px;
  border-radius: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
}

.composer :deep(textarea) {
  border: 0 !important;
  box-shadow: none !important;
  resize: none;
  padding: 4px 0;
}

.send-btn {
  flex-shrink: 0;
  height: 36px;
  border-radius: 10px;
  padding-inline: 18px;
}

.tokens {
  font-size: 12px;
  color: var(--text-3);
  font-weight: 400;
}
</style>
