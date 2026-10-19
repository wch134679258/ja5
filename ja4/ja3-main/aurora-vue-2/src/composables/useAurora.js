// 极光 + 粒子星座背景 canvas 逻辑
import { onMounted, onBeforeUnmount } from 'vue'

export function useAurora (canvasRef) {
  let ctx = null
  let W = 0, H = 0, dpr = 1, t = 0
  let parts = []
  let rafId = null
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches
  const mouse = { x: -9999, y: -9999 }

  const BLOBS = [
    { x: .18, y: .28, r: .62, c: '124,92,255', s: .00021, p: 0 },
    { x: .82, y: .24, r: .52, c: '34,211,238', s: .00017, p: 2.1 },
    { x: .72, y: .80, r: .60, c: '255,95,162', s: .00013, p: 4.2 },
    { x: .28, y: .86, r: .48, c: '59,130,246', s: .00025, p: 1.1 }
  ]

  function resize () {
    const cvs = canvasRef.value
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    W = window.innerWidth
    H = window.innerHeight
    cvs.width = W * dpr
    cvs.height = H * dpr
    cvs.style.width = W + 'px'
    cvs.style.height = H + 'px'
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    const n = reduce ? 0 : Math.min(110, Math.round((W * H) / 15000))
    parts = Array.from({ length: n }, () => ({
      x: Math.random() * W,
      y: Math.random() * H,
      vx: (Math.random() - .5) * .22,
      vy: (Math.random() - .5) * .22,
      r: Math.random() * 1.6 + .5,
      a: Math.random() * .5 + .25
    }))
  }

  function draw () {
    t += 16
    ctx.globalCompositeOperation = 'source-over'
    ctx.fillStyle = '#05060f'
    ctx.fillRect(0, 0, W, H)

    ctx.globalCompositeOperation = 'lighter'
    const m = Math.min(W, H)
    for (const b of BLOBS) {
      const x = (b.x + Math.sin(t * b.s + b.p) * .13) * W
      const y = (b.y + Math.cos(t * b.s * 1.27 + b.p) * .11) * H
      const r = b.r * m * (.85 + Math.sin(t * b.s * 1.9 + b.p) * .15)
      const g = ctx.createRadialGradient(x, y, 0, x, y, r)
      g.addColorStop(0, `rgba(${b.c},.50)`)
      g.addColorStop(.42, `rgba(${b.c},.13)`)
      g.addColorStop(1, `rgba(${b.c},0)`)
      ctx.fillStyle = g
      ctx.beginPath()
      ctx.arc(x, y, r, 0, 6.2832)
      ctx.fill()
    }

    for (const p of parts) {
      p.x += p.vx
      p.y += p.vy
      if (p.x < -20) p.x = W + 20
      if (p.x > W + 20) p.x = -20
      if (p.y < -20) p.y = H + 20
      if (p.y > H + 20) p.y = -20

      const dx = p.x - mouse.x
      const dy = p.y - mouse.y
      const d2 = dx * dx + dy * dy
      if (d2 < 22500 && d2 > 0) {
        const d = Math.sqrt(d2)
        const f = (150 - d) / 150
        p.x += (dx / d) * f * 1.9
        p.y += (dy / d) * f * 1.9
      }
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, 6.2832)
      ctx.fillStyle = `rgba(210,225,255,${p.a})`
      ctx.fill()
    }
    ctx.lineWidth = .6
    for (let i = 0; i < parts.length; i++) {
      for (let j = i + 1; j < parts.length; j++) {
        const a = parts[i]
        const b = parts[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const d2 = dx * dx + dy * dy
        if (d2 < 12100) {
          const al = (1 - d2 / 12100) * .22
          ctx.strokeStyle = `rgba(150,190,255,${al})`
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }

    if (mouse.x > -9000) {
      const g = ctx.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, 220)
      g.addColorStop(0, 'rgba(124,92,255,.16)')
      g.addColorStop(1, 'rgba(124,92,255,0)')
      ctx.fillStyle = g
      ctx.beginPath()
      ctx.arc(mouse.x, mouse.y, 220, 0, 6.2832)
      ctx.fill()
    }

    ctx.globalCompositeOperation = 'source-over'
    rafId = requestAnimationFrame(draw)
  }

  function drawOnce () {
    ctx.fillStyle = '#05060f'
    ctx.fillRect(0, 0, W, H)
    ctx.globalCompositeOperation = 'lighter'
    const m = Math.min(W, H)
    for (const b of BLOBS) {
      const g = ctx.createRadialGradient(b.x * W, b.y * H, 0, b.x * W, b.y * H, b.r * m)
      g.addColorStop(0, `rgba(${b.c},.5)`)
      g.addColorStop(1, `rgba(${b.c},0)`)
      ctx.fillStyle = g
      ctx.beginPath()
      ctx.arc(b.x * W, b.y * H, b.r * m, 0, 6.2832)
      ctx.fill()
    }
    ctx.globalCompositeOperation = 'source-over'
  }

  let onMove, onLeave, onResize

  onMounted(() => {
    ctx = canvasRef.value.getContext('2d')
    onMove = (e) => { mouse.x = e.clientX; mouse.y = e.clientY }
    onLeave = () => { mouse.x = mouse.y = -9999 }
    onResize = resize
    window.addEventListener('pointermove', onMove, { passive: true })
    window.addEventListener('pointerleave', onLeave)
    window.addEventListener('resize', onResize)
    resize()
    if (reduce) drawOnce()
    else draw()
  })

  onBeforeUnmount(() => {
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerleave', onLeave)
    window.removeEventListener('resize', onResize)
    if (rafId) cancelAnimationFrame(rafId)
  })
}
