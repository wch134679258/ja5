package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.ArticleItem;
import com.fenglin.springboottest.entity.Article;
import com.fenglin.springboottest.exception.BusinessException;
import com.fenglin.springboottest.mapper.ArticleMapper;
import com.fenglin.springboottest.mapper.FavoriteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章阅读服务：详情（浏览量自增 + 收藏状态）、相关文章推荐。
 */
@Service
public class ArticleService {

    /** 相关文章展示条数 */
    private static final int RELATED_LIMIT = 4;

    private final ArticleMapper articleMapper;
    private final FavoriteMapper favoriteMapper;

    public ArticleService(ArticleMapper articleMapper, FavoriteMapper favoriteMapper) {
        this.articleMapper = articleMapper;
        this.favoriteMapper = favoriteMapper;
    }

    /**
     * 文章详情。
     *
     * <p>打开详情即浏览量 +1（SQL 自增，避免并发覆盖）；
     * 登录用户额外返回 favorited（当前是否已收藏该文），未登录为 false。
     *
     * @param id        文章 id
     * @param viewerId  当前登录用户 id，未登录传 null
     */
    public Map<String, Object> detail(Long id, Long viewerId) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        articleMapper.incrementViews(id);

        boolean favorited = false;
        if (viewerId != null) {
            favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<com.fenglin.springboottest.entity.Favorite>()
                    .eq(com.fenglin.springboottest.entity.Favorite::getUserId, viewerId)
                    .eq(com.fenglin.springboottest.entity.Favorite::getArticleId, id)) > 0;
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", a.getId());
        resp.put("title", a.getTitle());
        resp.put("summary", a.getSummary());
        resp.put("content", a.getContent());
        resp.put("author", a.getAuthor());
        resp.put("categoryId", a.getCategoryId());
        resp.put("category", a.getCategory());
        resp.put("tags", ArticleItem.splitTags(a.getTags()));
        // 本地已自增 1，直接返回 +1 后的浏览量，无需再查一次库
        resp.put("views", (a.getViews() == null ? 0 : a.getViews()) + 1);
        resp.put("createdAt", a.getCreatedAt() == null ? "" : a.getCreatedAt().format(ArticleItem.FMT));
        resp.put("updatedAt", a.getUpdatedAt() == null ? "" : a.getUpdatedAt().format(ArticleItem.FMT));
        resp.put("favorited", favorited);
        return resp;
    }

    /**
     * 相关文章：优先推共享标签的文章（共同标签越多越相关）；
     * 一篇共享标签的文章都没有时，退化为同分类按热度推荐。
     */
    public List<ArticleItem> related(Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        List<Article> hits = articleMapper.selectRelatedByTags(id, RELATED_LIMIT);
        if (hits.isEmpty() && a.getCategoryId() != null) {
            hits = articleMapper.selectRelatedByCategory(id, a.getCategoryId(), RELATED_LIMIT);
        }
        List<ArticleItem> items = new ArrayList<>(hits.size());
        for (Article h : hits) {
            items.add(ArticleItem.from(h));
        }
        return items;
    }
}
