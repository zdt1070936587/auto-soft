<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { EyeInvisibleOutlined, EyeOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import { Alert, Button, Checkbox, Form, message } from 'ant-design-vue'
import AppLogo from '@/components/brand/AppLogo.vue'
import SolarSystemCanvas, { type PlanetInfo } from './SolarSystemCanvas.vue'
import { useAuthStore } from '@/stores/auth'

const REMEMBER_KEY = 'autosoft.remember'
const USERNAME_KEY = 'autosoft.remember.username'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const shake = ref(false)
const showPassword = ref(false)
const errorText = ref('')
const selected = ref<PlanetInfo | null>(null)
const form = reactive({
  username: '',
  password: '',
  remember: true,
})

function loadRemembered() {
  const remember = localStorage.getItem(REMEMBER_KEY)
  form.remember = remember !== '0'
  if (form.remember) {
    form.username = localStorage.getItem(USERNAME_KEY) || ''
  }
}

function persistRemember() {
  localStorage.setItem(REMEMBER_KEY, form.remember ? '1' : '0')
  if (form.remember) {
    localStorage.setItem(USERNAME_KEY, form.username.trim())
  } else {
    localStorage.removeItem(USERNAME_KEY)
  }
}

function onRememberChange(checked: boolean | string | number) {
  form.remember = Boolean(checked)
  if (!form.remember) {
    localStorage.setItem(REMEMBER_KEY, '0')
    localStorage.removeItem(USERNAME_KEY)
  }
}

function onPlanetSelect(planet: PlanetInfo | null) {
  selected.value = planet
}

function scrollToLogin() {
  document.getElementById('login-card')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  const input = document.querySelector<HTMLInputElement>('#login-card input')
  input?.focus()
}

async function onSubmit() {
  if (loading.value) {
    return
  }
  loading.value = true
  shake.value = false
  errorText.value = ''
  try {
    await auth.login(form.username.trim(), form.password, form.remember)
    persistRemember()
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect.startsWith('/') ? redirect : '/dashboard')
  } catch (err) {
    shake.value = true
    const msg = err instanceof Error && err.message ? err.message : '用户名或密码错误'
    errorText.value = msg
    window.setTimeout(() => {
      shake.value = false
    }, 320)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRemembered()
})
</script>

