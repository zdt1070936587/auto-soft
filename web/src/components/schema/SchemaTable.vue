<script setup lang="ts">
import SchemaRenderer from './SchemaRenderer.vue'
import type { RuntimeSchemaVO } from '@/api/meta'

defineProps<{
  schema: RuntimeSchemaVO
  preview?: boolean
}>()
</script>
<template>
  <SchemaRenderer :schema="schema" :preview="preview" />
</template>
