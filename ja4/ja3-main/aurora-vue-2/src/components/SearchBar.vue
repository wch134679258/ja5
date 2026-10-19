<template>
  <div class="search" :class="{ 'is-focus': focused, 'is-empty': !modelValue }">
    <!-- 外发光描边层：聚焦时点亮，延用登录卡片的旋转极光边 -->
    <div class="search__glow"></div>

    <div class="search__inner">
      <svg class="search__ico" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-3.6-3.6" />
      </svg>

      <input
        ref="inputEl"
        class="search__input"
        type="search"
        :value="modelValue"
        :placeholder="placeholder"
        autocomplete="off"
        spellcheck="false"
        role="combobox"
        :aria-expanded="open ? 'true' : 'false'"
        aria-controls="search-suggest"
        @input="onInput"
        @focus="onFocus"
        @blur="onBlur"
        @keydown="onKeydown"
      />

      <!-- 清空按钮：有内容时出现 -->
      <button
        v-show="modelValue"
        class="search__clear"
        type="button"
        title="清空"
        aria-label="清空搜索内容"
        @click="clear"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
             stroke-linecap="round">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>

      <!-- 快捷键提示：输入框为空且未聚焦时展示 -->
      <span v-show="!focused && !modelValue" class="search__kbd">
        <kbd>/</kbd>
      </span>

      <button class="search__btn" type="button" @click="submit">
        <span class="search__btn-txt">搜索</span>
      </button>
    </div>

    <!-- ============ 建议 / 历史 下拉 ============ -->
    <transition name="drop">
      <div v-show="open && (items.length || history.length)" id="search-suggest" class="drop" role="listbox">
        <!-- 后端搜索建议（有关键词时） -->
        <template v-if="modelValue && items.length">
          <button
            v-for="(item, i) in items"
            :key="item.type + '-' + item.id"
            class="drop__row"
            :class="{ 'drop__row--on': i === activeIndex }"
            type="button"
            role="option"
            :aria-selected="i === activeIndex"
            @mousedown.prevent="choose(item)"
            @mouseenter="activeIndex = i"
          >
            <span class="drop__ico" :class="'drop__ico--' + item.type">
              <!-- 标题：文档图标 -->
              <svg v-if="item.type === 'title'" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <path d="M14 2v6h6M9 13h6M9 17h6" />
              </svg>
              <!-- 标签：标签图标 -->
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2H2v10l9.3 9.3a1 1 0 0 0 1.4 0l8.6-8.6a1 1 0 0 0 0-1.4z" />
                <circle cx="7" cy="7" r="1.4" fill="currentColor" stroke="none" />
              </svg>
            </span>
            <span class="drop__text" v-html="highlight(item.text)" />
            <span class="drop__type">{{ item.type === 'title' ? '文章' : '标签' }}</span>
          </button>
        </template>

        <!-- 搜索历史（输入框为空时） -->
        <template v-else-if="!modelValue && history.length">
          <div class="drop__head">
            <span>搜索历史</span>
            <button class="drop__clear" type="button" @mousedown.prevent="clearHistory">清除</button>
          </div>
          <div class="drop__history">
            <button
              v-for="(h, i) in history"
              :key="h + '-' + i"
              class="drop__hchip"
              type="button"
              @mousedown.prevent="chooseText(h)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                   stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="9" />
                <path d="M12 8v4l3 2" />
              </svg>
              {{ h }}
            </button>
          </div>
        </template>

        <!-- 底部快捷键提示 -->
        <div class="drop__foot">
          <span><kbd>↑</kbd><kbd>↓</kbd> 选择</span>
          <span><kbd>Enter</kbd> 搜索</span>
          <span><kbd>Esc</kbd> 关闭</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { fetchSuggest } from '../api/search'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '搜索…' }
})

const emit = defineEmits(['update:modelValue', 'search', 'clear', 'pick'])

const HISTORY_KEY = 'aurora_search_history'
const HISTORY_MAX = 8
const DEBOUNCE_MS = 250

