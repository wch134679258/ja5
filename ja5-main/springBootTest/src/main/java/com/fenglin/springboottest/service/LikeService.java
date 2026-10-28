package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.ArticleLike;
import com.fenglin.springboottest.exception.BusinessException;
import com.fenglin.springboottest.mapper.ArticleLikeMapper;
import com.fenglin.springboottest.mapper.ArticleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章点赞服务：点赞 / 取消点赞 / 我点赞过的 id 列表 / 点赞数。
 */
@Service
public class LikeService {

    private final ArticleLikeMapper likeMapper;
    private final ArticleMapper articleMapper;

    public LikeService(ArticleLikeMapper likeMapper, ArticleMapper articleMapper) {
        this.likeMapper = likeMapper;
        this.articleMapper = articleMapper;
    }

    /** 当前用户是否已点赞（详情页展示点赞态） */
    public boolean isLiked(Long userId, Long articleId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId)) > 0;
    }

    /**
     * 点赞。文章不存在 / 重复点赞返回 400。
     *
     * @return { liked, likes }
     */
    public Map<String, Object> add(Long userId, Long articleId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        ArticleLike like = new ArticleLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        like.setCreatedAt(LocalDateTime.now());
        try {
            likeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("已经点赞过这篇文章了");
        }
        return stateOf(articleId, true);
    }

    /** 取消点赞。未点赞时返回 400。 */
    public Map<String, Object> remove(Long userId, Long articleId) {
        int rows = likeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId));
        if (rows == 0) {
            throw new BusinessException("还没有点赞这篇文章");
        }
        return stateOf(articleId, false);
    }

    /** 我点赞过的文章 id 列表（列表页标记点赞态） */
    public List<Long> likedIds(Long userId) {
        return likeMapper.selectList(new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, userId)
                        .select(ArticleLike::getArticleId))
                .stream()
                .map(ArticleLike::getArticleId)
                .toList();
    }

    /** 某文章的点赞总数（详情页展示） */
    public long countOfArticle(Long articleId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId));
    }

    /** 某用户点赞过的文章数（个人中心统计用） */
    public long countOfUser(Long userId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId));
    }

    /** 统一返回点赞后的状态，前端拿到即可直接更新 UI，无需再发一次查询 */
    private Map<String, Object> stateOf(Long articleId, boolean liked) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("liked", liked);
        resp.put("likes", countOfArticle(articleId));
        return resp;
    }
}
