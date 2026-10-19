package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.dto.TagStat;
import com.fenglin.springboottest.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 二级标签 Mapper
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 热门标签（用于首页标签快捷入口）。
     * 排序依据是该标签关联的文章数，最多返回 limit 个。
     *
     * @param limit 返回条数上限
     */
    @Select("""
            SELECT t.id   AS id,
                   t.name AS name,
                   t.slug AS slug,
                   t.category_id AS categoryId,
                   c.name AS categoryName,
                   COUNT(at.article_id) AS articleCount
            FROM tag t
            LEFT JOIN category  c  ON c.id = t.category_id
            LEFT JOIN article_tag at ON at.tag_id = t.id
            GROUP BY t.id, t.name, t.slug, t.category_id, c.name, t.sort
            ORDER BY articleCount DESC, t.sort ASC, t.id ASC
            LIMIT #{limit}
            """)
    List<TagStat> selectHotTags(@Param("limit") int limit);

    /**
     * 搜索建议：名称包含关键词的标签，按排序权重正序。
     * 只查 id / name 两列，用于搜索框自动补全。
     */
    @Select("""
            SELECT id, name FROM tag
            WHERE name LIKE CONCAT('%', #{kw}, '%')
            ORDER BY sort ASC, id ASC
            LIMIT #{limit}
            """)
    List<Tag> selectNameSuggest(@Param("kw") String kw, @Param("limit") int limit);
}
