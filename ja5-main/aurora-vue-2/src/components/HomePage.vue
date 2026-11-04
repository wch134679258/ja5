<template>
  <div class="home">
    <!-- ============ 顶部导航 ============ -->
    <SiteNav :user="user" active="home" @go="$emit('navigate', $event)" @logout="$emit('logout')" />

    <main class="page">
      <!-- ============================================================
           Hero：左侧「问候 + 搜索 + 热搜」，右侧「今日头条」沉浸封面卡
           ============================================================ -->
      <section class="hero">
        <div class="hero__copy">
          <p class="hero__eyebrow">
            <i class="hero__pulse" aria-hidden="true"></i>
            {{ greeting.text }} · {{ todayText }}
          </p>

          <h1 class="hero__title">
            {{ greeting.text }}，<em>{{ userName }}</em>
          </h1>

          <p class="hero__lede">
            <template v-if="overview">
              星海里躺着 <b>{{ fmtNum(overview.articles) }}</b> 篇文章、
              <b>{{ fmtNum(overview.tags) }}</b> 个标签<template v-if="overview.totalViews">，
              累计被读过 <b>{{ fmtNum(overview.totalViews) }}</b> 次</template>。
            </template>
            <template v-else>从这里找到你需要的技术与灵感。</template>
            挑一篇，开始今天的漫游。
          </p>

          <div class="hero__search">
            <SearchBar
              v-model="keyword"
              placeholder="搜索标题、标签、正文…"
              @search="onSearch"
              @clear="onClear"
              @pick="onPick"
            />
          </div>

          <div v-if="hotTags.length" class="hero__hot">
            <span class="hero__hot-label">热搜</span>
            <button
              v-for="t in hotTags.slice(0, 5)"
              :key="t.id"
              class="chip"
              :class="{ 'chip--on': tagId === t.id }"
              type="button"
              :title="t.categoryName ? t.categoryName + ' · ' + t.articleCount + ' 篇' : t.articleCount + ' 篇'"
              @click="selectTag(t)"
            >{{ t.name }}</button>
          </div>

          <ul v-if="overview" class="hero__stats" aria-label="站点数据">
            <li><b>{{ fmtNum(overview.articles) }}</b><span>篇文章</span></li>
            <li><b>{{ fmtNum(overview.categories) }}</b><span>个分类</span></li>
            <li><b>{{ fmtNum(overview.tags) }}</b><span>个标签</span></li>
            <li><b>{{ fmtNum(overview.totalViews) }}</b><span>次阅读</span></li>
          </ul>
        </div>

        <!-- 今日头条：封面铺满整卡，文字压在底部渐变上 -->
        <article
          v-if="leadArticle"
          class="lead"
          tabindex="0"
          role="button"
          :aria-label="'阅读头条：' + leadArticle.title"
          @click="$emit('open-article', leadArticle.id)"
          @keydown.enter="$emit('open-article', leadArticle.id)"
        >
          <div class="lead__cover" :style="{ backgroundImage: 'url(' + coverAt(0) + ')' }">
            <span class="lead__scrim"></span>
            <span class="lead__badge">
              <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="m12 2 2.2 6.1L20.5 10l-5 4.3 1.5 6.4L12 17.2 7 20.7l1.5-6.4L3.5 10l6.3-1.9z" />
              </svg>
              今日头条
            </span>

            <div class="lead__body">
              <button
                v-if="leadArticle.category"
                class="lead__cat"
                type="button"
                @click.stop="onOpenCategory(leadArticle.categoryId)"
              >{{ leadArticle.category }}</button>
              <h2 class="lead__title">{{ leadArticle.title }}</h2>
              <p class="lead__sum">{{ leadArticle.summary }}</p>
              <div class="lead__foot">
                <span class="lead__meta">
                  <template v-if="leadArticle.author">{{ leadArticle.author }}</template>
                  <template v-if="leadArticle.minutes"> · 约 {{ leadArticle.minutes }} 分钟</template>
                  · {{ leadArticle.views }} 次浏览
                </span>
                <span class="lead__go">阅读全文
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                       stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M5 12h13M13 6l6 6-6 6" />
                  </svg>
                </span>
              </div>
            </div>

            <button
              v-if="showFavBtn"
              class="lead__fav"
              :class="{ 'lead__fav--on': favIds.has(leadArticle.id) }"
              type="button"
              :title="favIds.has(leadArticle.id) ? '取消收藏' : '收藏'"
              :aria-label="favIds.has(leadArticle.id) ? '取消收藏' : '收藏'"
              @click.stop="toggleFavorite(leadArticle)"
            >
              <svg viewBox="0 0 24 24" :fill="favIds.has(leadArticle.id) ? 'currentColor' : 'none'"
                   stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8L3.5 9.7l5.9-.8z" />
              </svg>
            </button>
          </div>
        </article>
      </section>

      <!-- ============================================================
           吸顶筛选条：分类 / 排序 / 计数，滚到哪儿都能换条件
           ============================================================ -->
      <div ref="filterSentinel" class="stick-sentinel" aria-hidden="true"></div>

      <div class="filter" :class="{ 'filter--stuck': stuck }">
        <nav class="filter__cats" aria-label="分类导航">
          <button
            class="cat"
            :class="{ 'cat--on': !categoryId && !tagId }"
            type="button"
            @click="selectCategory(null)"
          >
            全部<i>{{ grandTotal }}</i>
          </button>
          <button
            v-for="c in categories"
            :key="c.id"
            class="cat"
            :class="{ 'cat--on': categoryId === c.id }"
            type="button"
            @click="selectCategory(c)"
          >
            {{ c.name }}<i>{{ c.articleCount }}</i>
          </button>
        </nav>

        <div class="filter__right">
          <span v-if="!loading && !error" class="filter__count">{{ total }} 条</span>
          <div class="seg" role="group" aria-label="排序方式">
            <button
              class="seg__btn"
              :class="{ 'seg__btn--on': sort === 'hot' }"
              type="button"
              @click="changeSort('hot')"
            >热度</button>
            <button
              class="seg__btn"
              :class="{ 'seg__btn--on': sort === 'new' }"
              type="button"
              @click="changeSort('new')"
            >最新</button>
          </div>
        </div>
      </div>

      <!-- 二级标签：选中分类后才展开 -->
      <div v-if="currentCategory && currentCategory.tags.length" class="subtags">
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

      <!-- ============================================================
           主体：内容流 + 侧栏
           ============================================================ -->
      <div class="board">
        <div class="board__main">
          <!-- ---------- 继续阅读（仅登录且读过文章） ---------- -->
          <section v-if="recent.length" class="block">
            <div class="block__head">
              <h2>继续阅读</h2>
              <span class="block__sub">最近读过</span>
            </div>
            <div class="recent">
              <button
                v-for="r in recent"
                :key="r.id"
                class="recent__item"
                type="button"
                @click="$emit('open-article', r.id)"
              >
                <span class="recent__top">
                  <span class="recent__time">{{ relTime(r.viewedAt) }}</span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                       stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M5 12h13M13 6l6 6-6 6" />
                  </svg>
                </span>
                <span class="recent__title">{{ r.title }}</span>
              </button>
            </div>
          </section>

          <!-- ---------- 编辑精选：2 张封面卡 + 一张「浏览全部」入口 ---------- -->
          <section v-if="showPicks" class="block">
            <div class="block__head">
              <h2>编辑精选</h2>
              <span class="block__sub">按热度挑选</span>
            </div>
            <div class="picks">
              <PickCard
                v-for="(a, i) in picks"
                :key="a.id"
                :article="a"
                :cover-src="coverAt(i + 1)"
                :favorited="favIds.has(a.id)"
                :show-fav="showFavBtn"
                @open="$emit('open-article', $event)"
                @open-category="onOpenCategory"
                @toggle-fav="toggleFavorite"
              />
              <button class="picks__more" type="button" @click="scrollToFeed">
                <span class="picks__more-num">{{ restCount }}</span>
                <span class="picks__more-txt">篇待读<br>沿着列表往下翻</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M12 5v14M6 13l6 6 6-6" />
                </svg>
              </button>
            </div>
          </section>

          <!-- ---------- 内容流 ---------- -->
          <section class="block block--feed" :class="{ 'block--feed-filtered': hasFilter }">
            <div class="block__head">
              <h2>{{ resultsTitle }}</h2>
              <span class="block__sub">{{ sort === 'hot' ? '按热度' : '按时间' }}</span>
            </div>

            <!-- 当前生效的筛选条件 -->
            <div v-if="hasFilter" class="crt">
              <span class="crt__label">筛选</span>
              <span v-if="query" class="crt__chip">
                “{{ query }}”
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
              <button class="crt__clear" type="button" @click="resetAll">清空</button>
            </div>

            <!-- 骨架 -->
            <div v-if="loading && !list.length" class="skeleton">
              <div v-for="n in 4" :key="n" class="sk-row">
                <span class="sk-no"></span>
                <div class="sk-body">
                  <div class="sk-line sk-line--title"></div>
                  <div class="sk-line sk-line--sub"></div>
                  <div class="sk-line sk-line--meta"></div>
                </div>
              </div>
            </div>

            <!-- 首屏失败 -->
            <div v-else-if="error && !list.length" class="state state--error">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                   stroke-linecap="round" aria-hidden="true">
                <circle cx="12" cy="12" r="9" />
                <path d="M12 8v4.5M12 16h.01" />
              </svg>
              <p>{{ error }}</p>
              <button class="state__btn" type="button" @click="runSearch(1)">重试</button>
            </div>

            <!-- 空结果：给几个能点的出口，而不是只写「暂无内容」 -->
            <div v-else-if="!list.length" class="state">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                   stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.6-3.6" />
                <path d="M8 11h6" />
              </svg>
              <p>没有找到符合条件的内容</p>
              <div v-if="hotTags.length" class="state__tips">
                <span>换个方向试试</span>
                <button
                  v-for="t in hotTags.slice(0, 4)"
                  :key="t.id"
                  class="chip"
                  type="button"
                  @click="selectTag(t)"
                >{{ t.name }}</button>
              </div>
              <button class="state__btn" type="button" @click="resetAll">查看全部内容</button>
            </div>

            <!-- 列表 -->
            <div v-else class="feed">
              <ArticleRow
                v-for="(a, i) in rows"
                :key="a.id"
                :article="a"
                :index="i + 1"
                :favorited="favIds.has(a.id)"
                :show-fav="showFavBtn"
                @open="$emit('open-article', $event)"
                @open-category="onOpenCategory"
                @open-tag="selectTagByName"
                @toggle-fav="toggleFavorite"
              />
            </div>

            <!-- 无限滚动哨兵 -->
            <div v-if="!loading && !error && list.length" ref="sentinelEl" class="feed-sentinel" aria-hidden="true"></div>

            <div v-if="!loading && error && list.length" class="feed-more feed-more--err">
              加载失败：<button type="button" class="feed-more__retry" @click="runSearch(page + 1, true)">重试</button>
            </div>

            <div v-if="error === '' && list.length && loading" class="feed-more">
              <span class="feed-more__spin"></span>正在加载更多…
            </div>

            <div v-else-if="!loading && !error && list.length && !hasMore" class="feed-more feed-more--end">
              <span class="feed-more__line"></span>已经到底啦<span class="feed-more__line"></span>
            </div>
          </section>
        </div>

        <!-- ---------- 侧栏 ---------- -->
        <aside class="board__side" aria-label="站点侧栏">
          <section v-if="hotTags.length" class="side">
            <h3 class="side__title">标签云<span>{{ hotTags.length }} 个</span></h3>
            <TagCloud
              :tags="hotTags"
              :active-id="tagId"
              :limit="18"
              @select="selectTag"
            />
            <button class="side__link" type="button" @click="$emit('navigate', 'discover')">
              在发现页浏览全部分类 →
            </button>
          </section>

          <section v-if="categories.length" class="side">
            <h3 class="side__title">分类分布<span>{{ categories.length }} 类</span></h3>
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

          <section class="side">
            <h3 class="side__title">继续探索</h3>
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
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M9 6l6 6-6 6" />
                </svg>
              </button>
            </div>
            <button class="dice" type="button" :disabled="!pool.length" @click="goRandom">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                   stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M4 17h3.5l8-10H19" />
                <path d="m16.5 4 3 3-3 3" />
                <path d="M4 7h3.5l2.6 3.2" />
              </svg>
              随便逛逛
            </button>
          </section>

          <p class="side__foot">AURORA · 星海日志<br>把读过的技术，留在这片星海里</p>
        </aside>
      </div>
    </main>

    <!-- 回到顶部 -->
    <Transition name="totop">
      <button v-if="showTop" class="totop" type="button" aria-label="回到顶部" @click="toTop">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
             stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 19V5M6 11l6-6 6 6" />
        </svg>
      </button>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import SearchBar from './SearchBar.vue'
