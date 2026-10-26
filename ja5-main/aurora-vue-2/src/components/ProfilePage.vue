<template>
  <div class="profile">
    <SiteNav :user="user" active="profile" @go="onGo" @logout="$emit('logout')" />

    <main class="profile__main">
      <!-- ============ 个人信息卡 ============ -->
      <section class="pcard">
        <StateBlock v-if="loading" variant="skeleton" shape="line" />
        <StateBlock
          v-else-if="error"
          variant="error"
          title="个人资料加载失败"
          :desc="error"
          @action="load"
        />
        <template v-else>
          <div class="pcard__head">
            <div class="pcard__avatar">{{ avatarText }}</div>
            <div class="pcard__id">
              <h1 class="pcard__name">{{ profile.user.name }}</h1>
              <p class="pcard__sub">
                ID · {{ profile.user.id }}
                <span v-if="profile.user.joinedAt"> · 加入于 {{ profile.user.joinedAt }}</span>
              </p>
            </div>
            <button class="pcard__logout" type="button" @click="$emit('logout')">退出登录</button>
          </div>

          <!-- 统计 -->
          <div class="stats">
            <button class="stat" type="button" @click="$emit('navigate', 'favorites')">
              <b>{{ profile.stats.favorites }}</b>
              <span>收藏</span>
            </button>
            <div class="stat">
              <b>{{ profile.stats.history }}</b>
              <span>浏览</span>
            </div>
            <div class="stat">
              <b>{{ profile.stats.likes }}</b>
              <span>点赞</span>
            </div>
            <button class="stat" type="button" @click="$emit('navigate', 'game')">
              <b>{{ profile.game.best }}</b>
              <span>游戏最高分</span>
            </button>
          </div>

          <p class="pcard__game" v-if="profile.game.playCount > 0">
            星空贪吃蛇：玩过 {{ profile.game.playCount }} 局，当前排名 第 {{ profile.game.rank }} 名
          </p>
          <p class="pcard__game pcard__game--muted" v-else>
            还没有游戏记录，去玩一局星空贪吃蛇吧 →
            <button class="pcard__link" type="button" @click="$emit('navigate', 'game')">开始游戏</button>
          </p>
        </template>
      </section>

      <!-- ============ 浏览历史 ============ -->
      <section class="hist">
        <div class="hist__head">
          <h2 class="hist__title">浏览历史</h2>
          <button
            v-if="history.length && !histLoading"
            class="hist__clear"
            type="button"
            :disabled="clearing"
            @click="clearAll"
          >
            {{ clearing ? '清空中…' : '清空' }}
          </button>
        </div>

        <div v-if="histLoading" class="hist__list">
          <StateBlock v-for="i in 3" :key="'sk' + i" variant="skeleton" shape="card" />
        </div>

        <StateBlock
          v-else-if="histError"
          variant="error"
          title="浏览历史加载失败"
          :desc="histError"
          @action="loadHistory"
        />

        <StateBlock
          v-else-if="!history.length"
          variant="empty"
          icon="◷"
          title="还没有浏览记录"
          desc="打开任意一篇文章，这里会留下你的阅读足迹。"
          action-text="去首页看看"
          @action="$emit('navigate', 'home')"
        />

        <template v-else>
          <div class="hist__list">
            <article v-for="h in history" :key="h.id" class="hcard">
              <div class="hcard__body" @click="$emit('open-article', h.id)">
                <h3 class="hcard__title">{{ h.title }}</h3>
                <p class="hcard__summary">{{ h.summary }}</p>
                <div class="hcard__foot">
                  <span class="hcard__cat">{{ h.category }}</span>
                  <span class="hcard__dot"></span>
                  <span>{{ h.author }}</span>
                  <span class="hcard__dot"></span>
                  <span>浏览于 {{ h.viewedAt }}</span>
                </div>
              </div>
              <button
                class="hcard__del"
                type="button"
                title="删除这条记录"
                @click="removeOne(h)"
              >×</button>
            </article>
          </div>

          <!-- 分页 -->
          <div v-if="histPages > 1" class="pager">
            <button class="pager__btn" :disabled="histPage <= 1" @click="goPage(histPage - 1)">上一页</button>
            <span class="pager__num">{{ histPage }} / {{ histPages }}</span>
            <button class="pager__btn" :disabled="histPage >= histPages" @click="goPage(histPage + 1)">下一页</button>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import SiteNav from './SiteNav.vue'
