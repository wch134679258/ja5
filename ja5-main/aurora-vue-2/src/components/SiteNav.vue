<template>
  <!-- ============ 顶部导航（首页 / 发现 / 收藏 / 关于 / 文章详情共用） ============ -->
  <header class="nav">
    <div class="nav__bar">
      <!-- 品牌：点击回首页 -->
      <a class="nav__brand" href="#" @click.prevent="$emit('go', 'home')">
        <span class="logo__mark"><i></i><b></b></span>
        <span class="nav__brand-txt">AURORA</span>
      </a>

      <!-- 导航菜单（≥900px 常驻） -->
      <nav class="nav__menu">
        <button
          v-for="item in menus"
          :key="item.key"
          class="nav__item"
          :class="{ on: active === item.key }"
          type="button"
          @click="$emit('go', item.key)"
        >
          {{ item.label }}
        </button>
      </nav>

      <!-- 用户区（头像可点，直达个人中心） -->
      <div class="nav__user">
        <button
          class="nav__avatar"
          type="button"
          :title="userName + ' · 个人中心'"
          aria-label="进入个人中心"
          @click="$emit('go', 'profile')"
        >{{ avatarText }}</button>
        <div class="nav__meta">
          <b>{{ userName }}</b>
          <span>{{ userSub }}</span>
        </div>
        <button class="nav__logout" type="button" @click="$emit('logout')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 17l5-5-5-5" />
            <path d="M20 12H9" />
            <path d="M12 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h6" />
          </svg>
          退出
        </button>

        <!-- 汉堡按钮：<900px 才出现，打开移动端抽屉 -->
        <button
          class="nav__burger"
          type="button"
          aria-label="打开导航菜单"
          aria-haspopup="dialog"
          :aria-expanded="open"
          @click="open = true"
        >
          <i></i><i></i><i></i>
        </button>
      </div>
    </div>
  </header>

  <!-- 移动端抽屉：Teleport 到 body，避开 .nav__bar 的 backdrop-filter 造成的定位容器 -->
  <Teleport to="body">
    <Transition name="md">
      <div v-if="open" class="md" role="dialog" aria-modal="true" aria-label="导航菜单" @click.self="close">
        <div class="md__panel">
          <div class="md__head">
            <span class="md__head-title">导航</span>
            <button class="md__close" type="button" aria-label="关闭菜单" @click="close">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                   stroke-linecap="round">
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </div>

          <div class="md__user">
            <span class="md__avatar">{{ avatarText }}</span>
            <div class="md__user-info">
              <b>{{ userName }}</b>
              <span>{{ userSub }}</span>
            </div>
          </div>

          <nav class="md__list">
            <button
              v-for="item in menus"
              :key="item.key"
              class="md__item"
              :class="{ on: active === item.key }"
              type="button"
              :aria-current="active === item.key ? 'page' : undefined"
              @click="pick(item.key)"
            >
              <span class="md__item-txt">{{ item.label }}</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                   stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 6l6 6-6 6" />
              </svg>
            </button>
          </nav>

          <button class="md__logout" type="button" @click="onLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 17l5-5-5-5" />
              <path d="M20 12H9" />
              <path d="M12 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h6" />
            </svg>
            退出登录
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  user: { type: Object, default: null },
  /** 当前高亮的菜单 key：home / discover / favorites / about / game */
  active: { type: String, default: 'home' }
})
const emit = defineEmits(['go', 'logout'])

const menus = [
  { key: 'home', label: '首页' },
  { key: 'discover', label: '发现' },
  { key: 'game', label: '游戏' },
  { key: 'favorites', label: '收藏' },
  { key: 'profile', label: '我的' },
  { key: 'about', label: '关于' }
]

const userName = computed(() => (props.user && props.user.name) || '探险家')
const userSub = computed(() => {
  const id = props.user && props.user.id
  return id != null ? `ID · ${id}` : '已登录'
})
const avatarText = computed(() => {
  const n = userName.value
  return n ? n.slice(0, 1).toUpperCase() : 'A'
})

/* ---------------- 移动端抽屉 ---------------- */
const open = ref(false)

/** 抽屉打开时锁住背景滚动，关闭时还原原来的内联值（可能为空） */
let prevOverflow = ''
function lockScroll () {
  prevOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
}
function unlockScroll () {
  // 原来没有内联值时直接移除属性，别在 body 上留一个空的 style=""
  if (prevOverflow) document.body.style.overflow = prevOverflow
  else document.body.style.removeProperty('overflow')
}

