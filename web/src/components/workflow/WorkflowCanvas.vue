<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Button, Drawer, Form, Input, InputNumber, Modal, Select, Space, Switch, Tag, message } from 'ant-design-vue'
import type { WorkflowDefinitionVO, WorkflowGraph, WorkflowNode, WorkflowRunVO } from '@/api/wf'
import { createWorkflowShare, dryRunWorkflow, saveWorkflowGraph, setWorkflowSchedule, validateWorkflow } from '@/api/wf'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  definition: WorkflowDefinitionVO
  readonly?: boolean
}>()

const emit = defineEmits<{
  refreshed: []
}>()

const auth = useAuthStore()
const graph = ref<WorkflowGraph>(cloneGraph(props.definition.graph))
const selectedId = ref<string | null>(null)
const saving = ref(false)
const dryOpen = ref(false)
const dryRunning = ref(false)
const dryInput = ref<Record<string, string>>({})
const lastRun = ref<WorkflowRunVO | null>(null)
const newFrom = ref('start')
const newTo = ref('end')
const newWhen = ref<string | undefined>(undefined)
const shareOpen = ref(false)
const sharePermission = ref<'preview' | 'copy'>('preview')
const expireDays = ref(7)
const shareLink = ref('')

const selected = computed(() => graph.value.nodes.find((n) => n.id === selectedId.value) || null)
const inputSchema = computed(() => graph.value.trigger?.input_schema || {})
const failedNodes = computed(() =>
  new Set((lastRun.value?.steps || []).filter((s) => s.status === 'failed').map((s) => s.nodeId)),
)
const fromNode = computed(() => graph.value.nodes.find((n) => n.id === newFrom.value))
const cronEnabled = computed({
  get: () => (graph.value.trigger?.enabled ?? 1) !== 0,
  set: (v: boolean) => {
    if (!graph.value.trigger) {
      graph.value.trigger = { type: 'cron' }
    }
    graph.value.trigger.enabled = v ? 1 : 0
  },
})

watch(
  () => props.definition,
  (next) => {
    graph.value = cloneGraph(next.graph)
  },
)

function cloneGraph(source?: WorkflowGraph): WorkflowGraph {
  const raw = source || { version: 1, nodes: [], edges: [] }
  return JSON.parse(JSON.stringify(raw)) as WorkflowGraph
}

function configText(node: WorkflowNode) {
  return JSON.stringify(node.config || {}, null, 2)
}

function onConfigChange(text: string) {
  if (!selected.value) {
    return
  }
  try {
    selected.value.config = JSON.parse(text) as Record<string, unknown>
  } catch {
    // 输入中
  }
}

async function persist() {
  if (props.readonly) {
    return
  }
  saving.value = true
  try {
    await saveWorkflowGraph(props.definition.id, graph.value)
    message.success('已保存')
    emit('refreshed')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function doValidate() {
  try {
    await persist()
    await validateWorkflow(props.definition.id)
    message.success('校验通过')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '校验失败')
  }
}

function addEdge() {
  if (!newFrom.value || !newTo.value) {
    return
  }
  const fromType = fromNode.value?.type
  const when = newWhen.value
  if (fromType === 'condition') {
    graph.value.edges = graph.value.edges.filter((e) => !(e.from === newFrom.value && e.when === when))
    graph.value.edges.push({ from: newFrom.value, to: newTo.value, when: when || 'true' })
    return
  }
  if (when === 'error') {
    graph.value.edges = graph.value.edges.filter((e) => !(e.from === newFrom.value && e.when === 'error'))
    graph.value.edges.push({ from: newFrom.value, to: newTo.value, when: 'error' })
    return
  }
  graph.value.edges = graph.value.edges.filter((e) => e.from !== newFrom.value || e.when === 'error')
  graph.value.edges.push({ from: newFrom.value, to: newTo.value })
}

function openDryRun() {
  const next: Record<string, string> = {}
  Object.keys(inputSchema.value).forEach((key) => {
    next[key] = dryInput.value[key] || ''
  })
  dryInput.value = next
  dryOpen.value = true
}

async function runDry() {
  dryRunning.value = true
  try {
    await persist()
    const input: Record<string, unknown> = {}
    for (const [key, value] of Object.entries(dryInput.value)) {
      const type = inputSchema.value[key]
      input[key] = type === 'long' || type === 'int' ? Number(value) : value
    }
    lastRun.value = await dryRunWorkflow(props.definition.id, input)
    message.success(`试跑结束：${lastRun.value.status}`)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '试跑失败')
  } finally {
    dryRunning.value = false
  }
}

function outgoing(id: string) {
  return graph.value.edges
    .filter((e) => e.from === id)
    .map((e) => (e.when ? `${e.when}→${e.to}` : e.to))
    .join(', ')
}

async function doShare() {
  try {
    const vo = await createWorkflowShare(props.definition.id, sharePermission.value, expireDays.value)
    shareLink.value = `${window.location.origin}/wf/share/${vo.token}`
    message.success('已生成分享链接')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '分享失败')
  }
}

async function toggleCron(enabled: boolean) {
  try {
    await setWorkflowSchedule(props.definition.id, enabled)
    cronEnabled.value = enabled
    message.success(enabled ? '定时已启用' : '定时已关停')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '操作失败')
  }
}

function copyLink() {
  if (!shareLink.value) {
    return
  }
  void navigator.clipboard.writeText(shareLink.value)
  message.success('已复制链接')
}
</script>