import SiteNav from './SiteNav.vue'
import PickCard from './PickCard.vue'
import ArticleRow from './ArticleRow.vue'
import TagCloud from './TagCloud.vue'
import { searchArticles, fetchCategories, fetchHotTags } from '../api/search'
import { fetchFavoriteIds, addFavorite, removeFavorite } from '../api/favorites'
import { fetchOverview } from '../api/stats'
import { fetchHistory } from '../api/history'
import { coverAt } from '../utils/cover'
import { toast } from '../composables/useToast'

const props = defineProps({
  user: { type: Object, default: null },
  /** 从其它页面跳转回来时携带的初始筛选条件（App 每次传入新对象触发更新） */
  initialFilter: { type: Object, default: null }
})
const emit = defineEmits(['navigate', 'open-article', 'logout', 'need-login'])

/* ---------- 用户信息 ---------- */
const userName = computed(() => (props.user && props.user.name) || '探险家')
const showFavBtn = computed(() => Boolean(props.user))

/* ---------- 检索状态 ---------- */
const PAGE_SIZE = 10

const keyword = ref('')      // 输入框当前内容（v-model）
const query = ref('')        // 已提交的关键词
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
const categories = ref([])    // 分类树
const hotTags = ref([])       // 热门标签
const favIds = ref(new Set()) // 当前用户已收藏的文章 id 集合

