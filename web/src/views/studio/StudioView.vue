<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { PaperClipOutlined, CloseOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { Button, Card, Empty, Input, Modal, Segmented, Select, Space, Spin, Tag, message } from 'ant-design-vue'
import PageRenderer from '@/components/page/PageRenderer.vue'
import { publishMetaApp, type PageViewVO } from '@/api/meta'
import {
  chatStream,
  createSession,
  deleteSession,
  extractAskUserQuestion,
  getSessionSchema,
  listMessages,
  listSessions,
  pauseSession,
  updateSessionMode,
  uploadAttachment,
  type AgentMode,
  type AiMessageVO,
  type AiSessionVO,
  type PendingAttachment,
} from '@/api/studio'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const sessions = ref<AiSessionVO[]>([])
const currentId = ref<number | null>(null)
const current = computed(() => sessions.value.find((item) => item.id === currentId.value) || null)
const messages = ref<AiMessageVO[]>([])
const pageView = ref<PageViewVO | null>(null)
const input = ref('')
const sending = ref(false)
const paused = ref(false)
const pauseRequested = ref(false)
const loading = ref(false)
const previewRefreshing = ref(false)
const uploading = ref(false)
const agentMode = ref<AgentMode>('develop')
const pendingAttachments = ref<PendingAttachment[]>([])
const inputRef = ref<{ focus?: () => void; resizableTextArea?: { textArea?: HTMLTextAreaElement } } | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const msgsEl = ref<HTMLElement | null>(null)

const thinkingSteps = ref<string[]>([])
const thinkingOpen = ref(true)
const thinkingDone = ref(false)

const modeOptions = [
  { label: '讨论', value: 'discuss' },
  { label: '计划', value: 'plan' },
  { label: '开发', value: 'develop' },
]

const visibleMessages = computed(() => {
  const list = messages.value
  return list.filter((msg, index) => {
    if (msg.role === 'tool' && msg.toolName !== 'ask_user') {
      return false
    }
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

const canSend = computed(() => {
  if (sending.value) {
    return false
  }
  return Boolean(input.value.trim()) || pendingAttachments.value.length > 0 || paused.value
})

const thinkingTitle = computed(() => {
  if (paused.value) {
    return '已暂停'
  }
  if (sending.value && !thinkingDone.value) {
    return pauseRequested.value ? '暂停中…' : '思考中'
  }
  return '已思考'
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
  syncModeFromCurrent()
}

function syncModeFromCurrent() {
  if (current.value?.agentMode) {
    agentMode.value = current.value.agentMode
  }
}

async function loadDetail() {
  if (currentId.value == null) {
    messages.value = []
    pageView.value = null
    return
  }
  loading.value = true
  try {
    messages.value = await listMessages(currentId.value)
    pageView.value = await getSessionSchema(currentId.value)
    syncModeFromCurrent()
  } finally {
    loading.value = false
  }
}

async function newSession() {
  currentId.value = await createSession()
  pendingAttachments.value = []
  paused.value = false
  pauseRequested.value = false
  await loadSessions()
  await loadDetail()
  thinkingSteps.value = []
  thinkingDone.value = false
}

function confirmDeleteSession(session: AiSessionVO) {
  if (sending.value) {
    message.warning('生成中无法删除，请先暂停或等待完成')
    return
  }
  Modal.confirm({
    title: '删除会话？',
    content: `将删除「${session.title || '该会话'}」的全部聊天记录，不可恢复。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      return performDeleteSession(session.id)
    },
  })
}

async function performDeleteSession(targetId: number) {
  try {
    await deleteSession(targetId)
    message.success('会话已删除')
    const deletingCurrent = currentId.value === targetId
    sessions.value = sessions.value.filter((item) => item.id !== targetId)
    if (deletingCurrent) {
      if (sessions.value.length) {
        currentId.value = sessions.value[0].id
      } else {
        currentId.value = await createSession()
        await loadSessions()
      }
      pendingAttachments.value = []
      paused.value = false
      pauseRequested.value = false
      thinkingSteps.value = []
      thinkingDone.value = false
      await loadDetail()
    }
  } catch (error) {
    const text = error instanceof Error ? error.message : '删除失败'
    message.error(text)
    return Promise.reject(error)
  }
}

function onSessionChange(value: unknown) {
  currentId.value = Number(value)
  pendingAttachments.value = []
  paused.value = false
  pauseRequested.value = false
  thinkingSteps.value = []
  thinkingDone.value = false
  void loadDetail()
}

async function onModeChange(value: string | number) {
  const mode = String(value) as AgentMode
  agentMode.value = mode
  if (currentId.value == null) {
    return
  }
  try {
    await updateSessionMode(currentId.value, mode)
    await loadSessions()
  } catch {
    syncModeFromCurrent()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (msgsEl.value) {
    msgsEl.value.scrollTop = msgsEl.value.scrollHeight
  }
}

async function ensureSession() {
  if (currentId.value == null) {
    await newSession()
  }
  return currentId.value!
}

async function pickAttachment() {
  const sessionId = await ensureSession()
  if (!sessionId) {
    return
  }
  fileInputRef.value?.click()
}

async function onFilesSelected(event: Event) {
  const inputEl = event.target as HTMLInputElement
  const files = inputEl.files
  if (!files?.length) {
    return
  }
  const sessionId = await ensureSession()
  if (!sessionId) {
    return
  }
  if (pendingAttachments.value.length + files.length > 5) {
    message.warning('单次最多 5 个附件')
    inputEl.value = ''
    return
  }
  uploading.value = true
  try {
    for (const file of Array.from(files)) {
      const uploaded = await uploadAttachment(sessionId, file)
      pendingAttachments.value.push({
        id: uploaded.id,
        fileName: uploaded.fileName,
        kind: uploaded.kind,
      })
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '附件上传失败')
  } finally {
    uploading.value = false
    inputEl.value = ''
  }
}

function removeAttachment(id: number) {
  pendingAttachments.value = pendingAttachments.value.filter((item) => item.id !== id)
}

async function requestPause() {
  if (!sending.value || pauseRequested.value || currentId.value == null) {
    return
  }
  pauseRequested.value = true
  thinkingSteps.value.push('已请求暂停，当前工具完成后停止…')
  try {
    await pauseSession(currentId.value)
  } catch (error) {
    pauseRequested.value = false
    message.error(error instanceof Error ? error.message : '暂停失败')
  }
}

async function send(overrideText?: string) {
  let text = (overrideText ?? input.value).trim()
  if (!text && paused.value && !overrideText) {
    text = '继续'
  }
  if (!text && !pendingAttachments.value.length) {
    return
  }
  if (sending.value) {
    return
  }
  const sessionId = await ensureSession()
  const attachmentIds = pendingAttachments.value.map((item) => item.id)
  const displayAttachments = [...pendingAttachments.value]
  if (!overrideText) {
    input.value = ''
  }
  pendingAttachments.value = []
  paused.value = false
  pauseRequested.value = false
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: text,
    attachments: displayAttachments.map((item) => ({
      id: item.id,
      fileName: item.fileName,
      contentType: '',
      sizeBytes: 0,
      kind: item.kind,
    })),
  })
  sending.value = true
  thinkingSteps.value = ['正在理解需求…']
  thinkingOpen.value = true
  thinkingDone.value = false
  await scrollToBottom()
  try {
    await chatStream(
      sessionId,
      {
        message: text || '请结合附件继续处理。',
        agentMode: agentMode.value,
        attachmentIds,
      },
      auth.token || '',
      (event) => {
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
        } else if (event.event === 'paused') {
          paused.value = true
          thinkingDone.value = true
          const content = String(event.data.message || '已暂停')
          messages.value.push({ id: -Date.now(), role: 'assistant', content })
          void scrollToBottom()
        } else if (event.event === 'error') {
          thinkingDone.value = true
          message.error(String(event.data.message || '对话失败'))
        } else if (event.event === 'done') {
          thinkingDone.value = true
          if (typeof event.data.agentMode === 'string') {
            agentMode.value = event.data.agentMode as AgentMode
          }
          void loadSessions()
        }
      },
    )
  } catch (error) {
    if (!(error instanceof DOMException && error.name === 'AbortError')) {
      message.error(error instanceof Error ? error.message : '对话失败')
    }
  } finally {
    sending.value = false
    pauseRequested.value = false
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
  pageView.value = await getSessionSchema(sessionId)
}

async function refreshPreview() {
  if (currentId.value == null) {
    message.info('请先选择或创建会话')
    return
  }
  previewRefreshing.value = true
  try {
    await refreshSchema(currentId.value)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '刷新预览失败')
  } finally {
    previewRefreshing.value = false
  }
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

watch(current, () => syncModeFromCurrent())

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
          @change="onSessionChange"
        >
          <Select.Option
            v-for="item in sessions"
            :key="item.id"
            :value="item.id"
            :label="item.title"
          >
            <div class="session-option">
              <span class="session-option__label">{{ item.title }}</span>
              <CloseOutlined
                class="session-option__remove"
                title="删除会话"
                @mousedown.prevent.stop
                @click.stop="confirmDeleteSession(item)"
              />
            </div>
          </Select.Option>
        </Select>

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
                <div v-if="msg.attachments?.length" class="attachment-list">
                  <Tag v-for="file in msg.attachments" :key="file.id" class="attachment-tag">
                    {{ file.kind === 'image' ? '图片' : '文本' }} · {{ file.fileName }}
                  </Tag>
                </div>
              </div>
            </div>

            <div v-if="sending || thinkingSteps.length" class="thinking">
              <div class="thinking-head">
                <button type="button" class="thinking-toggle" @click="thinkingOpen = !thinkingOpen">
                  <span class="thinking-label">
                    <span v-if="sending && !thinkingDone" class="dots" aria-hidden="true">
                      <i /><i /><i />
                    </span>
                    {{ thinkingTitle }}
                  </span>
                  <span class="thinking-chevron">{{ thinkingOpen ? '收起' : '展开' }}</span>
                </button>
                <Button
                  v-if="sending && !pauseRequested"
                  size="small"
                  class="pause-btn"
                  @click="requestPause"
                >
                  暂停
                </Button>
              </div>
              <ul v-show="thinkingOpen" class="thinking-steps">
                <li v-for="(step, idx) in thinkingSteps" :key="idx">{{ step }}</li>
              </ul>
            </div>
          </div>
        </Spin>

        <div class="composer">
          <div class="composer-main">
            <div class="composer-toolbar">
              <Segmented
                v-model:value="agentMode"
                size="small"
                :options="modeOptions"
                :disabled="sending"
                @change="onModeChange"
              />
              <Button
                size="small"
                type="text"
                class="attach-btn"
                :loading="uploading"
                :disabled="sending || pendingAttachments.length >= 5"
                @click="pickAttachment"
              >
                <PaperClipOutlined />
                附件
              </Button>
            </div>

            <div v-if="pendingAttachments.length" class="pending-attachments">
              <Tag
                v-for="file in pendingAttachments"
                :key="file.id"
                closable
                class="attachment-tag"
                @close.prevent="removeAttachment(file.id)"
              >
                {{ file.kind === 'image' ? '图片' : '文本' }} · {{ file.fileName }}
              </Tag>
            </div>

            <Input.TextArea
              ref="inputRef"
              v-model:value="input"
              :rows="3"
              :disabled="sending"
              placeholder="输入需求，Enter 发送 Ctrl+Enter 换行"
              @keydown.enter.exact.prevent="send()"
            />
          </div>

          <Button
            type="primary"
            class="send-btn"
            :loading="sending"
            :disabled="!canSend"
            @click="send()"
          >
            {{ paused ? '继续' : '发送' }}
          </Button>
        </div>

        <input
          ref="fileInputRef"
          class="hidden-file-input"
          type="file"
          multiple
          accept=".json,.txt,.md,.csv,.png,.jpg,.jpeg,.webp,.gif,image/*"
          @change="onFilesSelected"
        />
      </div>
    </Card>

    <Card class="right" title="预览">
      <template #extra>
        <Button
          size="small"
          :loading="previewRefreshing"
          :disabled="currentId == null"
          @click="refreshPreview"
        >
          <template #icon><ReloadOutlined /></template>
          刷新
        </Button>
      </template>
      <Spin :spinning="previewRefreshing">
        <Empty v-if="!pageView" description="右侧将显示当前草稿。确认方案后可点发布。" />
        <PageRenderer v-else :view="pageView" :preview="true" />
      </Spin>
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

.session-select :deep(.ant-select-item-option-content) {
  width: 100%;
}

.session-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-option__label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-option__remove {
  flex-shrink: 0;
  color: #ff4d4f;
  font-size: 12px;
  padding: 2px;
  opacity: 0.85;
}

.session-option__remove:hover {
  color: #ff7875;
  opacity: 1;
}

.msgs {
  flex: 1;
  min-height: 320px;
  max-height: calc(100vh - 360px);
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

.attachment-list,
.pending-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.attachment-tag {
  margin: 0;
  border-radius: 999px;
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

.thinking-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-right: 8px;
}

.thinking-toggle {
  flex: 1;
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

.pause-btn {
  flex-shrink: 0;
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

.composer-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.composer :deep(textarea) {
  border: 0 !important;
  box-shadow: none !important;
  resize: none;
  padding: 4px 0;
}

.attach-btn {
  color: var(--text-2);
}

.send-btn {
  flex-shrink: 0;
  height: 36px;
  border-radius: 10px;
  padding-inline: 18px;
}

.hidden-file-input {
  display: none;
}

.tokens {
  font-size: 12px;
  color: var(--text-3);
  font-weight: 400;
}
</style>