import StateBlock from './StateBlocks.vue'
import { fetchProfile } from '../api/profile'
import { fetchHistory, removeHistory, clearHistory } from '../api/history'
import { toast } from '../composables/useToast'

const props = defineProps({
  user: { type: Object, default: null }
})
const emit = defineEmits(['navigate', 'open-article', 'logout'])

const emptyProfile = {
  user: { id: null, name: '探险家', joinedAt: '' },
  stats: { favorites: 0, history: 0, likes: 0 },
  game: { best: 0, rank: 0, playCount: 0 }
}

const profile = ref(emptyProfile)
const loading = ref(false)
const error = ref('')

const history = ref([])
const histLoading = ref(false)
const histError = ref('')
const histPage = ref(1)
const histPages = ref(1)
const clearing = ref(false)

const avatarText = computed(() => {
  const n = (profile.value.user && profile.value.user.name) || 'A'
  return n.slice(0, 1).toUpperCase()
})

async function load () {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchProfile()
    profile.value = {
      user: (data && data.user) || emptyProfile.user,
      stats: (data && data.stats) || emptyProfile.stats,
      game: (data && data.game) || emptyProfile.game
    }
  } catch (e) {
    profile.value = emptyProfile
    error.value = (e && e.message) || '个人资料加载失败'
  } finally {
    loading.value = false
  }
}

async function loadHistory (page = histPage.value) {
  histLoading.value = true
  histError.value = ''
  try {
    const res = await fetchHistory({ page, size: 6 })
    history.value = (res && res.list) || []
    histPage.value = (res && res.page) || 1
    histPages.value = (res && res.pages) || 1
  } catch (e) {
    history.value = []
    histError.value = (e && e.message) || '浏览历史加载失败'
  } finally {
    histLoading.value = false
  }
}

function goPage (p) {
  if (p < 1 || p > histPages.value) return
  loadHistory(p)
}

async function removeOne (h) {
  try {
    await removeHistory(h.id)
    history.value = history.value.filter((x) => x.id !== h.id)
    profile.value.stats.history = Math.max(0, (profile.value.stats.history || 0) - 1)
    toast.success('已删除这条记录')
    // 删完当前页没内容了，回退一页继续看
    if (!history.value.length && histPage.value > 1) {
      goPage(histPage.value - 1)
    }
  } catch (e) {
    toast.error((e && e.message) || '删除失败，请重试')
  }
}

async function clearAll () {
  clearing.value = true
  try {
    const res = await clearHistory()
    history.value = []
    histPage.value = 1
    histPages.value = 1
    profile.value.stats.history = 0
    toast.success((res && res.message) || '已清空浏览历史')
  } catch (e) {
    toast.error((e && e.message) || '清空失败，请重试')
  } finally {
    clearing.value = false
  }
}

/** 页脚 / 导航跳转统一走 navigate，由 App 决定目标视图 */
function onGo (key) {
  emit('navigate', key)
}

onMounted(() => {
  load()
  loadHistory(1)
})
</script>

<style scoped>
.profile {
  min-height: 100%;
  padding: 18px 22px 40px;
}

.profile__main {
  width: min(1080px, 100%);
  margin: 24px auto 0;
  display: grid;
  gap: 22px;
}

/* ---------- 个人信息卡 ---------- */
.pcard {
  padding: 22px;
  border-radius: 22px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: var(--glass, rgba(255, 255, 255, .055));
  backdrop-filter: blur(18px);
}

