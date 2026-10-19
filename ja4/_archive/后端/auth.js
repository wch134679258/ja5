// ============================================================
//  认证后端接口（已对接 springBootTest 后端）
// ------------------------------------------------------------
//  后端接口（BASE 默认 "/api"，由 .env 的 VITE_API_BASE 覆盖）：
//    GET  /api/auth/captcha   -> { captchaId, image(base64 png) }
//    POST /api/auth/register  { name, email, password, captchaId, captcha } -> { token, userId }
//    POST /api/auth/login     { email, password, remember }  -> { token, userId }
//    GET  /api/auth/me        Authorization: Bearer <token>   -> { id, name, email }
//
//  错误约定：HTTP 非 2xx 时，响应体 { message: "错误说明" } 会被提取为
//  Error.message，直接展示在表单错误横幅。
// ============================================================

const USE_MOCK = false
const BASE = (import.meta.env && import.meta.env.VITE_API_BASE) || '/api'
const TOKEN_KEY = 'aurora_token'

// ---------- token 本地持久化 ----------
export function saveToken (token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
}
export function getToken () {
  return localStorage.getItem(TOKEN_KEY) || ''
}
export function clearToken () {
  localStorage.removeItem(TOKEN_KEY)
}
export function logout () {
  clearToken()
}

// POST 请求封装
async function request (path, body) {
  const res = await fetch(BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  if (!res.ok) {
    let message = '请求失败，请稍后再试'
    try {
      const data = await res.json()
      if (data && data.message) message = data.message
    } catch (_) { /* 忽略解析错误 */ }
    throw new Error(message)
  }
  return res.json()
}

// 获取图形验证码：返回 { captchaId, image(data:image/png;base64,...) }
export async function getCaptcha () {
  const res = await fetch(BASE + '/auth/captcha', { method: 'GET' })
  if (!res.ok) {
    let message = '获取验证码失败，请稍后重试'
    try {
      const data = await res.json()
      if (data && data.message) message = data.message
    } catch (_) { /* 忽略 */ }
    throw new Error(message)
  }
  return res.json()
}

// 注册（携带图形验证码）
export async function registerUser (payload) {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 1400))
    return { token: 'mock-token', userId: 'u_' + Date.now() }
  }
  const res = await request('/auth/register', payload)
  if (res && res.token) saveToken(res.token)   // 登录态持久化
  return res
}

// 登录
export async function loginUser (payload) {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 900))
    return { token: 'mock-token', userId: 'u_' + Date.now() }
  }
  const res = await request('/auth/login', payload)
  if (res && res.token) saveToken(res.token)   // 登录态持久化
  return res
}

// 拉取当前登录用户信息（受保护接口，需带 Bearer 令牌）
export async function getMe () {
  const token = getToken()
  const res = await fetch(BASE + '/auth/me', {
    method: 'GET',
    headers: token ? { Authorization: 'Bearer ' + token } : {}
  })
  if (res.status === 401) {
    clearToken()
    throw new Error('登录已过期，请重新登录')
  }
  if (!res.ok) {
    let message = '获取用户信息失败'
    try {
      const data = await res.json()
      if (data && data.message) message = data.message
    } catch (_) { /* 忽略 */ }
    throw new Error(message)
  }
  return res.json()
}

export { USE_MOCK, BASE }
