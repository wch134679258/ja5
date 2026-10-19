// ============================================================
//  星空贪吃蛇 · 成绩接口
// ------------------------------------------------------------
//  后端接口：
//    POST   /api/game/scores       { score, length, duration }  提交成绩（需登录）
//      -> { message, score, best, isNewBest, rank, playCount, leaderboard }
//    GET    /api/game/leaderboard  ?limit=10                    排行榜（公开）
//      -> { list: [{ rank, nickname, score, length, duration, createdAt }] }
//    GET    /api/game/my-best                                    我的最好成绩（需登录）
//      -> { best, rank, playCount }
//
//  未登录提交 -> 401 { message }，http 层会清除 token 并抛出
//  「登录已过期，请重新登录」，页面据此提示重新登录。
// ============================================================

import { http } from './http'

/**
 * 提交一局成绩。
 * @param {object} result { score, length, duration }
 * @returns {Promise<object>} 提交结果（含最新排行榜）
 */
export async function submitScore (result) {
  const { score, length = 0, duration = 0 } = result
  return http.post('/game/scores', { score, length, duration })
}

/**
 * 排行榜 Top N。
 * @param {number} limit 取前几名，默认 10
 * @returns {Promise<Array>} 榜单项数组
 */
export async function fetchLeaderboard (limit = 10) {
  const res = await http.get('/game/leaderboard', { limit })
  return (res && res.list) || []
}

/** 我的最好成绩与排名（未提交过成绩时 best / rank 为 0） */
export async function fetchMyBest () {
  return http.get('/game/my-best')
}