function close () {
  open.value = false
}
function pick (key) {
  close()
  emit('go', key)
}
function onLogout () {
  close()
  emit('logout')
}

/** ESC 关闭 */
function onKeydown (e) {
  if (e.key === 'Escape') close()
}
/** 转回桌面宽度时自动收起，避免抽屉悬在大屏上 */
function onResize () {
  if (window.innerWidth > 900) close()
}

watch(open, (v) => {
  if (v) {
    lockScroll()
    window.addEventListener('keydown', onKeydown)
    window.addEventListener('resize', onResize)
  } else {
    unlockScroll()
    window.removeEventListener('keydown', onKeydown)
    window.removeEventListener('resize', onResize)
  }
})

onBeforeUnmount(() => {
  if (open.value) unlockScroll()
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
/* ============ 顶部导航 ============ */
.nav {
  position: sticky;
  top: 18px;
  z-index: 5;
}
.nav__bar {
  display: flex;
  align-items: center;
  gap: 22px;
  height: 68px;
  padding: 0 20px;
  border-radius: 20px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.02) 45%, rgba(255, 255, 255, 0.06)),
    rgba(10, 12, 26, 0.72);
  backdrop-filter: blur(26px) saturate(150%);
  -webkit-backdrop-filter: blur(26px) saturate(150%);
  border: 1px solid var(--stroke);
  box-shadow: 0 26px 60px -30px rgba(0, 0, 0, 0.95);
  will-change: transform;
}

/* 品牌 */
.nav__brand {
  display: flex;
  align-items: center;
  gap: 11px;
  text-decoration: none;
  flex: none;
  cursor: pointer;
  transition: opacity 0.25s;
}
.nav__brand:hover {
  opacity: 0.85;
}
.nav__brand .logo__mark {
  width: 34px;
  height: 34px;
  position: relative;
  display: grid;
  place-items: center;
}
.nav__brand .logo__mark i {
  position: absolute;
  inset: 0;
  border-radius: 11px;
  border: 1.5px solid;
  border-color: transparent;
  background: conic-gradient(from 140deg, var(--a2), var(--a1), var(--a3), var(--a2)) border-box;
  -webkit-mask: linear-gradient(#000 0 0) padding-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: spin 5s linear infinite;
}
.nav__brand .logo__mark b {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: linear-gradient(140deg, var(--a2), var(--a1));
  box-shadow: 0 0 16px var(--a1);
}
.nav__brand-txt {
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: #cfd6ff;
}

/* 菜单 */
.nav__menu {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 6px;
}
.nav__item {
  position: relative;
  border: 0;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  padding: 9px 15px;
  border-radius: 11px;
  cursor: pointer;
  transition: color 0.3s, background 0.3s;
}
.nav__item:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.06);
}
.nav__item.on {
  color: #fff;
  background: linear-gradient(120deg, rgba(124, 92, 255, 0.9), rgba(34, 211, 238, 0.75));
  box-shadow: 0 8px 20px -8px var(--a1);
}

/* 用户区 */
.nav__user {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-left: auto;
  flex: none;
}
.nav__avatar {
  width: 38px;
  height: 38px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 12px;
  border: 1px solid transparent;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, var(--a1), var(--a2));
  box-shadow: 0 8px 20px -10px var(--a1);
  transition: transform 0.25s var(--ease), box-shadow 0.25s var(--ease);
}
.nav__avatar:hover {
  transform: translateY(-1px) scale(1.04);
  box-shadow: 0 12px 24px -10px var(--a1);
}
.nav__meta {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  min-width: 0;
}
.nav__meta b {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 12ch;
}
.nav__meta span {
  font-size: 11px;
  color: var(--muted);
  letter-spacing: 0.05em;
}
.nav__logout {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 15px;
  border-radius: 11px;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  background: var(--glass);
  border: 1px solid var(--stroke);
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.nav__logout:hover {
  color: #ffb3c0;
  background: rgba(255, 95, 120, 0.12);
  border-color: rgba(255, 95, 120, 0.4);
  transform: translateY(-1px);
}
.nav__logout svg {
  width: 15px;
  height: 15px;
}

/* 汉堡按钮（桌面隐藏） */
.nav__burger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 40px;
  height: 38px;
  padding: 0 10px;
  border-radius: 11px;
  cursor: pointer;
  background: var(--glass);
  border: 1px solid var(--stroke);
  transition: background 0.25s, border-color 0.25s;
}
.nav__burger i {
  display: block;
  height: 1.6px;
  width: 100%;
  border-radius: 2px;
  background: #cfd6ff;
  transition: opacity 0.2s;
}
.nav__burger i:nth-child(2) {
  width: 70%;
}
.nav__burger:hover {
  background: var(--glass-2);
  border-color: rgba(255, 255, 255, 0.2);
}

