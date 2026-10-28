package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.entity.Article;
import com.fenglin.springboottest.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 收藏 Mapper。
 *
 * <p>收藏列表需要展示文章信息，因此直接 JOIN article 查询，
 * 结果复用 ArticleMapper.ArticleMap 的列映射（MyBatis 支持跨 namespace
 * 用全限定名引用 resultMap，避免重复定义）。
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 分页查询某用户收藏的文章，按收藏时间倒序（最近收藏的在前）。
     *
     * @param page   分页对象（页码、页大小）
     * @param userId 用户 id
     */
    Page<Article> selectFavoriteArticles(Page<Article> page, @Param("userId") Long userId);
}
