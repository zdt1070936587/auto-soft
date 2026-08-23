import { defineStore } from 'pinia'

const SIDEBAR_KEY = 'autosoft.sidebar.collapsed'

function readCollapsed(): boolean {
  return localStorage.getItem(SIDEBAR_KEY) === '1'
}

export const useAppStore = defineStore('app', {
  state: () => ({
    appName: 'auto-soft',
    sidebarCollapsed: readCollapsed(),
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem(SIDEBAR_KEY, this.sidebarCollapsed ? '1' : '0')
    },
    setSidebarCollapsed(collapsed: boolean) {
      this.sidebarCollapsed = collapsed
      localStorage.setItem(SIDEBAR_KEY, collapsed ? '1' : '0')
    },
  },
})