<template>
  <div class="login-page">
    <SolarSystemCanvas @select="onPlanetSelect" />

    <div class="overlay">
      <section class="hero">
        <div class="hero-top">
          <AppLogo size="2xl" theme="dark" />
          <div class="hero-badge">
            <span class="pulse-dot" />
            SYSTEM ONLINE
          </div>
        </div>
        <h1>BUILDING SYSTEMS<br /><span class="gradient-text">BEYOND ORBIT</span></h1>
        <p class="lead">
          用元数据与智能体，把业务送上可扩展的轨道。点击星球探索，进入下一代管理控制台。
        </p>
        <button class="explore" type="button" @click="scrollToLogin">
          <span class="explore-inner">INITIATE ACCESS</span>
          <span aria-hidden="true" class="explore-arrow">→</span>
        </button>

        <Transition name="planet-fade">
          <aside v-if="selected" class="planet-card" :style="{ '--accent': selected.color }">
            <div class="planet-card__corner tl" />
            <div class="planet-card__corner br" />
            <p class="planet-en">{{ selected.nameEn }}</p>
            <h2>{{ selected.name }}</h2>
            <p>{{ selected.blurb }}</p>
            <p class="hint">点击空白处复位视角</p>
          </aside>
        </Transition>
      </section>

      <section class="login-panel" :class="{ shake }">
        <div id="login-card" class="login-card">
          <div class="card-glow" />
          <div class="card-corner tl" />
          <div class="card-corner tr" />
          <div class="card-corner bl" />
          <div class="card-corner br" />
          <div class="scanline" />

          <div class="card-header">
            <div class="secure-badge">
              <SafetyCertificateOutlined />
              <span>SECURE ACCESS</span>
            </div>
            <AppLogo size="sm" variant="icon" theme="dark" />
          </div>

          <h2>进入控制台</h2>
          <p class="sub">Enterprise AI Management Platform</p>

          <Alert v-if="errorText" class="error-alert" type="error" show-icon :message="errorText" />

          <Form :model="form" layout="vertical" @finish="onSubmit">
            <Form.Item label="账号 ID" name="username" :rules="[{ required: true, message: '请输入账号' }]">
              <div class="field">
                <input
                  v-model="form.username"
                  class="field-input"
                  type="text"
                  autocomplete="username"
                  placeholder="admin"
                />
              </div>
            </Form.Item>
            <Form.Item label="访问密钥" name="password" :rules="[{ required: true, message: '请输入密码' }]">
              <div class="field field--password">
                <input
                  v-model="form.password"
                  class="field-input"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  placeholder="••••••••"
                />
                <button
                  class="pwd-toggle"
                  type="button"
                  tabindex="-1"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  @click.prevent.stop="showPassword = !showPassword"
                >
                  <EyeOutlined v-if="showPassword" />
                  <EyeInvisibleOutlined v-else />
                </button>
              </div>
            </Form.Item>
            <div class="row">
              <Checkbox :checked="form.remember" @update:checked="onRememberChange">记住账号</Checkbox>
            </div>
            <Button class="login-btn" type="primary" html-type="submit" size="large" block :loading="loading">
              <span v-if="!loading">授权登录</span>
              <span v-else>验证中...</span>
            </Button>
          </Form>

          <p class="footer-note">TLS 1.3 · JWT · Role-Based Access</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500&family=Space+Grotesk:wght@500;700&family=Noto+Sans+SC:wght@400;500;700&display=swap');

.login-page {
  position: relative;
  width: 100%;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: #03050c;
  color: #f4f6fb;
  font-family: 'Inter', 'Noto Sans SC', sans-serif;
}

.overlay {
  position: relative;
  z-index: 2;
  height: 100%;
  display: grid;
  grid-template-columns: minmax(280px, 1.1fr) minmax(320px, 0.9fr);
  align-items: center;
  gap: 24px;
  padding: clamp(24px, 5vw, 72px);
  pointer-events: none;
}

.hero {
  max-width: 580px;
  pointer-events: none;
}

.hero-top {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: clamp(24px, 4vw, 38px);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  padding: 5px 14px 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(34, 211, 238, 0.25);
  background: rgba(34, 211, 238, 0.06);
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.14em;
  color: #22d3ee;
}

.pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 10px #22d3ee;
  animation: pulse-glow 2s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.85);
  }
}

.hero h1 {
  margin: 0;
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(36px, 5.5vw, 68px);
  font-weight: 700;
  line-height: 1.02;
  letter-spacing: -0.01em;
}

