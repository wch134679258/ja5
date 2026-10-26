package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.dto.HistoryItem;
import com.fenglin.springboottest.entity.ReadHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 浏览历史 Mapper。
 *
 * <p>历史列表要展示文章信息，因此 JOIN article 查询，
 * 结果映射到 {@link HistoryItem}（含浏览时间）。</p>
 */
@Mapper
public interface ReadHistoryMapper extends BaseMapper<ReadHistory> {

    /**
     * 分页查询某用户的浏览历史，按浏览时间倒序（最近看过在前）。
     *
     * @param page   分页对象
     * @param userId 用户 id
     */
    Page<HistoryItem> selectHistoryArticles(Page<HistoryItem> page, @Param("userId") Long userId);
}
