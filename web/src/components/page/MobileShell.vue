<script setup lang="ts">
defineProps<{ title?: string }>()
</script>

<template>
  <div class="mobile-shell">
    <header v-if="title" class="mobile-shell__header">{{ title }}</header>
    <main class="mobile-shell__body">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.mobile-shell {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100%;
  padding: env(safe-area-inset-top) 16px env(safe-area-inset-bottom);
  box-sizing: border-box;
}

.mobile-shell__header {
  font-size: 18px;
  font-weight: 600;
  padding: 16px 0 12px;
  text-align: center;
}

.mobile-shell__body :deep(.ant-btn) {
  min-height: 44px;
  font-size: 16px;
}

.mobile-shell__body :deep(.ant-input),
.mobile-shell__body :deep(textarea.ant-input) {
  font-size: 16px;
}
</style>
