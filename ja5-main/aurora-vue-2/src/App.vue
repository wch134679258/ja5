<template>
  <AuroraBackground />
  <div class="grain"></div>
  <div class="vignette"></div>

  <!-- 全局轻提示：任何组件调用 toast.xxx() 都会在这里显示 -->
  <ToastHost />

  <!-- 登录 / 注册页 -->
  <main v-if="view === 'auth'" class="stage">
    <section class="card" ref="card" @pointermove="onMove" @pointerleave="onLeave">
      <div class="card__spin"></div>
      <div class="card__fill"></div>
      <div class="card__spot" ref="spot"></div>

      <div class="card__inner">
        <BrandPanel />
        <AuthPanel ref="authPanel" @success="onSuccess" />
      </div>

      <SuccessOverlay :show="showDone" :title="doneTitle" :sub="doneSub" @reset="onReset" />
    </section>
  </main>

  <!-- ============ 登录后的页面（视图状态机） ============ -->
  <HomePage
    v-else-if="view === 'home'"
    :user="user"
    :initial-filter="homeFilter"
    @navigate="onNavigate"
    @open-article="openArticle"
    @logout="doLogout"
    @need-login="goAuth"
  />

  <ArticleDetail
    v-else-if="view === 'article'"
    :article-id="articleId"
    :user="user"
    @back="goHome()"
    @open-article="openArticle"
    @open-category="onOpenCategory"
    @open-tag="onOpenTag"
    @navigate="onNavigate"
    @logout="doLogout"
    @need-login="goAuth"
  />

  <FavoritesPage
    v-else-if="view === 'favorites'"
    :user="user"
    @navigate="onNavigate"
    @open-article="openArticle"
    @open-category="onOpenCategory"
    @open-tag="onOpenTag"
    @logout="doLogout"
    @need-login="goAuth"
  />

  <DiscoverPage
    v-else-if="view === 'discover'"
    :user="user"
    @navigate="onNavigate"
    @open-article="openArticle"
    @open-category="onOpenCategory"
    @open-tag="onOpenTag"
    @open-home="goHome"
    @logout="doLogout"
  />

  <AboutPage
    v-else-if="view === 'about'"
    :user="user"
    @navigate="onNavigate"
    @logout="doLogout"
  />

  <SpaceSnake
    v-else-if="view === 'game'"
    :user="user"
    @navigate="onNavigate"
    @open-article="openArticle"
    @open-category="onOpenCategory"
    @open-tag="onOpenTag"
    @open-home="goHome"
    @logout="doLogout"
  />

  <ProfilePage
    v-else-if="view === 'profile'"
    :user="user"
    @navigate="onNavigate"
    @open-article="openArticle"
    @logout="doLogout"
  />

  <!-- 全站页脚（登录页与全屏游戏页不显示） -->
  <div v-if="showFooter" class="footwrap">
    <SiteFooter @go="onNavigate" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AuroraBackground from './components/AuroraBackground.vue'
import ToastHost from './components/ToastHost.vue'
import SiteFooter from './components/SiteFooter.vue'
import ProfilePage from './components/ProfilePage.vue'
import BrandPanel from './components/BrandPanel.vue'
import AuthPanel from './components/AuthPanel.vue'
import SuccessOverlay from './components/SuccessOverlay.vue'
import HomePage from './components/HomePage.vue'
import ArticleDetail from './components/ArticleDetail.vue'
import FavoritesPage from './components/FavoritesPage.vue'
import DiscoverPage from './components/DiscoverPage.vue'
import AboutPage from './components/AboutPage.vue'
import SpaceSnake from './components/SpaceSnake.vue'
import { useTilt } from './composables/useTilt'
import { getMe, logout, getToken } from './api/auth'

const { card, spot, onMove, onLeave } = useTilt()

const authPanel = ref(null)
const showDone = ref(false)
const doneTitle = ref('')
const doneSub = ref('')

/**
 * 视图状态机（未引入 vue-router，单页应用用状态切换足够）：
 *   auth       登录 / 注册
 *   home       首页（搜索 + 分类）
 *   article    文章详情
 *   favorites  我的收藏
 *   discover   发现
 *   profile    个人中心（资料 / 统计 / 浏览历史 / 游戏成绩）
 *   about      关于
 *   game       星空贪吃蛇小游戏
 */
const view = ref('auth')
const user = ref(null)
const articleId = ref(null)

/** 登录页与全屏游戏页不显示页脚 */
const showFooter = computed(() => view.value !== 'auth' && view.value !== 'game')

/** 从其它页面跳回首页时携带的筛选条件（每次传新对象以触发 HomePage 更新） */
const homeFilter = ref(null)

// 除登录页外都需要文档级纵向滚动，切换 view 时同步 body 的滚动模式
watch(view, (v) => {
  document.body.classList.toggle('view-home', v !== 'auth')
  window.scrollTo({ top: 0 })
})

/* ---------- 登录流程 ---------- */

async function onSuccess (mode) {
  doneTitle.value = mode === 'signup' ? '账号创建成功' : '欢迎回来，探险家'
  doneSub.value = '正在为你打开星海首页…'
  showDone.value = true

  // 注册 / 登录成功后：用 JWT 拉取受保护信息并进入首页
  if (mode === 'signup' || mode === 'login') {
    try {
      user.value = await getMe()
      goHome()
      showDone.value = false
    } catch (e) {
      doneSub.value = (e && e.message) || '获取用户信息失败'
    }
  }
}

function onReset () {
  showDone.value = false
  if (authPanel.value) authPanel.value.resetToLogin()
}

function doLogout () {
  logout()
  user.value = null
  goAuth()
}

/** 回到登录页（收藏等需要登录的操作会引导到这里） */
function goAuth () {
  view.value = 'auth'
  showDone.value = false
  if (authPanel.value) authPanel.value.resetToLogin()
}

/* ---------- 页面导航 ---------- */

function onNavigate (key) {
  view.value = key
}

function openArticle (id) {
  articleId.value = id
  view.value = 'article'
}

/** 回首页，可携带筛选条件（分类 / 标签 / 排序） */
function goHome (filter = null) {
  homeFilter.value = filter ? { ...filter, _t: Date.now() } : null
  view.value = 'home'
}

function onOpenCategory (categoryId) {
  goHome({ categoryId: categoryId || null })
}

function onOpenTag (name) {
  goHome({ tagName: name })
}

// 刷新页面保持登录态：本地有 token 就直接进首页，token 失效则回登录页
onMounted(async () => {
  if (!getToken()) return
  try {
    user.value = await getMe()
    view.value = 'home'
  } catch (e) {
    // token 过期 / 后端未启动：静默回到登录页
    user.value = null
    view.value = 'auth'
  }
})
</script>

<style scoped>
/* 注意：这里曾经写过
     :global(body.view-home) .grain, :global(body.view-home) .vignette { position: fixed }
   以为只作用于 .grain/.vignette，但 Vue 的 scoped 编译会丢掉后面的后代选择器，
   实际产出是「body.view-home, body.view-home { position: fixed }」——
   等于给 body 加了固定定位，body 脱离文档流后 html 只高一屏，
   全站所有非登录页都滚不动（scrollTop 赋值无效、滚轮无反应）。
   .grain / .vignette 的固定定位在 style.css 里已经写了，这里无需重复。 */

/* 页脚容器：与各页面内容同宽，居中排布 */
.footwrap {
  position: relative;
  z-index: 2;
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: 0 24px 34px;
}
</style>
