<template>
  <div class="field" :data-for="forModes" :class="{ invalid }">
    <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="10" rx="2.5"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>

    <input
      ref="inputEl"
      :type="show ? 'text' : 'password'"
      :id="id"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <label :for="id">{{ label }}</label>

    <button type="button" class="eye" @click="$emit('toggle')" aria-label="显示密码">
      <svg v-if="!show" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3l18 18"/><path d="M10.6 5.2A9.9 9.9 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-2.4 3.3M6.5 7.8A17 17 0 0 0 2 12s3.6 7 10 7a9.6 9.6 0 0 0 3.6-.7"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></svg>
    </button>

    <span class="err">{{ error }}</span>
    <slot name="meter"></slot>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  id: String,
  label: String,
  error: String,
  forModes: String,
  modelValue: { type: String, default: '' },
  show: { type: Boolean, default: false },
  autocomplete: { type: String, default: 'off' },
  placeholder: { type: String, default: ' ' },
  invalid: { type: Boolean, default: false }
})
defineEmits(['update:modelValue', 'toggle'])

const inputEl = ref(null)
defineExpose({ focus: () => inputEl.value && inputEl.value.focus() })
</script>
