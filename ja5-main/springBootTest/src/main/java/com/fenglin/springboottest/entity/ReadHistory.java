package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 浏览历史实体：user_id + article_id 联合唯一。
 * 重复浏览同一篇文章时只更新 created_at（见 HistoryService.record），
 * 历史列表因此按「最近浏览」排序。
 */
@TableName("read_history")
public class ReadHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 浏览用户 id（user.id） */
    private Long userId;

    /** 被浏览文章 id（article.id） */
    private Long articleId;

    /** 本次浏览时间 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
