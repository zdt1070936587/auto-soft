package com.autosoft.system.mapper;

import com.autosoft.system.entity.OperLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * OperLog数据访问。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Mapper
public interface OperLogMapper extends BaseMapper<OperLogDO> {
}
