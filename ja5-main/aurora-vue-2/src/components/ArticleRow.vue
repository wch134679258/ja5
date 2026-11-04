<template>
  <!-- 内容条目：编号 + 标题 + 摘要 + 元信息。首页「全部内容」用 -->
  <article
    class="row"
    :class="{ 'row--fav': favorited }"
    tabindex="0"
    role="button"
    :aria-label="'阅读文章：' + article.title"
    @click="$emit('open', article.id)"
    @keydown.enter="$emit('open', article.id)"
  >
    <span class="row__no" aria-hidden="true">{{ indexLabel }}</span>

    <div class="row__body">
      <h3 class="row__title">{{ article.title }}</h3>
      <p v-if="article.summary" class="row__sum">{{ article.summary }}</p>
      <div class="row__meta">
        <button
          v-if="article.category"
          class="row__cat"
          type="button"
          @click.stop="$emit('open-category', article.categoryId, article.category)"
        >{{ article.category }}</button>
        <span class="row__dim">{{ shortDate(article.createdAt) }}</span>
        <template v-if="article.minutes">
          <i class="row__dot"></i>
          <span class="row__dim">约 {{ article.minutes }} 分钟</span>
        </template>
        <template v-if="article.tags && article.tags.length">
          <i class="row__dot"></i>
          <button
            v-for="t in article.tags.slice(0, 2)"
            :key="t"
            class="row__tag"
            type="button"
            @click.stop="$emit('open-tag', t)"
          >{{ t }}</button>
        </template>
      </div>
    </div>

    <div class="row__side">
      <span class="row__views" :title="article.views + ' 次浏览'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
          <circle cx="12" cy="12" r="2.6" />
        </svg>
        {{ article.views }}
      </span>

      <button
        v-if="showFav"
        class="row__fav"
        :class="{ 'row__fav--on': favorited }"
        type="button"
        :title="favorited ? '取消收藏' : '收藏'"
        :aria-label="favorited ? '取消收藏' : '收藏'"
        @click.stop="$emit('toggle-fav', article)"
      >
        <svg viewBox="0 0 24 24" :fill="favorited ? 'currentColor' : 'none'" stroke="currentColor"
             stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8L3.5 9.7l5.9-.8z" />
        </svg>
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  article: { type: Object, required: true },
  /** 序号：从 1 开始，渲染成 01 / 02 */
  index: { type: Number, default: 1 },
  favorited: { type: Boolean, default: false },
  showFav: { type: Boolean, default: false }
})
defineEmits(['open', 'open-category', 'open-tag', 'toggle-fav'])

const indexLabel = computed(() => String(props.index).padStart(2, '0'))

/** createdAt 形如 2026-10-22 00:00:00，列表里只需要日期部分 */
function shortDate (v) {
  const s = String(v || '')
  return s.length >= 10 ? s.slice(0, 10) : s
}
</script>

<style scoped>
.row {
  position: relative;
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  align-items: start;
  gap: 18px;
  padding: 20px 14px 20px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.3s, transform 0.3s var(--ease);
}
/* 左侧高亮条：悬停/聚焦时从中间长出来 */
.row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 2px;
  height: 0;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--a2), var(--a1));
  transform: translateY(-50%);
  transition: height 0.32s var(--ease);
}
.row:hover,
.row:focus-visible {
  background: rgba(255, 255, 255, 0.038);
  outline: none;
}
.row:hover::before,
.row:focus-visible::before {
  height: 56%;
}

/* 编号 */
.row__no {
  font-size: 12.5px;
  font-weight: 500;
  letter-spacing: 0.06em;
  font-variant-numeric: tabular-nums;
  color: rgba(141, 151, 189, 0.55);
  padding-top: 3px;
  transition: color 0.3s;
}
.row:hover .row__no {
  color: var(--a2);
}

/* 正文 */
.row__body {
  min-width: 0;
}
.row__title {
  font-size: 16.5px;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: -0.005em;
  color: var(--ink);
  transition: color 0.28s;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.row:hover .row__title {
  color: #c9ccff;
}
.row__sum {
  margin-top: 5px;
  font-size: 13px;
  font-weight: 300;
  line-height: 1.6;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 9px;
  font-size: 11.5px;
  color: var(--muted);
}
.row__cat {
  border: 1px solid rgba(124, 92, 255, 0.32);
  background: rgba(124, 92, 255, 0.1);
  color: #b9b0ff;
  font: inherit;
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s, color 0.25s;
}
.row__cat:hover {
  background: rgba(124, 92, 255, 0.24);
  border-color: rgba(124, 92, 255, 0.6);
  color: #ddd8ff;
}
.row__dim {
  font-weight: 300;
  letter-spacing: 0.02em;
}
.row__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(141, 151, 189, 0.45);
  flex: none;
}
.row__tag {
  border: 0;
  background: none;
  padding: 0;
  font: inherit;
  font-size: 11.5px;
  color: rgba(141, 151, 189, 0.9);
  cursor: pointer;
  transition: color 0.25s;
}
.row__tag::before {
  content: "#";
  opacity: 0.55;
  margin-right: 1px;
}
.row__tag:hover {
  color: var(--a2);
}

/* 右侧：浏览数 + 收藏 */
.row__side {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 1px;
  flex: none;
}
.row__views {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  color: rgba(141, 151, 189, 0.75);
  white-space: nowrap;
}
.row__views svg {
  width: 13px;
  height: 13px;
  opacity: 0.6;
}
.row__fav {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  border: 1px solid transparent;
  background: none;
  color: rgba(141, 151, 189, 0.6);
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.row__fav svg {
  width: 15px;
  height: 15px;
}
.row__fav:hover {
  color: #ffd76e;
  background: rgba(255, 215, 110, 0.1);
  border-color: rgba(255, 215, 110, 0.3);
  transform: translateY(-1px);
}
.row__fav--on {
  color: #ffd76e;
}
.row--fav .row__title {
  color: #f4e7c6;
}

@media (max-width: 720px) {
  .row {
    grid-template-columns: 30px minmax(0, 1fr) auto;
    gap: 11px;
    padding: 15px 8px 15px 4px;
  }
  .row__title {
    font-size: 15.5px;
  }
  .row__sum {
    display: none;
  }
  .row__meta {
    margin-top: 7px;
    font-size: 11px;
    gap: 5px;
  }
  .row__no {
    font-size: 11px;
    padding-top: 4px;
  }
  .row__views {
    font-size: 11px;
  }
  .row__views svg {
    width: 12px;
    height: 12px;
  }
  .row__fav {
    width: 28px;
    height: 28px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .row,
  .row::before {
    transition: none;
  }
}
</style>
