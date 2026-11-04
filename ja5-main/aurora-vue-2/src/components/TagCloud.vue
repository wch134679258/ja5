<template>
  <!-- 标签云：按文章数分四档字号，点一下即按该标签筛选 -->
  <div class="cloud">
    <button
      v-for="t in items"
      :key="t.id"
      class="cloud__tag"
      :class="[
        'cloud__tag--s' + t.size,
        { 'cloud__tag--on': activeId === t.id }
      ]"
      type="button"
      :title="tagTitle(t)"
      @click="$emit('select', t)"
    >{{ t.name }}<sup v-if="showCount">{{ t.articleCount }}</sup></button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** [{ id, name, articleCount, categoryName }] */
  tags: { type: Array, default: () => [] },
  activeId: { type: [Number, String], default: null },
  showCount: { type: Boolean, default: false },
  /** 最多展示多少个（按文章数倒序取） */
  limit: { type: Number, default: 22 }
})
defineEmits(['select'])

/**
 * 字号分档：以当前标签集的最大文章数为基准分四档。
 * 直接用绝对数值不靠谱——数据少时全都会挤进同一档，
 * 用相对比例才能保证「大标签大字号」的层次始终存在。
 */
const items = computed(() => {
  const list = (props.tags || []).slice().sort((a, b) => (b.articleCount || 0) - (a.articleCount || 0))
  const max = list.reduce((m, t) => Math.max(m, t.articleCount || 0), 0) || 1
  return list.slice(0, props.limit).map((t) => {
    const r = (t.articleCount || 0) / max
    const size = r >= 0.8 ? 4 : r >= 0.55 ? 3 : r >= 0.3 ? 2 : 1
    return { ...t, size }
  })
})

function tagTitle (t) {
  const parts = []
  if (t.categoryName) parts.push(t.categoryName)
  parts.push(`${t.articleCount || 0} 篇文章`)
  return parts.join(' · ')
}
</script>

<style scoped>
.cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 6px;
}
.cloud__tag {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  border: 1px solid transparent;
  background: none;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  font: inherit;
  font-weight: 400;
  line-height: 1.3;
  color: var(--muted);
  transition: color 0.25s, background 0.25s, border-color 0.25s, transform 0.25s var(--ease);
}
.cloud__tag::before {
  content: "#";
  opacity: 0.42;
  margin-right: 1px;
  font-size: 0.86em;
}
.cloud__tag sup {
  font-size: 9px;
  opacity: 0.6;
  transform: translateY(-1px);
}
.cloud__tag:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.055);
  border-color: var(--stroke);
}
.cloud__tag--on {
  color: #fff;
  background: rgba(124, 92, 255, 0.3);
  border-color: rgba(124, 92, 255, 0.6);
}

/* 四档字号 + 透明度：越大越亮，形成视觉重心 */
.cloud__tag--s1 {
  font-size: 12px;
  color: rgba(141, 151, 189, 0.72);
}
.cloud__tag--s2 {
  font-size: 13px;
  color: rgba(160, 170, 205, 0.85);
}
.cloud__tag--s3 {
  font-size: 14.5px;
  color: #c3cbe8;
}
.cloud__tag--s4 {
  font-size: 16px;
  font-weight: 500;
  color: #e6ebff;
}
.cloud__tag--s3::before,
.cloud__tag--s4::before {
  opacity: 0.55;
  color: var(--a2);
}

@media (prefers-reduced-motion: reduce) {
  .cloud__tag {
    transition: none;
  }
}
</style>
