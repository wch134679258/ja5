// 卡片 3D 倾斜 + 光标光斑逻辑
import { ref } from 'vue'

export function useTilt () {
  const card = ref(null)
  const spot = ref(null)
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches
  let raf = null

  function onMove (e) {
    if (reduce || !card.value) return
    const r = card.value.getBoundingClientRect()
    const px = (e.clientX - r.left) / r.width
    const py = (e.clientY - r.top) / r.height
    spot.value.style.setProperty('--mx', (px * 100).toFixed(2) + '%')
    spot.value.style.setProperty('--my', (py * 100).toFixed(2) + '%')
    if (raf) cancelAnimationFrame(raf)
    raf = requestAnimationFrame(() => {
      const ry = (px - .5) * 7
      const rx = (py - .5) * -7
      card.value.style.transform = `perspective(1400px) rotateX(${rx.toFixed(2)}deg) rotateY(${ry.toFixed(2)}deg) translateY(-4px)`
    })
  }

  function onLeave () {
    if (raf) cancelAnimationFrame(raf)
    if (card.value) card.value.style.transform = 'perspective(1400px) rotateX(0) rotateY(0) translateY(0)'
  }

  return { card, spot, onMove, onLeave }
}
