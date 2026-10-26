// ============================================================
//  全局轻提示（Toast）
// ------------------------------------------------------------
//  用法：
//    import { toast } from '../composables/useToast'
//    toast.success('已收藏')
//    toast.error('网络异常，请重试')
//    toast.info('已清空浏览历史')
//
//  设计要点：
//    - 模块级单例：任何组件调用都进同一个队列，App.vue 里挂一次 <ToastHost/> 即可
//    - 自动消失：默认 2.6 秒，鼠标悬停时不消失，避免误关
//    - 最多同时 3 条，超出先挤掉最早的，防止刷屏
// ============================================================

import { reactive } from 'vue'

const MAX = 3
const DEFAULT_DURATION = 2600

export const toastState = reactive({
  items: [] // { id, type: 'success'|'error'|'info', text, timer, paused }
})

let seed = 0

function dismiss (id) {
  const idx = toastState.items.findIndex((t) => t.id === id)
  if (idx === -1) return
  const item = toastState.items[idx]
  if (item.timer) clearTimeout(item.timer)
  toastState.items.splice(idx, 1)
}

function push (text, type = 'info', duration = DEFAULT_DURATION) {
  if (!text) return
  const id = ++seed

  // 超出上限：先关掉最早的一条
  while (toastState.items.length >= MAX) {
    dismiss(toastState.items[0].id)
  }

  const item = { id, type, text, timer: null, paused: false }
  const start = () => {
    item.timer = setTimeout(() => {
      if (item.paused) {
        // 悬停中：稍后再试，直到鼠标移开
        item.timer = setTimeout(() => dismiss(id), 600)
        return
      }
      dismiss(id)
    }, duration)
  }
  start()

  toastState.items.push(item)
  return id
}

export const toast = {
  success: (text, duration) => push(text, 'success', duration),
  error: (text, duration) => push(text, 'error', duration),
  info: (text, duration) => push(text, 'info', duration),
  dismiss
}

/** ToastHost 内部使用：鼠标悬停暂停 / 移开恢复 */
export function pauseToast (id) {
  const item = toastState.items.find((t) => t.id === id)
  if (item) item.paused = true
}
export function resumeToast (id) {
  const item = toastState.items.find((t) => t.id === id)
  if (item) item.paused = false
}