<template>
  <div class="wf-studio">
    <div class="wf-toolbar">
      <Space>
        <Button v-if="!readonly" size="small" type="primary" :loading="saving" @click="persist">保存图</Button>
        <Button v-if="!readonly" size="small" @click="doValidate">校验</Button>
        <Button v-if="!readonly" size="small" @click="openDryRun">试跑</Button>
        <Button v-if="!readonly" size="small" @click="shareOpen = true">分享</Button>
      </Space>
      <Tag>{{ definition.status }} · v{{ definition.version }}</Tag>
    </div>

    <div v-if="graph.trigger?.type === 'cron' && auth.isSuperAdmin && !readonly" class="cron-bar">
      <span>定时触发</span>
      <Switch size="small" :checked="cronEnabled" @change="(v: boolean) => toggleCron(v)" />
    </div>

    <div class="wf-body">
      <div class="wf-canvas">
        <article
          v-for="node in graph.nodes"
          :key="node.id"
          class="wf-node"
          :class="{
            active: selectedId === node.id,
            failed: failedNodes.has(node.id),
          }"
          @click="selectedId = node.id"
        >
          <div class="wf-node__type">{{ node.type }}</div>
          <div class="wf-node__title">{{ node.title || node.id }}</div>
          <div class="wf-node__id">{{ node.id }} → {{ outgoing(node.id) || '（无出边）' }}</div>
        </article>
      </div>

      <div class="wf-props">
        <h4>节点属性</h4>
        <template v-if="selected">
          <Form layout="vertical" size="small">
            <Form.Item label="标题">
              <Input v-model:value="selected.title" :disabled="readonly" />
            </Form.Item>
            <Form.Item label="config JSON">
              <Input.TextArea :value="configText(selected)" :rows="12" :disabled="readonly" @change="(e) => onConfigChange((e.target as HTMLTextAreaElement).value)" />
            </Form.Item>
          </Form>
        </template>
        <p v-else class="hint">点选左侧节点进行编辑。保存后写回服务端 IR，浏览器不执行 LLM。</p>

        <h4>连线</h4>
        <Space wrap>
          <Select v-model:value="newFrom" style="width: 110px" size="small" :disabled="readonly">
            <Select.Option v-for="n in graph.nodes" :key="n.id" :value="n.id">{{ n.id }}</Select.Option>
          </Select>
          <span>→</span>
          <Select v-model:value="newTo" style="width: 110px" size="small" :disabled="readonly">
            <Select.Option v-for="n in graph.nodes" :key="'t' + n.id" :value="n.id">{{ n.id }}</Select.Option>
          </Select>
          <Select v-model:value="newWhen" style="width: 90px" size="small" allowClear placeholder="when" :disabled="readonly">
            <Select.Option value="true">true</Select.Option>
            <Select.Option value="false">false</Select.Option>
            <Select.Option value="error">error</Select.Option>
          </Select>
          <Button v-if="!readonly" size="small" @click="addEdge">连接</Button>
        </Space>
        <p class="hint">condition 需分别连 true / false；其它节点可额外连 error。</p>
      </div>
    </div>

    <Drawer v-model:open="dryOpen" title="试跑" width="420">
      <Form layout="vertical">
        <Form.Item v-for="(type, key) in inputSchema" :key="key" :label="`${key} (${type})`">
          <Input v-model:value="dryInput[key]" />
        </Form.Item>
        <Button type="primary" :loading="dryRunning" @click="runDry">开始试跑</Button>
      </Form>
      <div v-if="lastRun" class="run-log">
        <p>状态：{{ lastRun.status }} {{ lastRun.errorMsg || '' }}</p>
        <ul>
          <li v-for="step in lastRun.steps" :key="step.id">
            {{ step.nodeId }} {{ step.nodeType }} {{ step.status }} {{ step.durationMs }}ms
            <pre>{{ step.outputSummary || step.errorMsg }}</pre>
          </li>
        </ul>
      </div>
    </Drawer>

    <Modal v-model:open="shareOpen" title="分享工作流" @ok="doShare">
      <Form layout="vertical">
        <Form.Item label="权限">
          <Select v-model:value="sharePermission">
            <Select.Option value="preview">只读预览</Select.Option>
            <Select.Option value="copy">允许复制草稿</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="有效天数">
          <InputNumber v-model:value="expireDays" :min="1" :max="90" />
        </Form.Item>
        <Form.Item v-if="shareLink" label="链接">
          <Space>
            <Input :value="shareLink" readonly />
            <Button @click="copyLink">复制</Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.wf-studio {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 480px;
}
.wf-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cron-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
}
.wf-body {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 12px;
  min-height: 420px;
}
.wf-canvas {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--bg-surface);
  overflow: auto;
}
.wf-node {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  background: var(--bg-elevated);
}
.wf-node.active {
  border-color: #5b8cff;
}
.wf-node.failed {
  border-color: #ff4d4f;
}
.wf-node__type {
  font-size: 12px;
  color: var(--text-3);
}
.wf-node__title {
  font-weight: 600;
}
.wf-node__id {
  font-size: 12px;
  color: var(--text-3);
}
.wf-props {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
}
.hint,
.run-log {
  font-size: 12px;
  color: var(--text-3);
}
.run-log pre {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 4px 0 10px;
}
</style>
