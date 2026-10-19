package com.fenglin.springboottest.dto;

import com.fenglin.springboottest.entity.Article;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果单项：只暴露前端展示需要的字段（不回传全文 content，减小响应体）。
 */
public class ArticleItem {

    /** 列表 / 卡片统一的时间格式 */
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;
    private String title;
    private String summary;
    private String author;
    private String category;
    /** 标签数组（后端把逗号分隔的字符串拆好再返回） */
    private String[] tags;
    private Integer views;
    /** 发布时间，格式 yyyy-MM-dd HH:mm */
    private String createdAt;

    public ArticleItem(Long id, String title, String summary, String author, String category,
                       String[] tags, Integer views, String createdAt) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.author = author;
        this.category = category;
        this.tags = tags;
        this.views = views;
        this.createdAt = createdAt;
    }

    /**
     * Article 实体 -> 列表 DTO。搜索、收藏、相关文章列表共用同一套转换规则。
     */
    public static ArticleItem from(Article a) {
        return new ArticleItem(
                a.getId(),
                a.getTitle(),
                a.getSummary(),
                a.getAuthor(),
                a.getCategory(),
                splitTags(a.getTags()),
                a.getViews(),
                a.getCreatedAt() == null ? "" : a.getCreatedAt().format(FMT)
        );
    }

    /** "Vue,前端" -> ["Vue", "前端"]，过滤空串 */
    public static String[] splitTags(String tags) {
        if (tags == null || tags.isBlank()) return new String[0];
        String[] raw = tags.split(",");
        List<String> out = new ArrayList<>(raw.length);
        for (String t : raw) {
            String v = t.trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out.toArray(new String[0]);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String[] getTags() {
        return tags;
    }

    public Integer getViews() {
        return views;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
