package com.autosoft.system.log;

import com.autosoft.system.entity.PageVisitDO;
import com.autosoft.system.mapper.PageVisitMapper;
import com.autosoft.system.vo.PageVisitVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * 当前用户页面浏览记录查询（Assistant 专用）。强制 user_id 隔离。
 */
@Service
public class PageVisitUserQueryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final PageVisitMapper pageVisitMapper;

    public PageVisitUserQueryService(PageVisitMapper pageVisitMapper) {
        this.pageVisitMapper = pageVisitMapper;
    }

    public List<PageVisitVO> queryMine(Long userId, Instant from, Instant to,
                                       String path, String titleKeyword, int limit) {
        int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        LambdaQueryWrapper<PageVisitDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PageVisitDO::getUserId, userId);
        if (from != null) {
            wrapper.ge(PageVisitDO::getVisitedAt, from);
        }
        if (to != null) {
            wrapper.le(PageVisitDO::getVisitedAt, to);
        }
        if (StringUtils.hasText(path)) {
            wrapper.eq(PageVisitDO::getPath, path);
        }
        if (StringUtils.hasText(titleKeyword)) {
            wrapper.like(PageVisitDO::getPageTitle, titleKeyword);
        }
        wrapper.orderByDesc(PageVisitDO::getVisitedAt);
        wrapper.last("LIMIT " + capped);
        return pageVisitMapper.selectList(wrapper).stream().map(this::toVo).toList();
    }

    private PageVisitVO toVo(PageVisitDO source) {
        PageVisitVO vo = new PageVisitVO();
        vo.setId(source.getId());
        vo.setPath(source.getPath());
        vo.setRouteName(source.getRouteName());
        vo.setPageTitle(source.getPageTitle());
        vo.setVisitedAt(source.getVisitedAt());
        return vo;
    }
}