/* ---------- 站点概览 & 侧栏 ---------- */
const overview = ref(null)  // { articles, categories, tags, totalViews, avgViews }
const pool = ref([])        // 全站文章池（最多 50 条，供头条与「随便逛逛」使用）
const recent = ref([])      // 最近阅读（登录用户）

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

/** 数字缩写：一万以上用「万」，让数据在窄容器里也能放得下 */
function fmtNum (n) {
  const v = Number(n) || 0
  if (v < 10000) return String(v)
  return (v / 10000).toFixed(v >= 100000 ? 0 : 1) + ' 万'
}

/** 「x 分钟前 / x 小时前 / x 天前 / 日期」 */
function relTime (v) {
  if (!v) return ''
  const t = new Date(String(v).replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return String(v).slice(0, 10)
  const diff = Date.now() - t
  if (diff < 0) return '刚刚'
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const h = Math.floor(min / 60)
  if (h < 24) return `${h} 小时前`
  const d = Math.floor(h / 24)
  if (d < 30) return `${d} 天前`
  return String(v).slice(0, 10)
}

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

/**
 * 首屏头条：优先用全站最热的一篇（pool 已按热度倒序），
 * 文章池还没回来时退到当前列表第一条，保证首屏右侧不空着。
 */
const leadArticle = computed(() => pool.value[0] || list.value[0] || null)

/**
 * 是否拆成「精选 + 列表」。
 * 只在默认推荐视图且内容够多时拆：搜索/筛选结果每条命中项地位相同，
 * 单独拎几条出来当精选会误导用户。
 */
const showPicks = computed(() => !hasFilter.value && list.value.length > 4)

/**
 * 精选卡：从列表里排除已上头条的那篇，取前 2。
 * 之前头条与精选第一条取的是同一批数据，首屏会出现两遍同一篇文章。
 */
const picks = computed(() => {
  if (!showPicks.value) return []
  const leadId = leadArticle.value && leadArticle.value.id
  return list.value.filter((a) => a.id !== leadId).slice(0, 2)
})

/** 列表：默认视图去掉「头条 + 精选」已经在首屏出现过的条目，避免重复 */
const rows = computed(() => {
  if (!showPicks.value) return list.value
  const shown = new Set(picks.value.map((a) => a.id))
  const leadId = leadArticle.value && leadArticle.value.id
  if (leadId != null) shown.add(leadId)
  return list.value.filter((a) => !shown.has(a.id))
})

/** 「还有 N 篇」用的剩余数量 */
const restCount = computed(() => Math.max((total.value || list.value.length) - 1 - picks.value.length, 0))

/** 结果区标题 */
const resultsTitle = computed(() => {
  if (query.value) return `“${query.value}” 的结果`
  if (activeFilterText.value) return activeFilterText.value
  return sort.value === 'new' ? '最新发布' : '全部内容'
})

/** 没有关键词但有筛选条件时，用文字描述当前条件 */
const activeFilterText = computed(() => {
  if (currentTag.value) {
    return currentCategory.value
      ? `${currentCategory.value.name} · ${currentTag.value.name}`
      : currentTag.value.name
  }
  if (currentCategory.value) return currentCategory.value.name
  return ''
})

/* ---------- 主流程 ---------- */

/** 发起请求。p 为目标页码；append=true 时把结果追加到已有列表（无限滚动），否则整体替换 */
async function runSearch (p = 1, append = false) {
  loading.value = true
  error.value = ''

  try {
    const res = await searchArticles(query.value, {
      categoryId: categoryId.value,
      tagId: tagId.value,
      sort: sort.value,
      page: p,
      size: PAGE_SIZE
    })
    const items = (res && res.list) || []
    // 追加模式下去重，防止并发触发导致同一条文章出现两次
    list.value = append
      ? list.value.concat(items.filter((it) => !list.value.some((x) => x.id === it.id)))
      : items
    total.value = (res && res.total) || 0
    pages.value = (res && res.pages) || 0
    page.value = (res && res.page) || p
  } catch (e) {
    if (!append) {
      list.value = []
      total.value = 0
      pages.value = 0
    }
    error.value = (e && e.message) || '搜索失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/* ---------- 无限滚动（视频网站式下拉浏览） ---------- */
const sentinelEl = ref(null)   // 列表末尾的哨兵元素
let observer = null

/** 是否还有更多内容可加载 */
const hasMore = computed(() => pages.value > 0 && page.value < pages.value)

/** 滚到底附近时自动加载下一页 */
function loadMore () {
  if (loading.value || !hasMore.value || error.value) return
  runSearch(page.value + 1, true)
}

/** 建立滚动哨兵监听（列表结构变化时重建） */
function setupObserver () {
  if (observer) { observer.disconnect(); observer = null }
  if (!sentinelEl.value || typeof IntersectionObserver === 'undefined') return
  observer = new IntersectionObserver((entries) => {
    if (entries.some((e) => e.isIntersecting)) loadMore()
  }, { rootMargin: '320px 0px' })
  observer.observe(sentinelEl.value)
}

watch(sentinelEl, (el) => { if (el) setupObserver() })

/* ---------- 吸顶筛选条 & 回到顶部 ---------- */
const filterSentinel = ref(null)
const stuck = ref(false)
const showTop = ref(false)
let stickObserver = null

/** 页面滚动位置：这站的滚动容器被 body 的 overflow 传播到了视口，三种取值兜底 */
function scrollTop () {
  return window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0
}

function onScroll () {
  showTop.value = scrollTop() > 620
}

function toTop () {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 「还有 N 篇」：滚到内容流起点（预留吸顶条高度） */
function scrollToFeed () {
  const el = document.querySelector('.block--feed')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/* ---------- 交互 ---------- */

/** 搜索框提交 */
function onSearch (kw) {
  const k = (kw || '').trim()
  keyword.value = k
  query.value = k
  runSearch(1)
  scrollToFeed()
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

  // 从热门标签 / 标签云点进来时，若该标签属于某个分类而当前未选中它，自动补上分类，
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

/* ---------- 收藏 ---------- */

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
  const next = new Set(favIds.value)
  if (on) next.delete(article.id)
  else next.add(article.id)
  favIds.value = next
  try {
    if (on) await removeFavorite(article.id)
    else await addFavorite(article.id)
    toast.success(on ? '已取消收藏' : '已收藏')
  } catch (e) {
    const rollback = new Set(favIds.value)
    if (on) rollback.add(article.id)
    else rollback.delete(article.id)
    favIds.value = rollback
    toast.error((e && e.message) || '操作失败，请稍后再试')
  }
}

/* ---------- 最近阅读 ---------- */
async function loadRecent () {
  if (!props.user) {
    recent.value = []
    return
  }
  try {
    const res = await fetchHistory({ page: 1, size: 3 })
    recent.value = (res && res.list) || []
  } catch (_) {
    recent.value = []
  }
}

/* ---------- 跨页面初始筛选 ---------- */
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
  loadRecent()

  // 概览与文章池：与主列表并行拉取，失败时静默降级（页面其余部分照常可用）
  fetchOverview()
    .then((o) => { overview.value = o })
    .catch(() => { overview.value = null })
  searchArticles('', { sort: 'hot', page: 1, size: 50 })
    .then((res) => { pool.value = (res && res.list) || [] })
    .catch(() => { pool.value = [] })

  runSearch(1)

  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()

  // 筛选条离开原位（被吸住）时加底色，否则它会和后面的内容糊在一起
  if (filterSentinel.value && typeof IntersectionObserver !== 'undefined') {
    stickObserver = new IntersectionObserver(([e]) => {
      stuck.value = e.boundingClientRect.top < 92
    }, { threshold: [0, 1] })
    stickObserver.observe(filterSentinel.value)
  }
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
  if (stickObserver) stickObserver.disconnect()
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
/* ============================================================
   首页版式：深空编辑风
   - 结构靠留白与发丝线，不靠层层玻璃卡片
   - 强调色只出现在「当前状态」与行动点
   ============================================================ */
.home {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 96px;
}
/* 背景是 position:fixed 的极光画布，它不跟着内容走。
   页面变长之后，滚到列表区时正文就压在大片高饱和光斑上，灰字直接读不清。
   这里铺一层「跟着内容走」的压暗渐变：首屏全透明（保留首屏的通透感），
   往下逐渐收成接近 bg 的深色，正文区始终落在安静底上。
   z-index:-1 让它落在 .home 的层叠上下文底部 —— 仍在 #bg/.grain 之上、正文之下。 */
.home::before {
  content: '';
  position: absolute;
  z-index: -1;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 100vw;
  transform: translateX(-50%);
  pointer-events: none;
  background: linear-gradient(
    180deg,
    rgba(5, 6, 15, 0) 0,
    rgba(5, 6, 15, 0) 700px,
    rgba(5, 6, 15, 0.42) 1120px,
    rgba(5, 6, 15, 0.78) 1700px,
    rgba(5, 6, 15, 0.88) 100%
  );
}
.page {
  padding-top: 4px;
}

/* ---------------- Hero ---------------- */
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 0.98fr);
  gap: 52px;
  align-items: center;
  padding: 54px 0 40px;
}
.hero__copy {
  min-width: 0;
}
.hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--muted);
}
.hero__pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--a2);
  box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.55);
  animation: pulse 2.6s ease-out infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.5); }
  70% { box-shadow: 0 0 0 9px rgba(34, 211, 238, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 211, 238, 0); }
}

