package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.AuthSupport;
import com.fenglin.springboottest.service.FavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 收藏接口（全部需要登录）。
 *
 * <pre>
 * POST   /api/favorites/{articleId}   收藏文章   -> { message }
 * DELETE /api/favorites/{articleId}   取消收藏   -> { message }
 * GET    /api/favorites?page=&size=   我的收藏（按收藏时间倒序）
 *   -> { page, size, total, pages, list: [ArticleItem] }
 * GET    /api/favorites/ids           已收藏的文章 id 列表
 * </pre>
 *
 * 未登录 / 令牌过期统一返回 401 { message }，前端收到后引导去登录。
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AuthSupport authSupport;

    public FavoriteController(FavoriteService favoriteService, AuthSupport authSupport) {
        this.favoriteService = favoriteService;
        this.authSupport = authSupport;
    }

    /** 收藏文章 */
    @PostMapping("/{articleId}")
    public Map<String, String> add(
            @PathVariable("articleId") Long articleId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        favoriteService.add(authSupport.requireUserId(auth), articleId);
        return Map.of("message", "已收藏");
    }

    /** 取消收藏 */
    @DeleteMapping("/{articleId}")
    public Map<String, String> remove(
            @PathVariable("articleId") Long articleId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        favoriteService.remove(authSupport.requireUserId(auth), articleId);
        return Map.of("message", "已取消收藏");
    }

    /** 我的收藏列表（分页） */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return favoriteService.list(authSupport.requireUserId(auth), page, size);
    }

    /** 当前用户已收藏的文章 id 列表（列表页标记收藏态） */
    @GetMapping("/ids")
    public Map<String, Object> favoritedIds(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", favoriteService.favoritedIds(authSupport.requireUserId(auth)));
        return resp;
    }
}
