<template>
  <div class="tabs" ref="root">
    <i ref="pill"></i>
    <button class="tab" :class="{ on: isOn('login') }" data-mode="login" @click="$emit('select', 'login')">登录</button>
    <button class="tab" :class="{ on: isOn('signup') }" data-mode="signup" @click="$emit('select', 'signup')">注册</button>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'

const props = defineProps({ mode: { type: String, default: 'login' } })
defineEmits(['select'])

const root = ref(null)
const pill = ref(null)

function isOn (m) {
  return m === props.mode
}

function movePill () {
  const on = root.value && root.value.querySelector('.tab.on')
  if (!on) return
  pill.value.style.width = on.offsetWidth + 'px'
  pill.value.style.left = on.offsetLeft + 'px'
}

watch(() => props.mode, () => nextTick(movePill))
onMounted(() => nextTick(movePill))

defineExpose({ movePill })
</script>