.hero__title {
  margin-top: 16px;
  font-size: clamp(30px, 3.5vw, 42px);
  font-weight: 600;
  line-height: 1.14;
  letter-spacing: -0.025em;
  color: var(--ink);
}
.hero__title em {
  font-style: normal;
  background: linear-gradient(100deg, var(--a2), var(--a1) 52%, var(--a3));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  background-size: 200% auto;
  animation: shine 7s linear infinite;
}

.hero__lede {
  margin-top: 14px;
  font-size: 14px;
  font-weight: 300;
  line-height: 1.8;
  color: var(--muted);
}
.hero__lede b {
  font-weight: 600;
  color: #cdd4f2;
  font-variant-numeric: tabular-nums;
}

.hero__search {
  margin-top: 26px;
  max-width: 540px;
}

.hero__hot {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 16px;
}
.hero__hot-label {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(141, 151, 189, 0.7);
  margin-right: 3px;
}
.chip {
  border: 1px solid var(--stroke);
  background: rgba(255, 255, 255, 0.03);
  color: var(--muted);
  font: inherit;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.25s, border-color 0.25s, background 0.25s;
}
.chip:hover {
  color: var(--ink);
  border-color: rgba(124, 92, 255, 0.5);
  background: rgba(124, 92, 255, 0.12);
}
.chip--on {
  color: #fff;
  background: rgba(124, 92, 255, 0.28);
  border-color: rgba(124, 92, 255, 0.65);
}

