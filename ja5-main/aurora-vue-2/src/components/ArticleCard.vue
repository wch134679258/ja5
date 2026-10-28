<template>
  <!-- 文章卡片：首页 / 收藏页 / 发现页 共用。整卡可点击进入详情 -->
  <article
    class="rcard"
    :class="{ 'rcard--fav': favorited, 'rcard--feat': featured }"
    tabindex="0"
    role="button"
    :aria-label="'阅读文章：' + article.title"
    @click="$emit('open', article.id)"
    @keydown.enter="$emit('open', article.id)"
  >
    <!-- 精选卡片的封面：用星空图按文章 id 轮转，纯装饰不承载信息 -->
    <div
      v-if="featured"
      class="rcard__cover"
      :style="{ backgroundImage: 'url(' + cover + ')' }"
      aria-hidden="true"
    >
      <span class="rcard__cover-veil"></span>
      <span class="rcard__pick">精选</span>
      <button
        v-if="article.category"
        class="rcard__cat rcard__cat--cover"
        type="button"
        @click.stop="$emit('open-category', article.categoryId, article.category)"
      >{{ article.category }}</button>
    </div>

    <div class="rcard__main">
      <h3 class="rcard__title">{{ article.title }}</h3>
      <p class="rcard__summary">{{ article.summary }}</p>
      <div class="rcard__foot">
        <div class="rcard__info">
          <button
            v-if="article.category && !featured"
            class="rcard__cat"
            type="button"
            @click.stop="$emit('open-category', article.categoryId, article.category)"
          >{{ article.category }}</button>
          <span v-if="article.category && !featured" class="rcard__dot"></span>
          <span>{{ article.author }}</span>
          <span class="rcard__dot"></span>
          <span>{{ article.createdAt }}</span>
          <span class="rcard__dot"></span>
          <span>{{ article.views }} 次浏览</span>
          <template v-if="article.minutes">
            <span class="rcard__dot"></span>
            <span>约 {{ article.minutes }} 分钟</span>
          </template>
        </div>
        <div class="rcard__tags">
          <button
            v-for="t in (article.tags || [])"
            :key="t"
            class="rtag"
            type="button"
            @click.stop="$emit('open-tag', t)"
          >{{ t }}</button>
        </div>
      </div>
    </div>

    <!-- 收藏按钮（登录后才显示；点击不进入详情） -->
    <button
      v-if="showFav"
      class="rcard__fav"
      :class="{ 'rcard__fav--on': favorited }"
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
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  article: { type: Object, required: true },
  /** 当前用户是否已收藏该文章 */
  favorited: { type: Boolean, default: false },
  /** 是否显示收藏按钮（收藏页自身就是收藏列表，不显示） */
  showFav: { type: Boolean, default: false },
  /** 精选样式：首页推荐位第一条用带封面的大卡片 */
  featured: { type: Boolean, default: false }
})
defineEmits(['open', 'open-category', 'open-tag', 'toggle-fav'])

/** 封面按文章 id 在 3 张星空图里轮转，同一篇文章每次进来都是同一张 */
const COVERS = ['/covers/1.jpg', '/covers/2.jpg', '/covers/3.jpg']
const cover = computed(() => COVERS[(Number(props.article.id) || 0) % COVERS.length])
</script>

<style scoped>
/* ============ 结果卡片 ============ */
.rcard {
  position: relative;
  padding: 22px 24px 18px;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.015) 50%, rgba(255, 255, 255, 0.04)),
    rgba(10, 12, 26, 0.55);
  backdrop-filter: blur(22px) saturate(140%);
  -webkit-backdrop-filter: blur(22px) saturate(140%);
  border: 1px solid var(--stroke);
  cursor: pointer;
  transition: transform 0.3s var(--ease), border-color 0.3s, background 0.3s, box-shadow 0.3s;
}
.rcard:hover {
  transform: translateY(-3px);
  border-color: rgba(124, 92, 255, 0.45);
  box-shadow: 0 24px 50px -30px rgba(124, 92, 255, 0.55);
}
.rcard:focus-visible {
  outline: 2px solid var(--a1);
  outline-offset: 2px;
}
.rcard--fav {
  border-color: rgba(255, 215, 110, 0.3);
}

