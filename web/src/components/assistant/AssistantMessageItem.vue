<script setup lang="ts">
import { computed } from 'vue'
import type { AssistantStructuredPayload } from '@/api/assistant'
import { parsePayloadJson } from '@/api/assistant'
import NavLinkCard from './NavLinkCard.vue'
import OperTimeline from './OperTimeline.vue'
import ActionPlanCard from './ActionPlanCard.vue'

const props = defineProps<{
  role: string
  content?: string
  payloadJson?: string
  structured?: AssistantStructuredPayload | null
}>()

const emit = defineEmits<{ navigate: [] }>()

const payload = computed(() => props.structured || parsePayloadJson(props.payloadJson))

const isUser = computed(() => props.role === 'user')
</script>

<template>
  <div class="msg-row" :class="{ user: isUser, assistant: !isUser }">
    <div class="bubble">
      <div v-if="content" class="text">{{ content }}</div>
      <NavLinkCard
        v-if="payload?.type === 'nav_link' && payload.items?.length"
        :items="payload.items"
        @navigate="emit('navigate')"
      />
      <OperTimeline v-if="payload?.type === 'oper_timeline' && payload.items?.length" :items="payload.items" />
      <ActionPlanCard
        v-if="payload?.type === 'action_plan'"
        :plan="payload"
        @navigate="emit('navigate')"
      />
    </div>
  </div>
</template>

<style scoped>
.msg-row {
  display: flex;
  margin-bottom: 12px;
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-row.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 92%;
  padding: 10px 12px;
  border-radius: 12px;
  line-height: 1.5;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

.user .bubble {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.assistant .bubble {
  background: var(--bg-elevated);
  color: var(--text-1);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
}

.text + :deep(.nav-card),
.text + .oper-timeline {
  margin-top: 8px;
}
</style>
