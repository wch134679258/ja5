package com.fenglin.springboottest.dto;

/**
 * 分类树节点：一级分类 + 其下二级标签 + 该分类文章数。
 *
 * 该对象同时承载「分类行」与「标签行」的扁平查询结果：
 *   一行 = 一个 (分类, 标签) 组合；标签为 null 表示该分类下没有标签。
 * 由 Service 层按 categoryId 归组成 {@link CategoryTree}。
 */
public class CategoryNode {

    /* ---- 一级分类字段 ---- */
    private Long id;
    private String name;
    private String slug;
    private Integer sort;

    /* ---- 二级标签字段（可为 null，表示该分类下暂无标签） ---- */
    private Long tagId;
    private String tagName;
    private String tagSlug;
    private Integer tagSort;

    /** 该一级分类下的文章数 */
    private Integer articleCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagSlug() {
        return tagSlug;
    }

    public void setTagSlug(String tagSlug) {
        this.tagSlug = tagSlug;
    }

    public Integer getTagSort() {
        return tagSort;
    }

    public void setTagSort(Integer tagSort) {
        this.tagSort = tagSort;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
