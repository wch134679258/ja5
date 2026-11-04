<template>
  <!-- 精选卡：封面在上，标题/摘要/元信息在下。首页「编辑精选」三列与右上头图共用 -->
  <article
    class="pick"
    :class="{ 'pick--lead': lead, 'pick--fav': favorited }"
    tabindex="0"
    role="button"
    :aria-label="'阅读文章：' + article.title"
    @click="$emit('open', article.id)"
    @keydown.enter="$emit('open', article.id)"
  >
    <div class="pick__cover" :style="{ backgroundImage: 'url(' + cover + ')' }" aria-hidden="true">
      <span class="pick__veil"></span>
      <span v-if="lead" class="pick__badge">编辑精选</span>
      <button
        v-if="article.category && lead"
        class="pick__cat"
        type="button"
        @click.stop="$emit('open-category', article.categoryId, article.category)"
      >{{ article.category }}</button>
    </div>

    <button
      v-if="showFav"
      class="pick__fav"
      :class="{ 'pick__fav--on': favorited }"
      type="button"
      :title="favorited ? '取消收藏' : '收藏'"
      :aria-label="favorited ? '取消收藏' : '收藏'"
      @click.stop="$emit('toggle-fav', article)"
    >
      <svg viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="1.5"
           stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8L3.5 9.7l5.9-.8z" />
      </svg>
    </button>

    <div class="pick__body">
      <h3 class="pick__title">{{ article.title }}</h3>
      <p class="pick__summary">{{ article.summary }}</p>
      <div class="pick__meta">
        <span v-if="!lead && article.tags && article.tags.length" class="pick__tag">{{ article.tags[0] }}</span>
        <span v-if="lead" class="pick__dim">{{ article.author }}</span>
        <i v-if="lead && article.author" class="pick__dot"></i>
        <span v-if="article.minutes" class="pick__dim">约 {{ article.minutes }} 分钟</span>
        <i v-if="article.minutes" class="pick__dot"></i>
        <span class="pick__dim">{{ article.views }} 次浏览</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { coverFor } from '../utils/cover'

const props = defineProps({
  article: { type: Object, required: true },
  /** 头条样式：加大标题、显示「编辑精选」角标与分类 */
  lead: { type: Boolean, default: false },
  favorited: { type: Boolean, default: false },
  showFav: { type: Boolean, default: false },
  /** 指定封面图；首屏按版位取图时传入，避免多个位置撞同一张素材 */
  coverSrc: { type: String, default: '' }
})
defineEmits(['open', 'open-category', 'toggle-fav'])

const cover = computed(() => props.coverSrc || coverFor(props.article.id))
</script>

<style scoped>
.pick {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid var(--stroke);
  cursor: pointer;
  transition: transform 0.35s var(--ease), border-color 0.35s, background 0.35s, box-shadow 0.35s;
}
.pick:hover {
  transform: translateY(-4px);
  border-color: rgba(124, 92, 255, 0.45);
  background: rgba(124, 92, 255, 0.07);
  box-shadow: 0 26px 54px -32px rgba(124, 92, 255, 0.6);
}
.pick:focus-visible {
  outline: 2px solid var(--a1);
  outline-offset: 2px;
}
.pick--fav {
  border-color: rgba(255, 215, 110, 0.3);
}

/* ---------- 封面 ---------- */
.pick__cover {
  position: relative;
  height: 116px;
  background-color: #0b0e1c;
  background-size: cover;
  background-position: center;
}
.pick__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(5, 6, 15, 0.05) 30%, rgba(8, 10, 22, 0.85) 100%);
}
.pick__badge {
  position: absolute;
  top: 12px;
  left: 12px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.14em;
  padding: 3px 10px;
  border-radius: 999px;
  color: #1a1030;
  background: linear-gradient(115deg, #ffe08a, #ffb347);
  box-shadow: 0 6px 18px -8px rgba(255, 179, 71, 0.9);
}
.pick__cat {
  position: absolute;
  bottom: 12px;
  left: 12px;
  font: inherit;
  font-size: 11px;
  color: var(--ink);
  padding: 3px 10px;
  border-radius: 999px;
  cursor: pointer;
  background: rgba(6, 10, 22, 0.6);
  border: 1px solid rgba(34, 211, 238, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

/* ---------- 收藏按钮 ---------- */
.pick__fav {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 27px;
  height: 27px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.55);
  background: rgba(6, 10, 22, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: color 0.2s, border-color 0.2s, transform 0.2s;
}
.pick__fav svg {
  width: 13px;
  height: 13px;
}
.pick__fav:hover {
  color: #ffe08a;
  border-color: rgba(255, 215, 110, 0.6);
  transform: scale(1.08);
}
.pick__fav--on {
  color: #ffd76e;
  border-color: rgba(255, 215, 110, 0.65);
}

/* ---------- 文案 ---------- */
.pick__body {
  padding: 12px 13px 13px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.pick__title {
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: -0.005em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pick__summary {
  font-size: 11.5px;
  line-height: 1.65;
  color: var(--muted);
  margin-top: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pick__meta {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: auto;
  padding-top: 10px;
  font-size: 11px;
  color: var(--muted);
}
.pick__tag {
  font-size: 10.5px;
  color: #bdb0ff;
  background: rgba(124, 92, 255, 0.16);
  border-radius: 5px;
  padding: 1px 6px;
}
.pick__dim {
  color: var(--muted);
}
.pick__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--muted);
  opacity: 0.55;
  display: inline-block;
}

/* ---------- 头条（lean 变体） ---------- */
.pick--lead .pick__cover {
  height: 214px;
}
.pick--lead .pick__body {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16px 18px 17px;
  z-index: 2;
}
.pick--lead .pick__title {
  font-size: 19px;
  margin-top: 2px;
  line-height: 1.45;
  -webkit-line-clamp: 2;
  padding-right: 34px;
}
.pick--lead .pick__summary {
  display: none;
}
.pick--lead {
  border-radius: 20px;
  box-shadow: 0 34px 70px -34px rgba(0, 0, 0, 0.95);
}
.pick--lead:hover {
  box-shadow: 0 44px 84px -34px rgba(124, 92, 255, 0.55);
}
/* 头条的收藏按钮要避开标题，放到右上角靠内 */
.pick--lead .pick__fav {
  top: 12px;
  right: 12px;
}
/* 头条封面底部需要更深的压暗，文字才压得住 */
.pick--lead .pick__veil {
  background: linear-gradient(180deg, rgba(5, 6, 15, 0) 30%, rgba(5, 6, 15, 0.5) 62%, rgba(5, 6, 15, 0.95) 100%);
}

@media (max-width: 700px) {
  .pick__cover {
    height: 152px;
  }
  .pick--lead .pick__cover {
    height: 190px;
  }
  .pick--lead .pick__title {
    font-size: 17px;
  }
}
</style>
