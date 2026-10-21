<template>
  <div class="page">
    <SiteNav :user="user" active="about" @go="$emit('navigate', $event)" @logout="$emit('logout')" />

    <main class="main">
      <header class="head">
        <span class="head__badge">ABOUT</span>
        <h1 class="head__title">关于 AURORA</h1>
        <p class="head__sub">星海入口 · 一个前后端分离的内容检索演示项目</p>
      </header>

      <section class="card">
        <h2 class="card__title">项目简介</h2>
        <p class="card__text">
          AURORA（星海入口）是一个全栈内容检索站点：前端采用 Vue 3 + Vite 打造的极光玻璃拟态界面，
          后端采用 Spring Boot 3 + MyBatis-Plus 提供认证与检索能力，数据存储在 MySQL 中。
        </p>
        <p class="card__text">
          站点内置技术文章与小说内容（如《凡人修仙传》专题），支持关键词搜索、两级分类筛选、
          排序切换、分页浏览、文章阅读、收藏管理等完整功能，还内置了一颗休闲彩蛋——星空贪吃蛇。
        </p>
      </section>

      <section class="grid">
        <div class="feature">
          <div class="feature__ico feature__ico--a">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.6-3.6" />
            </svg>
          </div>
          <h3 class="feature__title">分级分类检索</h3>
          <p class="feature__text">一级分类 + 二级标签交叉筛选，关键词命中标题优先的相关度排序，支持搜索建议与历史记录。</p>
        </div>

        <div class="feature">
          <div class="feature__ico feature__ico--b">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20" />
            </svg>
          </div>
          <h3 class="feature__title">沉浸式阅读</h3>
          <p class="feature__text">文章详情页带正文排版、标签跳转、相关文章推荐与浏览量统计，收藏功能需登录后使用。</p>
        </div>

        <div class="feature">
          <div class="feature__ico feature__ico--c">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="10" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>
          <h3 class="feature__title">JWT 认证</h3>
          <p class="feature__text">昵称 + 密码注册登录，图形验证码防护，BCrypt 加密存储，令牌本地持久化，刷新页面保持登录态。</p>
        </div>

        <div class="feature">
          <div class="feature__ico feature__ico--d">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3v3M12 18v3M3 12h3M18 12h3" />
              <circle cx="12" cy="12" r="4" />
            </svg>
          </div>
          <h3 class="feature__title">极光视觉</h3>
          <p class="feature__text">深色星海背景、玻璃拟态卡片、旋转极光描边与微交互，桌面与移动端自适应。</p>
        </div>

        <div class="feature">
          <div class="feature__ico feature__ico--e">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="9" />
              <ellipse cx="12" cy="12" rx="9" ry="3.4" transform="rotate(-18 12 12)" />
            </svg>
          </div>
          <h3 class="feature__title">星空贪吃蛇</h3>
          <p class="feature__text">光蛇吞噬小星球的休闲小游戏，航速渐增，成绩实时入库，排行榜全站可见。</p>
        </div>
      </section>

      <section class="card">
        <h2 class="card__title">技术栈</h2>
        <div class="stack">
          <span v-for="s in stack" :key="s" class="stack__chip">{{ s }}</span>
        </div>
      </section>

      <section class="card card--tip">
        <h2 class="card__title">测试账号</h2>
        <p class="card__text">
          昵称 <code>verify用户</code> · 密码 <code>abc12345</code>
          （已入库，可直接登录验证收藏功能；也可自行注册新账号）
        </p>
      </section>

      <footer class="foot">
        <span>AURORA · 星海入口</span>
        <span class="foot__dot"></span>
        <span>Vue 3 · Spring Boot 3 · MySQL</span>
      </footer>
    </main>
  </div>
</template>

<script setup>
defineProps({
  user: { type: Object, default: null }
})
defineEmits(['navigate', 'logout'])

const stack = [
  'Vue 3', 'Vite', 'Vue Router（视图状态机）', 'Spring Boot 3', 'MyBatis-Plus',
  'MySQL 8', 'JWT', 'BCrypt', 'REST API'
]
</script>

