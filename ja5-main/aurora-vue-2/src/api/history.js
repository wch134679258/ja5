// ============================================================
//  浏览历史接口（全部需要登录）
// ------------------------------------------------------------
//  后端接口：
//    POST   /api/history/{articleId}      记录一次浏览 -> { message }
//    GET    /api/history?page=&size=      我的浏览历史（按浏览时间倒序）
//      -> { page, size, total, pages, list: [HistoryItem] }
//    DELETE /api/history/{articleId}      删除一条     -> { message }
//    DELETE /api/history                  清空         -> { message, removed }
// ============================================================

import { http } from './http'

/**
 * 我的浏览历史（分页）。
 * @param {object} opts { page, size }
 */
export async function fetchHistory (opts = {}) {
  const { page = 1, size = 10 } = opts
  return http.get('/history', { page, size })
}

/** 记录一次浏览（重复浏览只刷新时间，后端不会报错） */
export async function recordHistory (articleId) {
  return http.post('/history/' + encodeURIComponent(articleId))
}

/** 删除单条浏览记录 */
export async function removeHistory (articleId) {
  return http.del('/history/' + encodeURIComponent(articleId))
}

/** 清空浏览历史 */
export async function clearHistory () {
  return http.del('/history')
}
