<script setup lang="ts">
import { computed, inject } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { Button, Card, DatePicker, Divider, Input, InputNumber, Select, Space } from 'ant-design-vue'
import { LOWCODE_RUN_ACTION } from '@/lowcode/inject'
import type { LowCodeBlock, LowCodeToolbarItem } from '@/lowcode/types'

const props = defineProps<{
  block: LowCodeBlock
  state: Record<string, string>
  h5?: boolean
}>()

const emit = defineEmits<{
  toolbar: [item: LowCodeToolbarItem]
}>()

const runAction = inject<(action: string, params: Record<string, unknown>) => void>(LOWCODE_RUN_ACTION)

const widget = computed(() => {
  if (props.block.type === 'textarea') {
    return 'textarea'
  }
  if (props.block.widget) {
    return props.block.widget
  }
  if (props.block.type === 'text') {
    return 'text'
  }
  return props.block.type
})

const bindKey = computed(() => props.block.bind || '')

const selectOptions = computed(() =>
  (props.block.options || []).map((item) => ({
    label: item.label,
    value: item.value,
  })),
)

function setState(key: string, value: string) {
  if (key) {
    props.state[key] = value
  }
}

function datetimeValue(key: string): Dayjs | undefined {
  const raw = props.state[key]
  if (!raw) {
    return undefined
  }
  const parsed = dayjs(raw)
  return parsed.isValid() ? parsed : undefined
}

function onDatetimeChange(key: string, value: Dayjs | string | null) {
  if (!key) {
    return
  }
  if (!value) {
    props.state[key] = ''
    return
  }
  props.state[key] = typeof value === 'string' ? value : value.format('YYYY-MM-DD HH:mm:ss')
}

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

  <div v-else-if="widget === 'textarea'" class="lowcode-field">
    <label v-if="block.label" class="lowcode-label">
      {{ block.label }}<span v-if="block.required" class="required">*</span>
    </label>
    <Input.TextArea
      :value="bindKey ? state[bindKey] : ''"
      :rows="block.rows || 4"
      :readonly="block.readonly"
      :placeholder="block.placeholder"
      @update:value="(val: string) => bindKey && !block.readonly && setState(bindKey, val)"
    />
  </div>

  <div v-else-if="widget === 'select'" class="lowcode-field">
    <label v-if="block.label" class="lowcode-label">
      {{ block.label }}<span v-if="block.required" class="required">*</span>
    </label>
    <Select
      :value="bindKey ? state[bindKey] || undefined : undefined"
      :options="selectOptions"
      allow-clear
      :placeholder="block.placeholder || '请选择'"
      style="width: 100%"
      @update:value="(val) => setState(bindKey, val == null ? '' : String(val))"
    />
  </div>

  <div v-else-if="widget === 'datetime'" class="lowcode-field">
    <label v-if="block.label" class="lowcode-label">
      {{ block.label }}<span v-if="block.required" class="required">*</span>
    </label>
    <DatePicker
      :value="bindKey ? datetimeValue(bindKey) : undefined"
      show-time
      format="YYYY-MM-DD HH:mm:ss"
      value-format="YYYY-MM-DD HH:mm:ss"
      :placeholder="block.placeholder || '请选择时间'"
      style="width: 100%"
      @update:value="(val) => onDatetimeChange(bindKey, val as Dayjs | string | null)"
    />
  </div>

  <div v-else-if="widget === 'number'" class="lowcode-field">
    <label v-if="block.label" class="lowcode-label">
      {{ block.label }}<span v-if="block.required" class="required">*</span>
    </label>
    <InputNumber
      :value="bindKey && state[bindKey] !== '' ? Number(state[bindKey]) : undefined"
      :placeholder="block.placeholder"
      style="width: 100%"
      @update:value="(val) => setState(bindKey, val == null ? '' : String(val))"
    />
  </div>

  <div v-else-if="widget === 'text'" class="lowcode-field">
    <label v-if="block.label" class="lowcode-label">
      {{ block.label }}<span v-if="block.required" class="required">*</span>
    </label>
    <Input
      :value="bindKey ? state[bindKey] : ''"
      :placeholder="block.placeholder"
      @update:value="(val: string) => setState(bindKey, val)"
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
    <Button
      v-for="item in block.items || []"
      :key="item.label"
      :type="item.action === 'submit' ? 'primary' : 'default'"
      @click="emit('toolbar', item)"
    >
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

.required {
  margin-left: 2px;
  color: #ff4d4f;
}

.lowcode-card {
  margin-bottom: 0;
}
</style>
