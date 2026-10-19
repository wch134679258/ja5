package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文章 Mapper —— 首页展示 + 分级分类搜索的核心 SQL。
 *
 * <h3>检索维度（全部可选，可任意组合）</h3>
 * <ul>
 *   <li><b>关键词 kw</b>：命中 标题 / 摘要 / 正文 / 作者 / 标签串，任一字段模糊匹配即可</li>
 *   <li><b>一级分类 categoryId</b>：按 category.id 精确过滤（分级分类的第一级）</li>
 *   <li><b>二级标签 tagId</b>：按 article_tag 关联过滤（分级分类的第二级）</li>
 *   <li><b>排序 sort</b>：hot（热度，views 倒序，默认）/ new（最新，created_at 倒序）</li>
 * </ul>
 *
 * <h3>为什么用 XML 而不是 @Select</h3>
 * 条件组合 + 相关度加权排序 + 动态排序，用注解拼字符串既难读也容易出错，
 * 因此该查询放在 resources/mapper/ArticleMapper.xml 中维护。
 *
 * <h3>分页</h3>
 * 把 Page 作为第一个参数传入，MyBatis-Plus 分页插件会自动追加 LIMIT/OFFSET
 * 并执行 count 查询；返回值即 Page 本身（记录同时写入其中）。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 分页搜索文章。
     *
     * @param page       分页对象（页码、页大小）
     * @param keyword    关键词，null / 空串表示不按关键词过滤
     * @param categoryId 一级分类 id，null 表示不限
     * @param tagId      二级标签 id，null 表示不限
     * @param sort       排序方式：hot / new，null 时默认 hot
     */
    Page<Article> search(Page<Article> page,
                         @Param("kw") String keyword,
                         @Param("categoryId") Long categoryId,
                         @Param("tagId") Long tagId,
                         @Param("sort") String sort);

    /**
     * 浏览量 +1（文章详情页打开时调用）。
     * 用 SQL 自增而不是先查后改，避免并发覆盖。
     */
    @Update("UPDATE article SET views = views + 1 WHERE id = #{id}")
    int incrementViews(@Param("id") Long id);

    /**
     * 搜索建议：标题包含关键词的文章（按热度倒序）。
     * 只查 id / title 两列，用于搜索框自动补全。
     */
    @Select("""
            SELECT id, title FROM article
            WHERE title LIKE CONCAT('%', #{kw}, '%')
            ORDER BY views DESC, id DESC
            LIMIT #{limit}
            """)
    List<Article> selectTitleSuggest(@Param("kw") String kw, @Param("limit") int limit);

    /**
     * 相关文章（共享标签）：与当前文章有共同标签的其它文章，
     * 按共同标签数倒序、热度倒序。SQL 在 ArticleMapper.xml 维护（含 GROUP BY）。
     */
    List<Article> selectRelatedByTags(@Param("id") Long id, @Param("limit") int limit);

    /**
     * 相关文章兜底（同分类）：没有共享标签的文章时，退化为同分类按热度推荐。
     */
    List<Article> selectRelatedByCategory(@Param("id") Long id,
                                          @Param("categoryId") Long categoryId,
                                          @Param("limit") int limit);
}
