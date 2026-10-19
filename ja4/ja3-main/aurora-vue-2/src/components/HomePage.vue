<template>
  <div class="home">
    <!-- ============ 顶部导航 ============ -->
    <SiteNav :user="user" active="home" @go="$emit('navigate', $event)" @logout="$emit('logout')" />

    <!-- ============ 主体 ============ -->
    <main class="main">
      <!-- Hero：欢迎语 + 搜索栏 -->
      <section class="hero">
        <span class="hero__badge">HOME</span>
        <h1 class="hero__title">
          你好，<em>{{ userName }}</em>
          <br />今天想找点什么？
        </h1>
        <p class="hero__sub">在星海中检索你需要的任何内容</p>

        <div class="hero__search">
          <SearchBar
            v-model="keyword"
            placeholder="输入关键词开始搜索…"
            @search="onSearch"
            @clear="onClear"
            @pick="onPick"
          />
        </div>

        <!-- 热门标签：点击触发二级标签筛选 -->
        <div v-if="hotTags.length" class="hero__tags">
          <span class="hero__tags-label">热门</span>
          <button
            v-for="t in hotTags"
            :key="t.id"
            class="tag"
            :class="{ 'tag--on': tagId === t.id }"
            type="button"
            :title="t.categoryName ? t.categoryName + ' · ' + t.articleCount + ' 篇' : t.articleCount + ' 篇'"
            @click="selectTag(t)"
          >{{ t.name }}</button>
        </div>
      </section>

      <!-- ============ 分级分类导航 ============ -->
      <section v-if="categories.length" class="cats">
        <!-- 第一级：分类 -->
        <div class="cats__row">
          <span class="cats__label">分类</span>
          <div class="cats__chips">
            <button
              class="cat"
              :class="{ 'cat--on': !categoryId && !tagId }"
              type="button"
              @click="selectCategory(null)"
            >
              全部
              <em>{{ grandTotal }}</em>
            </button>
            <button
              v-for="c in categories"
              :key="c.id"
              class="cat"
              :class="{ 'cat--on': categoryId === c.id }"
              type="button"
              @click="selectCategory(c)"
            >
              {{ c.name }}
              <em>{{ c.articleCount }}</em>
            </button>
          </div>
        </div>

        <!-- 第二级：当前分类下的标签 -->
        <div v-if="currentCategory && currentCategory.tags.length" class="cats__row cats__row--sub">
          <span class="cats__label">{{ currentCategory.name }}标签</span>
          <div class="cats__chips">
            <button
              class="subtag"
              :class="{ 'subtag--on': !tagId }"
              type="button"
              @click="selectTag(null)"
            >不限</button>
            <button
              v-for="t in currentCategory.tags"
              :key="t.id"
              class="subtag"
              :class="{ 'subtag--on': tagId === t.id }"
              type="button"
              @click="selectTag(t)"
            >{{ t.name }}</button>
          </div>
        </div>
      </section>

      <!-- ============ 搜索结果区 ============ -->
      <section class="results">
        <!-- 结果头部：标题 + 当前筛选条件 + 排序 -->
        <div class="results__head">
          <div class="results__headline">
            <h2 class="results__title">
              <template v-if="loading">正在搜索…</template>
              <template v-else-if="query">“{{ query }}” 的搜索结果</template>
              <template v-else-if="activeFilterText">{{ activeFilterText }}</template>
              <template v-else>推荐内容</template>
            </h2>
            <span v-if="!loading && !error" class="results__count">共 {{ total }} 条</span>
          </div>

          <!-- 排序切换：热度 / 最新 -->
          <div class="sorter">
            <button
              class="sorter__btn"
              :class="{ 'sorter__btn--on': sort === 'hot' }"
              type="button"
              @click="changeSort('hot')"
            >热度</button>
            <button
              class="sorter__btn"
              :class="{ 'sorter__btn--on': sort === 'new' }"
              type="button"
              @click="changeSort('new')"
            >最新</button>
          </div>
        </div>

        <!-- 当前生效的筛选条件（可逐个移除） -->
        <div v-if="hasFilter" class="crt">
          <span class="crt__label">筛选</span>
          <span v-if="query" class="crt__chip">
            关键词：{{ query }}
            <button type="button" aria-label="移除关键词" @click="removeKeyword">×</button>
          </span>
          <span v-if="currentCategory" class="crt__chip">
            {{ currentCategory.name }}
            <button type="button" aria-label="移除分类" @click="selectCategory(null)">×</button>
          </span>
          <span v-if="currentTag" class="crt__chip crt__chip--tag">
            {{ currentTag.name }}
            <button type="button" aria-label="移除标签" @click="selectTag(null)">×</button>
          </span>
          <button class="crt__clear" type="button" @click="resetAll">清空全部</button>
        </div>

        <!-- 加载骨架 -->
        <div v-if="loading" class="skeleton">
          <div v-for="n in 4" :key="n" class="sk-card">
            <div class="sk-line sk-line--title"></div>
            <div class="sk-line sk-line--sub"></div>
            <div class="sk-line sk-line--sub sk-line--short"></div>
            <div class="sk-meta">
              <span class="sk-chip"></span>
              <span class="sk-chip"></span>
            </div>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-else-if="error" class="state state--error">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
               stroke-linecap="round">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 8v4.5M12 16h.01" />
          </svg>
          <p>{{ error }}</p>
          <button class="state__btn" type="button" @click="runSearch(page)">重试</button>
        </div>

        <!-- 空结果 -->
        <div v-else-if="!list.length" class="state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
               stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.6-3.6" />
            <path d="M8 11h6" />
          </svg>
          <p v-if="query || hasFilter">没有找到符合条件的内容，试试更换关键词或放宽筛选</p>
          <p v-else>暂无内容</p>
        </div>

        <!-- 结果列表（卡片可点击进入详情） -->
        <div v-else class="rlist">
          <ArticleCard
            v-for="a in list"
            :key="a.id"
            :article="a"
            :favorited="favIds.has(a.id)"
            :show-fav="!!user"
            @open="$emit('open-article', $event)"
            @open-category="onOpenCategory"
            @open-tag="selectTagByName"
            @toggle-fav="toggleFavorite"
          />
        </div>

        <!-- 数字分页 -->
        <div v-if="!loading && !error && pages > 1" class="pager">
          <button class="pager__btn" type="button" :disabled="page <= 1" @click="go(page - 1)">
            上一页
          </button>
          <div class="pager__nums">
            <button
              v-for="(n, i) in pageList"
              :key="i"
              class="pager__num"
              :class="{ 'pager__num--on': n === page, 'pager__num--gap': n === '…' }"
              type="button"
              :disabled="n === '…' || n === page"
              @click="typeof n === 'number' && go(n)"
            >{{ n }}</button>
          </div>
          <button class="pager__btn" type="button" :disabled="page >= pages" @click="go(page + 1)">
            下一页
          </button>
        </div>
      </section>
    </main>

    <footer class="foot">
      <span>AURORA · 星海入口</span>
      <span class="foot__dot"></span>
      <span>首页 · 分级分类检索已接入后端接口</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import SearchBar from './SearchBar.vue'
