package com.autosoft.system.log;

import com.autosoft.common.core.PageResult;
import com.autosoft.system.dto.OperLogQuery;
import com.autosoft.system.entity.OperLogDO;
import com.autosoft.system.mapper.OperLogMapper;
import com.autosoft.system.vo.OperLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志查询。仅超管入口调用。
 */
@Service
public class OperLogQueryService {

    private final OperLogMapper operLogMapper;

    public OperLogQueryService(OperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    public PageResult<OperLogVO> page(OperLogQuery query) {
        Page<OperLogDO> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<OperLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getModule()), OperLogDO::getModule, query.getModule());
        wrapper.like(StringUtils.hasText(query.getUsername()), OperLogDO::getUsername, query.getUsername());
        wrapper.orderByDesc(OperLogDO::getId);
        operLogMapper.selectPage(page, wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords().stream().map(this::toVo).toList());
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
        vo.setDetailJson(source.getDetailJson());
        vo.setCreatedAt(source.getCreatedAt());
        return vo;
    }
}
