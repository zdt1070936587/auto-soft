import { createRouter, createWebHistory } from 'vue-router'
import BlankLayout from '@/layouts/BlankLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import LoginView from '@/views/login/LoginView.vue'
import HealthView from '@/views/health/HealthView.vue'
import WorkbenchView from '@/views/dashboard/WorkbenchView.vue'
import UserView from '@/views/system/user/UserView.vue'
import RoleView from '@/views/system/role/RoleView.vue'
import MetaAppView from '@/views/meta/MetaAppView.vue'
import RuntimePageView from '@/views/runtime/RuntimePageView.vue'
import LowCodePageView from '@/views/runtime/LowCodePageView.vue'
import StudioView from '@/views/studio/StudioView.vue'
import WorkflowRunView from '@/views/workflow/WorkflowRunView.vue'
import WorkflowShareView from '@/views/workflow/WorkflowShareView.vue'
import LlmSettingView from '@/views/system/LlmSettingView.vue'
import WorkflowHttpHostView from '@/views/system/WorkflowHttpHostView.vue'
import TodoView from '@/views/flow/TodoView.vue'
import DoneView from '@/views/flow/DoneView.vue'
import OperLogView from '@/views/system/OperLogView.vue'
import ForbiddenView from '@/views/error/ForbiddenView.vue'
import NotFoundView from '@/views/error/NotFoundView.vue'

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
          meta: { title: '工作台' },
          component: WorkbenchView,
        },
        {
          path: 'system/users',
          name: 'users',
          meta: { title: '用户管理' },
          component: UserView,
        },
        {
          path: 'system/roles',
          name: 'roles',
          meta: { title: '角色管理' },
          component: RoleView,
        },
        {
          path: 'meta/apps',
          name: 'meta-apps',
          meta: { title: '应用建模' },
          component: MetaAppView,
        },
        {
          path: 'app/:app/:entity',
          name: 'runtime',
          meta: { title: '业务实体' },
          component: RuntimePageView,
        },
        {
          path: 'page/:app/:page',
          name: 'lowcode-page',
          meta: { title: '工具页面' },
          component: LowCodePageView,
        },
        {
          path: 'studio',
          name: 'studio',
          meta: { title: '功能开发' },
          component: StudioView,
        },
        {
          path: 'wf/share/:token',
          name: 'workflow-share',
          meta: { title: '工作流分享' },
          component: WorkflowShareView,
        },
        {
          path: 'wf/:code',
          name: 'workflow-run',
          meta: { title: '运行工作流' },
          component: WorkflowRunView,
        },
        {
          path: 'system/llm',
          name: 'llm',
          meta: { title: '模型配置' },
          component: LlmSettingView,
        },
        {
          path: 'system/workflow-http-hosts',
          name: 'workflow-http-hosts',
          meta: { title: 'HTTP 出站白名单' },
          component: WorkflowHttpHostView,
        },
        {
          path: 'flow/todo',
          name: 'flow-todo',
          meta: { title: '我的待办' },
          component: TodoView,
        },
        {
          path: 'flow/done',
          name: 'flow-done',
          meta: { title: '已办事项' },
          component: DoneView,
        },
        {
          path: 'system/logs',
          name: 'logs',
          meta: { title: '操作日志' },
          component: OperLogView,
        },
      ],
    },
    {
      path: '/h5',
      component: BlankLayout,
      children: [
        {
          path: ':app/:page',
          name: 'h5-page',
          meta: { title: 'H5 页面' },
          component: LowCodePageView,
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
    },
  ],
})

export default router
