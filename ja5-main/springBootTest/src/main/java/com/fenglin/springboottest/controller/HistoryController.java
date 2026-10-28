package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.AuthSupport;
import com.fenglin.springboottest.service.HistoryService;
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
 * 浏览历史接口（全部需要登录）。
 *
 * <pre>
 * POST   /api/history/{articleId}     记录一次浏览（重复浏览只刷新时间） -> { message }
 * GET    /api/history?page=&size=     我的浏览历史（按浏览时间倒序）
 *   -> { page, size, total, pages, list: [HistoryItem] }
 * DELETE /api/history/{articleId}     删除一条浏览记录 -> { message }
 * DELETE /api/history                 清空浏览历史     -> { message, removed }
 * </pre>
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;
    private final AuthSupport authSupport;

    public HistoryController(HistoryService historyService, AuthSupport authSupport) {
        this.historyService = historyService;
        this.authSupport = authSupport;
    }

    /** 记录一次浏览 */
    @PostMapping("/{articleId}")
    public Map<String, String> record(
            @PathVariable("articleId") Long articleId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        historyService.record(authSupport.requireUserId(auth), articleId);
        return Map.of("message", "已记录");
    }

    /** 我的浏览历史（分页） */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return historyService.list(authSupport.requireUserId(auth), page, size);
    }

    /** 删除单条浏览记录 */
    @DeleteMapping("/{articleId}")
    public Map<String, String> remove(
            @PathVariable("articleId") Long articleId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        historyService.remove(authSupport.requireUserId(auth), articleId);
        return Map.of("message", "已删除");
    }

    /** 清空浏览历史 */
    @DeleteMapping
    public Map<String, Object> clear(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        int removed = historyService.clear(authSupport.requireUserId(auth));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "已清空浏览历史");
        resp.put("removed", removed);
        return resp;
    }
}
