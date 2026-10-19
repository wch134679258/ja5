package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.dto.ArticleItem;
import com.fenglin.springboottest.entity.Article;
import com.fenglin.springboottest.entity.Favorite;
import com.fenglin.springboottest.exception.BusinessException;
import com.fenglin.springboottest.mapper.ArticleMapper;
import com.fenglin.springboottest.mapper.FavoriteMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏服务：收藏 / 取消收藏 / 我的收藏列表。
 *
 * <p>所有方法都要求已登录（userId 由 Controller 从 JWT 解析，缺失即 401）。
 */
@Service
public class FavoriteService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final FavoriteMapper favoriteMapper;
    private final ArticleMapper articleMapper;

    public FavoriteService(FavoriteMapper favoriteMapper, ArticleMapper articleMapper) {
        this.favoriteMapper = favoriteMapper;
        this.articleMapper = articleMapper;
    }

    /** 当前用户是否已收藏某文章（详情页 / 列表页展示收藏态） */
    public boolean isFavorited(Long userId, Long articleId) {
        return favoriteMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getArticleId, articleId)) > 0;
    }

    /**
     * 收藏文章。文章不存在返回 400；重复收藏返回 400（前端正常不会触发，双重点击等极端场景兜底）。
     */
    @Transactional
    public void add(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setArticleId(articleId);
        try {
            favoriteMapper.insert(fav);
        } catch (DuplicateKeyException e) {
            // 联合唯一索引兜底：已收藏过
            throw new BusinessException("已经收藏过这篇文章了");
        }
    }

    /** 取消收藏。未收藏时返回 400，避免前端误删后状态错乱。 */
    @Transactional
    public void remove(Long userId, Long articleId) {
        int rows = favoriteMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getArticleId, articleId));
        if (rows == 0) {
            throw new BusinessException("还没有收藏这篇文章");
        }
    }

    /**
     * 我的收藏列表（分页，按收藏时间倒序）。
     *
     * @return { page, size, total, pages, list: [ArticleItem] }
     */
    public Map<String, Object> list(Long userId, Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Page<Article> result = favoriteMapper.selectFavoriteArticles(new Page<>(p, s), userId);

        List<ArticleItem> items = new ArrayList<>(result.getRecords().size());
        for (Article a : result.getRecords()) {
            items.add(ArticleItem.from(a));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("page", result.getCurrent());
        resp.put("size", result.getSize());
        resp.put("total", result.getTotal());
        resp.put("pages", result.getPages());
        resp.put("list", items);
        return resp;
    }

    /**
     * 当前用户已收藏的文章 id 列表。供首页 / 发现页在文章卡片上标记收藏态，
     * 只查一列，比拉全量收藏列表轻量。
     */
    public List<Long> favoritedIds(Long userId) {
        return favoriteMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .select(Favorite::getArticleId))
                .stream()
                .map(Favorite::getArticleId)
                .toList();
    }
}
