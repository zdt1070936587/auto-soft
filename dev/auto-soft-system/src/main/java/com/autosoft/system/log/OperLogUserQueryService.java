package com.autosoft.system.log;

import com.autosoft.system.entity.OperLogDO;
import com.autosoft.system.mapper.OperLogMapper;
import com.autosoft.system.vo.OperLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * 当前用户操作日志查询（Assistant 专用）。强制 user_id 隔离。
 */
@Service
public class OperLogUserQueryService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final OperLogMapper operLogMapper;
    private final OperLogDisplaySanitizer sanitizer;

    public OperLogUserQueryService(OperLogMapper operLogMapper, OperLogDisplaySanitizer sanitizer) {
        this.operLogMapper = operLogMapper;
        this.sanitizer = sanitizer;
    }

    public List<OperLogVO> queryMine(Long userId, Instant from, Instant to,
                                     String module, String action, int limit) {
        int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        LambdaQueryWrapper<OperLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperLogDO::getUserId, userId);
        if (from != null) {
            wrapper.ge(OperLogDO::getCreatedAt, from);
        }
        if (to != null) {
            wrapper.le(OperLogDO::getCreatedAt, to);
        }
        wrapper.eq(StringUtils.hasText(module), OperLogDO::getModule, module);
        wrapper.eq(StringUtils.hasText(action), OperLogDO::getAction, action);
        wrapper.orderByDesc(OperLogDO::getCreatedAt);
        wrapper.last("LIMIT " + capped);
        return operLogMapper.selectList(wrapper).stream().map(this::toVo).toList();
    }

    public List<OperLogVO> timelineMine(Long userId, Instant from, Instant to, int paddingMinutes, int limit) {
        int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        Instant paddedFrom = from == null ? null : from.minusSeconds(paddingMinutes * 60L);
        Instant paddedTo = to == null ? null : to.plusSeconds(paddingMinutes * 60L);
        LambdaQueryWrapper<OperLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperLogDO::getUserId, userId);
        if (paddedFrom != null) {
            wrapper.ge(OperLogDO::getCreatedAt, paddedFrom);
        }
        if (paddedTo != null) {
            wrapper.le(OperLogDO::getCreatedAt, paddedTo);
        }
        wrapper.orderByAsc(OperLogDO::getCreatedAt);
        wrapper.last("LIMIT " + capped);
        return operLogMapper.selectList(wrapper).stream().map(this::toVo).toList();
    }

    private OperLogVO toVo(OperLogDO source) {
        OperLogVO vo = new OperLogVO();
        vo.setId(source.getId());
        vo.setUserId(source.getUserId());
        vo.setUsername(source.getUsername());
        vo.setModule(source.getModule());
        vo.setAction(source.getAction());
        vo.setBizId(source.getBizId());
        vo.setSuccess(source.getSuccess());
        vo.setIp(source.getIp());
        vo.setCostMs(source.getCostMs());
        vo.setDetailJson(sanitizer.sanitizeDetail(source.getDetailJson()));
        vo.setCreatedAt(source.getCreatedAt());
        return vo;
    }
}
