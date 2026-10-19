package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 一级分类（分级分类搜索的第一级）
 *   例：前端 / 后端 / 设计 / 数据库 / 工程
 */
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名（展示用），唯一 */
    private String name;

    /** 分类英文名 / URL 别名，唯一 */
    private String slug;

    /** 排序权重，越小越靠前 */
    private Integer sort;

    private LocalDateTime createdAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