/* ---------- 精选卡片 ---------- */
.rcard--feat {
  padding: 0;
  overflow: hidden;
}
.rcard--feat:hover {
  transform: translateY(-4px);
  box-shadow: 0 34px 60px -34px rgba(124, 92, 255, 0.75);
}
.rcard__cover {
  position: relative;
  height: 172px;
  background-color: #0b0e1c;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 16px;
}
/* 底部压一层深色，封面图和下方文字才衔接得上 */
.rcard__cover-veil {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(5, 6, 15, 0.15) 0%, rgba(5, 6, 15, 0.05) 45%, rgba(8, 10, 22, 0.92) 100%);
}
.rcard__pick {
  position: relative;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.18em;
  padding: 3px 11px;
  border-radius: 999px;
  color: #1a1030;
  background: linear-gradient(115deg, #ffe08a, #ffb347);
  box-shadow: 0 6px 18px -8px rgba(255, 179, 71, 0.9);
}
.rcard__cat--cover {
  position: relative;
  background: rgba(6, 10, 22, 0.55);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-color: rgba(34, 211, 238, 0.5);
}
.rcard--feat .rcard__main {
  padding: 18px 22px 17px;
}
.rcard--feat .rcard__title {
  font-size: 20px;
  line-height: 1.42;
  letter-spacing: -0.015em;
  padding-right: 44px;
}
.rcard--feat .rcard__summary {
  -webkit-line-clamp: 3;
  font-size: 13.8px;
}
/* 精选卡的收藏按钮压在封面上，换成半透明深底才看得清 */
.rcard--feat .rcard__fav {
  top: 12px;
  right: 12px;
  background: rgba(6, 10, 22, 0.55);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-color: rgba(255, 255, 255, 0.22);
}

.rcard__title {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: -0.01em;
  padding-right: 40px;
  transition: color 0.25s;
}
.rcard:hover .rcard__title {
  color: #fff;
}
.rcard__summary {
  margin-top: 9px;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--muted);
  font-weight: 300;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rcard__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}
.rcard__info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--muted);
  font-weight: 300;
}
.rcard__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--muted);
  opacity: 0.6;
  flex: none;
}
.rcard__cat {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--a2);
  background: rgba(34, 211, 238, 0.1);
  border: 1px solid rgba(34, 211, 238, 0.35);
  border-radius: 999px;
  padding: 2px 10px;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.25s, border-color 0.25s, background 0.25s;
}
.rcard__cat:hover {
  color: #fff;
  background: rgba(34, 211, 238, 0.22);
  border-color: rgba(34, 211, 238, 0.7);
}
.rcard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.rtag {
  border: 1px solid var(--stroke);
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 11.5px;
  padding: 3px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.rtag:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

/* 卡片右上角收藏按钮 */
.rcard__fav {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid var(--stroke);
  border-radius: 10px;
  cursor: pointer;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.04);
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.rcard__fav svg {
  width: 15px;
  height: 15px;
}
.rcard__fav:hover {
  color: #ffd76e;
  border-color: rgba(255, 215, 110, 0.45);
  transform: translateY(-1px);
}
.rcard__fav--on {
  color: #ffd76e;
  background: rgba(255, 215, 110, 0.12);
  border-color: rgba(255, 215, 110, 0.5);
}

@media (max-width: 640px) {
  .rcard {
    padding: 18px 18px 16px;
  }
  .rcard__foot {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .rcard--feat {
    padding: 0;
  }
  .rcard--feat .rcard__cover {
    height: 138px;
  }
  .rcard--feat .rcard__main {
    padding: 15px 17px 15px;
  }
  .rcard--feat .rcard__title {
    font-size: 17.5px;
  }
}
</style>