.gradient-text {
  background: linear-gradient(135deg, #5b8cff 0%, #22d3ee 50%, #a78bfa 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.lead {
  margin: 20px 0 32px;
  max-width: 440px;
  color: rgba(244, 246, 251, 0.65);
  font-size: 15px;
  line-height: 1.75;
}

.explore {
  pointer-events: auto;
  display: inline-flex;
  align-items: center;
  gap: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.explore-inner {
  display: inline-flex;
  align-items: center;
  padding: 13px 24px;
  border: 1px solid rgba(91, 140, 255, 0.45);
  border-right: none;
  background: rgba(91, 140, 255, 0.08);
  color: #e8eeff;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12em;
  transition: background 200ms ease, border-color 200ms ease;
}

.explore-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(91, 140, 255, 0.45);
  background: rgba(91, 140, 255, 0.15);
  color: #5b8cff;
  font-size: 18px;
  transition: background 200ms ease, transform 200ms ease;
}

.explore:hover .explore-inner {
  background: rgba(91, 140, 255, 0.18);
  border-color: rgba(91, 140, 255, 0.7);
}

.explore:hover .explore-arrow {
  background: #5b8cff;
  color: #03050c;
  transform: translateX(2px);
}

.planet-card {
  position: relative;
  pointer-events: none;
  margin-top: 32px;
  max-width: 400px;
  padding: 20px 22px 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(8, 14, 28, 0.55);
  backdrop-filter: blur(16px);
}

.planet-card__corner {
  position: absolute;
  width: 16px;
  height: 16px;
  border-color: var(--accent, #d9784a);
  border-style: solid;
}

.planet-card__corner.tl {
  top: -1px;
  left: -1px;
  border-width: 2px 0 0 2px;
}

.planet-card__corner.br {
  right: -1px;
  bottom: -1px;
  border-width: 0 2px 2px 0;
}

.planet-en {
  margin: 0;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.22em;
  color: var(--accent, #d9784a);
}

.planet-card h2 {
  margin: 8px 0 8px;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 26px;
  font-weight: 700;
}

.planet-card p {
  margin: 0;
  color: rgba(244, 246, 251, 0.75);
  line-height: 1.65;
  font-size: 14px;
}

.planet-card .hint {
  margin-top: 12px;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  color: rgba(244, 246, 251, 0.35);
}

.planet-fade-enter-active,
.planet-fade-leave-active {
  transition: opacity 280ms ease, transform 280ms ease;
}

.planet-fade-enter-from,
.planet-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.login-panel {
  display: flex;
  justify-content: flex-end;
  pointer-events: none;
}

.login-card {
  position: relative;
  width: min(400px, 100%);
  padding: 28px 28px 22px;
  border: 1px solid rgba(91, 140, 255, 0.2);
  border-radius: 4px;
  background: rgba(10, 16, 32, 0.72);
  backdrop-filter: blur(24px) saturate(1.4);
  color: #f4f6fb;
  pointer-events: auto;
  overflow: hidden;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.04) inset,
    0 24px 80px rgba(0, 0, 0, 0.5),
    0 0 60px rgba(91, 140, 255, 0.08);
}

.card-glow {
  position: absolute;
  top: -40%;
  left: 50%;
  width: 120%;
  height: 60%;
  transform: translateX(-50%);
  background: radial-gradient(ellipse, rgba(91, 140, 255, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

.card-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border-color: rgba(34, 211, 238, 0.5);
  border-style: solid;
  z-index: 1;
}

.card-corner.tl {
  top: 8px;
  left: 8px;
  border-width: 2px 0 0 2px;
}

.card-corner.tr {
  top: 8px;
  right: 8px;
  border-width: 2px 2px 0 0;
}

.card-corner.bl {
  bottom: 8px;
  left: 8px;
  border-width: 0 0 2px 2px;
}

.card-corner.br {
  right: 8px;
  bottom: 8px;
  border-width: 0 2px 2px 0;
}

.scanline {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(255, 255, 255, 0.012) 2px,
    rgba(255, 255, 255, 0.012) 4px
  );
  pointer-events: none;
  opacity: 0.5;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  position: relative;
  z-index: 2;
}

.secure-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  letter-spacing: 0.1em;
  color: #22d3ee;
}

.status-row {
  display: flex;
  gap: 6px;
}

.status-pill {
  padding: 2px 8px;
  border-radius: 3px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-family: 'JetBrains Mono', monospace;
  font-size: 9px;
  letter-spacing: 0.08em;
  color: rgba(244, 246, 251, 0.45);
}

.status-pill.accent {
  border-color: rgba(34, 211, 238, 0.3);
  color: #22d3ee;
  background: rgba(34, 211, 238, 0.06);
}

.login-card h2 {
  position: relative;
  z-index: 2;
  margin: 0;
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.sub {
  position: relative;
  z-index: 2;
  margin: 6px 0 22px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: rgba(244, 246, 251, 0.4);
  text-transform: uppercase;
}

.error-alert {
  position: relative;
  z-index: 2;
  margin-bottom: 16px;
}

.row {
  position: relative;
  z-index: 2;
  margin-bottom: 18px;
}

.login-card :deep(.ant-form) {
  position: relative;
  z-index: 2;
}

.login-card :deep(.ant-form-item-label > label) {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(244, 246, 251, 0.55) !important;
}

.login-card :deep(.ant-checkbox-wrapper) {
  color: rgba(244, 246, 251, 0.65);
  font-size: 13px;
}

.field {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 0 14px;
  border-radius: 4px;
  border: 1px solid rgba(91, 140, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03) 0%, transparent 100%),
    rgba(4, 10, 22, 0.75);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  transition: border-color 200ms ease, box-shadow 200ms ease, background 200ms ease;
}

.field::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.35), transparent);
  opacity: 0;
  transition: opacity 200ms ease;
}

