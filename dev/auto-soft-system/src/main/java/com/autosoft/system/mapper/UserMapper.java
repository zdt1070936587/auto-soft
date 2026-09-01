package com.autosoft.system.mapper;

import com.autosoft.system.entity.UserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * User数据访问。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
