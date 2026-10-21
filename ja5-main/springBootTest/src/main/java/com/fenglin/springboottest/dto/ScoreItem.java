package com.fenglin.springboottest.dto;

import com.fenglin.springboottest.entity.GameScore;

import java.time.format.DateTimeFormatter;

/**
 * 星空贪吃蛇 · 榜单单项（回给前端展示）。
 *
 * <p>与 ArticleItem 保持同一套约定：时间格式化为字符串再下发，
 * 不让前端解析时间格式。</p>
 */
public record ScoreItem(
        int rank,
        String nickname,
        int score,
        int length,
        int duration,
        String createdAt) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /** GameScore -> 榜单项，rank 由调用方按排序结果填充 */
    public static ScoreItem from(GameScore g, int rank) {
        return new ScoreItem(
                rank,
                g.getNickname() == null ? "" : g.getNickname(),
                g.getScore() == null ? 0 : g.getScore(),
                g.getLength() == null ? 0 : g.getLength(),
                g.getDuration() == null ? 0 : g.getDuration(),
                g.getCreatedAt() == null ? "" : g.getCreatedAt().format(FMT));
    }
}