const inputEl = ref(null)
const focused = ref(false)
const open = ref(false)
const items = ref([])        // 后端建议
const history = ref([])      // 本地搜索历史
const activeIndex = ref(-1)  // 键盘高亮下标
let debounceTimer = null

/* ---------- 搜索历史（localStorage） ---------- */
function loadHistory () {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    const arr = raw ? JSON.parse(raw) : []
    history.value = Array.isArray(arr) ? arr.filter((x) => typeof x === 'string').slice(0, HISTORY_MAX) : []
  } catch (_) {
    history.value = []
  }
}
function pushHistory (kw) {
  const k = (kw || '').trim()
  if (!k) return
  const next = [k, ...history.value.filter((h) => h !== k)].slice(0, HISTORY_MAX)
  history.value = next
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(next))
  } catch (_) { /* 隐私模式等场景下写入失败可忽略 */ }
}
function clearHistory () {
  history.value = []
  try {
    localStorage.removeItem(HISTORY_KEY)
  } catch (_) { /* 忽略 */ }
}

/* ---------- 建议拉取（防抖） ---------- */
async function pullSuggest (kw) {
  try {
    const res = await fetchSuggest(kw)
    // 防抖期间用户可能又改了输入，仅接受最新一轮结果
    if (inputEl.value && inputEl.value.value === kw) {
      items.value = res || []
      activeIndex.value = -1
    }
  } catch (_) {
    items.value = []
  }
}

/* ---------- 交互 ---------- */
function onInput (e) {
  const v = e.target.value
  emit('update:modelValue', v)
  clearTimeout(debounceTimer)
  const kw = v.trim()
  if (!kw) {
    items.value = []
    open.value = true
    return
  }
  open.value = true
  debounceTimer = setTimeout(() => pullSuggest(kw), DEBOUNCE_MS)
}

function onFocus () {
  focused.value = true
  open.value = true
}
function onBlur () {
  focused.value = false
  // 延迟关闭，给 mousedown 预留点击机会
  setTimeout(() => { open.value = false }, 160)
}

function onKeydown (e) {
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    if (!items.value.length) return
    e.preventDefault()
    open.value = true
    const dir = e.key === 'ArrowDown' ? 1 : -1
    const n = items.value.length
    activeIndex.value = (activeIndex.value + dir + n) % n
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (activeIndex.value >= 0 && items.value[activeIndex.value]) {
      choose(items.value[activeIndex.value])
    } else {
      submit()
    }
  } else if (e.key === 'Escape') {
    if (open.value) {
      open.value = false
    } else if (props.modelValue) {
      clear()
    } else if (inputEl.value) {
      inputEl.value.blur()
    }
  }
}

/** 选中一条建议：标签 -> 通知父级按标签筛选；文章标题 -> 按关键词搜索 */
function choose (item) {
  const text = item.text || ''
  emit('update:modelValue', text)
  pushHistory(text)
  open.value = false
  items.value = []
  emit('pick', { type: item.type, id: item.id, text })
}

/** 选中一条历史记录 */
function chooseText (text) {
  emit('update:modelValue', text)
  pushHistory(text)
  open.value = false
  emit('search', text)
}

function submit () {
  const kw = (props.modelValue || '').trim()
  pushHistory(kw)
  open.value = false
  items.value = []
  emit('search', kw)
}

function clear () {
  emit('update:modelValue', '')
  emit('clear')
  items.value = []
  open.value = true
  nextTick(() => inputEl.value && inputEl.value.focus())
}

/** 关键词高亮：把命中的部分包一层 <em> */
function highlight (text) {
  const kw = (props.modelValue || '').trim()
  if (!kw) return escapeHtml(text)
  const idx = text.toLowerCase().indexOf(kw.toLowerCase())
  if (idx < 0) return escapeHtml(text)
  return escapeHtml(text.slice(0, idx)) +
    '<em>' + escapeHtml(text.slice(idx, idx + kw.length)) + '</em>' +
    escapeHtml(text.slice(idx + kw.length))
}
function escapeHtml (s) {
  return String(s).replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ))
}