/* ============ 移动端抽屉 ============ */
.md {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  justify-content: flex-end;
  background: rgba(4, 5, 14, 0.62);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.md__panel {
  width: min(320px, 84vw);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
  background:
    linear-gradient(170deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.02) 45%, rgba(255, 255, 255, 0.05)),
    rgba(9, 11, 24, 0.96);
  border-left: 1px solid var(--stroke);
  box-shadow: -30px 0 70px -30px rgba(0, 0, 0, 0.9);
  overflow-y: auto;
  overscroll-behavior: contain;
}
.md__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 14px;
}
.md__head-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.24em;
  color: var(--muted);
  text-transform: uppercase;
}
.md__close {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  cursor: pointer;
  color: var(--muted);
  background: var(--glass);
  border: 1px solid var(--stroke);
  transition: color 0.2s, background 0.2s;
}
.md__close:hover {
  color: var(--ink);
  background: var(--glass-2);
}
.md__close svg {
  width: 16px;
  height: 16px;
}

.md__user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 14px;
  border-radius: 16px;
  background: var(--glass);
  border: 1px solid var(--stroke);
}
.md__avatar {
  width: 42px;
  height: 42px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 13px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--a1), var(--a2));
  box-shadow: 0 8px 20px -10px var(--a1);
}
.md__user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
  min-width: 0;
}
.md__user-info b {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.md__user-info span {
  font-size: 11.5px;
  color: var(--muted);
  letter-spacing: 0.06em;
}

.md__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.md__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 13px 14px;
  border-radius: 13px;
  border: 1px solid transparent;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 15px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}
.md__item svg {
  width: 16px;
  height: 16px;
  opacity: 0.45;
  transition: opacity 0.2s, transform 0.2s var(--ease);
}
.md__item:hover,
.md__item:active {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.06);
}
.md__item:hover svg {
  opacity: 1;
  transform: translateX(2px);
}
.md__item.on {
  color: #fff;
  background: linear-gradient(120deg, rgba(124, 92, 255, 0.85), rgba(34, 211, 238, 0.6));
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: 0 10px 24px -14px var(--a1);
}
.md__item.on svg {
  opacity: 0.95;
}

.md__logout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: auto;
  padding: 13px;
  border-radius: 13px;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  color: var(--muted);
  background: var(--glass);
  border: 1px solid var(--stroke);
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}
.md__logout:hover {
  color: #ffb3c0;
  background: rgba(255, 95, 120, 0.12);
  border-color: rgba(255, 95, 120, 0.4);
}
.md__logout svg {
  width: 15px;
  height: 15px;
}

/* 进出场：遮罩淡入 + 面板右滑 */
.md-enter-active,
.md-leave-active {
  transition: opacity 0.26s var(--ease);
}
.md-enter-active .md__panel,
.md-leave-active .md__panel {
  transition: transform 0.3s var(--ease);
}
.md-enter-from,
.md-leave-to {
  opacity: 0;
}
.md-enter-from .md__panel,
.md-leave-to .md__panel {
  transform: translateX(100%);
}

@media (max-width: 900px) {
  .nav__menu {
    display: none;
  }
  .nav__logout {
    display: none;
  }
  .nav__burger {
    display: flex;
  }
}
@media (max-width: 640px) {
  .nav__brand-txt {
    display: none;
  }
  .nav__meta {
    display: none;
  }
  .nav__bar {
    height: 60px;
    padding: 0 14px;
    gap: 12px;
    border-radius: 17px;
  }
  .nav__avatar {
    width: 34px;
    height: 34px;
    border-radius: 11px;
    font-size: 14px;
  }
  .nav__burger {
    height: 34px;
  }
}
</style>
