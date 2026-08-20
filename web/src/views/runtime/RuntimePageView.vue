<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Card, Spin } from 'ant-design-vue'
import SchemaRenderer from '@/components/schema/SchemaRenderer.vue'
import { getRuntimeSchema, type RuntimeSchemaVO } from '@/api/meta'

const route = useRoute()
const loading = ref(false)
const schema = ref<RuntimeSchemaVO | null>(null)

const app = computed(() => String(route.params.app || ''))
const entity = computed(() => String(route.params.entity || ''))
const preview = computed(() => route.query.preview === '1')

async function load() {
  loading.value = true
  try {
    schema.value = await getRuntimeSchema(app.value, entity.value, preview.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

watch([app, entity], () => {
  void load()
})
</script>

<template>
  <Card :title="schema ? `${schema.appName} / ${schema.entityName}` : '动态页面'">
    <Spin :spinning="loading">
      <SchemaRenderer v-if="schema" :schema="schema" :preview="preview" />
    </Spin>
  </Card>
</template>
