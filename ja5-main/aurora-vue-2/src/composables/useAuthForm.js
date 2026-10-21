// 表单全部逻辑：模式切换、校验、密码强度、提交状态机
// 纯逻辑，不触碰 DOM（聚焦/高度同步等交由组件处理）
import { reactive, ref, computed, onBeforeUnmount } from 'vue'
import { getCaptcha, registerUser, loginUser } from '../api/auth'

const COPY = {
  login:  { t: '欢迎回来', s: '登录后继续你的探索之旅', b: '登 录' },
  signup: { t: '创建账号', s: '加入我们，开启星海之旅', b: '注 册' }
}

export function useAuthForm (emit) {
  const mode = ref('login')
  const form = reactive({ name: '', pass: '', pass2: '', code: '' })
  const remember = ref(true)
  const agree = ref(false)
  const agreeInvalid = ref(false)
  const invalid = reactive({ name: false, pass: false, pass2: false, code: false })
  const showPass = reactive({ pass: false, pass2: false })
  const submitState = ref('idle')
  const formError = ref('')        // 提交级错误（接口失败等）

  const headSwap = ref(false)
  const headTitle = ref(COPY.login.t)
  const headSub = ref(COPY.login.s)
  const submitText = ref(COPY.login.b)

  const strength = reactive({ width: '0', color: '', label: '密码强度：—' })

  // ===== 图形验证码（看图输入 4 位字母 + 数字）=====
  const captchaId = ref('')        // 后端返回的验证码 id
  const captchaImage = ref('')     // base64 图片（data:image/png;base64,...）
  const captchaLoading = ref(false)
  const captchaError = ref('')     // 验证码字段的报错文案

  // 拉取一张新图形验证码
  async function loadCaptcha () {
    captchaLoading.value = true
    captchaError.value = ''
    try {
      const res = await getCaptcha()
      captchaId.value = res.captchaId
      captchaImage.value = res.image
    } catch (e) {
      captchaError.value = (e && e.message) || '验证码加载失败，请点击刷新'
    } finally {
      captchaLoading.value = false
    }
  }
  function refreshCaptcha () { loadCaptcha() }

  function clearField (k) {
    invalid[k] = false
    if (k === 'code') captchaError.value = ''
  }
  function clearErrors () {
    invalid.name = invalid.pass = invalid.pass2 = false
    agreeInvalid.value = false
  }

  function tabOn (m) {
    return m === mode.value
  }

  function setMode (next) {
    if (next === mode.value) return
    mode.value = next
    headSwap.value = true
    setTimeout(() => {
      headTitle.value = COPY[mode.value].t
      headSub.value = COPY[mode.value].s
      submitText.value = COPY[mode.value].b
      headSwap.value = false
    }, 200)
    if (next === 'signup') loadCaptcha()   // 进入注册页即拉取图形验证码
    clearErrors()
  }

  function togglePass (k) { showPass[k] = !showPass[k] }

  function onPassInput () {
    clearField('pass')
    const v = form.pass
    let score = 0
    if (v.length >= 8) score++
    if (/[A-Za-z]/.test(v) && /\d/.test(v)) score++
    if (v.length >= 12) score++
    if (/[^A-Za-z0-9]/.test(v)) score++
    const pct = [0, 30, 58, 80, 100][score]
    const label = ['弱', '一般', '不错', '很强', '极强'][Math.min(score, 4)]
    strength.width = pct + '%'
    strength.color = score <= 1
      ? 'linear-gradient(90deg,#ff5f78,#ff8a5f)'
      : score === 2
        ? 'linear-gradient(90deg,#fbbf24,#f59e0b)'
        : 'linear-gradient(90deg,#22d3ee,#7c5cff)'
    strength.label = v ? '密码强度：' + label : '密码强度：—'
  }

  function validate () {
    clearErrors()
    let ok = true
    let first = null
    if (form.name.trim().length < 2) { invalid.name = true; ok = false; first = first || 'name' }
    if (mode.value === 'signup') {
      if (!(form.pass.length >= 8 && /[A-Za-z]/.test(form.pass) && /\d/.test(form.pass))) { invalid.pass = true; ok = false; first = first || 'pass' }
      if (form.pass2 !== form.pass || !form.pass2) { invalid.pass2 = true; ok = false; first = first || 'pass2' }
      if (!form.code.trim()) { invalid.code = true; captchaError.value = '请输入图形验证码'; ok = false; first = first || 'code' }
      if (!agree.value) { ok = false; agreeInvalid.value = true }
    }
    if (mode.value === 'login') {
      if (form.pass.length < 6) { invalid.pass = true; ok = false; first = first || 'pass' }
    }
    return { ok, first }
  }

  // 客户端校验（同步）：返回 { ok, first } 供组件聚焦首个错误项
  function onSubmit () {
    const { ok, first } = validate()
    if (!ok) return { ok, first }
    runSubmit()
    return { ok: true }
  }

  // 提交：调用后端接口（注册 / 登录），处理 loading / done / 错误
  async function runSubmit () {
    submitState.value = 'loading'
    formError.value = ''
    try {
      if (mode.value === 'signup') {
        await registerUser({
          name: form.name.trim(),
          password: form.pass,
          captchaId: captchaId.value,
          captcha: form.code.trim()
        })
      } else {
        await loginUser({
          name: form.name.trim(),
          password: form.pass,
          remember: remember.value
        })
      }
      submitState.value = 'done'
      setTimeout(() => {
        submitState.value = 'idle'
        emit('success', mode.value)
      }, 480)
    } catch (e) {
      submitState.value = 'idle'
      formError.value = (e && e.message) || '操作失败，请稍后再试'
      // 注册失败时刷新验证码（图形码一次性，避免重复使用）
      if (mode.value === 'signup') refreshCaptcha()
    }
  }

  function resetToLogin () {
    form.name = ''
    form.pass = ''
    form.pass2 = ''
    form.code = ''
    agree.value = false
    remember.value = true
    agreeInvalid.value = false
    clearErrors()
    invalid.code = false
    captchaError.value = ''
    captchaId.value = ''
    captchaImage.value = ''
    captchaLoading.value = false
    formError.value = ''
    strength.width = '0'
    strength.color = ''
    strength.label = '密码强度：—'
    showPass.pass = false
    showPass.pass2 = false
    setMode('login')
  }

  return {
    mode, form, remember, agree, agreeInvalid, invalid, showPass, submitState, formError,
    headSwap, headTitle, headSub, submitText, strength,
    captchaId, captchaImage, captchaLoading, captchaError,
    clearField, clearErrors, tabOn, setMode, togglePass, onPassInput,
    validate, onSubmit, refreshCaptcha, loadCaptcha, resetToLogin
  }
}
