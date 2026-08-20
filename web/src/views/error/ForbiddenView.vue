<script setup lang="ts">
import { Button, Result } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="wrap">
    <Result status="403" title="无权限" sub-title="当前角色不能访问该页面。">
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