import SiteNav from './SiteNav.vue'
import ArticleCard from './ArticleCard.vue'
import { searchArticles, fetchCategories, fetchHotTags } from '../api/search'
import { fetchFavoriteIds, addFavorite, removeFavorite } from '../api/favorites'

const props = defineProps({
  user: { type: Object, default: null },
  /** 从其它页面跳转回来时携带的初始筛选条件（App 每次传入新对象触发更新） */
  initialFilter: { type: Object, default: null }
})
defineEmits(['navigate', 'open-article', 'logout', 'need-login'])

/* ---------- 用户信息 ---------- */
const userName = computed(() => (props.user && props.user.name) || '探险家')

/* ---------- 检索状态 ---------- */
const PAGE_SIZE = 10

const keyword = ref('')     // 输入框当前内容（v-model）
const query = ref('')       // 已提交的关键词
const categoryId = ref(null) // 一级分类 id
const tagId = ref(null)      // 二级标签 id
const sort = ref('hot')      // hot | new

const list = ref([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const loading = ref(false)
const error = ref('')

/* ---------- 分级分类数据 ---------- */
const categories = ref([])   // 分类树
const hotTags = ref([])      // 热门标签
const favIds = ref(new Set()) // 当前用户已收藏的文章 id 集合

/* ---------- 派生状态 ---------- */
const currentCategory = computed(() => categories.value.find((c) => c.id === categoryId.value) || null)

/** 当前选中的二级标签对象（先从当前分类里找，再退到热门标签） */
const currentTag = computed(() => {
  const inCat = currentCategory.value && currentCategory.value.tags.find((t) => t.id === tagId.value)
  if (inCat) return inCat
  return hotTags.value.find((t) => t.id === tagId.value) || null
})

/** 全部文章总数（各分类文章数之和，用于「全部」标签的计数） */
const grandTotal = computed(() => categories.value.reduce((s, c) => s + (c.articleCount || 0), 0))

const hasFilter = computed(() => Boolean(query.value || categoryId.value || tagId.value))

/** 结果区标题：没有关键词但有筛选条件时，用文字描述当前条件 */
const activeFilterText = computed(() => {
  if (currentTag.value) {
    return currentCategory.value
      ? `${currentCategory.value.name} · ${currentTag.value.name}`
      : currentTag.value.name
  }
  if (currentCategory.value) return currentCategory.value.name
  return ''
})

/** 数字分页：总页数 <= 7 全展示；否则 1 … cur-1 cur cur+1 … last */
const pageList = computed(() => {
  const totalPages = pages.value
  const cur = page.value
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1)
  }
  const set = new Set([1, 2, cur - 1, cur, cur + 1, totalPages - 1, totalPages])
  const arr = [...set].filter((n) => n >= 1 && n <= totalPages).sort((a, b) => a - b)
  const out = []
  let prev = 0
  for (const n of arr) {
    if (prev && n - prev > 1) out.push('…')
    out.push(n)
    prev = n
  }
  return out
})

