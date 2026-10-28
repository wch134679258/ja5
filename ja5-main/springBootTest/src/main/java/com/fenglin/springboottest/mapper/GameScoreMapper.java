package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.GameScore;
import org.apache.ibatis.annotations.Mapper;

/**
 * 星空贪吃蛇 · 分数 Mapper。
 *
 * <p>排行榜 / 个人最好成绩都走 MyBatis-Plus 的 Lambda 查询即可，
 * 排名由 Service 层用「严格高于我的成绩条数 + 1」计算（并列同名次）。</p>
 */
@Mapper
public interface GameScoreMapper extends BaseMapper<GameScore> {
}
