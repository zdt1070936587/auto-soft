import { defineStore } from 'pinia'

export interface PendingActionDraft {
  draftId: string
  capabilityId: string
  targetPath: string
  targetType: 'system_modal' | 'runtime_form'
  modalKey?: string
  values: Record<string, unknown>
  displayValues: Record<string, unknown>
  missing: string[]
  unknown: string[]
}

export const useAssistantActionStore = defineStore('assistantAction', {
  state: () => ({
    pending: null as PendingActionDraft | null,
  }),
  actions: {
    setPending(draft: PendingActionDraft) {
      this.pending = draft
    },
    consume(capabilityId: string): PendingActionDraft | null {
      if (!this.pending || this.pending.capabilityId !== capabilityId) {
        return null
      }
      const draft = this.pending
      this.pending = null
      return draft
    },
    clear() {
      this.pending = null
    },
  },
})
