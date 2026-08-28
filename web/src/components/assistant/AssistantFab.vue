<script setup lang="ts">
defineEmits<{ toggle: [] }>()
</script>

<template>
  <button class="assistant-fab" type="button" title="AI 助手" aria-label="打开 AI 助手" @click="$emit('toggle')">
    <svg class="robot-icon" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient id="fabBg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#1a2744" />
          <stop offset="100%" stop-color="#0d1526" />
        </linearGradient>
      </defs>
      <circle cx="32" cy="32" r="30" fill="url(#fabBg)" />
      <rect x="18" y="22" width="28" height="22" rx="6" fill="#fff" opacity="0.95" />
      <rect x="22" y="28" width="20" height="10" rx="2" fill="#1e88e5" />
      <path d="M26 32 Q28 34 30 32" stroke="#00e5ff" stroke-width="1.5" fill="none" stroke-linecap="round" />
      <path d="M34 32 Q36 34 38 32" stroke="#00e5ff" stroke-width="1.5" fill="none" stroke-linecap="round" />
      <circle cx="32" cy="48" r="4" fill="#5b8cff" />
      <text x="32" y="50" text-anchor="middle" fill="#fff" font-size="5" font-weight="700">AI</text>
      <rect x="14" y="26" width="4" height="8" rx="2" fill="#5b8cff" opacity="0.8" />
      <rect x="46" y="26" width="4" height="8" rx="2" fill="#5b8cff" opacity="0.8" />
    </svg>
  </button>
</template>

<style scoped>
.assistant-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
  width: 56px;
  height: 56px;
  padding: 0;
  border: 3px solid #fff;
  border-radius: 50%;
  background: transparent;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.22);
  cursor: pointer;
  transition: transform 150ms ease, box-shadow 150ms ease;
}

.assistant-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.28);
}

.robot-icon {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 50%;
}

@media (prefers-reduced-motion: no-preference) {
  .assistant-fab {
    animation: fabPulse 3s ease-in-out infinite;
  }
}

@keyframes fabPulse {
  0%,
  100% {
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.22);
  }
  50% {
    box-shadow: 0 8px 24px rgba(91, 140, 255, 0.35);
  }
}
</style>
