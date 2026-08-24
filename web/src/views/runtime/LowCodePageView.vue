<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Spin, Tag } from 'ant-design-vue'
import PageShell from '@/components/layout/PageShell.vue'
import PageRenderer from '@/components/page/PageRenderer.vue'
import { getPageView, type PageViewVO } from '@/api/meta'

const route = useRoute()
const loading = ref(false)
const view = ref<PageViewVO | null>(null)

const app = computed(() => String(route.params.app || ''))
const page = computed(() => String(route.params.page || ''))
const preview = computed(() => route.query.preview === '1')
const isH5 = computed(() => route.path.startsWith('/h5/'))

async function load() {
  loading.value = true
  try {
    view.value = await getPageView(app.value, page.value, preview.value)
  } finally {
    loading.value = false
  }
}

const pageTitle = computed(() => {
  if (!view.value) {
    return '工具页面'
  }
  return view.value.pageTitle || view.value.pageCode || '工具页面'
})

const pageSubtitle = computed(() => {
  if (!view.value) {
    return ''
  }
  return `${view.value.appName} · ${view.value.appCode}/${view.value.pageCode}`
})

onMounted(() => {
  void load()
})

watch([app, page], () => {
  void load()
})
</script>

<template>
  <PageShell v-if="!isH5" :title="pageTitle" :subtitle="pageSubtitle">
    <template #actions>
      <Tag v-if="preview" color="warning">预览模式</Tag>
      <Tag v-if="view?.published" color="success">已发布</Tag>
      <Tag v-else-if="view" color="default">草稿</Tag>
    </template>

    <section class="page-panel lowcode-panel">
      <Spin :spinning="loading">
        <PageRenderer v-if="view" :view="view" :preview="preview" />
      </Spin>
    </section>
  </PageShell>

  <div v-else class="h5-page">
    <Spin :spinning="loading">
      <PageRenderer v-if="view" :view="view" :preview="preview" />
    </Spin>
  </div>
</template>

<style scoped>
.lowcode-panel {
  min-height: 360px;
}

.h5-page {
  min-height: 100vh;
  background: #f5f5f5;
}
</style>
