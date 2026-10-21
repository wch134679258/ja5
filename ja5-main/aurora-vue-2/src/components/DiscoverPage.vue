<template>
  <div class="page">
    <SiteNav :user="user" active="discover" @go="$emit('navigate', $event)" @logout="$emit('logout')" />

    <main class="main">
      <header class="head">
        <span class="head__badge">DISCOVER</span>
        <h1 class="head__title">发现</h1>
        <p class="head__sub">最新发布、本周热门，以及全部内容分类</p>
      </header>

      <!-- 分类入口 -->
      <section v-if="categories.length" class="cats">
        <div class="sec__head">
          <h2 class="sec__title">逛分类</h2>
          <button class="sec__more" type="button" @click="$emit('open-home', {})">全部内容 →</button>
        </div>
        <div class="cats__chips">
          <button
            v-for="c in categories"
            :key="c.id"
            class="cat"
            type="button"
            @click="$emit('open-home', { categoryId: c.id })"
          >
            {{ c.name }}
            <em>{{ c.articleCount }}</em>
          </button>
        </div>
      </section>

      <!-- 最新文章 -->
      <section class="sec">
        <div class="sec__head">
          <h2 class="sec__title">最新发布</h2>
          <button class="sec__more" type="button" @click="$emit('open-home', { sort: 'new' })">更多 →</button>
        </div>
        <div v-if="loading" class="skeleton">
          <div v-for="n in 3" :key="'n' + n" class="sk-card">
            <div class="sk-line sk-line--title"></div>
            <div class="sk-line sk-line--sub"></div>
          </div>
        </div>
        <div v-else-if="error" class="state state--error">
          <p>{{ error }}</p>
          <button class="state__btn" type="button" @click="load">重试</button>
        </div>
        <div v-else-if="!latest.length" class="state">
          <p>暂无最新内容</p>
        </div>
        <div v-else class="rlist">
          <ArticleCard
            v-for="a in latest"
            :key="a.id"
            :article="a"
            :favorited="favIds.has(a.id)"
            :show-fav="!!user"
            @open="$emit('open-article', $event)"
            @open-category="$emit('open-category', $event)"
            @open-tag="$emit('open-tag', $event)"
            @toggle-fav="toggleFav"
          />
        </div>
      </section>

      <!-- 热门文章 -->
      <section class="sec">
        <div class="sec__head">
          <h2 class="sec__title">热门推荐</h2>
          <button class="sec__more" type="button" @click="$emit('open-home', { sort: 'hot' })">更多 →</button>
        </div>
        <div v-if="loading" class="skeleton">
          <div v-for="n in 3" :key="'h' + n" class="sk-card">
            <div class="sk-line sk-line--title"></div>
            <div class="sk-line sk-line--sub"></div>
          </div>
        </div>
        <div v-else-if="!hot.length" class="state">
          <p>暂无热门内容</p>
        </div>
        <div v-else class="rlist">
          <ArticleCard
            v-for="a in hot"
            :key="a.id"
            :article="a"
            :favorited="favIds.has(a.id)"
            :show-fav="!!user"
            @open="$emit('open-article', $event)"
            @open-category="$emit('open-category', $event)"
            @open-tag="$emit('open-tag', $event)"
            @toggle-fav="toggleFav"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import SiteNav from './SiteNav.vue'
import ArticleCard from './ArticleCard.vue'
import { searchArticles, fetchCategories } from '../api/search'
import { fetchFavoriteIds, addFavorite, removeFavorite } from '../api/favorites'

const props = defineProps({
  user: { type: Object, default: null }
})
defineEmits(['navigate', 'open-article', 'open-category', 'open-tag', 'open-home', 'logout'])

const LIST_SIZE = 6

const categories = ref([])
const latest = ref([])
const hot = ref([])
const favIds = ref(new Set())
const loading = ref(false)
const error = ref('')

const hasContent = computed(() => latest.value.length || hot.value.length)

async function load () {
  loading.value = true
  error.value = ''
  try {
    // 最新 / 热门并行拉取；分类树与收藏态失败不阻塞列表
    const [newest, hottest, cats] = await Promise.all([
      searchArticles('', { sort: 'new', page: 1, size: LIST_SIZE }),
      searchArticles('', { sort: 'hot', page: 1, size: LIST_SIZE }),
      fetchCategories().catch(() => [])
    ])
    latest.value = (newest && newest.list) || []
    hot.value = (hottest && hottest.list) || []
    categories.value = cats || []

    // 登录用户：拉取收藏 id 集合，用于卡片收藏态
    if (props.user) {
      try {
        const ids = await fetchFavoriteIds()
        favIds.value = new Set(ids)
      } catch (_) { /* 收藏态加载失败不阻塞浏览 */ }
    }
  } catch (e) {
    latest.value = []
    hot.value = []
    error.value = (e && e.message) || '加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/** 卡片收藏 / 取消收藏：直接更新本地集合，无需整页刷新 */
async function toggleFav (article) {
  const on = favIds.value.has(article.id)
  try {
    if (on) await removeFavorite(article.id)
    else await addFavorite(article.id)
  } catch (e) {
    error.value = (e && e.message) || '操作失败，请稍后再试'
    return
  }
  const next = new Set(favIds.value)
  if (on) next.delete(article.id)
  else next.add(article.id)
  favIds.value = next
}

onMounted(load)
</script>

<style scoped>
/* ============ 页面骨架 ============ */
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
  gap: 34px;
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
  color: var(--a2);
  border: 1px solid rgba(34, 211, 238, 0.45);
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

/* 区块 */
.sec {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sec__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.sec__title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.sec__more {
  border: 0;
  background: none;
  color: var(--a2);
  font: inherit;
  font-size: 12.5px;
  cursor: pointer;
  padding: 0;
  transition: filter 0.25s;
}
.sec__more:hover {
  filter: brightness(1.3);
}

/* 分类 chips */
.cats {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 22px;
  border-radius: 20px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.015) 50%, rgba(255, 255, 255, 0.045)),
    rgba(10, 12, 26, 0.52);
  backdrop-filter: blur(22px) saturate(140%);
  -webkit-backdrop-filter: blur(22px) saturate(140%);
  border: 1px solid var(--stroke);
  animation: rise 0.55s var(--ease) both;
}
.cats__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cat {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--ink);
  font: inherit;
  font-size: 13.5px;
  padding: 7px 15px;
  border-radius: 11px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.cat em {
  font-style: normal;
  font-size: 11px;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.07);
  border-radius: 999px;
  padding: 1px 7px;
}
.cat:hover {
  border-color: rgba(34, 211, 238, 0.5);
  background: rgba(34, 211, 238, 0.12);
  transform: translateY(-1px);
}

/* 列表 */
.rlist {
  display: flex;
  flex-direction: column;
  gap: 14px;
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
@keyframes shimmer {
  to { background-position: -200% 0; }
}
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 48px 24px;
  border-radius: 18px;
  background: rgba(10, 12, 26, 0.5);
  border: 1px dashed var(--stroke);
  color: var(--muted);
  font-size: 13.5px;
  font-weight: 300;
  text-align: center;
}
.state--error {
  border-color: rgba(255, 95, 120, 0.35);
  color: #ffb3c0;
}
.state__btn {
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--ink);
  font: inherit;
  font-size: 13px;
  padding: 8px 20px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.25s, transform 0.25s var(--ease);
}
.state__btn:hover {
  background: var(--glass-2);
  transform: translateY(-1px);
}

@media (max-width: 640px) {
  .page {
    padding: 14px 14px 40px;
    gap: 14px;
  }
  .main {
    gap: 26px;
  }
}
</style>
