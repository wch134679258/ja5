<template>
  <div class="panel" :class="'mode-' + mode">
    <div class="head" :class="{ swap: headSwap }">
      <h2>{{ headTitle }}</h2>
      <p>{{ headSub }}</p>
    </div>

    <AuthTabs :mode="mode" @select="setMode" ref="tabs" />

    <form id="form" novalidate @submit.prevent="handleSubmit">
      <div id="fields" ref="fields">
        <FormField
          id="name" kind="user" label="昵称" for-modes="signup"
          :error="'请输入至少 2 个字符'"
          v-model="form.name" :invalid="invalid.name"
          @update:modelValue="clearField('name')" ref="nameField" />

        <FormField
          id="email" kind="mail" label="邮箱地址" for-modes="login signup reset"
          :error="'请输入有效的邮箱地址'"
          v-model="form.email" :invalid="invalid.email" autocomplete="email"
          @update:modelValue="clearField('email')" ref="emailField" />

        <PasswordField
          id="pass" label="密码" for-modes="login signup"
          :error="'密码至少 8 位，且包含字母和数字'"
          v-model="form.pass" :show="showPass.pass" :invalid="invalid.pass"
          autocomplete="current-password" @toggle="togglePass('pass')"
          @update:modelValue="onPassInput" ref="passField">
          <template #meter>
            <PasswordStrength :width="strength.width" :color="strength.color" :label="strength.label" />
          </template>
        </PasswordField>

        <PasswordField
          id="pass2" label="确认密码" for-modes="signup"
          :error="'两次输入的密码不一致'"
          v-model="form.pass2" :show="showPass.pass2" :invalid="invalid.pass2"
          autocomplete="new-password" @toggle="togglePass('pass2')"
          @update:modelValue="clearField('pass2')" ref="pass2Field" />

        <FormField
          id="code" kind="lock" label="图形验证码" for-modes="signup"
          :error="captchaError"
          v-model="form.code" :invalid="invalid.code"
          autocomplete="off" inputmode="text" maxlength="4"
          @update:modelValue="clearField('code')" ref="codeField">
          <template #append>
            <span class="captcha-wrap">
              <img v-if="captchaImage" :src="captchaImage" class="captcha-img" alt="验证码"
                   title="点击图片刷新" @click="refreshCaptcha" />
              <button type="button" class="code-btn" :disabled="captchaLoading" @click="refreshCaptcha">
                {{ captchaLoading ? '加载中' : '换一张' }}
              </button>
            </span>
          </template>
        </FormField>

        <p class="code-hint" v-show="mode === 'signup'">请输入上方图片中的 4 位字母和数字（不区分大小写）</p>
      </div>

      <!-- rows -->
      <div class="row" data-for="login">
        <label class="check">
          <input type="checkbox" id="remember" v-model="remember" />
          <span class="box"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round"><path d="m5 13 4 4L19 7"/></svg></span>
          记住我
        </label>
        <button type="button" class="link" @click="setMode('reset')">忘记密码？</button>
      </div>

      <div class="row" data-for="signup">
        <label class="check">
          <input type="checkbox" id="agree" v-model="agree" @change="agreeInvalid = false" />
          <span class="box" :style="agreeInvalid ? { borderColor: 'rgba(255,95,120,.8)' } : null">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round"><path d="m5 13 4 4L19 7"/></svg>
          </span>
          <span class="agree-text">我已阅读并同意 <a class="link-inline" @click.stop.prevent>服务条款</a> 与 <a class="link-inline" @click.stop.prevent>隐私政策</a></span>
        </label>
        <button type="button" class="link" @click="setMode('login')">已有账号？去登录</button>
      </div>

      <div class="row" data-for="reset">
        <span style="color:var(--muted)">我们将发送重置链接到你的邮箱</span>
        <button type="button" class="link" @click="setMode('login')">返回登录</button>
      </div>

      <p class="form-error" v-if="formError">{{ formError }}</p>

      <button class="submit" ref="submitEl" type="submit"
        :data-state="submitState !== 'idle' ? submitState : null"
        @pointerdown="addRipple" @pointermove="onSubmitMove" @pointerleave="onSubmitLeave">
        <span class="txt">{{ submitText }}</span>
        <span class="spin"></span>
      </button>
    </form>

    <div class="divider">或使用以下方式</div>
    <SocialButtons />
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useAuthForm } from '../composables/useAuthForm'
import AuthTabs from './AuthTabs.vue'
import FormField from './FormField.vue'
import PasswordField from './PasswordField.vue'
import PasswordStrength from './PasswordStrength.vue'
import SocialButtons from './SocialButtons.vue'

