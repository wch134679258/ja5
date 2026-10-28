<template>
  <div class="home">
    <!-- ============ 顶部导航 ============ -->
    <SiteNav :user="user" active="home" @go="$emit('navigate', $event)" @logout="$emit('logout')" />

    <!-- ============ 主体 ============ -->
    <main class="main">
      <!-- Hero：问候 + 搜索栏 -->
      <section class="hero">
        <div class="hero__aura" aria-hidden="true"></div>
        <span class="hero__badge">{{ greeting.badge }}</span>
        <h1 class="hero__title">
          {{ greeting.text }}，<em>{{ userName }}</em>
          <br />今天想找点什么？
        </h1>
        <p class="hero__sub">
          {{ todayText }}<template v-if="overview">，星海已收录 {{ overview.articles }} 篇内容</template>
        </p>

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

      <!-- ============ 数据概览条 ============ -->
      <section class="stats" aria-label="站点数据概览">
        <div v-for="s in statCards" :key="s.key" class="stat">
          <span class="stat__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round">
              <path :d="s.icon" />
            </svg>
          </span>
          <div class="stat__body">
            <b class="stat__num">{{ s.value }}</b>
            <span class="stat__label">{{ s.label }}</span>
          </div>
        </div>
      </section>

      <!-- ============ 主区：左侧内容 + 右侧侧栏 ============ -->
      <div class="board">
        <div class="board__main">
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
        </div>
        <!-- /board__main -->

        <!-- ============ 右侧侧栏 ============ -->
        <aside class="board__side" aria-label="站点侧栏">
          <!-- 今日星语 + 随机探索 -->
          <section class="side-card side-card--muse">
            <h3 class="side-card__title">今日星语</h3>
            <p class="muse">{{ muse }}</p>
            <button class="muse__btn" type="button" :disabled="!pool.length" @click="goRandom">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                   stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 17h3.5l8-10H19" />
                <path d="m16.5 4 3 3-3 3" />
                <path d="M4 7h3.5l2.6 3.2" />
              </svg>
              随便逛逛
            </button>
          </section>

          <!-- 热门榜单 -->
          <section v-if="topArticles.length" class="side-card">
            <h3 class="side-card__title">热门榜单</h3>
            <ol class="rank">
              <li
                v-for="(a, i) in topArticles"
                :key="a.id"
                class="rank__item"
                tabindex="0"
                role="button"
                :aria-label="'阅读：' + a.title"
                @click="$emit('open-article', a.id)"
                @keydown.enter="$emit('open-article', a.id)"
              >
                <span class="rank__no" :class="'rank__no--' + (i + 1)">{{ i + 1 }}</span>
                <span class="rank__body">
                  <span class="rank__title">{{ a.title }}</span>
                  <span class="rank__meta">{{ a.views }} 次浏览<template v-if="a.category"> · {{ a.category }}</template></span>
                </span>
              </li>
            </ol>
          </section>

          <!-- 分类分布 -->
          <section v-if="categories.length" class="side-card">
            <h3 class="side-card__title">分类分布</h3>
            <div class="dist">
              <button
                v-for="c in categories"
                :key="c.id"
                class="dist__row"
                :class="{ 'dist__row--on': categoryId === c.id }"
                type="button"
                @click="selectCategory(c)"
              >
                <span class="dist__name">{{ c.name }}</span>
                <span class="dist__bar"><i :style="{ width: distWidth(c.articleCount) }"></i></span>
                <span class="dist__num">{{ c.articleCount }}</span>
              </button>
            </div>
          </section>

          <!-- 快捷入口 -->
          <section class="side-card">
            <h3 class="side-card__title">继续探索</h3>
            <div class="quick">
              <button
                v-for="q in quickLinks"
                :key="q.key"
                class="quick__item"
                type="button"
                @click="$emit('navigate', q.key)"
              >
                <span class="quick__dot" :style="{ background: q.color }"></span>
                {{ q.name }}
              </button>
            </div>
          </section>
        </aside>
      </div>
      <!-- /board -->
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
import { fetchOverview } from '../api/stats'
import { toast } from '../composables/useToast'

const props = defineProps({
  user: { type: Object, default: null },
  /** 从其它页面跳转回来时携带的初始筛选条件（App 每次传入新对象触发更新） */
  initialFilter: { type: Object, default: null }
})
const emit = defineEmits(['navigate', 'open-article', 'logout', 'need-login'])

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