// 按 "/" 快捷聚焦搜索框（与输入框内操作不冲突）
function onKeydownGlobal (e) {
  if (e.key !== '/') return
  const tag = (e.target && e.target.tagName) || ''
  if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target && e.target.isContentEditable)) return
  e.preventDefault()
  if (inputEl.value) inputEl.value.focus()
}

onMounted(() => {
  loadHistory()
  document.addEventListener('keydown', onKeydownGlobal)
})
onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  document.removeEventListener('keydown', onKeydownGlobal)
})

defineExpose({ focus: () => inputEl.value && inputEl.value.focus() })
</script>

<style scoped>
/* ============ 外壳：玻璃胶囊 ============ */
.search {
  position: relative;
  width: 100%;
  border-radius: 999px;
  transition: transform 0.35s var(--ease);
}
.search.is-focus {
  transform: translateY(-2px);
}

/* 聚焦时的极光光晕（背后模糊光斑） */
.search__glow {
  position: absolute;
  inset: -2px;
  border-radius: 999px;
  opacity: 0;
  transition: opacity 0.45s var(--ease);
  background: conic-gradient(
    from 140deg,
    var(--a2),
    var(--a1) 40%,
    var(--a3) 65%,
    var(--a2)
  );
  filter: blur(14px);
  pointer-events: none;
}
.search.is-focus .search__glow {
  opacity: 0.55;
  animation: spin 7s linear infinite;
}

.search__inner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 58px;
  padding: 0 8px 0 20px;
  border-radius: 999px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.02) 45%, rgba(255, 255, 255, 0.06)),
    rgba(10, 12, 26, 0.72);
  backdrop-filter: blur(26px) saturate(150%);
  -webkit-backdrop-filter: blur(26px) saturate(150%);
  border: 1px solid var(--stroke);
  box-shadow: 0 20px 50px -24px rgba(0, 0, 0, 0.9);
  transition: border-color 0.35s, box-shadow 0.35s;
}
.search.is-focus .search__inner {
  border-color: rgba(124, 92, 255, 0.55);
  box-shadow:
    0 0 0 4px rgba(124, 92, 255, 0.12),
    0 24px 60px -26px rgba(124, 92, 255, 0.75);
}

/* ============ 放大镜图标 ============ */
.search__ico {
  width: 20px;
  height: 20px;
  flex: none;
  color: var(--muted);
  transition: color 0.3s;
}
.search.is-focus .search__ico {
  color: var(--a2);
}

/* ============ 输入框 ============ */
.search__input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: none;
  background: none;
  color: var(--ink);
  font: inherit;
  font-size: 15.5px;
  font-weight: 300;
  letter-spacing: 0.01em;
}
.search__input::placeholder {
  color: var(--muted);
  font-weight: 300;
}
/* 去掉浏览器原生 search 清除按钮，避免与自绘按钮重复 */
.search__input::-webkit-search-cancel-button {
  -webkit-appearance: none;
  appearance: none;
}

/* ============ 清空按钮 ============ */
.search__clear {
  flex: none;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.07);
  transition: color 0.25s, background 0.25s, transform 0.25s var(--ease);
}
.search__clear:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.14);
  transform: rotate(90deg);
}
.search__clear svg {
  width: 14px;
  height: 14px;
}

/* ============ "/" 快捷键提示 ============ */
.search__kbd {
  flex: none;
  display: grid;
  place-items: center;
  padding-right: 4px;
}
.search__kbd kbd {
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--stroke);
  border-radius: 7px;
  padding: 3px 9px;
  line-height: 1;
}

