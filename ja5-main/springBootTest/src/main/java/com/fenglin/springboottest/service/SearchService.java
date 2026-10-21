package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.dto.ArticleItem;
import com.fenglin.springboottest.dto.CategoryNode;
import com.fenglin.springboottest.dto.CategoryTree;
import com.fenglin.springboottest.dto.TagStat;
import com.fenglin.springboottest.entity.Article;
import com.fenglin.springboottest.entity.Tag;
import com.fenglin.springboottest.mapper.ArticleMapper;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页搜索 / 展示服务。
 *
 * <h3>支持的检索能力</h3>
 * <ol>
 *   <li><b>首页展示</b>：不带任何条件时返回推荐内容（默认按热度 views 倒序）</li>
 *   <li><b>关键词搜索</b>：命中标题 / 摘要 / 正文 / 作者 / 标签</li>
 *   <li><b>一级分类筛选</b>：categoryId</li>
 *   <li><b>二级标签筛选</b>：tagId（可与一级分类叠加，形成「分级 + 分类」交叉检索）</li>
 *   <li><b>排序切换</b>：hot 热度 / new 最新</li>
 *   <li><b>分页</b>：page / size，size 有上限保护</li>
 *   <li><b>搜索建议</b>：suggest，命中关键词的标题 / 标签，供搜索框自动补全</li>
 * </ol>
 */
@Service
public class SearchService {

    /** 每页条数上限，防止前端传超大 size 拖垮数据库 */
    private static final int MAX_SIZE = 50;
    private static final int DEFAULT_SIZE = 10;

    /** 首页热门标签展示条数 */
    private static final int HOT_TAG_LIMIT = 12;

    /** 搜索建议：标题命中条数 / 标签命中条数 */
    private static final int TITLE_SUGGEST_LIMIT = 5;
    private static final int TAG_SUGGEST_LIMIT = 5;

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public SearchService(ArticleMapper articleMapper, CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    // ============================================================
    //  一、搜索主入口
    // ============================================================

    /**
     * 分级分类 + 关键词 组合搜索。
     *
     * @param keyword    关键词，可为空
     * @param categoryId 一级分类 id，可为 null
     * @param tagId      二级标签 id，可为 null
     * @param sort       排序：hot / new
     * @param page       页码，从 1 开始
     * @param size       每页条数
     * @return { keyword, categoryId, tagId, sort, page, size, total, pages, list }
     */
    public Map<String, Object> search(String keyword, Long categoryId, Long tagId,
                                      String sort, Integer page, Integer size) {
        String kw = keyword == null ? "" : keyword.trim();
        String st = normalizeSort(sort);
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        // 关键词为空时，ORDER BY 里的 LIKE 打分没有意义，直接走纯分类/热度排序，
        // 因此这里按「空关键词」与「有关键词」两条路径分别处理，保持 SQL 简单可预测。
        Page<Article> result = articleMapper.search(new Page<>(p, s), kw, categoryId, tagId, st);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("keyword", kw);
        resp.put("categoryId", categoryId);
        resp.put("tagId", tagId);
        resp.put("sort", st);
        resp.put("page", result.getCurrent());
        resp.put("size", result.getSize());
        resp.put("total", result.getTotal());
        resp.put("pages", result.getPages());
        resp.put("list", toItems(result.getRecords()));
        return resp;
    }

    // ============================================================
    //  二、分类树 & 热门标签
    // ============================================================

    /**
     * 分类树：一级分类 -> 二级标签。供首页「分级分类」导航渲染。
     * 一次性查询后按 categoryId 归组，避免 N+1。
     */
    public List<CategoryTree> categoryTree() {
        List<CategoryNode> flat = categoryMapper.selectCategoryTree();
        Map<Long, CategoryTree> byId = new LinkedHashMap<>();
        for (CategoryNode n : flat) {
            CategoryTree node = byId.get(n.getId());
            if (node == null) {
                node = new CategoryTree(n.getId(), n.getName(), n.getSlug(), n.getArticleCount());
                byId.put(n.getId(), node);
            }
            if (n.getTagId() != null) {
                TagStat tag = new TagStat();
                tag.setId(n.getTagId());
                tag.setName(n.getTagName());
                tag.setSlug(n.getTagSlug());
                tag.setCategoryId(n.getId());
                tag.setCategoryName(n.getName());
                node.getTags().add(tag);
            }
        }
        return new ArrayList<>(byId.values());
    }

    /** 热门标签（首页快捷入口） */
    public List<TagStat> hotTags() {
        return tagMapper.selectHotTags(HOT_TAG_LIMIT);
    }

    // ============================================================
    //  二、补充：搜索建议（搜索框自动补全）
    // ============================================================

    /**
     * 搜索建议：命中关键词的文章标题 + 标签，供搜索框下拉展示。
     * 标题在前（用户多数想找的是具体文章），标签在后。
     *
     * @param keyword 关键词（调用方负责 trim；空串直接返回空列表）
     * @return [ { type: "title", id, text }, { type: "tag", id, text } ]
     */
    public List<Map<String, Object>> suggest(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> out = new ArrayList<>();
        for (Article a : articleMapper.selectTitleSuggest(kw, TITLE_SUGGEST_LIMIT)) {
            out.add(suggestItem("title", a.getId(), a.getTitle()));
        }
        for (Tag t : tagMapper.selectNameSuggest(kw, TAG_SUGGEST_LIMIT)) {
            out.add(suggestItem("tag", t.getId(), t.getName()));
        }
        return out;
    }

    private Map<String, Object> suggestItem(String type, Long id, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("id", id);
        m.put("text", text);
        return m;
    }

    // ============================================================
    //  三、工具方法
    // ============================================================

    /** 排序参数白名单：只允许 hot / new，其余一律按 hot 处理 */
    private String normalizeSort(String sort) {
        return "new".equalsIgnoreCase(sort) ? "new" : "hot";
    }

    /** Article 实体列表 -> 前端展示 DTO 列表（转换规则统一收敛在 ArticleItem.from） */
    private List<ArticleItem> toItems(List<Article> records) {
        List<ArticleItem> items = new ArrayList<>(records.size());
        for (Article a : records) {
            items.add(ArticleItem.from(a));
        }
        return items;
    }
}
