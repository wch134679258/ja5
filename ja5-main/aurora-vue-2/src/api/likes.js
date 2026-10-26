// ============================================================
//  文章点赞接口
// ------------------------------------------------------------
//  后端接口：
//    POST   /api/likes/{articleId}       点赞     -> { liked: true,  likes }
//    DELETE /api/likes/{articleId}       取消点赞 -> { liked: false, likes }
//    GET    /api/likes/ids               我点赞过的文章 id 列表 -> { list: [id] }
//    GET    /api/likes/{articleId}/count 某文章点赞数（公开）    -> { likes }
// ============================================================

import { http } from './http'

/** 点赞 */
export async function addLike (articleId) {
  return http.post('/likes/' + encodeURIComponent(articleId))
}

/** 取消点赞 */
export async function removeLike (articleId) {
  return http.del('/likes/' + encodeURIComponent(articleId))
}

/** 我点赞过的文章 id 列表 */
export async function fetchLikedIds () {
  const res = await http.get('/likes/ids')
  return (res && res.list) || []
}

/** 某文章的点赞总数（公开，未登录也能看） */
export async function fetchLikeCount (articleId) {
  const res = await http.get('/likes/' + encodeURIComponent(articleId) + '/count')
  return (res && res.likes) || 0
}
