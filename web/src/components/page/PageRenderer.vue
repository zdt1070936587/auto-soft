<script setup lang="ts">
import { computed } from 'vue'
import { Empty } from 'ant-design-vue'
import SchemaRenderer from '@/components/schema/SchemaRenderer.vue'
import LowCodeRenderer from '@/components/page/LowCodeRenderer.vue'
import type { PageViewVO } from '@/api/meta'

const props = defineProps<{
  view: PageViewVO
  preview?: boolean
}>()

const isLowCode = computed(() => {
  if (props.view.pageType === 'PAGE') {
    return true
  }
  return Boolean(props.view.schemaJson)
})
</script>

<template>
  <LowCodeRenderer
    v-if="isLowCode && view.schemaJson"
    :key="`${view.pageCode || 'page'}-${view.schemaJson}`"
    :schema-json="view.schemaJson"
    :layout="view.layout"
    :title="view.pageTitle"
    :preview="preview"
  />
  <SchemaRenderer
    v-else-if="view.crudSchema"
    :key="`${view.crudSchema.appCode}-${view.crudSchema.entityCode}-${view.crudSchema.fields.length}`"
    :schema="view.crudSchema"
    :preview="preview"
  />
  <Empty v-else description="暂无可预览内容" />
</template>