.field:focus-within {
  border-color: rgba(34, 211, 238, 0.45);
  background:
    linear-gradient(180deg, rgba(34, 211, 238, 0.04) 0%, transparent 100%),
    rgba(4, 12, 26, 0.9);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 0 0 1px rgba(34, 211, 238, 0.08),
    0 0 24px rgba(34, 211, 238, 0.06);
}

.field:focus-within::before {
  opacity: 1;
}

.field--password {
  padding-right: 10px;
}

.field-input {
  flex: 1;
  width: 100%;
  min-width: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: #f4f6fb;
  font-family: 'JetBrains Mono', monospace;
  font-size: 14px;
  line-height: 1.4;
}

.field-input::placeholder {
  color: rgba(244, 246, 251, 0.28);
}

.field-input:-webkit-autofill,
.field-input:-webkit-autofill:hover,
.field-input:-webkit-autofill:focus {
  -webkit-text-fill-color: #f4f6fb;
  caret-color: #22d3ee;
  transition: background-color 99999s ease-in-out 0s;
  box-shadow: 0 0 0 1000px rgba(4, 12, 26, 0.95) inset !important;
}

.pwd-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: rgba(244, 246, 251, 0.45);
  cursor: pointer;
  line-height: 1;
  transition: color 150ms ease, background 150ms ease;
}

.pwd-toggle:hover {
  color: #22d3ee;
  background: rgba(34, 211, 238, 0.08);
}

.login-card :deep(.ant-form-item-explain-error) {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
}

.login-card :deep(.error-alert) {
  background: rgba(248, 113, 113, 0.08) !important;
  border: 1px solid rgba(248, 113, 113, 0.25) !important;
}

.login-btn {
  position: relative;
  z-index: 2;
  height: 46px !important;
  border: none !important;
  border-radius: 4px !important;
  background: linear-gradient(135deg, #3b6fd9 0%, #5b8cff 40%, #22d3ee 100%) !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  letter-spacing: 0.1em !important;
  color: #fff !important;
  box-shadow: 0 4px 24px rgba(91, 140, 255, 0.35) !important;
  transition: transform 150ms ease, box-shadow 150ms ease !important;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 32px rgba(91, 140, 255, 0.5) !important;
  background: linear-gradient(135deg, #4a7ee8 0%, #6b9aff 40%, #33e0ff 100%) !important;
  color: #fff !important;
}

.footer-note {
  position: relative;
  z-index: 2;
  margin: 18px 0 0;
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: rgba(244, 246, 251, 0.25);
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

@media (max-width: 960px) {
  .overlay {
    grid-template-columns: 1fr;
    align-content: end;
    padding-bottom: 28px;
  }

  .hero h1 {
    font-size: clamp(28px, 9vw, 40px);
  }

  .login-panel {
    justify-content: stretch;
  }

  .login-card {
    width: 100%;
  }
}
</style>
