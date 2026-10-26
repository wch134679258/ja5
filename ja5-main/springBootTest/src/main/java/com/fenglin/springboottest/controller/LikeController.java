package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.AuthSupport;
import com.fenglin.springboottest.service.LikeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文章点赞接口。
 *
 * <pre>
 * POST   /api/likes/{articleId}   点赞     -> { liked: true,  likes }
 * DELETE /api/likes/{articleId}   取消点赞 -> { liked: false, likes }
 * GET    /api/likes/ids           我点赞过的文章 id 列表 -> { list: [id] }
 * GET    /api/likes/{articleId}/count  某文章的点赞数（公开）
 * </pre>
 *
 * 点赞 / 取消点赞需要登录；点赞数公开，未登录也能看到热度。
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeService likeService;
    private final AuthSupport authSupport;

    public LikeController(LikeService likeService, AuthSupport authSupport) {
        this.likeService = likeService;
        this.authSupport = authSupport;
    }

    /** 点赞 */
    @PostMapping("/{articleId}")
    public Map<String, Object> add(
            @PathVariable("articleId") Long articleId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return likeService.add(authSupport.requireUserId(auth), articleId);
    }

    /** 取消点赞 */
    @DeleteMapping("/{articleId}")
    public Map<String, Object> remove(
            @PathVariable("articleId") Long articleId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return likeService.remove(authSupport.requireUserId(auth), articleId);
    }

    /** 我点赞过的文章 id 列表 */
    @GetMapping("/ids")
    public Map<String, Object> likedIds(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", likeService.likedIds(authSupport.requireUserId(auth)));
        return resp;
    }

    /** 某文章的点赞总数（公开） */
    @GetMapping("/{articleId}/count")
    public Map<String, Object> count(@PathVariable("articleId") Long articleId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("likes", likeService.countOfArticle(articleId));
        return resp;
    }
}
