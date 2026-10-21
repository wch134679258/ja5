package com.fenglin.springboottest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树（对外响应结构）：一级分类 -> 二级标签。
 */
public class CategoryTree {

    private Long id;
    private String name;
    private String slug;

    /** 该一级分类下的文章数 */
    private Integer articleCount;

    /** 二级标签列表 */
    private List<TagStat> tags = new ArrayList<>();

    public CategoryTree(Long id, String name, String slug, Integer articleCount) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.articleCount = articleCount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    public List<TagStat> getTags() {
        return tags;
    }

    public void setTags(List<TagStat> tags) {
        this.tags = tags;
    }
}
