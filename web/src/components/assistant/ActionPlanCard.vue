<script setup lang="ts">
import { Button, Card, message } from 'ant-design-vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getActionDraft, type ActionPlanPayload } from '@/api/assistant'
import { useAssistantActionStore } from '@/stores/assistantAction'

const props = defineProps<{ plan: ActionPlanPayload }>()
const emit = defineEmits<{ navigate: [] }>()

const router = useRouter()
const actionStore = useAssistantActionStore()
const loading = ref(false)

async function confirm() {
  if (!props.plan.canConfirm || loading.value) {
    return
  }
  loading.value = true
  try {
    const draft = await getActionDraft(props.plan.draftId)
    actionStore.setPending({
      draftId: draft.draftId,
      capabilityId: draft.capabilityId,
      targetPath: draft.targetPath,
      targetType: draft.targetType,
      modalKey: draft.modalKey,
      values: draft.values || {},
      displayValues: draft.displayValues || {},
      missing: draft.missing || [],
      unknown: draft.unknown || [],
    })
    await router.push(draft.targetPath)
    emit('navigate')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载操作计划失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Card size="small" class="action-plan-card" :title="plan.label || '操作计划'">
    <div v-if="plan.summary" class="summary">{{ plan.summary }}</div>
    <div v-if="plan.fields?.length" class="field-list">
      <div v-for="(item, idx) in plan.fields" :key="idx" class="field-row">
        <span class="field-label">{{ item.label }}</span>
        <span class="field-value">{{ item.display }}</span>
      </div>
    </div>
    <div class="actions">
      <Button type="primary" size="small" :loading="loading" :disabled="!plan.canConfirm" @click="confirm">
        确认并前往
      </Button>
    </div>
  </Card>
</template>

<style scoped>
.action-plan-card {
  margin-top: 8px;
  background: var(--bg-elevated);
}

.summary {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}

.field-list {
  margin-bottom: 8px;
}

.field-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  padding: 2px 0;
}

.field-label {
  color: var(--text-3);
}

.field-value {
  color: var(--text-1);
  text-align: right;
}

.missing {
  color: #ff4d4f;
  font-size: 12px;
  margin-bottom: 4px;
}

.unknown {
  color: var(--text-3);
  font-size: 12px;
  margin-bottom: 8px;
}

.actions {
  margin-top: 8px;
}
</style>