/* ---------- 站点概览 & 侧栏 ---------- */
const overview = ref(null)  // { articles, categories, tags, totalViews, avgViews }
const pool = ref([])        // 全站文章池（最多 50 条，供热门榜单与「随便逛逛」使用）

/** 热门榜单只取前 5（pool 已按热度倒序） */
const topArticles = computed(() => pool.value.slice(0, 5))

/** 分布条基准：取分类里的最大文章数，最小保留 8% 让零散分类也看得见 */
const maxCategoryCount = computed(
  () => categories.value.reduce((m, c) => Math.max(m, c.articleCount || 0), 0) || 1
)
function distWidth (count) {
  return Math.max(8, Math.round(((count || 0) / maxCategoryCount.value) * 100)) + '%'
}

/** 问候语：按本地时间切换，badge 用英文单词保持原有的设计语言 */
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 5) return { text: '夜深了', badge: 'NIGHT' }
  if (h < 9) return { text: '早上好', badge: 'MORNING' }
  if (h < 12) return { text: '上午好', badge: 'FORENOON' }
  if (h < 14) return { text: '中午好', badge: 'NOON' }
  if (h < 18) return { text: '下午好', badge: 'AFTERNOON' }
  return { text: '晚上好', badge: 'EVENING' }
})

/** 今天几号：手写中文格式，避免 toLocaleDateString 各环境输出不一致 */
const todayText = computed(() => {
  const d = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日 · ${week}`
})

/** 数字缩写：一万以上用「万」，让数据条在窄卡片里也能放得下 */
function fmtNum (n) {
  const v = Number(n) || 0
  if (v < 10000) return String(v)
  return (v / 10000).toFixed(v >= 100000 ? 0 : 1) + ' 万'
}

/** 顶部数据条：全部来自 /api/stats/overview，取不到就整条不渲染 */
const statCards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    {
      key: 'articles',
      label: '篇文章',
      value: fmtNum(o.articles),
      icon: 'M5 4h11l3 3v13H5zM15 4v4h4M8.5 12h7M8.5 16h5'
    },
    {
      key: 'categories',
      label: '个分类',
      value: fmtNum(o.categories),
      icon: 'M4 5h6v6H4zM14 5h6v6h-6zM4 15h6v4H4zM14 15h6v4h-6z'
    },
    {
      key: 'tags',
      label: '个标签',
      value: fmtNum(o.tags),
      icon: 'M4 11V5h6l9 9-6 6zM7.6 8.4h.01'
    },
    {
      key: 'views',
      label: '次累计浏览',
      value: fmtNum(o.totalViews),
      icon: 'M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12ZM12 9.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z'
    }
  ]
})

/** 今日星语：按「天」取模，同一天刷新不会变，跨天才换一句 */
const MUSES = [
  '所有遥远的星，都曾被某个人的目光点亮。',
  '别急着抵达，先让路上有光。',
  '看得越远，脚下的路越清楚。',
  '星图不会告诉你终点，只提醒你方向。',
  '慢一点也没关系，宇宙走了一百三十八亿年才到这里。',
  '好奇心是最省油的引擎。',
  '每一篇读完的文章，都是你走过的一段航线。',
  '把问题问得再准一点，答案就会自己亮起来。'
]
const muse = computed(() => MUSES[Math.floor(Date.now() / 86400000) % MUSES.length])

/** 随便逛逛：从全站文章池里随机打开一篇 */
function goRandom () {
  if (!pool.value.length) return
  const pick = pool.value[Math.floor(Math.random() * pool.value.length)]
  if (pick) emit('open-article', pick.id)
}

/** 侧栏快捷入口：key 与 SiteNav 的导航键保持一致 */
const quickLinks = [
  { key: 'discover', name: '发现', color: 'linear-gradient(135deg,#7c5cff,#b49cff)' },
  { key: 'game', name: '星空贪吃蛇', color: 'linear-gradient(135deg,#22d3ee,#6ee7f9)' },
  { key: 'favorites', name: '我的收藏', color: 'linear-gradient(135deg,#ff5fa2,#ff9ec4)' },
  { key: 'profile', name: '个人中心', color: 'linear-gradient(135deg,#ffd76e,#ffb347)' }
]

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
    toast.success(on ? '已取消收藏' : '已收藏')
  } catch (e) {
    // 失败回滚
    const rollback = new Set(favIds.value)
    if (on) rollback.add(article.id)
    else rollback.delete(article.id)
    favIds.value = rollback
    toast.error((e && e.message) || '操作失败，请稍后再试')
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

  // 数据条与侧栏：与主列表并行拉取，失败时静默降级（页面其余部分照常可用）
  fetchOverview()
    .then((o) => { overview.value = o })
    .catch(() => { overview.value = null })
  searchArticles('', { sort: 'hot', page: 1, size: 50 })
    .then((res) => { pool.value = (res && res.list) || [] })
    .catch(() => { pool.value = [] })

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
  position: relative;
  text-align: center;
  max-width: 760px;
  width: 100%;
  margin: 0 auto;
  padding-top: 6px;
  animation: rise 0.65s var(--ease) both;
}
/* 标题背后的柔光，让居中的标题不再是「孤零零一行字」 */
.hero__aura {
  position: absolute;
  left: 50%;
  top: -90px;
  width: 780px;
  height: 460px;
  transform: translateX(-50%);
  background: radial-gradient(50% 50% at 50% 50%, rgba(124, 92, 255, 0.3), rgba(34, 211, 238, 0.08) 45%, transparent 72%);
  filter: blur(28px);
  pointer-events: none;
  z-index: -1;
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
  background: rgba(34, 211, 238, 0.06);
}
.hero__title {
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.16;
  font-weight: 600;
  letter-spacing: -0.02em;
  text-shadow: 0 6px 40px rgba(124, 92, 255, 0.25);
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
  letter-spacing: 0.01em;
}

/* ============ 数据概览条 ============ */
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  animation: rise 0.7s var(--ease) both;
}
.stat {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 17px;
  border-radius: 16px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.015) 60%),
    rgba(10, 12, 26, 0.5);
  border: 1px solid var(--stroke);
  backdrop-filter: blur(20px) saturate(140%);
  -webkit-backdrop-filter: blur(20px) saturate(140%);
  transition: transform 0.3s var(--ease), border-color 0.3s, background 0.3s;
}
.stat:hover {
  transform: translateY(-2px);
  border-color: rgba(124, 92, 255, 0.4);
}
.stat__icon {
  flex: none;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: rgba(124, 92, 255, 0.16);
  color: #c3b3ff;
}
.stat__icon svg {
  width: 17px;
  height: 17px;
}
.stat__num {
  display: block;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.stat__label {
  display: block;
  font-size: 11.5px;
  color: var(--muted);
  font-weight: 300;
  margin-top: 3px;
}
/* 四个数据各给一点色偏，避免整排看起来像复制粘贴 */
.stat:nth-child(1) .stat__icon { background: rgba(124, 92, 255, 0.18); color: #c3b3ff; }
.stat:nth-child(2) .stat__icon { background: rgba(34, 211, 238, 0.15); color: #8ceaf7; }
.stat:nth-child(3) .stat__icon { background: rgba(255, 95, 162, 0.15); color: #ffa8c9; }
.stat:nth-child(4) .stat__icon { background: rgba(255, 215, 110, 0.15); color: #ffdf9b; }

/* ============ 主区两栏 ============ */
.board {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 322px;
  gap: 26px;
  align-items: start;
}
.board__main {
  display: flex;
  flex-direction: column;
  gap: 26px;
  min-width: 0;
}
.board__side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-self: start;
  min-width: 0;
}

/* 侧栏卡片 */
.side-card {
  position: relative;
  padding: 16px 16px 14px;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.012) 55%, rgba(255, 255, 255, 0.04)),
    rgba(10, 12, 26, 0.55);
  border: 1px solid var(--stroke);
  backdrop-filter: blur(22px) saturate(140%);
  -webkit-backdrop-filter: blur(22px) saturate(140%);
  overflow: hidden;
  animation: rise 0.6s var(--ease) both;
}
.side-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #dfe4ff;
  margin-bottom: 11px;
}
.side-card__title::before {
  content: '';
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--a2), var(--a1));
}

/* 今日星语 */
.side-card--muse {
  background:
    radial-gradient(120% 110% at 0% 0%, rgba(124, 92, 255, 0.26), transparent 58%),
    radial-gradient(120% 110% at 100% 100%, rgba(34, 211, 238, 0.18), transparent 58%),
    rgba(10, 12, 26, 0.6);
  border-color: rgba(124, 92, 255, 0.28);
}
.muse {
  font-size: 13.5px;
  line-height: 1.85;
  color: #d5daf2;
  font-weight: 300;
  letter-spacing: 0.01em;
  min-height: 50px;
}
.muse__btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 13px;
  padding: 7px 13px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.07);
  color: var(--ink);
  font: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.muse__btn svg {
  width: 14px;
  height: 14px;
  opacity: 0.8;
}
.muse__btn:hover:not(:disabled) {
  background: rgba(124, 92, 255, 0.28);
  border-color: rgba(124, 92, 255, 0.55);
  transform: translateY(-1px);
}
.muse__btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* 热门榜单 */
.rank {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.rank__item {
  display: flex;
  gap: 10px;
  padding: 6px 8px;
  margin: 0 -8px;
  border-radius: 11px;
  cursor: pointer;
  transition: background 0.25s, transform 0.25s var(--ease);
}
.rank__item:hover {
  background: rgba(255, 255, 255, 0.055);
  transform: translateX(2px);
}
.rank__item:focus-visible {
  outline: 2px solid var(--a1);
  outline-offset: 1px;
}
.rank__no {
  flex: none;
  width: 20px;
  height: 20px;
  margin-top: 1px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.07);
  font-variant-numeric: tabular-nums;
}
.rank__no--1 { background: linear-gradient(135deg, #ffd76e, #ff9f45); color: #2b1a00; }
.rank__no--2 { background: linear-gradient(135deg, #e2ebff, #9fb0cf); color: #1a2135; }
.rank__no--3 { background: linear-gradient(135deg, #f2b98f, #cb8256); color: #2a1400; }
.rank__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.rank__title {
  font-size: 12.8px;
  line-height: 1.45;
  color: #dfe4ff;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.25s;
}
.rank__item:hover .rank__title {
  color: #fff;
}
.rank__meta {
  font-size: 11px;
  color: var(--muted);
  font-weight: 300;
}

/* 分类分布 */
.dist {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.dist__row {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) 22px;
  align-items: center;
  gap: 9px;
  border: 0;
  background: none;
  padding: 2px 0;
  font: inherit;
  color: var(--muted);
  cursor: pointer;
  text-align: left;
}
.dist__name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.25s;
}
.dist__bar {
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}
.dist__bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--a1), var(--a2));
  transition: width 0.7s var(--ease), filter 0.25s;
}
.dist__num {
  font-size: 11.5px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.dist__row:hover .dist__name,
.dist__row--on .dist__name {
  color: #fff;
}
.dist__row:hover .dist__bar i,
.dist__row--on .dist__bar i {
  filter: brightness(1.35);
}
.dist__row--on .dist__num {
  color: var(--a2);
}

/* 快捷入口 */
.quick {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.quick__item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 10px;
  border: 1px solid var(--stroke);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--muted);
  font: inherit;
  font-size: 12.2px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.quick__item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(124, 92, 255, 0.45);
  transform: translateY(-1px);
}
.quick__dot {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
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
/* 桌面端让侧栏跟随滚动：正文很长时右侧不会拖着一大片空白 */
@media (min-width: 1081px) {
  .board__side {
    position: sticky;
    top: 18px;
    max-height: calc(100vh - 36px);
    overflow-y: auto;
    scrollbar-width: none;
  }
  .board__side::-webkit-scrollbar {
    display: none;
  }
}
/* 1024 以下放不下「正文 + 322 侧栏」，侧栏改为正文下方的两列卡片 */
@media (max-width: 1080px) {
  .board {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .board__side {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    /* 不加这条，同一行的卡片会被拉成等高，短卡片（今日星语）下方会空一大块 */
    align-items: start;
  }
}
@media (max-width: 900px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
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
    gap: 22px;
  }
  .board__side {
    grid-template-columns: 1fr;
  }
  .stats {
    gap: 9px;
  }
  .stat {
    padding: 12px 13px;
    gap: 9px;
  }
  .stat__icon {
    width: 31px;
    height: 31px;
    border-radius: 9px;
  }
  .stat__num {
    font-size: 17px;
  }
  .stat__label {
    font-size: 10.5px;
  }
  .hero__aura {
    width: 460px;
    height: 320px;
    top: -60px;
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
  gap: 6px;
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--ink);
  font: inherit;
  font-size: 13.5px;
  font-weight: 400;
  padding: 6px 13px;
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
/* 原样式表里缺了这一条（FavoritesPage 有、这里没有），导致「热度/最新」掉到标题下一行 */
.results__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
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
