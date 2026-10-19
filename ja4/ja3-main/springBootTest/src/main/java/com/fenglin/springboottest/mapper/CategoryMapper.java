package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.dto.CategoryNode;
import com.fenglin.springboottest.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 一级分类 Mapper
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 分类树：一级分类 + 其下二级标签 + 该分类下的文章数。
     *
     * 说明：标签是稀疏数据（几十条），这里一次性查出来，在 Service 层按 category_id 归组，
     *       避免 N+1 查询；文章数用左连接聚合，没有文章的分类也返回（articleCount = 0）。
     */
    @Select("""
            SELECT
              c.id            AS id,
              c.name          AS name,
              c.slug          AS slug,
              c.sort          AS sort,
              t.id            AS tagId,
              t.name          AS tagName,
              t.slug          AS tagSlug,
              t.sort          AS tagSort,
              (SELECT COUNT(*) FROM article a WHERE a.category_id = c.id) AS articleCount
            FROM category c
            LEFT JOIN tag t ON t.category_id = c.id
            ORDER BY c.sort ASC, c.id ASC, t.sort ASC, t.id ASC
            """)
    List<CategoryNode> selectCategoryTree();
}
