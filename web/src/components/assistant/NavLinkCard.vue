<script setup lang="ts">
import { Button, Card } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { NavLinkItem } from '@/api/assistant'
import { useAuthStore } from '@/stores/auth'

defineProps<{ items: NavLinkItem[] }>()
const emit = defineEmits<{ navigate: [] }>()

const router = useRouter()
const auth = useAuthStore()

function go(item: NavLinkItem) {
  if (!auth.canAccess(item.path)) {
    message.warning('无权限访问该页面')
    return
  }
  void router.push(item.path)
  emit('navigate')
}
</script>

<template>
  <Card size="small" class="nav-card" title="相关菜单">
    <div v-for="item in items" :key="item.path" class="nav-row">
      <div class="nav-info">
        <div class="nav-name">{{ item.name }}</div>
        <div v-if="item.parentName" class="nav-parent">{{ item.parentName }}</div>
      </div>
      <Button type="link" size="small" @click="go(item)">前往</Button>
    </div>
  </Card>
</template>

<style scoped>
.nav-card {
  margin-top: 8px;
  background: var(--bg-elevated);
}

.nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 0;
}

.nav-row + .nav-row {
  border-top: 1px solid var(--border);
}

.nav-name {
  font-weight: 600;
  color: var(--text-1);
}

.nav-parent {
  font-size: 12px;
  color: var(--text-3);
}
</style>