.hero__stats {
  display: flex;
  align-items: stretch;
  gap: 0;
  margin-top: 30px;
  list-style: none;
  padding: 0;
}
.hero__stats li {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 0 22px;
  border-left: 1px solid rgba(255, 255, 255, 0.09);
}
.hero__stats li:first-child {
  padding-left: 0;
  border-left: 0;
}
.hero__stats b {
  font-size: 19px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.hero__stats span {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: rgba(141, 151, 189, 0.8);
}

/* ---- 今日头条：整卡封面 + 底部渐变 ---- */
.lead {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--stroke);
  box-shadow: 0 40px 80px -46px rgba(0, 0, 0, 0.95);
  transition: transform 0.45s var(--ease), border-color 0.4s, box-shadow 0.45s;
  outline: none;
}
.lead:hover {
  transform: translateY(-4px);
  border-color: rgba(124, 92, 255, 0.4);
  box-shadow: 0 48px 90px -44px rgba(124, 92, 255, 0.5);
}
.lead:focus-visible {
  outline: 2px solid var(--a1);
  outline-offset: 3px;
}
.lead__cover {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 348px;
  padding: 26px;
  background-color: #0a0d1c;
  background-size: cover;
  background-position: center;
}
.lead__scrim {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(6, 7, 16, 0.15) 0%, rgba(6, 7, 16, 0.55) 46%, rgba(6, 7, 16, 0.94) 100%),
    radial-gradient(120% 80% at 20% 100%, rgba(124, 92, 255, 0.28), transparent 60%);
}
.lead__badge {
  position: absolute;
  top: 18px;
  left: 18px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #1a1030;
  background: linear-gradient(115deg, #ffe08a, #ffb347);
  box-shadow: 0 8px 20px -10px rgba(255, 179, 71, 0.9);
}
.lead__badge svg {
  width: 11px;
  height: 11px;
}
.lead__body {
  position: relative;
  z-index: 1;
}
.lead__cat {
  display: inline-block;
  margin-bottom: 10px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #e7ecff;
  font: inherit;
  font-size: 11px;
  padding: 3px 11px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s;
}
.lead__cat:hover {
  background: rgba(124, 92, 255, 0.4);
  border-color: rgba(124, 92, 255, 0.7);
}
.lead__title {
  font-size: 21px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.01em;
  color: #fff;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.lead__sum {
  margin-top: 8px;
  font-size: 12.5px;
  font-weight: 300;
  line-height: 1.65;
  color: rgba(216, 222, 245, 0.82);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.lead__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}
.lead__meta {
  font-size: 11.5px;
  font-weight: 300;
  letter-spacing: 0.02em;
  color: rgba(206, 214, 240, 0.72);
}
.lead__go {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
}
.lead__go svg {
  width: 14px;
  height: 14px;
  transition: transform 0.3s var(--ease);
}
.lead:hover .lead__go svg {
  transform: translateX(4px);
}
.lead__fav {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 2;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(10, 12, 24, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s;
}
.lead__fav svg {
  width: 15px;
  height: 15px;
}
.lead__fav:hover,
.lead__fav--on {
  color: #ffd76e;
  border-color: rgba(255, 215, 110, 0.45);
  background: rgba(255, 215, 110, 0.16);
}

/* ---------------- 吸顶筛选条 ---------------- */
.stick-sentinel {
  height: 1px;
  margin-bottom: -1px;
}
.filter {
  position: sticky;
  top: 92px;
  z-index: 4;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 10px;
  margin: 0 -10px 6px;
  border-radius: 14px;
  border: 1px solid transparent;
  transition: background 0.3s, border-color 0.3s, box-shadow 0.3s;
}
.filter--stuck {
  background: rgba(8, 10, 22, 0.86);
  backdrop-filter: blur(20px) saturate(140%);
  -webkit-backdrop-filter: blur(20px) saturate(140%);
  border-color: var(--stroke);
  box-shadow: 0 20px 44px -30px rgba(0, 0, 0, 0.95);
}
.filter__cats {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.filter__cats::-webkit-scrollbar {
  display: none;
}
.cat {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
  border: 1px solid transparent;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 13.5px;
  font-weight: 500;
  white-space: nowrap;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s;
}
.cat i {
  font-style: normal;
  font-size: 10.5px;
  font-variant-numeric: tabular-nums;
  opacity: 0.55;
}
.cat:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.05);
}
/* 当前分类：反白的实心药丸，和满屏紫色渐变按钮区分开 */
.cat--on {
  color: #0a0d1c;
  background: #eef2ff;
  border-color: #eef2ff;
}
.cat--on i {
  opacity: 0.5;
}
.filter__right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}
.filter__count {
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
  color: rgba(141, 151, 189, 0.75);
}
.seg {
  display: flex;
  padding: 2px;
  border-radius: 10px;
  border: 1px solid var(--stroke);
  background: rgba(255, 255, 255, 0.03);
}
.seg__btn {
  border: 0;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s;
}
.seg__btn:hover {
  color: var(--ink);
}
.seg__btn--on {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

/* 二级标签 */
.subtags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  padding: 12px 2px 4px;
}
.subtag {
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.03);
  color: var(--muted);
  font: inherit;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s;
}
.subtag:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.07);
}
.subtag--on {
  color: #c9c2ff;
  background: rgba(124, 92, 255, 0.18);
  border-color: rgba(124, 92, 255, 0.5);
}