/* ============ 搜索按钮（复用登录页 submit 的渐变语言） ============ */
.search__btn {
  flex: none;
  height: 44px;
  padding: 0 26px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  font: inherit;
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #fff;
  background: linear-gradient(115deg, var(--a1), #5b8bff 45%, var(--a2));
  background-size: 180% auto;
  box-shadow: 0 12px 28px -12px rgba(124, 92, 255, 0.85);
  transition: background-position 0.55s, box-shadow 0.35s, transform 0.3s var(--ease);
}
.search__btn:hover {
  background-position: right center;
  box-shadow: 0 16px 34px -12px rgba(34, 211, 238, 0.7);
}
.search__btn:active {
  transform: scale(0.97);
}
.search__btn-txt {
  display: block;
}

/* ============ 建议 / 历史 下拉 ============ */
.drop {
  position: absolute;
  top: calc(100% + 12px);
  left: 0;
  right: 0;
  z-index: 40;
  border-radius: 18px;
  overflow: hidden;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.02) 45%, rgba(255, 255, 255, 0.05)),
    rgba(10, 12, 26, 0.92);
  backdrop-filter: blur(26px) saturate(150%);
  -webkit-backdrop-filter: blur(26px) saturate(150%);
  border: 1px solid var(--stroke);
  box-shadow: 0 30px 70px -30px rgba(0, 0, 0, 0.95);
}
.drop-enter-active,
.drop-leave-active {
  transition: opacity 0.25s var(--ease), transform 0.25s var(--ease);
}
.drop-enter-from,
.drop-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.drop__row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 18px;
  border: 0;
  background: none;
  cursor: pointer;
  text-align: left;
  font: inherit;
  transition: background 0.2s;
}
.drop__row--on,
.drop__row:hover {
  background: rgba(124, 92, 255, 0.14);
}
.drop__ico {
  flex: none;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: #fff;
}
.drop__ico svg {
  width: 14px;
  height: 14px;
}
.drop__ico--title {
  background: linear-gradient(135deg, var(--a1), #5b8bff);
  box-shadow: 0 6px 14px -6px var(--a1);
}
.drop__ico--tag {
  background: linear-gradient(135deg, #5b8bff, var(--a2));
  box-shadow: 0 6px 14px -6px var(--a2);
}
.drop__text {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: var(--ink);
  font-weight: 400;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.drop__text em {
  font-style: normal;
  color: var(--a2);
  font-weight: 600;
}
.drop__type {
  flex: none;
  font-size: 11px;
  color: var(--muted);
  letter-spacing: 0.06em;
}

/* 历史记录区 */
.drop__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px 6px;
  font-size: 11.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
}
.drop__clear {
  border: 0;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 12px;
  letter-spacing: 0;
  text-transform: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.25s;
}
.drop__clear:hover {
  color: #ff8098;
}
.drop__history {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 6px 18px 12px;
}
.drop__hchip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--stroke);
  background: var(--glass);
  color: var(--ink);
  font: inherit;
  font-size: 12.5px;
  padding: 5px 13px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s;
}
.drop__hchip svg {
  width: 12px;
  height: 12px;
  color: var(--muted);
  flex: none;
}
.drop__hchip:hover {
  background: var(--glass-2);
  border-color: rgba(124, 92, 255, 0.45);
}

/* 底部快捷键提示 */
.drop__foot {
  display: flex;
  gap: 16px;
  padding: 10px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  font-size: 11px;
  color: var(--muted);
  font-weight: 300;
}
.drop__foot kbd {
  font-family: inherit;
  display: inline-grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  margin-right: 3px;
  font-size: 10.5px;
  color: var(--ink);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--stroke);
  border-radius: 5px;
}

/* ============ 响应式 ============ */
@media (max-width: 640px) {
  .search__inner {
    height: 52px;
    padding-left: 16px;
    gap: 9px;
  }
  .search__input {
    font-size: 14.5px;
  }
  .search__btn {
    height: 40px;
    padding: 0 18px;
    font-size: 14px;
  }
  .search__kbd {
    display: none;
  }
  .drop__foot {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .search.is-focus .search__glow {
    animation: none;
  }
}
</style>
