<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button, Result, Spin, message } from 'ant-design-vue'
import PageShell from '@/components/layout/PageShell.vue'
import WorkflowCanvas from '@/components/workflow/WorkflowCanvas.vue'
import { copyWorkflowShare, getWorkflowShare, type WorkflowDefinitionVO, type WorkflowShareVO } from '@/api/wf'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const share = ref<WorkflowShareVO | null>(null)
const copying = ref(false)

const asDefinition = (): WorkflowDefinitionVO | null => {
  if (!share.value?.graph) {
    return null
  }
  return {
    id: 0,
    appId: 0,
    code: share.value.code || '',
    name: share.value.name || '分享预览',
    status: 'SHARED',
    version: 0,
    published: false,
    graph: share.value.graph,
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    share.value = await getWorkflowShare(String(route.params.token || ''))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分享不存在或已过期'
    share.value = null
  } finally {
    loading.value = false
  }
}

async function copy() {
  copying.value = true
  try {
    const id = await copyWorkflowShare(String(route.params.token || ''))
    message.success('已复制为草稿')
    await router.push({ path: '/studio', query: { kind: 'workflow' } })
    return id
  } catch (err) {
    message.error(err instanceof Error ? err.message : '复制失败')
  } finally {
    copying.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageShell :title="share?.name || '工作流分享'" subtitle="只读预览，密钥字段已剥离">
    <Spin :spinning="loading">
      <Result v-if="error" status="warning" :title="error" />
      <div v-else-if="share && asDefinition()">
        <Button v-if="share.permission === 'copy'" type="primary" :loading="copying" style="margin-bottom: 12px" @click="copy">
          复制为草稿
        </Button>
        <WorkflowCanvas :definition="asDefinition()!" readonly />
      </div>
    </Spin>
  </PageShell>
</template>