/* ---------------- 主体 ---------------- */
.board {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 286px;
  gap: 48px;
  align-items: start;
  margin-top: 26px;
}
.board__main {
  min-width: 0;
}

.block {
  margin-bottom: 34px;
  scroll-margin-top: 108px;
}
.block--feed {
  scroll-margin-top: 108px;
}
.block__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--stroke);
}
.block__head h2 {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.005em;
  color: var(--ink);
}
.block__sub {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(141, 151, 189, 0.7);
}

/* ---- 继续阅读 ---- */
.recent {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}
.recent__item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
  padding: 13px 14px;
  border-radius: 12px;
  border: 1px solid var(--stroke);
  background: rgba(255, 255, 255, 0.022);
  color: var(--ink);
  font: inherit;
  cursor: pointer;
  transition: background 0.28s, border-color 0.28s, transform 0.28s var(--ease);
}
.recent__item:hover {
  background: rgba(124, 92, 255, 0.1);
  border-color: rgba(124, 92, 255, 0.4);
  transform: translateY(-2px);
}
.recent__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: rgba(141, 151, 189, 0.85);
}
.recent__top svg {
  width: 13px;
  height: 13px;
  opacity: 0.7;
  transition: transform 0.28s var(--ease);
}
.recent__item:hover .recent__top svg {
  transform: translateX(3px);
  color: var(--a2);
}
.recent__time {
  font-size: 10.5px;
  letter-spacing: 0.08em;
}
.recent__title {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
  color: #dfe4fb;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ---- 精选 ---- */
.picks {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}
.picks :deep(.pick__cover) {
  height: 132px;
}
.picks__more {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 6px;
  padding: 20px;
  border-radius: 14px;
  border: 1px dashed rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.015);
  color: var(--muted);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.3s, background 0.3s, color 0.3s;
}
.picks__more:hover {
  border-color: rgba(124, 92, 255, 0.5);
  background: rgba(124, 92, 255, 0.08);
  color: var(--ink);
}
.picks__more-num {
  font-size: 30px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
  color: #fff;
}
.picks__more-txt {
  font-size: 12.5px;
  line-height: 1.55;
  font-weight: 300;
}
.picks__more svg {
  width: 16px;
  height: 16px;
  margin-top: 4px;
  opacity: 0.6;
}

