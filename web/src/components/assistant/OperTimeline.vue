<script setup lang="ts">
import { Timeline } from 'ant-design-vue'
import type { OperLogItem } from '@/api/assistant'

defineProps<{ items: OperLogItem[] }>()

const MODULE_LABEL: Record<string, string> = {
  USER: '用户',
  ROLE: '角色',
  META: '元数据',
  STUDIO: '功能开发',
  FLOW: '审批流',
  WORKFLOW: '工作流',
}

const ACTION_LABEL: Record<string, string> = {
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  PUBLISH: '发布',
  RUN: '运行',
  SHARE: '分享',
  SUBMIT: '提交',
  COMPLETE: '通过',
  REJECT: '驳回',
}

function label(item: OperLogItem) {
  const mod = MODULE_LABEL[item.module] || item.module
  const act = ACTION_LABEL[item.action] || item.action
  return `${mod} · ${act}`
}

function formatTime(value?: string) {
  if (!value) {
    return ''
  }
  try {
    return new Date(value).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return value
  }
}
</script>

<template>
  <div class="oper-timeline">
    <Timeline>
      <Timeline.Item v-for="item in items" :key="item.id" :color="item.success === 0 ? 'red' : 'blue'">
        <div class="time">{{ formatTime(item.createdAt) }}</div>
        <div class="action">{{ label(item) }}</div>
        <div v-if="item.bizId" class="biz">业务 ID：{{ item.bizId }}</div>
      </Timeline.Item>
    </Timeline>
  </div>
</template>

<style scoped>
.oper-timeline {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: var(--bg-elevated);
  border: 1px solid var(--border);
}

.time {
  font-size: 12px;
  color: var(--text-3);
}

.action {
  font-weight: 600;
  color: var(--text-1);
}

.biz {
  font-size: 12px;
  color: var(--text-2);
}
</style>
