// ============================================================
//  站点概览接口
// ------------------------------------------------------------
//  后端：GET /api/stats/overview  （公开接口，无需登录）
//    -> { articles, categories, tags, totalViews, avgViews }
//  用于首页顶部的数据条。接口失败时首页仍可正常浏览，
//  只是数据条不显示，因此调用方需要吞掉异常。
// ============================================================

import { http } from './http'

export async function fetchOverview () {
  return http.get('/stats/overview')
}
