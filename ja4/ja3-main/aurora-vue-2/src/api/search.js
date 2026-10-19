// ============================================================
//  搜索接口（对接 springBootTest 后端）
// ------------------------------------------------------------
//  后端接口：
//    GET /api/search?q=&categoryId=&tagId=&sort=hot|new&page=&size=
//      q          关键词，可为空；空则返回推荐内容
//      categoryId 一级分类 id，可选
//      tagId      二级标签 id，可选（可与 categoryId 叠加）
//      sort       hot 热度（默认）/ new 最新
//      page       从 1 开始，默认 1
//      size       默认 10，后端上限 50
//    -> { keyword, categoryId, tagId, sort, page, size, total, pages,
//         list: [{ id, title, summary, author, category, tags[], views, createdAt }] }
//
//    GET /api/search/categories  分类树（一级分类 -> 二级标签）
//    GET /api/search/hot-tags    热门标签
//    GET /api/search/suggest?q=  搜索建议（标题 + 标签）
//    -> [ { type: "title"|"tag", id, text } ]
//
//  错误约定：HTTP 非 2xx 时，响应体 { message: "错误说明" } 会被提取为 Error.message
// ============================================================

import { http } from './http'

/**
 * 分级分类 + 关键词 组合搜索
 * @param {string} keyword 关键词，可为空
 * @param {object} opts    { categoryId, tagId, sort, page, size }
 */
export async function searchArticles (keyword = '', opts = {}) {
  const { categoryId = null, tagId = null, sort = 'hot', page = 1, size = 10 } = opts
  return http.get('/search', { q: keyword || '', categoryId, tagId, sort, page, size })
}

/**
 * 分类树：一级分类 -> 二级标签
 * @returns {Promise<Array<{id,name,slug,articleCount,tags:Array}>>}
 */
export async function fetchCategories () {
  return http.get('/search/categories')
}

/**
 * 热门标签
 * @returns {Promise<Array<{id,name,slug,categoryId,categoryName,articleCount}>>}
 */
export async function fetchHotTags () {
  return http.get('/search/hot-tags')
}

/**
 * 搜索建议：命中关键词的文章标题 + 标签
 * @param {string} keyword 关键词（至少 1 个字符）
 * @returns {Promise<Array<{type:'title'|'tag', id, text}>>}
 */
export async function fetchSuggest (keyword = '') {
  const kw = (keyword || '').trim()
  if (!kw) return []
  return http.get('/search/suggest', { q: kw })
}