/* ---------- 主流程 ---------- */

/** 发起请求。p 为目标页码 */
async function runSearch (p = 1) {
  loading.value = true
  error.value = ''
  page.value = p

  try {
    const res = await searchArticles(query.value, {
      categoryId: categoryId.value,
      tagId: tagId.value,
      sort: sort.value,
      page: p,
      size: PAGE_SIZE
    })
    list.value = (res && res.list) || []
    total.value = (res && res.total) || 0
    pages.value = (res && res.pages) || 0
    page.value = (res && res.page) || p
  } catch (e) {
    list.value = []
    total.value = 0
    pages.value = 0
    error.value = (e && e.message) || '搜索失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/** 搜索框提交 */
function onSearch (kw) {
  const k = (kw || '').trim()
  keyword.value = k
  query.value = k
  runSearch(1)
}

/** 搜索建议选中：标签 -> 按标签筛选；文章标题 -> 按关键词搜索 */
function onPick (item) {
  if (item && item.type === 'tag') {
    selectTagByName(item.text)
  } else {
    onSearch(item && item.text)
  }
}

/** 清空搜索框 -> 保留分类筛选，仅去掉关键词 */
function onClear () {
  keyword.value = ''
  query.value = ''
  runSearch(1)
}

/** 选择一级分类：切换分类时清掉二级标签（避免标签不属于该分类） */
function selectCategory (c) {
  const next = c ? c.id : null
  if (categoryId.value !== next) tagId.value = null
  categoryId.value = next
  runSearch(1)
}

/** 卡片上的分类按钮：直接按该分类筛选 */
function onOpenCategory (categoryIdVal) {
  const c = categories.value.find((x) => x.id === categoryIdVal)
  if (c) selectCategory(c)
}

/** 选择二级标签：标签自带所属分类时，同步切到该分类下 */
function selectTag (t) {
  if (!t) {
    tagId.value = null
    runSearch(1)
    return
  }
  const nextTag = tagId.value === t.id ? null : t.id
  tagId.value = nextTag

  // 从热门标签点进来时，若该标签属于某个分类而当前未选中它，自动补上分类，
  // 这样二级导航会展开到正确的位置，用户能看清自己在哪一层。
  if (nextTag && t.categoryId && categoryId.value !== t.categoryId) {
    categoryId.value = t.categoryId
  }
  runSearch(1)
}

/** 按标签名选中（点击结果卡片上的标签 / 搜索建议里的标签时用） */
function selectTagByName (name) {
  let t = null
  for (const c of categories.value) {
    const hit = c.tags.find((x) => x.name === name)
    if (hit) { t = hit; break }
  }
  if (!t) t = hotTags.value.find((x) => x.name === name)
  if (t) selectTag(t)
  else onSearch(name)   // 兜底：没匹配到标签，就当关键词搜
}

/** 切换排序 */
function changeSort (s) {
  if (sort.value === s) return
  sort.value = s
  runSearch(1)
}

/** 移除关键词筛选 */
function removeKeyword () {
  keyword.value = ''
  query.value = ''
  runSearch(1)
}

/** 回到初始状态：无关键词、无分类、无标签、热度排序 */
function resetAll () {
  keyword.value = ''
  query.value = ''
  categoryId.value = null
  tagId.value = null
  sort.value = 'hot'
  runSearch(1)
}

/** 翻页 */
function go (p) {
  if (p < 1 || (pages.value && p > pages.value)) return
  runSearch(p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ---------- 收藏（卡片星星按钮） ---------- */

/** 加载当前用户收藏 id 集合（未登录跳过） */
async function loadFavIds () {
  if (!props.user) {
    favIds.value = new Set()
    return
  }
  try {
    const ids = await fetchFavoriteIds()
    favIds.value = new Set(ids)
  } catch (_) {
    favIds.value = new Set()
  }
}

/** 卡片收藏 / 取消收藏（未登录由 App 引导去登录） */
async function toggleFavorite (article) {
  if (!props.user) return
  const on = favIds.value.has(article.id)
  // 乐观更新
  const next = new Set(favIds.value)
  if (on) next.delete(article.id)
  else next.add(article.id)
  favIds.value = next
  try {
    if (on) await removeFavorite(article.id)
    else await addFavorite(article.id)
  } catch (e) {
    // 失败回滚
    const rollback = new Set(favIds.value)
    if (on) rollback.add(article.id)
    else rollback.delete(article.id)
    favIds.value = rollback
    error.value = (e && e.message) || '操作失败，请稍后再试'
  }
}

/* ---------- 跨页面初始筛选 ---------- */
// 从发现页 / 详情页带着筛选条件跳回来时应用（initialFilter 是新对象即触发）
watch(() => props.initialFilter, (f) => {
  if (!f) return
  keyword.value = ''
  query.value = ''
  categoryId.value = f.categoryId || null
  if (f.tagName) {
    const t = findTagByName(f.tagName)
    tagId.value = t ? t.id : null
  } else {
    tagId.value = f.tagId || null
  }
  sort.value = f.sort || 'hot'
  runSearch(1)
})

function findTagByName (name) {
  for (const c of categories.value) {
    const hit = c.tags.find((x) => x.name === name)
    if (hit) return hit
  }
  return hotTags.value.find((x) => x.name === name) || null
}

/* ---------- 初始化 ---------- */
onMounted(async () => {
  // 分类树 / 热门标签用于渲染导航，失败不阻塞内容列表
  try {
    const [cats, tags] = await Promise.all([fetchCategories(), fetchHotTags()])
    categories.value = cats || []
    hotTags.value = tags || []
  } catch (e) {
    categories.value = []
    hotTags.value = []
  }
  await loadFavIds()
  runSearch(1)
})
</script>

<style scoped>
/* ============ 页面骨架 ============ */
.home {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1180px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 22px 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ============ 主体 ============ */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 34px;
  padding-top: 22px;
}

/* Hero */
.hero {
  text-align: center;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  animation: rise 0.65s var(--ease) both;
}
.hero__badge {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--a2);
  border: 1px solid rgba(34, 211, 238, 0.45);
  border-radius: 999px;
  padding: 3px 13px;
  margin-bottom: 20px;
}
.hero__title {
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.16;
  font-weight: 600;
  letter-spacing: -0.02em;
}
.hero__title em {
  font-style: normal;
  background: linear-gradient(100deg, var(--a2), var(--a1) 45%, var(--a3));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  background-size: 200% auto;
  animation: shine 6s linear infinite;
}
.hero__sub {
  color: var(--muted);
  font-size: 15px;
  font-weight: 300;
  margin-top: 14px;
}

.hero__search {
  margin-top: 30px;
  text-align: left;
}

/* 热门标签 */
.hero__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 9px;
  margin-top: 22px;
}
.hero__tags-label {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  margin-right: 2px;
}
.tag {
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--muted);
  font: inherit;
  font-size: 13px;
  font-weight: 400;
  padding: 6px 15px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.tag:hover {
  color: #fff;
  background: rgba(124, 92, 255, 0.22);
  border-color: rgba(124, 92, 255, 0.55);
  transform: translateY(-2px);
}

/* ============ 底部 ============ */
.foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 8px 0 2px;
  font-size: 12px;
  font-weight: 300;
  color: var(--muted);
  letter-spacing: 0.04em;
}
.foot__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--muted);
  opacity: 0.6;
}

