<template>
  <AuroraBackground />
  <div class="grain"></div>
  <div class="vignette"></div>

  <main class="stage">
    <!-- 登录 / 注册卡片 -->
    <section v-if="view === 'auth'" class="card" ref="card" @pointermove="onMove" @pointerleave="onLeave">
      <div class="card__spin"></div>
      <div class="card__fill"></div>
      <div class="card__spot" ref="spot"></div>

      <div class="card__inner">
        <BrandPanel />
        <AuthPanel ref="authPanel" @success="onSuccess" />
      </div>

      <SuccessOverlay :show="showDone" :title="doneTitle" :sub="doneSub" @reset="onReset" />
    </section>

    <!-- 登录后控制台（JWT 受保护页） -->
    <section v-else class="card card--console" ref="card" @pointermove="onMove" @pointerleave="onLeave">
      <div class="card__spin"></div>
      <div class="card__fill"></div>
      <div class="card__spot" ref="spot"></div>

      <div class="card__inner">
        <BrandPanel />
        <div class="console">
          <div class="console__badge">PROTECTED</div>
          <h2>欢迎回来，{{ user?.name || '探险家' }} 🚀</h2>
          <p class="console__sub">这是通过 JWT 校验的受保护页面</p>

          <ul class="console__info" v-if="user">
            <li><span>用户 ID</span><b>{{ user.id }}</b></li>
            <li><span>昵称</span><b>{{ user.name }}</b></li>
            <li><span>邮箱</span><b>{{ user.email }}</b></li>
          </ul>

          <button class="console__logout" @click="doLogout">注销并退出</button>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import AuroraBackground from './components/AuroraBackground.vue'
import BrandPanel from './components/BrandPanel.vue'
import AuthPanel from './components/AuthPanel.vue'
import SuccessOverlay from './components/SuccessOverlay.vue'
import { useTilt } from './composables/useTilt'
import { getMe, logout } from './api/auth'

const { card, spot, onMove, onLeave } = useTilt()

const authPanel = ref(null)
const showDone = ref(false)
const doneTitle = ref('')
const doneSub = ref('')

const view = ref('auth')
const user = ref(null)

async function onSuccess (mode) {
  doneTitle.value = mode === 'signup' ? '账号创建成功' : '欢迎回来，探险家'
  doneSub.value = mode === 'reset'
    ? '请查收邮箱，点击链接重置密码'
    : '正在为你打开星海控制台…'
  showDone.value = true

  // 注册 / 登录成功后：用 JWT 拉取受保护信息并进入控制台
  if (mode === 'signup' || mode === 'login') {
    try {
      user.value = await getMe()
      view.value = 'console'
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
  view.value = 'auth'
  if (authPanel.value) authPanel.value.resetToLogin()
}
</script>

<style scoped>
.console {
  text-align: center;
  padding: 8px 4px 4px;
}
.console__badge {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 2px;
  color: #7c5cff;
  border: 1px solid rgba(124, 92, 255, 0.5);
  border-radius: 999px;
  padding: 3px 12px;
  margin-bottom: 14px;
}
.console h2 {
  font-size: 24px;
  margin: 0 0 6px;
  background: linear-gradient(90deg, #22d3ee, #7c5cff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.console__sub {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  margin: 0 0 20px;
}
.console__info {
  list-style: none;
  padding: 0;
  margin: 0 0 22px;
  display: grid;
  gap: 10px;
}
.console__info li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
}
.console__info span {
  color: rgba(255, 255, 255, 0.55);
}
.console__info b {
  color: #fff;
  font-weight: 600;
}
.console__logout {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(90deg, #ff5f78, #ff8a5f);
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.console__logout:hover {
  transform: translateY(-1px);
  opacity: 0.95;
}
</style>
