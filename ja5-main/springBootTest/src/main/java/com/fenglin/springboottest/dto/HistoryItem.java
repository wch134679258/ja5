package com.fenglin.springboottest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 浏览历史列表项：文章展示字段 + 本次浏览时间。
 *
 * <p>发布时间与浏览时间都在本类里格式化成字符串，前端直接展示，
 * 避免各处各自处理时间格式。</p>
 */
public class HistoryItem {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;
    private String title;
    private String summary;
    private String author;
    private String category;
    /** 数据库里的原始标签串（逗号分隔），对外不暴露，由 getTags() 拆成数组 */
    private String tagsRaw;
    private Integer views;
    private LocalDateTime createdAt;
    private LocalDateTime viewedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /** 对外暴露拆好的标签数组 */
    public String[] getTags() {
        return ArticleItem.splitTags(tagsRaw);
    }

    @JsonIgnore
    public String getTagsRaw() {
        return tagsRaw;
    }

    public void setTagsRaw(String tagsRaw) {
        this.tagsRaw = tagsRaw;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    /** 发布时间（字符串） */
    public String getCreatedAt() {
        return createdAt == null ? "" : createdAt.format(FMT);
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** 本次浏览时间（字符串） */
    public String getViewedAt() {
        return viewedAt == null ? "" : viewedAt.format(FMT);
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