/* ============ 响应式 ============ */
@media (max-width: 900px) {
  .cats__row {
    flex-direction: column;
    align-items: flex-start;
    gap: 9px;
  }
  .cats__label {
    min-width: 0;
  }
  .results__head {
    flex-wrap: wrap;
  }
}
@media (max-width: 640px) {
  .home {
    padding: 14px 14px 12px;
    gap: 14px;
  }
  .main {
    padding-top: 12px;
    gap: 26px;
  }
  .hero__tags {
    gap: 7px;
  }
  .results__head {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .foot {
    flex-direction: column;
    gap: 5px;
  }
  .foot__dot {
    display: none;
  }
}

/* ============ 分级分类导航 ============ */
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
.cats__row {
  display: flex;
  align-items: center;
  gap: 16px;
}
/* 第二级：与第一级之间加一条分隔，体现层级关系 */
.cats__row--sub {
  padding-top: 13px;
  border-top: 1px dashed rgba(255, 255, 255, 0.09);
  animation: rise 0.4s var(--ease) both;
}
.cats__label {
  flex: none;
  font-size: 11.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
  min-width: 66px;
}
.cats__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 一级分类 chip */
.cat {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--ink);
  font: inherit;
  font-size: 13.5px;
  font-weight: 400;
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
  transition: color 0.25s, background 0.25s;
}
.cat:hover {
  border-color: rgba(34, 211, 238, 0.5);
  background: rgba(34, 211, 238, 0.12);
  transform: translateY(-1px);
}
.cat--on {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(115deg, var(--a1), #5b8bff 55%, var(--a2));
  box-shadow: 0 12px 26px -14px rgba(124, 92, 255, 0.9);
}
.cat--on em {
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
}

/* 二级标签 chip */
.subtag {
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
.subtag:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}
.subtag--on {
  color: #06202b;
  background: linear-gradient(115deg, var(--a2), #7ee8f5);
  border-color: transparent;
  font-weight: 600;
}

/* 结果区头部：标题 + 排序 */
.results__headline {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}
.results__title {
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.results__count {
  font-size: 12.5px;
  color: var(--muted);
  font-weight: 300;
  white-space: nowrap;
}
.sorter {
  display: flex;
  gap: 4px;
  flex: none;
  padding: 3px;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--stroke);
}
.sorter__btn {
  border: 0;
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 12.5px;
  padding: 5px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s;
}
.sorter__btn:hover {
  color: var(--ink);
}
.sorter__btn--on {
  color: #fff;
  background: rgba(124, 92, 255, 0.5);
}

/* ============ 当前筛选条件（面包屑） ============ */
.crt {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: -6px 0 18px;
  padding: 0 4px;
}
.crt__label {
  font-size: 11.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
}
.crt__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--ink);
  background: rgba(124, 92, 255, 0.2);
  border: 1px solid rgba(124, 92, 255, 0.45);
  border-radius: 999px;
  padding: 3px 6px 3px 12px;
}
.crt__chip--tag {
  background: rgba(34, 211, 238, 0.16);
  border-color: rgba(34, 211, 238, 0.45);
}
.crt__chip button {
  border: 0;
  background: rgba(255, 255, 255, 0.12);
  color: inherit;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  font: inherit;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: background 0.25s;
}
.crt__chip button:hover {
  background: rgba(255, 255, 255, 0.3);
}
.crt__clear {
  border: 0;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 12.5px;
  padding: 0 4px;
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.25s;
}
.crt__clear:hover {
  color: var(--ink);
}

