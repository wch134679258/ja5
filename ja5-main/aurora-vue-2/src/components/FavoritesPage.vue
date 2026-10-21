<template>
  <div class="page">
    <SiteNav :user="user" active="favorites" @go="$emit('navigate', $event)" @logout="$emit('logout')" />

    <main class="main">
      <header class="head">
        <span class="head__badge">FAVORITES</span>
        <h1 class="head__title">我的收藏</h1>
        <p class="head__sub" v-if="user">收藏的文章会按时间倒序排列，点击卡片可继续阅读</p>
      </header>

      <!-- 未登录：引导去登录 -->
      <div v-if="!user" class="state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
             stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8L3.5 9.7l5.9-.8z" />
        </svg>
        <p>登录后即可查看和管理你的收藏</p>
        <button class="state__btn state__btn--primary" type="button" @click="$emit('need-login')">
          去登录
        </button>
      </div>

      <!-- 加载中 -->
      <div v-else-if="loading" class="skeleton">
        <div v-for="n in 3" :key="n" class="sk-card">
          <div class="sk-line sk-line--title"></div>
          <div class="sk-line sk-line--sub"></div>
          <div class="sk-line sk-line--sub sk-line--short"></div>
        </div>
      </div>

      <!-- 加载失败 -->
      <div v-else-if="error" class="state state--error">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
             stroke-linecap="round">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v4.5M12 16h.01" />
        </svg>
        <p>{{ error }}</p>
        <button class="state__btn" type="button" @click="load(1)">重试</button>
      </div>

      <!-- 空收藏 -->
      <div v-else-if="!list.length" class="state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
             stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8L3.5 9.7l5.9-.8z" />
        </svg>
        <p>还没有收藏任何文章，去首页看看感兴趣的内容吧</p>
        <button class="state__btn state__btn--primary" type="button" @click="$emit('navigate', 'home')">
          回首页
        </button>
      </div>

      <!-- 收藏列表 -->
      <template v-else>
        <div class="results__head">
          <div class="results__headline">
            <span class="results__count">共 {{ total }} 篇收藏</span>
          </div>
        </div>

        <div class="rlist">
          <ArticleCard
            v-for="a in list"
            :key="a.id"
            :article="a"
            :favorited="true"
            :show-fav="true"
            @open="$emit('open-article', $event)"
            @open-category="$emit('open-category', $event)"
            @open-tag="$emit('open-tag', $event)"
            @toggle-fav="unfavorite"
          />
        </div>

        <!-- 分页 -->
        <div v-if="pages > 1" class="pager">
          <button class="pager__btn" type="button" :disabled="page <= 1" @click="go(page - 1)">
            上一页
          </button>
          <span class="pager__info">{{ page }} / {{ pages }}</span>
          <button class="pager__btn" type="button" :disabled="page >= pages" @click="go(page + 1)">
            下一页
          </button>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SiteNav from './SiteNav.vue'
import ArticleCard from './ArticleCard.vue'
import { fetchFavorites, removeFavorite } from '../api/favorites'

const props = defineProps({
  user: { type: Object, default: null }
})
defineEmits(['navigate', 'open-article', 'open-category', 'open-tag', 'logout', 'need-login'])

const PAGE_SIZE = 10
const list = ref([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const loading = ref(false)
const error = ref('')

async function load (p = 1) {
  loading.value = true
  error.value = ''
  page.value = p
  try {
    const res = await fetchFavorites({ page: p, size: PAGE_SIZE })
    list.value = (res && res.list) || []
    total.value = (res && res.total) || 0
    pages.value = (res && res.pages) || 0
    page.value = (res && res.page) || p
  } catch (e) {
    list.value = []
    total.value = 0
    pages.value = 0
    error.value = (e && e.message) || '加载收藏失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

function go (p) {
  if (p < 1 || (pages.value && p > pages.value)) return
  load(p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 在收藏页直接取消收藏：删除后刷新当前页（删空则回退一页） */
async function unfavorite (article) {
  try {
    await removeFavorite(article.id)
  } catch (e) {
    error.value = (e && e.message) || '取消收藏失败，请稍后再试'
    return
  }
  const target = list.value.length === 1 && page.value > 1 ? page.value - 1 : page.value
  load(target)
}

onMounted(() => {
  if (props.user) load(1)
})
</script>

<style scoped>
/* ============ 页面骨架（与首页一致） ============ */
.page {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1180px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 22px 24px 60px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 26px;
  padding-top: 22px;
}

/* 页头 */
.head {
  text-align: center;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  animation: rise 0.55s var(--ease) both;
}
.head__badge {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--a3);
  border: 1px solid rgba(255, 95, 162, 0.45);
  border-radius: 999px;
  padding: 3px 13px;
  margin-bottom: 16px;
}
.head__title {
  font-size: clamp(28px, 3.6vw, 40px);
  font-weight: 600;
  letter-spacing: -0.02em;
}
.head__sub {
  color: var(--muted);
  font-size: 14px;
  font-weight: 300;
  margin-top: 12px;
}

/* 结果头部 */
.results__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.results__headline {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}
.results__count {
  font-size: 12.5px;
  color: var(--muted);
  font-weight: 300;
}

/* 列表 */
.rlist {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 分页 */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 10px 0 4px;
}
.pager__btn {
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--ink);
  font: inherit;
  font-size: 13px;
  padding: 8px 18px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.25s, transform 0.25s var(--ease), opacity 0.25s;
}
.pager__btn:hover:not(:disabled) {
  background: var(--glass-2);
  transform: translateY(-1px);
}
.pager__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.pager__info {
  font-size: 12.5px;
  color: var(--muted);
  font-weight: 300;
}

/* ============ 骨架 / 状态 ============ */
.skeleton {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.sk-card {
  padding: 22px 24px 18px;
  border-radius: 18px;
  background: rgba(10, 12, 26, 0.5);
  border: 1px solid var(--stroke);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sk-line {
  height: 13px;
  border-radius: 7px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.06));
  background-size: 200% 100%;
  animation: shimmer 1.4s linear infinite;
}
.sk-line--title { width: 62%; height: 17px; }
.sk-line--sub { width: 88%; }
.sk-line--short { width: 45%; }
@keyframes shimmer {
  to { background-position: -200% 0; }
}
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 80px 24px;
  border-radius: 22px;
  background: rgba(10, 12, 26, 0.5);
  border: 1px dashed var(--stroke);
  color: var(--muted);
  font-size: 14px;
  font-weight: 300;
  text-align: center;
}
.state svg {
  width: 42px;
  height: 42px;
  opacity: 0.6;
}
.state--error svg {
  color: #ff8098;
  opacity: 0.9;
}
.state__btn {
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--ink);
  font: inherit;
  font-size: 13px;
  padding: 9px 22px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.25s, transform 0.25s var(--ease);
}
.state__btn:hover {
  background: var(--glass-2);
  transform: translateY(-1px);
}
.state__btn--primary {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(115deg, var(--a1), #5b8bff 45%, var(--a2));
  box-shadow: 0 12px 28px -12px rgba(124, 92, 255, 0.85);
}
.state__btn--primary:hover {
  background-position: right center;
}

@media (max-width: 640px) {
  .page {
    padding: 14px 14px 40px;
    gap: 14px;
  }
}
</style>
