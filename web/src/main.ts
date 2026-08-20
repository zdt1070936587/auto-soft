import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { setupRouterGuards } from './router/guards'
import { setUnauthorizedHandler } from './api/http'

const app = createApp(App)
app.use(createPinia())
setupRouterGuards(router)
setUnauthorizedHandler(() => {
  if (router.currentRoute.value.path !== '/login') {
    void router.replace('/login')
  }
})
app.use(router)
app.use(Antd)
app.mount('#app')

