package com.fenglin.springboottest.dto;

/**
 * 标签统计：标签基本信息 + 所属一级分类 + 关联文章数。
 * 用于「热门标签」与「分类树里的二级标签」两种场景。
 */
public class TagStat {

    private Long id;
    private String name;
    private String slug;

    /** 所属一级分类 id，可为 null */
    private Long categoryId;

    /** 所属一级分类名，可为 null */
    private String categoryName;

    /** 关联文章数 */
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
