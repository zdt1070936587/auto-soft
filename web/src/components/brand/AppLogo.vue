<script setup lang="ts">
import { computed, useId } from 'vue'

const props = withDefaults(
  defineProps<{
    size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl'
    variant?: 'full' | 'icon'
    theme?: 'dark' | 'light'
    collapsed?: boolean
  }>(),
  {
    size: 'md',
    variant: 'full',
    theme: 'dark',
    collapsed: false,
  },
)

const uid = useId().replace(/:/g, '')
const gradId = `logo-mark-${uid}`

const showIconOnly = computed(() => props.collapsed || props.variant === 'icon')

const metrics = computed(() => {
  const map = {
    sm: { mark: 20, gap: 7, height: 20, fontSize: 15, wordWidth: 82 },
    md: { mark: 24, gap: 8, height: 24, fontSize: 17.5, wordWidth: 96 },
    lg: { mark: 28, gap: 9, height: 28, fontSize: 20, wordWidth: 110 },
    xl: { mark: 36, gap: 11, height: 36, fontSize: 25, wordWidth: 132 },
    '2xl': { mark: 46, gap: 13, height: 46, fontSize: 31, wordWidth: 164 },
  }
  return map[props.size]
})

const autoColor = computed(() => (props.theme === 'dark' ? '#F4F6FB' : '#173A6E'))
const softColor = computed(() => (props.theme === 'dark' ? '#7EA8FF' : '#5B7FD4'))
</script>

<template>
  <span
    class="app-logo"
    :class="[`size-${size}`, `theme-${theme}`, { 'icon-only': showIconOnly }]"
    :style="{ height: `${metrics.height}px`, width: showIconOnly ? `${metrics.mark}px` : 'auto' }"
    role="img"
    aria-label="auto-soft"
  >
    <svg
      class="logo-mark"
      :width="metrics.mark"
      :height="metrics.mark"
      viewBox="0 0 40 40"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient :id="gradId" x1="4" y1="6" x2="36" y2="34" gradientUnits="userSpaceOnUse">
          <stop stop-color="#3B9EFF" />
          <stop offset="0.45" stop-color="#5B8CFF" />
          <stop offset="1" stop-color="#A855F7" />
        </linearGradient>
      </defs>
      <circle cx="10.5" cy="9.5" r="3.2" :fill="`url(#${gradId})`" />
      <path
        d="M10.5 12.8C8.2 8.8 12.4 4.8 16.8 6.6C20.2 7.9 21.4 11.6 19.2 14.4C17.4 16.7 13.8 17.2 12.1 19.8C10.1 22.9 12.4 27.2 16.2 28.4C20.8 29.8 25.4 27.1 27.8 23C29.6 19.9 29.1 16.1 26.8 13.6C24.9 11.6 22 10.8 19.4 11.6"
        :stroke="`url(#${gradId})`"
        stroke-width="4.2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>

    <svg
      v-if="!showIconOnly"
      class="logo-wordmark"
      :width="metrics.wordWidth"
      :height="metrics.height"
      :viewBox="`0 0 ${metrics.wordWidth} ${metrics.height}`"
      fill="none"
      aria-hidden="true"
    >
      <text
        x="0"
        :y="metrics.height - 6"
        font-family="'Space Grotesk', 'Segoe UI', system-ui, sans-serif"
        :font-size="metrics.fontSize"
        font-weight="700"
        letter-spacing="-0.03em"
      >
        <tspan :fill="autoColor">auto</tspan>
        <tspan :fill="softColor">-soft</tspan>
      </text>
    </svg>
  </span>
</template>

<style scoped>
.app-logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  line-height: 0;
  vertical-align: middle;
}

.app-logo.icon-only {
  justify-content: center;
}

.logo-mark,
.logo-wordmark {
  display: block;
  flex-shrink: 0;
}

.size-sm {
  gap: 7px;
}

.size-lg {
  gap: 9px;
}

.size-xl {
  gap: 11px;
}

.size-2xl {
  gap: 13px;
}
</style>
