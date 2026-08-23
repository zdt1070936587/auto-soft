<script setup lang="ts">
import { ConfigProvider } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { RouterView } from 'vue-router'
import { appTheme } from '@/styles/theme'

const locale = zhCN
</script>

<template>
  <ConfigProvider :theme="appTheme" :locale="locale">
    <RouterView />
  </ConfigProvider>
</template>
