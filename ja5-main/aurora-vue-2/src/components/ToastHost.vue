<template>
  <!-- 全局提示宿主：在 App.vue 里挂载一次，所有页面共用 -->
  <div class="toasts" role="status" aria-live="polite">
    <TransitionGroup name="toast">
      <div
        v-for="t in toastState.items"
        :key="t.id"
        class="toast"
        :class="'toast--' + t.type"
        @mouseenter="pauseToast(t.id)"
        @mouseleave="resumeToast(t.id)"
        @click="toast.dismiss(t.id)"
      >
        <span class="toast__icon" aria-hidden="true">{{ iconOf(t.type) }}</span>
        <span class="toast__text">{{ t.text }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { toastState, toast, pauseToast, resumeToast } from '../composables/useToast'

function iconOf (type) {
  if (type === 'success') return '✓'
  if (type === 'error') return '!'
  return 'i'
}
</script>

<style scoped>
.toasts {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  pointer-events: none;
}

.toast {
  pointer-events: auto;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: min(88vw, 460px);
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: rgba(16, 18, 38, .92);
  backdrop-filter: blur(14px);
  color: var(--ink, #eef2ff);
  font-size: 14px;
  line-height: 1.4;
  box-shadow: 0 12px 32px rgba(0, 0, 0, .45);
}

.toast__icon {
  flex: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #061018;
}

.toast--success .toast__icon { background: #34d399; }
.toast--error .toast__icon { background: #ff6b8b; }
.toast--info .toast__icon { background: #7cc4ff; }

.toast--success { border-color: rgba(52, 211, 153, .45); }
.toast--error { border-color: rgba(255, 107, 139, .45); }

.toast__text { word-break: break-word; }

/* 入场 / 出场动画 */
.toast-enter-from { opacity: 0; transform: translateY(-10px) scale(.96); }
.toast-enter-active { transition: all .28s cubic-bezier(.22, 1, .36, 1); }
.toast-leave-active { transition: all .22s ease; position: absolute; }
.toast-leave-to { opacity: 0; transform: translateY(-8px) scale(.96); }
.toast-move { transition: transform .22s ease; }
</style>
