package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.dto.ScoreItem;
import com.fenglin.springboottest.dto.ScoreRequest;
import com.fenglin.springboottest.entity.GameScore;
import com.fenglin.springboottest.exception.BusinessException;
import com.fenglin.springboottest.mapper.GameScoreMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 星空贪吃蛇 · 成绩服务：提交分数 / 排行榜 / 个人最好成绩。
 *
 * <p>提交分数必须已登录（userId 由 Controller 从 JWT 解析）；
 * 排行榜公开访问，用于把成绩晒给所有探险家。</p>
 *
 * <p>排名口径：并列同名次（「严格高于我的成绩条数 + 1」），
 * 与榜单展示顺序一致：分数高的在前，同分比用时，再相同比提交先后。</p>
 */
@Service
public class GameScoreService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final GameScoreMapper gameScoreMapper;

    public GameScoreService(GameScoreMapper gameScoreMapper) {
        this.gameScoreMapper = gameScoreMapper;
    }

    /**
     * 提交一局成绩。
     *
     * @param userId   当前用户 id
     * @param nickname 当前用户昵称（榜单快照）
     * @param req      本局成绩（已完成服务端范围校验）
     * @return { message, score, best, isNewBest, rank, playCount, leaderboard }
     */
    public Map<String, Object> submit(Long userId, String nickname, ScoreRequest req) {
        // 先查历史最好成绩（必须在 insert 之前查，否则首局查到的就是本局自己，
        // isNewBest 会永远为 false）
        GameScore prevBest = selectUserBest(userId);

        GameScore g = new GameScore();
        g.setUserId(userId);
        g.setNickname(nickname == null ? "" : nickname);
        g.setScore(req.getScore());
        g.setLength(req.getLength() == null ? 0 : req.getLength());
        g.setDuration(req.getDuration() == null ? 0 : req.getDuration());
        gameScoreMapper.insert(g);

        // 本局成绩在全部成绩中的名次（并列同名次）
        long higher = countHigherThan(req.getScore());
        int rank = (int) higher + 1;

        int best = prevBest == null ? req.getScore() : Math.max(prevBest.getScore(), req.getScore());
        boolean isNewBest = prevBest == null || req.getScore() > prevBest.getScore();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", isNewBest ? "新纪录！成绩已入榜" : "成绩已记录");
        resp.put("score", req.getScore());
        resp.put("best", best);
        resp.put("isNewBest", isNewBest);
        resp.put("rank", rank);
        resp.put("playCount", countPlays(userId));
        resp.put("leaderboard", leaderboardItems(DEFAULT_LIMIT));
        return resp;
    }

    /**
     * 排行榜 Top N（公开接口，无需登录）。
     *
     * @param limit 取前几名，1~50，缺省 10
     */
    public List<ScoreItem> leaderboard(Integer limit) {
        return leaderboardItems(limit);
    }

    /**
     * 我的最好成绩（需登录）。没有成绩时 best 为 0、rank 为 0。
     *
     * @return { best, rank, playCount }
     */
    public Map<String, Object> myBest(Long userId) {
        GameScore bestRow = selectUserBest(userId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("best", bestRow == null ? 0 : bestRow.getScore());
        resp.put("rank", bestRow == null ? 0 : (int) countHigherThan(bestRow.getScore()) + 1);
        resp.put("playCount", countPlays(userId));
        return resp;
    }

    /* ---------- 内部查询 ---------- */

    private List<ScoreItem> leaderboardItems(Integer limit) {
        int n = (limit == null || limit < 1) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<GameScore> rows = gameScoreMapper.selectList(new LambdaQueryWrapper<GameScore>()
                .orderByDesc(GameScore::getScore)
                .orderByAsc(GameScore::getDuration)
                .orderByAsc(GameScore::getId)
                .last("limit " + n));

        List<ScoreItem> items = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            items.add(ScoreItem.from(rows.get(i), i + 1));
        }
        return items;
    }

    /** 个人最好成绩：分数优先，同分比用时 */
    private GameScore selectUserBest(Long userId) {
        return gameScoreMapper.selectOne(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getUserId, userId)
                .orderByDesc(GameScore::getScore)
                .orderByAsc(GameScore::getDuration)
                .last("limit 1"));
    }

    /** 严格高于给定分数的成绩条数（用于计算并列同名次） */
    private long countHigherThan(int score) {
        return gameScoreMapper.selectCount(new LambdaQueryWrapper<GameScore>()
                .gt(GameScore::getScore, score));
    }

    /** 该用户累计提交过的局数 */
    private long countPlays(Long userId) {
        return gameScoreMapper.selectCount(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getUserId, userId));
    }
}
