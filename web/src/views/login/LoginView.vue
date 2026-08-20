<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button, Checkbox, Form, Input, message } from 'ant-design-vue'
import SolarSystemCanvas from './SolarSystemCanvas.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const shake = ref(false)
const form = reactive({
  username: '',
  password: '',
  remember: true,
})

async function onSubmit() {
  if (loading.value) {
    return
  }
  loading.value = true
  shake.value = false
  try {
    await auth.login(form.username.trim(), form.password, form.remember)
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect.startsWith('/') ? redirect : '/dashboard')
  } catch {
    shake.value = true
    window.setTimeout(() => {
      shake.value = false
    }, 320)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <SolarSystemCanvas />
    <div class="login-panel" :class="{ shake }">
      <div class="login-card">
        <h1>auto-soft</h1>
        <p class="sub">AI 管理后台</p>
        <Form :model="form" layout="vertical" @finish="onSubmit">
          <Form.Item label="账号" name="username" :rules="[{ required: true, message: '请输入账号' }]">
            <Input v-model:value="form.username" size="large" autocomplete="username" placeholder="admin" />
          </Form.Item>
          <Form.Item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
            <Input.Password v-model:value="form.password" size="large" autocomplete="current-password" placeholder="密码" />
          </Form.Item>
          <div class="row">
            <Checkbox v-model:checked="form.remember">记住登录</Checkbox>
          </div>
          <Button type="primary" html-type="submit" size="large" block :loading="loading">登录</Button>
        </Form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #050814;
}

.login-panel {
  position: relative;
  z-index: 2;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 24px 8vw 24px 24px;
}

.login-card {
  width: 380px;
  padding: 32px 28px 28px;
  border-radius: 16px;
  background: rgba(12, 18, 40, 0.58);
  border: 1px solid rgba(180, 200, 255, 0.22);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(16px);
  color: #e8eeff;
}

.login-card h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 0.08em;
}

.sub {
  margin: 6px 0 24px;
  color: rgba(232, 238, 255, 0.7);
}

.row {
  margin-bottom: 16px;
}

.login-card :deep(.ant-form-item-label > label) {
  color: #e8eeff;
}

.shake {
  animation: shake 300ms ease-in-out;
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-8px);
  }
  75% {
    transform: translateX(8px);
  }
}

@media (max-width: 768px) {
  .login-panel {
    justify-content: center;
    padding: 24px;
  }
}
</style>