const emit = defineEmits(['success'])

const {
  mode, form, remember, agree, agreeInvalid, invalid, showPass, submitState, formError,
  headSwap, headTitle, headSub, submitText, strength,
  captchaImage, captchaLoading, captchaError, refreshCaptcha, loadCaptcha,
  clearField, setMode, togglePass, onPassInput, onSubmit, resetToLogin: resetForm
} = useAuthForm(emit)

const fields = ref(null)
const submitEl = ref(null)
const tabs = ref(null)
const nameField = ref(null)
const emailField = ref(null)
const passField = ref(null)
const pass2Field = ref(null)
const codeField = ref(null)
const fieldMap = { name: nameField, email: emailField, pass: passField, pass2: pass2Field, code: codeField }

const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches

function syncHeight () {
  if (fields.value) fields.value.style.height = fields.value.scrollHeight + 'px'
}

function handleSubmit () {
  const { ok, first } = onSubmit()
  if (!ok) {
    syncHeight()
    if (first && fieldMap[first].value) fieldMap[first].value.focus()
  }
}

function addRipple (e) {
  const r = submitEl.value.getBoundingClientRect()
  const s = Math.max(r.width, r.height)
  const el = document.createElement('span')
  el.className = 'ripple'
  el.style.width = el.style.height = s + 'px'
  el.style.left = (e.clientX - r.left - s / 2) + 'px'
  el.style.top = (e.clientY - r.top - s / 2) + 'px'
  submitEl.value.appendChild(el)
  setTimeout(() => el.remove(), 700)
}

function onSubmitMove (e) {
  if (reduce) return
  const r = submitEl.value.getBoundingClientRect()
  const dx = (e.clientX - (r.left + r.width / 2)) / r.width
  const dy = (e.clientY - (r.top + r.height / 2)) / r.height
  submitEl.value.style.transform = `translate(${dx * 9}px, ${dy * 5}px)`
}
function onSubmitLeave () {
  if (submitEl.value) submitEl.value.style.transform = ''
}

function resetToLogin () {
  resetForm()
  if (submitEl.value) submitEl.value.style.transform = ''
  nextTick(syncHeight)
}

let onResize
function onResizeHandler () { syncHeight(); if (tabs.value) tabs.value.movePill() }

// 模式变化后，重新计算字段高度 + 移动 tab 滑块
watch(mode, () => {
  nextTick(() => { if (tabs.value) tabs.value.movePill(); syncHeight() })
  setTimeout(syncHeight, 480)
})

// 提交状态回到 idle 时，复位按钮位移
watch(submitState, (val) => { if (val === 'idle' && submitEl.value) submitEl.value.style.transform = '' })

onMounted(() => {
  nextTick(() => { if (tabs.value) tabs.value.movePill(); syncHeight() })
  onResize = onResizeHandler
  window.addEventListener('resize', onResize)
  setTimeout(() => { if (tabs.value) tabs.value.movePill(); syncHeight() }, 400)
  loadCaptcha()   // 预加载图形验证码
})

onBeforeUnmount(() => {
  if (onResize) window.removeEventListener('resize', onResize)
})

defineExpose({ resetToLogin })
</script>
