<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Button, Card, Form, Input, Select, Space, Typography, message } from 'ant-design-vue'
import { getLlmConfig, listLlmModels, saveLlmConfig, type LlmModelVO } from '@/api/llm'

const loading = ref(false)
const models = ref<LlmModelVO[]>([])
const form = reactive({
  apiKey: '',
  defaultModel: 'kimi-k2.7-code',
  keyConfigured: false,
  keyMask: '',
})

async function load() {
  loading.value = true
  try {
    const config = await getLlmConfig()
    form.defaultModel = config.defaultModel || 'kimi-k2.7-code'
    form.keyConfigured = config.keyConfigured
    form.keyMask = config.keyMask || ''
    form.apiKey = ''
    if (config.keyConfigured) {
      try {
        models.value = await listLlmModels()
      } catch {
        models.value = []
      }
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  await saveLlmConfig({
    apiKey: form.apiKey || undefined,
    defaultModel: form.defaultModel,
  })
  message.success('已保存，Key 不会回显')
  await load()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <Card title="模型设置">
    <Typography.Paragraph>
      OpenCode Go 额度约 $12/5h、$30/周、$60/月。优先选择 Chat Completions 模型（如 kimi-k2.7-code），不要默认选 kimi-k3。
      遇到 429 请更换模型或稍后重试。API Key 加密存储，接口只回显掩码。
    </Typography.Paragraph>
    <Form layout="vertical" style="max-width: 520px">
      <Form.Item :label="form.keyConfigured ? `API Key（已配置 ${form.keyMask}）` : 'API Key'">
        <Input.Password v-model:value="form.apiKey" placeholder="只写不回显，留空表示不修改" />
      </Form.Item>
      <Form.Item label="默认模型">
        <Select
          v-model:value="form.defaultModel"
          show-search
          :options="(models.length ? models : [{ id: form.defaultModel }]).map((item) => ({ value: item.id, label: item.id }))"
        />
      </Form.Item>
      <Space>
        <Button type="primary" :loading="loading" @click="save">保存</Button>
        <Button :loading="loading" @click="load">刷新模型列表</Button>
      </Space>
    </Form>
  </Card>
</template>