/* 热门标签选中态 */
.tag--on {
  color: #fff;
  background: linear-gradient(115deg, var(--a3), var(--a1));
  border-color: transparent;
  font-weight: 600;
}

/* ============ 结果列表 ============ */
.rlist {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ============ 数字分页 ============ */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 14px 0 4px;
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
.pager__nums {
  display: flex;
  align-items: center;
  gap: 5px;
}
.pager__num {
  min-width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--muted);
  font: inherit;
  font-size: 13px;
  padding: 0 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.pager__num:hover:not(:disabled) {
  color: var(--ink);
  border-color: rgba(124, 92, 255, 0.45);
  transform: translateY(-1px);
}
.pager__num--on {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(115deg, var(--a1), #5b8bff 55%, var(--a2));
  box-shadow: 0 10px 22px -12px rgba(124, 92, 255, 0.9);
  cursor: default;
}
.pager__num--gap {
  border: 0;
  background: none;
  cursor: default;
  color: var(--muted);
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
.sk-line--title { width: 58%; height: 17px; }
.sk-line--sub { width: 88%; }
.sk-line--short { width: 45%; }
@keyframes shimmer {
  to { background-position: -200% 0; }
}
.sk-meta {
  display: flex;
  gap: 8px;
}
.sk-chip {
  width: 64px;
  height: 20px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  animation: shimmer 1.4s linear infinite;
  background-image: linear-gradient(90deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  background-size: 200% 100%;
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
</style>
