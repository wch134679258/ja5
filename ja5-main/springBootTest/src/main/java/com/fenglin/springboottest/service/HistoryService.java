package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.dto.HistoryItem;
import com.fenglin.springboottest.entity.ReadHistory;
import com.fenglin.springboottest.exception.BusinessException;
import com.fenglin.springboottest.mapper.ArticleMapper;
import com.fenglin.springboottest.mapper.ReadHistoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 浏览历史服务：记录浏览 / 我的历史列表 / 删除单条 / 清空。
 *
 * <p>记录浏览是「写了就算成功」的弱操作：文章不存在才报 400，
 * 重复浏览只刷新时间，不返回错误，避免影响阅读主流程。</p>
 */
@Service
public class HistoryService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final ReadHistoryMapper historyMapper;
    private final ArticleMapper articleMapper;

    public HistoryService(ReadHistoryMapper historyMapper, ArticleMapper articleMapper) {
        this.historyMapper = historyMapper;
        this.articleMapper = articleMapper;
    }

    /**
     * 记录一次浏览。已存在则刷新浏览时间（保证「最近浏览」排序正确）。
     */
    public void record(Long userId, Long articleId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        ReadHistory exist = historyMapper.selectOne(new LambdaQueryWrapper<ReadHistory>()
                .eq(ReadHistory::getUserId, userId)
                .eq(ReadHistory::getArticleId, articleId));
        if (exist != null) {
            exist.setCreatedAt(LocalDateTime.now());
            historyMapper.updateById(exist);
            return;
        }
        ReadHistory h = new ReadHistory();
        h.setUserId(userId);
        h.setArticleId(articleId);
        h.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(h);
    }

    /**
     * 我的浏览历史（分页，按浏览时间倒序）。
     *
     * @return { page, size, total, pages, list: [HistoryItem] }
     */
    public Map<String, Object> list(Long userId, Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Page<HistoryItem> result = historyMapper.selectHistoryArticles(new Page<>(p, s), userId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("page", result.getCurrent());
        resp.put("size", result.getSize());
        resp.put("total", result.getTotal());
        resp.put("pages", result.getPages());
        resp.put("list", result.getRecords());
        return resp;
    }

    /** 删除单条浏览记录。记录不存在返回 400，避免前端状态错乱。 */
    public void remove(Long userId, Long articleId) {
        int rows = historyMapper.delete(new LambdaQueryWrapper<ReadHistory>()
                .eq(ReadHistory::getUserId, userId)
                .eq(ReadHistory::getArticleId, articleId));
        if (rows == 0) {
            throw new BusinessException("浏览记录不存在");
        }
    }

    /** 清空浏览历史，返回被清掉的条数。 */
    public int clear(Long userId) {
        return historyMapper.delete(new LambdaQueryWrapper<ReadHistory>()
                .eq(ReadHistory::getUserId, userId));
    }

    /** 某用户的浏览文章数（个人中心统计用） */
    public long count(Long userId) {
        return historyMapper.selectCount(new LambdaQueryWrapper<ReadHistory>()
                .eq(ReadHistory::getUserId, userId));
    }
}