/* ---- 筛选面包屑 ---- */
.crt {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  padding: 14px 0 4px;
  font-size: 12px;
}
.crt__label {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(141, 151, 189, 0.6);
  margin-right: 2px;
}
.crt__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 6px 3px 11px;
  border-radius: 999px;
  border: 1px solid rgba(124, 92, 255, 0.38);
  background: rgba(124, 92, 255, 0.14);
  color: #cfc8ff;
}
.crt__chip--tag {
  border-color: rgba(34, 211, 238, 0.3);
  background: rgba(34, 211, 238, 0.1);
  color: #a8ecf7;
}
.crt__chip button {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  color: inherit;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s;
}
.crt__chip button:hover {
  background: rgba(255, 255, 255, 0.26);
}
.crt__clear {
  border: 0;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  padding: 3px 4px;
  transition: color 0.25s;
}
.crt__clear:hover {
  color: #ff9db0;
}

/* ---- 列表 ---- */
.feed {
  display: flex;
  flex-direction: column;
}
.feed :deep(.row) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.055);
}
.feed :deep(.row:last-child) {
  border-bottom: 0;
}

/* ---- 骨架 ---- */
.skeleton {
  padding-top: 8px;
}
.sk-row {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 18px;
  padding: 20px 12px;
}
.sk-no {
  width: 16px;
  height: 11px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.07);
}
.sk-line {
  height: 11px;
  border-radius: 5px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.11), rgba(255, 255, 255, 0.055));
  background-size: 200% 100%;
  animation: sk 1.5s ease-in-out infinite;
}
.sk-line--title {
  width: 62%;
  height: 14px;
}
.sk-line--sub {
  width: 88%;
  margin-top: 10px;
}
.sk-line--meta {
  width: 34%;
  height: 9px;
  margin-top: 12px;
}
@keyframes sk {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ---- 状态 ---- */
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 54px 20px;
  text-align: center;
  color: var(--muted);
}
.state svg {
  width: 30px;
  height: 30px;
  opacity: 0.5;
}
.state p {
  font-size: 13.5px;
}
.state__tips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  gap: 7px;
  margin-top: 4px;
  font-size: 11.5px;
  color: rgba(141, 151, 189, 0.75);
}
.state__btn {
  margin-top: 6px;
  border: 1px solid var(--stroke);
  background: rgba(255, 255, 255, 0.04);
  color: var(--ink);
  font: inherit;
  font-size: 12.5px;
  padding: 8px 18px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s;
}
.state__btn:hover {
  background: rgba(124, 92, 255, 0.16);
  border-color: rgba(124, 92, 255, 0.5);
}
.state--error {
  color: #ffb3c0;
}

/* ---- 加载更多 / 到底 ---- */
.feed-sentinel {
  height: 1px;
}
.feed-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 22px 0 8px;
  font-size: 12px;
  color: rgba(141, 151, 189, 0.8);
}
.feed-more--end {
  color: rgba(141, 151, 189, 0.55);
  gap: 12px;
}
.feed-more__line {
  width: 44px;
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
}
.feed-more__spin {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.16);
  border-top-color: var(--a2);
  animation: spin 0.9s linear infinite;
}
.feed-more__retry {
  border: 0;
  background: none;
  color: var(--a2);
  font: inherit;
  cursor: pointer;
  text-decoration: underline;
}

/* ---------------- 侧栏 ---------------- */
.board__side {
  position: sticky;
  top: 108px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.side {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--stroke);
  background: rgba(255, 255, 255, 0.02);
}
.side__title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(141, 151, 189, 0.85);
}
.side__title span {
  font-size: 10px;
  letter-spacing: 0.08em;
  font-weight: 400;
  color: rgba(141, 151, 189, 0.5);
  text-transform: none;
}
.side__link {
  margin-top: 10px;
  border: 0;
  background: none;
  padding: 0;
  font: inherit;
  font-size: 11.5px;
  color: rgba(141, 151, 189, 0.9);
  cursor: pointer;
  transition: color 0.25s;
}
.side__link:hover {
  color: var(--a2);
}

