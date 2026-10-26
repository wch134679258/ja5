<template>
  <footer class="foot">
    <div class="foot__inner">
      <div class="foot__brand">
        <span class="logo__mark"><i></i><b></b></span>
        <div>
          <p class="foot__name">AURORA · 星海</p>
          <p class="foot__slogan">在极光下，把学到的每一件事写下来。</p>
        </div>
      </div>

      <nav class="foot__links">
        <button v-for="l in links" :key="l.key" class="foot__link" type="button" @click="$emit('go', l.key)">
          {{ l.label }}
        </button>
      </nav>

      <div class="foot__meta">
        <span>© {{ year }} Aurora 星海 · 演示项目</span>
        <span class="foot__dot">·</span>
        <span>Vue 3 + Vite + Spring Boot 3</span>
        <span class="foot__dot">·</span>
        <span>数据仅用于本地演示</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
defineProps({
  /** 是否显示快捷链接（登录页不需要） */
  showLinks: { type: Boolean, default: true }
})
defineEmits(['go'])

const links = [
  { key: 'home', label: '首页' },
  { key: 'discover', label: '发现' },
  { key: 'favorites', label: '收藏' },
  { key: 'profile', label: '我的' },
  { key: 'about', label: '关于' }
]

const year = new Date().getFullYear()
</script>

<style scoped>
.foot {
  margin-top: 46px;
  padding: 0 4px 10px;
}

.foot__inner {
  border-top: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  padding-top: 22px;
  display: grid;
  gap: 14px;
}

.foot__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo__mark {
  width: 30px;
  height: 30px;
  position: relative;
  display: grid;
  place-items: center;
  flex: none;
}
.logo__mark i {
  position: absolute;
  inset: 0;
  border-radius: 10px;
  border: 1.5px solid transparent;
  background: conic-gradient(from 140deg, var(--a2, #22d3ee), var(--a1, #7c5cff), var(--a3, #ff5fa2), var(--a2, #22d3ee)) border-box;
  -webkit-mask: linear-gradient(#000 0 0) padding-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: spin 6s linear infinite;
}
.logo__mark b {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(140deg, var(--a2, #22d3ee), var(--a1, #7c5cff));
  box-shadow: 0 0 14px var(--a1, #7c5cff);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.foot__name {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #cfd6ff;
}
.foot__slogan {
  font-size: 12.5px;
  color: var(--muted, #8d97bd);
  margin-top: 3px;
}

.foot__links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.foot__link {
  border: 0;
  background: none;
  font: inherit;
  font-size: 13px;
  color: var(--muted, #8d97bd);
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: color .25s, background .25s;
}
.foot__link:hover {
  color: var(--ink, #eef2ff);
  background: rgba(255, 255, 255, .07);
}

.foot__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(141, 151, 189, .8);
}
.foot__dot { opacity: .5; }
</style>
