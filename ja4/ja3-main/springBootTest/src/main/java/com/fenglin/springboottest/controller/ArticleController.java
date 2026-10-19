package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.AuthSupport;
import com.fenglin.springboottest.dto.ArticleItem;
import com.fenglin.springboottest.service.ArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 文章阅读接口。
 *
 * <pre>
 * GET /api/article/{id}            文章详情（浏览量 +1）
 *     Authorization: Bearer &lt;token&gt;  可选；携带时额外返回 favorited（是否已收藏）
 *   -> { id, title, summary, content, author, categoryId, category,
 *        tags[], views, createdAt, updatedAt, favorited }
 *
 * GET /api/article/{id}/related    相关文章（共享标签优先，同分类兜底）
 *   -> [ ArticleItem, ... ] 最多 4 条
 * </pre>
 *
 * 说明：详情接口公开，未登录也能阅读；收藏态仅对登录用户返回。
 */
@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleService articleService;
    private final AuthSupport authSupport;

    public ArticleController(ArticleService articleService, AuthSupport authSupport) {
        this.articleService = articleService;
        this.authSupport = authSupport;
    }

    /** 文章详情：浏览量自增；带令牌时附带收藏状态 */
    @GetMapping("/article/{id}")
    public Map<String, Object> detail(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return articleService.detail(id, authSupport.optionalUserId(auth));
    }

    /** 相关文章推荐 */
    @GetMapping("/article/{id}/related")
    public List<ArticleItem> related(@PathVariable("id") Long id) {
        return articleService.related(id);
    }
}
