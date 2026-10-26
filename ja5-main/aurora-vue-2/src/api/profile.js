// ============================================================
//  个人中心接口（需要登录）
// ------------------------------------------------------------
//  后端接口：
//    GET /api/user/profile
//      -> { user: { id, name, joinedAt },
//           stats: { favorites, history, likes },
//           game: { best, rank, playCount } }
// ============================================================

import { http } from './http'

/** 个人中心汇总数据（一次请求拿齐统计与游戏成绩） */
export async function fetchProfile () {
  return http.get('/user/profile')
}