/* 分类分布 */
.dist {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.dist__row {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) 22px;
  align-items: center;
  gap: 9px;
  padding: 5px 6px;
  border-radius: 7px;
  border: 0;
  background: none;
  cursor: pointer;
  font: inherit;
  transition: background 0.25s;
}
.dist__row:hover,
.dist__row--on {
  background: rgba(255, 255, 255, 0.05);
}
.dist__name {
  font-size: 12px;
  color: var(--muted);
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dist__row--on .dist__name {
  color: var(--ink);
}
.dist__bar {
  height: 3px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}
.dist__bar i {
  display: block;
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--a1), var(--a2));
  transition: width 0.5s var(--ease);
}
.dist__row--on .dist__bar i {
  background: linear-gradient(90deg, var(--a2), #eef2ff);
}
.dist__num {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: rgba(141, 151, 189, 0.75);
  text-align: right;
}

/* 快捷入口 */
.quick {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.quick__item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 8px 6px;
  border: 0;
  border-radius: 8px;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 12.5px;
  text-align: left;
  cursor: pointer;
  transition: background 0.25s, color 0.25s;
}
.quick__item svg {
  width: 13px;
  height: 13px;
  margin-left: auto;
  opacity: 0.45;
  transition: transform 0.25s var(--ease), opacity 0.25s;
}
.quick__item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--ink);
}
.quick__item:hover svg {
  opacity: 1;
  transform: translateX(2px);
}
.quick__dot {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  flex: none;
}

.dice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  margin-top: 10px;
  padding: 9px;
  border-radius: 10px;
  border: 1px dashed rgba(255, 255, 255, 0.16);
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.25s, border-color 0.25s, background 0.25s;
}
.dice svg {
  width: 14px;
  height: 14px;
}
.dice:hover:not(:disabled) {
  color: var(--ink);
  border-color: rgba(34, 211, 238, 0.45);
  background: rgba(34, 211, 238, 0.07);
}
.dice:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.side__foot {
  padding: 4px 4px 0;
  font-size: 10.5px;
  line-height: 1.7;
  letter-spacing: 0.04em;
  color: rgba(141, 151, 189, 0.42);
}

/* ---------------- 回到顶部 ---------------- */
.totop {
  position: fixed;
  right: 26px;
  bottom: 26px;
  z-index: 8;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  border: 1px solid var(--stroke);
  background: rgba(10, 12, 26, 0.8);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  color: var(--muted);
  cursor: pointer;
  box-shadow: 0 18px 40px -22px rgba(0, 0, 0, 0.9);
  transition: color 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.totop svg {
  width: 16px;
  height: 16px;
}
.totop:hover {
  color: var(--ink);
  border-color: rgba(124, 92, 255, 0.5);
  transform: translateY(-2px);
}
.totop-enter-active,
.totop-leave-active {
  transition: opacity 0.25s var(--ease), transform 0.25s var(--ease);
}
.totop-enter-from,
.totop-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1080px) {
  .board {
    grid-template-columns: minmax(0, 1fr) 250px;
    gap: 34px;
  }
  .hero {
    gap: 34px;
  }
  .lead__cover {
    min-height: 312px;
    padding: 22px;
  }
}

@media (max-width: 1040px) {
  .hero {
    grid-template-columns: minmax(0, 1fr);
    gap: 30px;
    padding: 40px 0 30px;
  }
  .hero__lede {
    max-width: none;
  }
  .hero__search {
    max-width: none;
  }
  .lead__cover {
    min-height: 300px;
  }
  .board {
    grid-template-columns: minmax(0, 1fr);
    gap: 30px;
  }
  .board__side {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
  .side__foot {
    grid-column: 1 / -1;
  }
  .filter {
    top: 86px;
  }
}

@media (max-width: 720px) {
  .home {
    padding: 0 16px 72px;
  }
  .hero {
    padding: 30px 0 24px;
  }
  /* 数据已在副文案里说过一遍，移动端不再重复占高度 */
  .hero__stats {
    display: none;
  }
  .hero__hot .chip:nth-of-type(n + 4) {
    display: none;
  }
  .lead__cover {
    min-height: 268px;
    padding: 18px;
  }
  .lead__title {
    font-size: 18.5px;
  }
  .hero__title {
    font-size: 28px;
  }
  .board__side {
    grid-template-columns: minmax(0, 1fr);
  }
  .picks {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 11px;
  }
  .picks__more {
    grid-column: 1 / -1;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
  }
  .picks__more-num {
    font-size: 22px;
  }
  .picks__more-txt br {
    display: none;
  }
  .picks__more svg {
    margin-top: 0;
  }
  .recent {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .recent__item:last-child:nth-child(odd) {
    display: none;
  }
  .filter {
    top: 78px;
    gap: 8px;
    padding: 6px 6px;
    margin: 0 -6px 4px;
  }
  .cat {
    padding: 6px 11px;
    font-size: 12.5px;
  }
  .filter__count {
    display: none;
  }
  .seg__btn {
    padding: 5px 9px;
    font-size: 11.5px;
  }
  .block__head h2 {
    font-size: 15.5px;
  }
  .totop {
    right: 16px;
    bottom: 16px;
    width: 38px;
    height: 38px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero__pulse,
  .hero__title em,
  .sk-line,
  .feed-more__spin {
    animation: none;
  }
}
</style>
