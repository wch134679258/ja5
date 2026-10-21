package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.AuthSupport;
import com.fenglin.springboottest.dto.ScoreItem;
import com.fenglin.springboottest.dto.ScoreRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.exception.UnauthorizedException;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.GameScoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 星空贪吃蛇 · 成绩接口。
 *
 * <pre>
 * POST /api/game/scores        { score, length, duration }  提交成绩（需登录）
 *   -> { message, score, best, isNewBest, rank, playCount, leaderboard }
 * GET  /api/game/leaderboard   ?limit=10                    排行榜（公开）
 *   -> { list: [ScoreItem] }
 * GET  /api/game/my-best                                       我的最好成绩（需登录）
 *   -> { best, rank, playCount }
 * </pre>
 *
 * 未登录 / 令牌过期统一返回 401 { message }，前端引导去登录。
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameScoreService gameScoreService;
    private final AuthSupport authSupport;
    private final UserMapper userMapper;

    public GameController(GameScoreService gameScoreService, AuthSupport authSupport,
                          UserMapper userMapper) {
        this.gameScoreService = gameScoreService;
        this.authSupport = authSupport;
        this.userMapper = userMapper;
    }

    /** 提交一局成绩：昵称从服务端按 userId 反查，客户端伪造不了 */
    @PostMapping("/scores")
    public Map<String, Object> submit(
            @Valid @RequestBody ScoreRequest req,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long uid = authSupport.requireUserId(auth);
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new UnauthorizedException("用户不存在或已被注销");
        }
        return gameScoreService.submit(uid, user.getName(), req);
    }

    /** 排行榜 Top N（公开，无需登录） */
    @GetMapping("/leaderboard")
    public Map<String, Object> leaderboard(
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<ScoreItem> list = gameScoreService.leaderboard(limit);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", list);
        return resp;
    }

    /** 我的最好成绩与排名 */
    @GetMapping("/my-best")
    public Map<String, Object> myBest(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return gameScoreService.myBest(authSupport.requireUserId(auth));
    }
}
