<template>
  <div class="meter">
    <div class="meter__bar"><i :style="{ width: width, background: color }"></i></div>
    <div class="meter__txt">{{ label }}</div>
  </div>
</template>

<script setup>
defineProps({
  width: { type: String, default: '0' },
  color: { type: String, default: '' },
  label: { type: String, default: '密码强度：—' }
})
</script>
