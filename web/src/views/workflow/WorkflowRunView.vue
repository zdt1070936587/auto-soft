<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Button, Form, Input, Result, Spin, message } from 'ant-design-vue'
import PageShell from '@/components/layout/PageShell.vue'
import { getPublishedWorkflow, runWorkflow, type WorkflowDefinitionVO, type WorkflowRunVO } from '@/api/wf'

const route = useRoute()
const loading = ref(true)
const running = ref(false)
const def = ref<WorkflowDefinitionVO | null>(null)
const input = reactive<Record<string, string>>({})
const run = ref<WorkflowRunVO | null>(null)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const code = String(route.params.code || '')
    def.value = await getPublishedWorkflow(code)
    const schema = def.value.graph?.trigger?.input_schema || {}
    Object.keys(schema).forEach((key) => {
      if (input[key] === undefined) {
        input[key] = ''
      }
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
    def.value = null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!def.value) {
    return
  }
  running.value = true
  try {
    const payload: Record<string, unknown> = {}
    const schema = def.value.graph?.trigger?.input_schema || {}
    for (const [key, type] of Object.entries(schema)) {
      payload[key] = type === 'long' || type === 'int' ? Number(input[key]) : input[key]
    }
    run.value = await runWorkflow(def.value.code, payload)
    message.success(`运行结束：${run.value.status}`)
  } catch (err) {
    message.error(err instanceof Error ? err.message : '运行失败')
  } finally {
    running.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageShell :title="def?.name || '运行工作流'" :subtitle="def ? `/wf/${def.code}` : ''">
    <Spin :spinning="loading">
      <Result v-if="error" status="warning" :title="error" />
      <div v-else-if="def" class="run-panel">
        <Form layout="vertical" class="run-form">
          <Form.Item
            v-for="(type, key) in def.graph?.trigger?.input_schema || {}"
            :key="key"
            :label="`${key} (${type})`"
          >
            <Input v-model:value="input[key]" />
          </Form.Item>
          <Button type="primary" :loading="running" @click="submit">运行</Button>
        </Form>
        <div v-if="run" class="run-result">
          <p>状态 {{ run.status }} · token {{ run.tokenInput }}/{{ run.tokenOutput }}</p>
          <p v-if="run.errorMsg">{{ run.errorMsg }}</p>
          <ul>
            <li v-for="step in run.steps" :key="step.id">
              {{ step.nodeId }} {{ step.status }} {{ step.durationMs }}ms
              <pre>{{ step.outputSummary || step.errorMsg }}</pre>
            </li>
          </ul>
        </div>
      </div>
    </Spin>
  </PageShell>
</template>

<style scoped>
.run-panel {
  display: grid;
  grid-template-columns: minmax(280px, 420px) 1fr;
  gap: 24px;
}
.run-result pre {
  white-space: pre-wrap;
  word-break: break-word;
}
@media (max-width: 900px) {
  .run-panel {
    grid-template-columns: 1fr;
  }
}
</style>
