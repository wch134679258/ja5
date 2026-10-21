// ============================================================
//  文章详情接口
// ------------------------------------------------------------
//  后端接口：
//    GET /api/article/{id}           详情（浏览量 +1；带 token 时返回 favorited）
//      -> { id, title, summary, content, author, categoryId, category,
//           tags[], views, createdAt, updatedAt, favorited }
//    GET /api/article/{id}/related   相关文章（共享标签优先，同分类兜底，最多 4 条）
//      -> [ { id, title, summary, author, category, tags[], views, createdAt } ]
// ============================================================

import { http } from './http'

/**
 * 文章详情。
 * @param {number|string} id 文章 id
 */
export async function fetchArticle (id) {
  return http.get('/article/' + encodeURIComponent(id))
}

/**
 * 相关文章推荐。
 * @param {number|string} id 文章 id
 */
export async function fetchRelated (id) {
  return http.get('/article/' + encodeURIComponent(id) + '/related')
}
