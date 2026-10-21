package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.dto.CategoryTree;
import com.fenglin.springboottest.dto.TagStat;
import com.fenglin.springboottest.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 首页搜索 / 展示接口。
 *
 * <pre>
 * GET /api/search
 *     ?q=关键词          关键词，可为空（空则返回推荐内容）
 *     &categoryId=1     一级分类 id，可选
 *     &tagId=3          二级标签 id，可选（可与 categoryId 叠加，实现分级分类交叉检索）
 *     &sort=hot|new     排序：hot 热度（默认）/ new 最新
 *     &page=1&size=10   分页，page 从 1 开始，size 上限 50
 *   -> { keyword, categoryId, tagId, sort, page, size, total, pages, list:[...] }
 *
 * GET /api/search/categories   一级分类 -> 二级标签 的分类树（供首页导航渲染）
 *   -> [ { id, name, slug, articleCount, tags:[ { id, name, slug, categoryId, categoryName } ] } ]
 *
 * GET /api/search/hot-tags     热门标签（按关联文章数倒序）
 *   -> [ { id, name, slug, categoryId, categoryName, articleCount } ]
 *
 * GET /api/search/suggest?q=关键词   搜索建议（标题 + 标签，供搜索框自动补全）
 *   -> [ { type: "title"|"tag", id, text } ]
 * </pre>
 *
 * 说明：以上均为公开接口，浏览内容无需登录。
 */
@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** 分级分类 + 关键词 组合搜索 / 首页默认展示 */
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "sort", required = false, defaultValue = "hot") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return searchService.search(q, categoryId, tagId, sort, page, size);
    }

    /** 分类树：一级分类 + 其下二级标签 */
    @GetMapping("/search/categories")
    public List<CategoryTree> categories() {
        return searchService.categoryTree();
    }

    /** 热门标签 */
    @GetMapping("/search/hot-tags")
    public List<TagStat> hotTags() {
        return searchService.hotTags();
    }

    /** 搜索建议：命中关键词的文章标题 + 标签（搜索框自动补全） */
    @GetMapping("/search/suggest")
    public List<Map<String, Object>> suggest(
            @RequestParam(value = "q", required = false, defaultValue = "") String q) {
        return searchService.suggest(q);
    }
}
