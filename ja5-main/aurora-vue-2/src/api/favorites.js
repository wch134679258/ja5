// ============================================================
//  收藏接口（全部需要登录；token 由 http 层自动注入）
// ------------------------------------------------------------
//  后端接口：
//    POST   /api/favorites/{articleId}   收藏文章   -> { message }
//    DELETE /api/favorites/{articleId}   取消收藏   -> { message }
//    GET    /api/favorites?page=&size=   我的收藏（按收藏时间倒序）
//      -> { page, size, total, pages, list: [ArticleItem] }
//    GET    /api/favorites/ids           当前用户已收藏的文章 id 列表
//      -> { list: [1, 2, 3] }
//
//  未登录 / 令牌过期 -> 401 { message }，http 层会清除 token 并抛出
//  「登录已过期，请重新登录」，页面据此引导用户去登录。
// ============================================================

import { http } from './http'

/**
 * 我的收藏列表（分页）。
 * @param {object} opts { page, size }
 */
export async function fetchFavorites (opts = {}) {
  const { page = 1, size = 10 } = opts
  return http.get('/favorites', { page, size })
}

/** 当前用户已收藏的文章 id 列表（用于在列表页标记收藏态） */
export async function fetchFavoriteIds () {
  const res = await http.get('/favorites/ids')
  return (res && res.list) || []
}

/** 收藏文章 */
export async function addFavorite (articleId) {
  return http.post('/favorites/' + encodeURIComponent(articleId))
}

/** 取消收藏 */
export async function removeFavorite (articleId) {
  return http.del('/favorites/' + encodeURIComponent(articleId))
}
