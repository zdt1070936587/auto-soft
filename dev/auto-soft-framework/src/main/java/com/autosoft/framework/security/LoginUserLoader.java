package com.autosoft.framework.security;

/**
 * 按用户 ID 加载登录上下文。由 system 模块实现，避免 framework 依赖业务表。
 */
public interface LoginUserLoader {

    /**
     * @return 用户不存在、已删除或停用时返回 null
     */
    LoginUser loadById(Long userId);
}