.pcard__head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pcard__avatar {
  width: 54px;
  height: 54px;
  flex: none;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--a1, #7c5cff), var(--a2, #22d3ee));
  box-shadow: 0 12px 28px -14px var(--a1, #7c5cff);
}

.pcard__id { min-width: 0; }
.pcard__name { font-size: 20px; font-weight: 650; }
.pcard__sub { font-size: 12.5px; color: var(--muted, #8d97bd); margin-top: 4px; }

.pcard__logout {
  margin-left: auto;
  height: 36px;
  padding: 0 16px;
  border-radius: 11px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: var(--glass, rgba(255, 255, 255, .055));
  color: var(--muted, #8d97bd);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: color .25s, border-color .25s, background .25s;
}
.pcard__logout:hover {
  color: #ffb3c0;
  border-color: rgba(255, 95, 120, .4);
  background: rgba(255, 95, 120, .12);
}

.stats {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
}

.stat {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: rgba(255, 255, 255, .04);
  display: grid;
  gap: 4px;
  text-align: left;
  font: inherit;
  color: inherit;
  cursor: default;
  transition: transform .25s var(--ease, ease), border-color .25s;
}
button.stat { cursor: pointer; }
button.stat:hover {
  transform: translateY(-2px);
  border-color: rgba(124, 92, 255, .45);
}
.stat b { font-size: 22px; font-weight: 650; }
.stat span { font-size: 12.5px; color: var(--muted, #8d97bd); }

.pcard__game {
  margin-top: 14px;
  font-size: 13px;
  color: var(--muted, #8d97bd);
}
.pcard__game--muted { opacity: .9; }
.pcard__link {
  border: 0;
  background: none;
  font: inherit;
  color: var(--a2, #22d3ee);
  cursor: pointer;
  text-decoration: underline;
}

/* ---------- 浏览历史 ---------- */
.hist {
  padding: 20px 22px 22px;
  border-radius: 22px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: var(--glass, rgba(255, 255, 255, .04));
}

.hist__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.hist__title { font-size: 16px; font-weight: 600; }
.hist__clear {
  margin-left: auto;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: transparent;
  color: var(--muted, #8d97bd);
  font: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: color .25s, border-color .25s;
}
.hist__clear:hover:not(:disabled) {
  color: #ffb3c0;
  border-color: rgba(255, 95, 120, .4);
}
.hist__clear:disabled { opacity: .5; cursor: not-allowed; }

.hist__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.hcard {
  position: relative;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .1));
  background: rgba(255, 255, 255, .035);
  transition: transform .25s var(--ease, ease), border-color .25s, background .25s;
}
.hcard:hover {
  transform: translateY(-2px);
  border-color: rgba(124, 92, 255, .4);
  background: rgba(255, 255, 255, .06);
}

.hcard__body { cursor: pointer; }
.hcard__title {
  font-size: 14.5px;
  font-weight: 600;
  line-height: 1.4;
  padding-right: 22px;
}
.hcard__summary {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--muted, #8d97bd);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.hcard__foot {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: rgba(141, 151, 189, .9);
}
.hcard__cat {
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(124, 92, 255, .16);
  color: #cbb8ff;
}
.hcard__dot { opacity: .4; }

.hcard__del {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, .06);
  color: var(--muted, #8d97bd);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  transition: color .2s, background .2s;
}
.hcard__del:hover {
  color: #ffb3c0;
  background: rgba(255, 95, 120, .16);
}

/* ---------- 分页 ---------- */
.pager {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.pager__btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: var(--glass, rgba(255, 255, 255, .05));
  color: var(--ink, #eef2ff);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: background .25s, border-color .25s;
}
.pager__btn:hover:not(:disabled) {
  background: rgba(124, 92, 255, .2);
  border-color: rgba(124, 92, 255, .45);
}
.pager__btn:disabled { opacity: .45; cursor: not-allowed; }
.pager__num { font-size: 13px; color: var(--muted, #8d97bd); }

@media (max-width: 640px) {
  .profile { padding: 14px 14px 32px; }
  .pcard__logout { display: none; }
}
</style>
