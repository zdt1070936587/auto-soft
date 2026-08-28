import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd, { message } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/styles/global.css'
import App from './App.vue'
import router from './router'
import { setupRouterGuards } from './router/guards'
import { setupPageVisitTracker } from './telemetry/pageVisitTracker'
import { setUnauthorizedHandler } from './api/http'
import { migrateLegacyToken } from './utils/token'

migrateLegacyToken()

message.config({
  top: '48px',
  duration: 3,
  maxCount: 3,
})

const app = createApp(App)
app.use(createPinia())
setupRouterGuards(router)
setupPageVisitTracker(router)
setUnauthorizedHandler(() => {
  if (router.currentRoute.value.path !== '/login') {
    void router.replace('/login')
  }
})
app.use(router)
app.use(Antd)
app.mount('#app')

