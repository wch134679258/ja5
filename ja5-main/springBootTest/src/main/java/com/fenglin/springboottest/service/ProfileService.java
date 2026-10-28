package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人中心服务：把散落在收藏 / 历史 / 点赞 / 游戏服务里的统计汇总成一个接口，
 * 避免个人中心页为了几个数字发四五次请求。
 */
@Service
public class ProfileService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserMapper userMapper;
    private final FavoriteService favoriteService;
    private final HistoryService historyService;
    private final LikeService likeService;
    private final GameScoreService gameScoreService;

    public ProfileService(UserMapper userMapper,
                          FavoriteService favoriteService,
                          HistoryService historyService,
                          LikeService likeService,
                          GameScoreService gameScoreService) {
        this.userMapper = userMapper;
        this.favoriteService = favoriteService;
        this.historyService = historyService;
        this.likeService = likeService;
        this.gameScoreService = gameScoreService;
    }

    /**
     * 个人中心汇总数据。
     *
     * @return { user: { id, name }, stats: { favorites, history, likes, views }, game: { best, rank, playCount } }
     */
    public Map<String, Object> profile(Long userId) {
        User user = userMapper.selectById(userId);

        Map<String, Object> userVo = new LinkedHashMap<>();
        userVo.put("id", userId);
        // 用户可能已被清理，昵称兜底成「探险家」
        userVo.put("name", user == null ? "探险家" : user.getName());
        userVo.put("joinedAt", user == null || user.getCreatedAt() == null
                ? "" : user.getCreatedAt().format(FMT));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("favorites", favoriteService.count(userId));
        stats.put("history", historyService.count(userId));
        stats.put("likes", likeService.countOfUser(userId));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("user", userVo);
        resp.put("stats", stats);
        // 游戏成绩：没有玩过时 myBest 内部会返回 best=0 / rank=0 / playCount=0
        resp.put("game", gameScoreService.myBest(userId));
        return resp;
    }
}
