package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 星空贪吃蛇 · 提交一局成绩的请求体。
 *
 * <p>上限值同时是防滥用兜底：分数、长度、时长都做服务端硬校验，
 * 防止客户端伪造离谱数值污染排行榜。</p>
 */
public class ScoreRequest {

    /** 本局得分 */
    @NotNull(message = "分数不能为空")
    @Min(value = 0, message = "分数不能为负")
    @Max(value = 1000000, message = "分数超出上限")
    private Integer score;

    /** 结束时蛇身长度（节） */
    @Min(value = 0, message = "蛇身长度不能为负")
    @Max(value = 100000, message = "蛇身长度超出上限")
    private Integer length;

    /** 本局时长（秒） */
    @Min(value = 0, message = "时长不能为负")
    @Max(value = 86400, message = "时长超出上限")
    private Integer duration;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}
