<template>
  <!-- ============ 骨架屏：加载中占位，避免页面「白一下」 ============ -->
  <div v-if="variant === 'skeleton'" class="sk" :class="'sk--' + shape" aria-busy="true">
    <template v-if="shape === 'card'">
      <div class="sk__line sk__line--title"></div>
      <div class="sk__line"></div>
      <div class="sk__line sk__line--short"></div>
      <div class="sk__foot">
        <span class="sk__chip"></span>
        <span class="sk__chip sk__chip--sm"></span>
      </div>
    </template>
    <template v-else-if="shape === 'paper'">
      <div class="sk__line sk__line--title"></div>
      <div class="sk__line"></div>
      <div class="sk__line"></div>
      <div class="sk__line"></div>
      <div class="sk__line sk__line--short"></div>
      <div class="sk__line"></div>
      <div class="sk__line sk__line--short"></div>
    </template>
    <template v-else>
      <div class="sk__line"></div>
      <div class="sk__line sk__line--short"></div>
    </template>
  </div>

  <!-- ============ 空状态：有图标、有说明、有行动按钮 ============ -->
  <div v-else-if="variant === 'empty'" class="state">
    <div class="state__icon" aria-hidden="true">{{ icon }}</div>
    <p class="state__title">{{ title }}</p>
    <p v-if="desc" class="state__desc">{{ desc }}</p>
    <button v-if="actionText" class="state__btn" type="button" @click="$emit('action')">
      {{ actionText }}
    </button>
  </div>

  <!-- ============ 错误状态：说明 + 重试 ============ -->
  <div v-else class="state state--error">
    <div class="state__icon" aria-hidden="true">!</div>
    <p class="state__title">{{ title || '加载失败' }}</p>
    <p v-if="desc" class="state__desc">{{ desc }}</p>
    <button class="state__btn" type="button" @click="$emit('action')">重新加载</button>
  </div>
</template>

<script setup>
/**
 * 三合一状态组件：骨架屏 / 空状态 / 错误重试。
 *
 * variant: skeleton（加载中占位，shape 可选 card / paper / line）
 *          empty（空状态，可配 icon / title / desc / actionText）
 *          error（加载失败，内置「重新加载」按钮）
 * 三个分支都以 @action 回调向外抛出交互事件。
 */
defineProps({
  variant: { type: String, default: 'skeleton' }, // skeleton | empty | error
  shape: { type: String, default: 'card' },       // skeleton 专用：card | paper | line
  icon: { type: String, default: '☆' },
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  actionText: { type: String, default: '' }
})
defineEmits(['action'])
</script>

<style scoped>
/* ---------- 骨架屏 ---------- */
.sk {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--stroke, rgba(255, 255, 255, .11));
  background: var(--glass, rgba(255, 255, 255, .055));
  display: grid;
  gap: 10px;
}

.sk--paper { gap: 14px; padding: 22px; }

.sk__line {
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, .07) 25%,
    rgba(255, 255, 255, .16) 37%,
    rgba(255, 255, 255, .07) 63%
  );
  background-size: 400% 100%;
  animation: skShine 1.4s ease infinite;
}

.sk__line--title { height: 18px; width: 62%; }
.sk__line--short { width: 42%; }

.sk__foot { display: flex; gap: 8px; margin-top: 4px; }
.sk__chip {
  height: 20px; width: 64px; border-radius: 999px;
  background: rgba(255, 255, 255, .08);
  animation: skShine 1.4s ease infinite;
}
.sk__chip--sm { width: 40px; }

@keyframes skShine {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* ---------- 空 / 错误状态 ---------- */
.state {
  padding: 40px 24px;
  border-radius: 20px;
  border: 1px dashed var(--stroke, rgba(255, 255, 255, .14));
  background: var(--glass, rgba(255, 255, 255, .04));
  text-align: center;
  display: grid;
  justify-items: center;
  gap: 10px;
}

.state__icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 20px;
  color: var(--ink, #eef2ff);
  background: rgba(124, 92, 255, .18);
  border: 1px solid rgba(124, 92, 255, .35);
}

.state--error .state__icon {
  background: rgba(255, 107, 139, .16);
  border-color: rgba(255, 107, 139, .4);
  font-weight: 700;
}

.state__title { font-size: 16px; font-weight: 600; }
.state__desc { font-size: 13px; color: var(--muted, #8d97bd); max-width: 42ch; line-height: 1.6; }

.state__btn {
  margin-top: 4px;
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid rgba(124, 92, 255, .5);
  background: rgba(124, 92, 255, .18);
  color: var(--ink, #eef2ff);
  font-size: 13px;
  cursor: pointer;
  transition: transform .2s var(--ease, ease), background .2s var(--ease, ease);
}
.state__btn:hover { background: rgba(124, 92, 255, .32); transform: translateY(-1px); }

@media (prefers-reduced-motion: reduce) {
  .sk__line, .sk__chip { animation: none; }
}
</style>