<style scoped>
/* ============ 页面骨架 ============ */
.page {
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
  gap: 22px;
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
  color: var(--a1);
  border: 1px solid rgba(124, 92, 255, 0.5);
  border-radius: 999px;
  padding: 3px 13px;
  margin-bottom: 16px;
}
.head__title {
  font-size: clamp(28px, 3.6vw, 40px);
  font-weight: 600;
  letter-spacing: -0.02em;
  background: linear-gradient(100deg, var(--a2), var(--a1) 45%, var(--a3));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  background-size: 200% auto;
  animation: shine 6s linear infinite;
}
.head__sub {
  color: var(--muted);
  font-size: 14px;
  font-weight: 300;
  margin-top: 12px;
}

/* ============ 内容卡片 ============ */
.card {
  padding: 26px 30px;
  border-radius: 20px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.015) 50%, rgba(255, 255, 255, 0.045)),
    rgba(10, 12, 26, 0.55);
  backdrop-filter: blur(22px) saturate(140%);
  -webkit-backdrop-filter: blur(22px) saturate(140%);
  border: 1px solid var(--stroke);
  animation: rise 0.55s var(--ease) both;
}
.card--tip {
  border-color: rgba(34, 211, 238, 0.35);
  background:
    linear-gradient(160deg, rgba(34, 211, 238, 0.08), rgba(255, 255, 255, 0.015) 50%, rgba(255, 255, 255, 0.03)),
    rgba(10, 12, 26, 0.55);
}
.card__title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
  margin-bottom: 14px;
}
.card__text {
  font-size: 14px;
  line-height: 1.9;
  color: rgba(238, 242, 255, 0.78);
  font-weight: 300;
}
.card__text + .card__text {
  margin-top: 10px;
}
.card__text code {
  font-family: inherit;
  font-size: 13px;
  color: var(--a2);
  background: rgba(34, 211, 238, 0.12);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 7px;
  padding: 1px 8px;
}

/* 技术栈 chips */
.stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.stack__chip {
  font-size: 12.5px;
  color: var(--ink);
  background: var(--glass);
  border: 1px solid var(--stroke);
  border-radius: 999px;
  padding: 5px 14px;
  font-weight: 400;
}

/* ============ 特性网格 ============ */
.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.feature {
  padding: 22px 24px;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015)),
    rgba(10, 12, 26, 0.5);
  border: 1px solid var(--stroke);
  transition: transform 0.3s var(--ease), border-color 0.3s;
}
.feature:hover {
  transform: translateY(-3px);
  border-color: rgba(124, 92, 255, 0.4);
}
.feature__ico {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  margin-bottom: 14px;
  color: #fff;
}
.feature__ico svg {
  width: 19px;
  height: 19px;
}
.feature__ico--a { background: linear-gradient(135deg, var(--a1), #5b8bff); box-shadow: 0 10px 22px -10px var(--a1); }
.feature__ico--b { background: linear-gradient(135deg, #5b8bff, var(--a2)); box-shadow: 0 10px 22px -10px var(--a2); }
.feature__ico--c { background: linear-gradient(135deg, var(--a3), #ff8fab); box-shadow: 0 10px 22px -10px var(--a3); }
.feature__ico--d { background: linear-gradient(135deg, var(--a2), var(--a3)); box-shadow: 0 10px 22px -10px var(--a3); }
.feature__ico--e { background: linear-gradient(135deg, #5b8bff, var(--a3)); box-shadow: 0 10px 22px -10px #5b8bff; }
.feature__title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
.feature__text {
  font-size: 13px;
  line-height: 1.75;
  color: var(--muted);
  font-weight: 300;
}

/* 页脚 */
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

@media (max-width: 640px) {
  .page {
    padding: 14px 14px 40px;
    gap: 14px;
  }
  .grid {
    grid-template-columns: 1fr;
  }
  .card {
    padding: 20px 20px;
  }
}
</style>
