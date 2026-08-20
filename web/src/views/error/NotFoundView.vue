<script setup lang="ts">
import { Button, Result } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="wrap">
    <Result status="404" title="页面不存在" sub-title="请检查地址，或从工作台进入。">
      <template #extra>
        <Button type="primary" @click="router.push('/dashboard')">返回工作台</Button>
      </template>
    </Result>
  </div>
</template>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
</style>
