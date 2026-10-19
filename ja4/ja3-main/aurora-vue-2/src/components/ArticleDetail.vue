<template>
  <div class="detail">
    <SiteNav :user="user" active="article" @go="$emit('navigate', $event)" @logout="$emit('logout')" />

    <main class="main">
      <!-- 返回 + 操作行 -->
      <div class="topbar">
        <button class="back" type="button" @click="$emit('back')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
          返回
        </button>

        <!-- 收藏按钮：未登录点击 -> 引导登录 -->
        <button
          v-if="article"
          class="fav"
          :class="{ 'fav--on': article.favorited, 'fav--pending': favPending }"
          type="button"
          :disabled="favPending"
          @click="toggleFavorite"
        >
          <svg viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="1.5"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8L3.5 9.7l5.9-.8z" />
          </svg>
          <span>{{ favText }}</span>
        </button>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading" class="skeleton">
        <div class="sk-line sk-line--cat"></div>
        <div class="sk-line sk-line--title"></div>
        <div class="sk-line sk-line--meta"></div>
        <div class="sk-line"></div>
        <div class="sk-line"></div>
        <div class="sk-line sk-line--short"></div>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="error" class="state state--error">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
             stroke-linecap="round">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v4.5M12 16h.01" />
        </svg>
        <p>{{ error }}</p>
        <button class="state__btn" type="button" @click="load">重试</button>
      </div>

      <!-- 文章正文 -->
      <template v-else-if="article">
        <article class="paper">
          <header class="paper__head">
            <button
              v-if="article.category"
              class="paper__cat"
              type="button"
              @click="$emit('open-category', article.categoryId, article.category)"
            >{{ article.category }}</button>
            <h1 class="paper__title">{{ article.title }}</h1>
            <div class="paper__meta">
              <span class="paper__author">{{ article.author || '佚名' }}</span>
              <span class="paper__dot"></span>
              <span>{{ article.createdAt }}</span>
              <span class="paper__dot"></span>
              <span>{{ article.views }} 次浏览</span>
            </div>
          </header>

          <div class="paper__body">
            <p v-for="(para, i) in paragraphs" :key="i">{{ para }}</p>
          </div>

          <footer v-if="article.tags && article.tags.length" class="paper__tags">
            <button
              v-for="t in article.tags"
              :key="t"
              class="rtag"
              type="button"
              @click="$emit('open-tag', t)"
            >{{ t }}</button>
          </footer>
        </article>

        <!-- 收藏引导条：未登录时说明 -->
        <p v-if="!user" class="login-hint">
          登录后即可收藏这篇文章 ·
          <button class="link-inline" type="button" @click="$emit('need-login')">去登录</button>
        </p>

        <!-- 相关文章 -->
        <section v-if="related.length" class="related">
          <h2 class="related__title">相关文章</h2>
          <div class="related__grid">
            <article
              v-for="a in related"
              :key="a.id"
              class="relcard"
              tabindex="0"
              role="button"
              @click="$emit('open-article', a.id)"
              @keydown.enter="$emit('open-article', a.id)"
            >
              <h3 class="relcard__title">{{ a.title }}</h3>
              <p class="relcard__summary">{{ a.summary }}</p>
              <div class="relcard__foot">
                <span>{{ a.author }}</span>
                <span class="paper__dot"></span>
                <span>{{ a.views }} 次浏览</span>
              </div>
            </article>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import SiteNav from './SiteNav.vue'
import { fetchArticle, fetchRelated } from '../api/article'
import { addFavorite, removeFavorite } from '../api/favorites'

const props = defineProps({
  articleId: { type: [String, Number], required: true },
  user: { type: Object, default: null }
})
defineEmits(['back', 'open-article', 'open-category', 'open-tag', 'navigate', 'logout', 'need-login'])

const article = ref(null)
const related = ref([])
const loading = ref(false)
const error = ref('')
const favPending = ref(false)

/** 正文按空行拆段，空行合并 */
const paragraphs = computed(() => {
  const c = article.value && article.value.content
  if (!c) return []
  return String(c)
    .split(/\n\s*\n/)
    .map((p) => p.replace(/\n/g, ' ').trim())
    .filter(Boolean)
})

const favText = computed(() => {
  if (favPending.value) return '处理中…'
  return article.value && article.value.favorited ? '已收藏' : '收藏'
})

async function load () {
  loading.value = true
  error.value = ''
  try {
    // 详情与相关文章并行拉取；相关文章失败不阻塞正文阅读
    const [detail, rel] = await Promise.all([
      fetchArticle(props.articleId),
      fetchRelated(props.articleId).catch(() => [])
    ])
    article.value = detail
    related.value = rel || []
  } catch (e) {
    article.value = null
    error.value = (e && e.message) || '文章加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/** 收藏 / 取消收藏（未登录由 App 引导去登录页） */
async function toggleFavorite () {
  if (!props.user || favPending.value) return
  const id = article.value.id
  const wasFav = article.value.favorited
  favPending.value = true
  // 乐观更新：先翻转 UI，失败再回滚
  article.value.favorited = !wasFav
  try {
    if (wasFav) await removeFavorite(id)
    else await addFavorite(id)
  } catch (e) {
    article.value.favorited = wasFav
    error.value = (e && e.message) || '操作失败，请稍后再试'
  } finally {
    favPending.value = false
  }
}

// 切换文章（从相关文章点进来）时重新加载
watch(() => props.articleId, (id) => { if (id) load() })

onMounted(load)
</script>

<style scoped>
/* ============ 页面骨架 ============ */
.detail {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 860px;
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
  animation: rise 0.55s var(--ease) both;
}

/* ============ 返回 + 收藏 ============ */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 16px;
  border-radius: 11px;
  cursor: pointer;
  font: inherit;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--muted);
  background: var(--glass);
  border: 1px solid var(--stroke);
  transition: color 0.25s, background 0.25s, transform 0.25s var(--ease);
}
.back:hover {
  color: var(--ink);
  background: var(--glass-2);
  transform: translateX(-2px);
}
.back svg {
  width: 15px;
  height: 15px;
}
.fav {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 16px;
  border-radius: 11px;
  cursor: pointer;
  font: inherit;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--muted);
  background: var(--glass);
  border: 1px solid var(--stroke);
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.fav svg {
  width: 15px;
  height: 15px;
}
.fav:hover:not(:disabled) {
  color: #ffd76e;
  border-color: rgba(255, 215, 110, 0.45);
  transform: translateY(-1px);
}
.fav--on {
  color: #ffd76e;
  background: rgba(255, 215, 110, 0.12);
  border-color: rgba(255, 215, 110, 0.5);
}
.fav--pending {
  opacity: 0.6;
  pointer-events: none;
}

