<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Spin, Tag } from 'ant-design-vue'
import PageShell from '@/components/layout/PageShell.vue'
import SchemaRenderer from '@/components/schema/SchemaRenderer.vue'
import { getRuntimeSchema, type RuntimeSchemaVO } from '@/api/meta'

const route = useRoute()
const loading = ref(false)
const schema = ref<RuntimeSchemaVO | null>(null)

const app = computed(() => String(route.params.app || ''))
const entity = computed(() => String(route.params.entity || ''))
const preview = computed(() => route.query.preview === '1')

const pageTitle = computed(() => {
  if (!schema.value) {
    return '动态页面'
  }
  return schema.value.entityName
})

const pageSubtitle = computed(() => {
  if (!schema.value) {
    return ''
  }
  return `${schema.value.appName} · ${schema.value.appCode}/${schema.value.entityCode}`
})

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
  <PageShell :title="pageTitle" :subtitle="pageSubtitle">
    <template #actions>
      <Tag v-if="schema?.flowBound" color="processing">已绑定流程</Tag>
      <Tag v-if="preview" color="warning">预览模式</Tag>
      <Tag v-if="schema?.published" color="success">已发布</Tag>
      <Tag v-else-if="schema" color="default">草稿</Tag>
    </template>

    <section class="page-panel runtime-panel">
      <Spin :spinning="loading">
        <SchemaRenderer v-if="schema" :schema="schema" :preview="preview" />
      </Spin>
    </section>
  </PageShell>
</template>

<style scoped>
.runtime-panel {
  min-height: 360px;
}
</style>
