<template>
  <!-- ============ 顶部导航（首页 / 发现 / 收藏 / 关于 / 文章详情共用） ============ -->
  <header class="nav">
    <div class="nav__bar">
      <!-- 品牌：点击回首页 -->
      <a class="nav__brand" href="#" @click.prevent="$emit('go', 'home')">
        <span class="logo__mark"><i></i><b></b></span>
        <span class="nav__brand-txt">AURORA</span>
      </a>

      <!-- 导航菜单（全部可用：发现 / 收藏 / 关于） -->
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

      <!-- 用户区 -->
      <div class="nav__user">
        <div class="nav__avatar" :title="userName">{{ avatarText }}</div>
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
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  user: { type: Object, default: null },
  /** 当前高亮的菜单 key：home / discover / favorites / about / game */
  active: { type: String, default: 'home' }
})
defineEmits(['go', 'logout'])

const menus = [
  { key: 'home', label: '首页' },
  { key: 'discover', label: '发现' },
  { key: 'game', label: '游戏' },
  { key: 'favorites', label: '收藏' },
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
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--a1), var(--a2));
  box-shadow: 0 8px 20px -10px var(--a1);
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

@media (max-width: 900px) {
  .nav__menu {
    display: none;
  }
}
@media (max-width: 640px) {
  .nav__brand-txt {
    display: none;
  }
  .nav__meta {
    display: none;
  }
  .nav__logout {
    padding: 0 12px;
  }
  .nav__bar {
    height: 60px;
    padding: 0 14px;
    gap: 12px;
    border-radius: 17px;
  }
}
</style>
