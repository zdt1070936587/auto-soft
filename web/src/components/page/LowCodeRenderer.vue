<script setup lang="ts">
import { computed, provide } from 'vue'
import MobileShell from '@/components/page/MobileShell.vue'
import LowCodeBlockView from '@/components/page/LowCodeBlockView.vue'
import { useLowCodeState } from '@/lowcode/useLowCodeState'
import { LOWCODE_RUN_ACTION } from '@/lowcode/inject'
import type { LowCodeSchema, LowCodeToolbarItem } from '@/lowcode/types'

const props = defineProps<{
  schemaJson: string
  layout?: string
  title?: string
  preview?: boolean
}>()

const parsed = computed<LowCodeSchema | null>(() => {
  try {
    return JSON.parse(props.schemaJson) as LowCodeSchema
  } catch {
    return null
  }
})

const effectiveLayout = computed(() => props.layout || parsed.value?.layout || 'admin')
const pageTitle = computed(() => props.title || parsed.value?.title || '')
const { state, runAction } = useLowCodeState(() => parsed.value, { preview: props.preview })

provide(LOWCODE_RUN_ACTION, runAction)

function onToolbar(item: LowCodeToolbarItem) {
  runAction(item.action, { from: item.from, to: item.to })
}
</script>

<template>
  <MobileShell v-if="effectiveLayout === 'h5'" :title="pageTitle">
    <div v-if="parsed" class="lowcode-blocks">
      <LowCodeBlockView
        v-for="(block, index) in parsed.blocks"
        :key="block.id || `${block.type}-${index}`"
        :block="block"
        :state="state"
        :h5="true"
        @toolbar="onToolbar"
      />
    </div>
  </MobileShell>

  <div v-else class="lowcode-renderer">
    <h3 v-if="pageTitle" class="lowcode-title">{{ pageTitle }}</h3>
    <div v-if="parsed" class="lowcode-blocks">
      <LowCodeBlockView
        v-for="(block, index) in parsed.blocks"
        :key="block.id || `${block.type}-${index}`"
        :block="block"
        :state="state"
        @toolbar="onToolbar"
      />
    </div>
  </div>
</template>

<style scoped>
.lowcode-renderer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lowcode-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.lowcode-blocks {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
