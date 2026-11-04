// ============================================================
//  文章封面：按文章 id 在 public/covers 的星空图里轮转
// ------------------------------------------------------------
//  ArticleCard / PickCard / ArticleRow 共用，保证同一篇文章
//  在首页不同位置（头条、精选、列表）拿到的是同一张图。
//  后续如果后端补了真实封面字段，只要在这里改一行：
//    return article.cover || COVERS[seed % COVERS.length]
// ============================================================

const COVERS = ['/covers/1.jpg', '/covers/2.jpg', '/covers/3.jpg']

/**
 * @param {number|string} id  文章 id
 * @returns {string} 封面图 URL
 */
export function coverFor (id) {
  const seed = Math.abs(Number(id) || 0)
  return COVERS[seed % COVERS.length]
}

/**
 * 按「版面位置」取图，而不是按文章 id。
 * 首页首屏只有 3 个封面位（头条 + 精选 2 张），素材也正好 3 张；
 * 用 id 取模会出现两张卡撞同一张图，用槽位取能保证首屏 3 张互不重复。
 * @param {number} slot 槽位下标，从 0 开始
 */
export function coverAt (slot) {
  const i = Math.abs(Number(slot) || 0) % COVERS.length
  return COVERS[i]
}
