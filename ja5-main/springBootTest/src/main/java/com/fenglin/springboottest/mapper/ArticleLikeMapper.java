package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章点赞 Mapper：单表操作全部走 BaseMapper。
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {
}
