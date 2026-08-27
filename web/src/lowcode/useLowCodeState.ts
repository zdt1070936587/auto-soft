import { reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { runLowCodeAction } from './actions'
import type { LowCodeSchema } from './types'

export function useLowCodeState(schema: () => LowCodeSchema | null, options?: { preview?: boolean }) {
  const state = reactive<Record<string, string>>({})

  watch(
    schema,
    (value) => {
      for (const key of Object.keys(state)) {
        delete state[key]
      }
      if (!value?.state) {
        return
      }
      for (const [key, val] of Object.entries(value.state)) {
        state[key] = val ?? ''
      }
    },
    { immediate: true },
  )

  function runAction(action: string, params: Record<string, unknown>) {
    runLowCodeAction(
      action,
      {
        state,
        preview: options?.preview,
        message: (text, type = 'info') => {
          if (type === 'success') {
            message.success(text)
          } else if (type === 'error') {
            message.error(text)
          } else {
            message.info(text)
          }
        },
      },
      params,
    )
  }

  return { state, runAction }
}
