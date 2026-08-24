<script setup lang="ts">
import { inject } from 'vue'
import { Button, Card, Divider, Input, Space } from 'ant-design-vue'
import { LOWCODE_RUN_ACTION } from '@/lowcode/inject'
import type { LowCodeBlock, LowCodeToolbarItem } from '@/lowcode/types'

defineProps<{
  block: LowCodeBlock
  state: Record<string, string>
  h5?: boolean
}>()

const emit = defineEmits<{
  toolbar: [item: LowCodeToolbarItem]
}>()

const runAction = inject<(action: string, params: Record<string, unknown>) => void>(LOWCODE_RUN_ACTION)

function runBlockAction(block: LowCodeBlock) {
  if (!block.action || !runAction) {
    return
  }
  runAction(block.action, {
    from: block.from,
    to: block.to,
    bind: block.bind,
  })
}
</script>

<template>
  <Divider v-if="block.type === 'divider'" />

  <Card v-else-if="block.type === 'card'" :title="block.title" class="lowcode-card">
    <LowCodeBlockView
      v-for="(child, index) in block.blocks || []"
      :key="child.id || `${child.type}-${index}`"
      :block="child"
      :state="state"
      :h5="h5"
      @toolbar="emit('toolbar', $event)"
    />
  </Card>

  <div v-else-if="block.type === 'textarea'" class="lowcode-field">
    <label v-if="block.label" class="lowcode-label">{{ block.label }}</label>
    <Input.TextArea
      :value="block.bind ? state[block.bind] : ''"
      :rows="block.rows || 4"
      :readonly="block.readonly"
      :placeholder="block.placeholder"
      @update:value="(val: string) => block.bind && !block.readonly && (state[block.bind] = val)"
    />
  </div>

  <div v-else-if="block.type === 'text'" class="lowcode-field">
    <label v-if="block.label" class="lowcode-label">{{ block.label }}</label>
    <Input
      :value="block.bind ? state[block.bind] : ''"
      :placeholder="block.placeholder"
      @update:value="(val: string) => block.bind && (state[block.bind] = val)"
    />
  </div>

  <Button
    v-else-if="block.type === 'button'"
    type="primary"
    :block="h5"
    @click="runBlockAction(block)"
  >
    {{ block.label || '按钮' }}
  </Button>

  <Space v-else-if="block.type === 'toolbar'" wrap class="lowcode-toolbar">
    <Button v-for="item in block.items || []" :key="item.label" @click="emit('toolbar', item)">
      {{ item.label }}
    </Button>
  </Space>
</template>

<style scoped>
.lowcode-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.lowcode-label {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}

.lowcode-card {
  margin-bottom: 0;
}
</style>
