import { createRouter, createWebHistory } from 'vue-router'
import BlankLayout from '@/layouts/BlankLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import LoginView from '@/views/login/LoginView.vue'
import HealthView from '@/views/health/HealthView.vue'
import WorkbenchView from '@/views/dashboard/WorkbenchView.vue'
import UserView from '@/views/system/user/UserView.vue'
import RoleView from '@/views/system/role/RoleView.vue'
import ForbiddenView from '@/views/error/ForbiddenView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/dev/health',
      component: BlankLayout,
      children: [
        {
          path: '',
          name: 'health',
          component: HealthView,
        },
      ],
    },
    {
      path: '/403',
      name: 'forbidden',
      component: ForbiddenView,
    },
    {
      path: '/',
      component: AdminLayout,
      children: [
        {
          path: '',
          redirect: '/dashboard',
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: WorkbenchView,
        },
        {
          path: 'system/users',
          name: 'users',
          component: UserView,
        },
        {
          path: 'system/roles',
          name: 'roles',
          component: RoleView,
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard',
    },
  ],
})

export default router