/* ============ 文章卡片 ============ */
.paper {
  padding: 44px 48px 40px;
  border-radius: 22px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.015) 50%, rgba(255, 255, 255, 0.045)),
    rgba(10, 12, 26, 0.6);
  backdrop-filter: blur(22px) saturate(140%);
  -webkit-backdrop-filter: blur(22px) saturate(140%);
  border: 1px solid var(--stroke);
  box-shadow: 0 30px 70px -40px rgba(0, 0, 0, 0.9);
}
.paper__head {
  padding-bottom: 26px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.paper__cat {
  display: inline-block;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--a2);
  background: rgba(34, 211, 238, 0.12);
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 999px;
  padding: 4px 13px;
  margin-bottom: 18px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.25s, color 0.25s;
}
.paper__cat:hover {
  color: #fff;
  background: rgba(34, 211, 238, 0.28);
}
.paper__title {
  font-size: clamp(24px, 3.4vw, 34px);
  line-height: 1.28;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.paper__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
  font-size: 13px;
  color: var(--muted);
  font-weight: 300;
}
.paper__author {
  color: var(--ink);
  font-weight: 500;
}
.paper__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--muted);
  opacity: 0.6;
  flex: none;
}

/* 正文 */
.paper__body {
  padding-top: 28px;
  font-size: 15.5px;
  line-height: 2;
  color: rgba(238, 242, 255, 0.86);
  font-weight: 300;
}
.paper__body p {
  margin-bottom: 1.4em;
  text-align: justify;
}
.paper__body p:last-child {
  margin-bottom: 0;
}

/* 文末标签 */
.paper__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 34px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.rtag {
  border: 1px solid var(--stroke);
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 12.5px;
  padding: 5px 13px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.rtag:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

/* 未登录收藏引导 */
.login-hint {
  font-size: 13px;
  color: var(--muted);
  text-align: center;
  font-weight: 300;
}

/* ============ 相关文章 ============ */
.related__title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
  margin-bottom: 16px;
}
.related__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.relcard {
  padding: 18px 20px 16px;
  border-radius: 16px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015)),
    rgba(10, 12, 26, 0.5);
  border: 1px solid var(--stroke);
  cursor: pointer;
  transition: transform 0.3s var(--ease), border-color 0.3s, background 0.3s;
}
.relcard:hover {
  transform: translateY(-3px);
  border-color: rgba(124, 92, 255, 0.45);
  background:
    linear-gradient(160deg, rgba(124, 92, 255, 0.12), rgba(255, 255, 255, 0.02)),
    rgba(10, 12, 26, 0.55);
}
.relcard:focus-visible {
  outline: 2px solid var(--a1);
  outline-offset: 2px;
}
.relcard__title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
}
.relcard__summary {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted);
  font-weight: 300;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.relcard__foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 11.5px;
  color: var(--muted);
  font-weight: 300;
}

/* ============ 骨架 / 状态 ============ */
.skeleton {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 44px 48px;
  border-radius: 22px;
  background: rgba(10, 12, 26, 0.5);
  border: 1px solid var(--stroke);
}
.sk-line {
  height: 14px;
  border-radius: 7px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.06));
  background-size: 200% 100%;
  animation: shimmer 1.4s linear infinite;
}
.sk-line--cat { width: 64px; height: 22px; border-radius: 999px; }
.sk-line--title { width: 72%; height: 26px; }
.sk-line--meta { width: 40%; height: 12px; }
.sk-line--short { width: 55%; }
@keyframes shimmer {
  to { background-position: -200% 0; }
}
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 70px 24px;
  border-radius: 22px;
  background: rgba(10, 12, 26, 0.5);
  border: 1px dashed var(--stroke);
  color: var(--muted);
  font-size: 14px;
  font-weight: 300;
  text-align: center;
}
.state svg {
  width: 40px;
  height: 40px;
  opacity: 0.7;
}
.state--error svg {
  color: #ff8098;
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

/* ============ 响应式 ============ */
@media (max-width: 640px) {
  .detail {
    padding: 14px 14px 40px;
    gap: 14px;
  }
  .paper {
    padding: 28px 22px 26px;
    border-radius: 18px;
  }
  .paper__body {
    font-size: 14.5px;
    line-height: 1.9;
  }
  .related__grid {
    grid-template-columns: 1fr;
  }
  .skeleton {
    padding: 28px 22px;
  }
}
</style>
