package com.autosoft.framework.security;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 读取当前用户。Controller 不要自行解析 token。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long currentUserId() {
        LoginUser user = currentUser();
        return user == null || user.getUserId() == null ? 0L : user.getUserId();
    }

    public static LoginUser requireUser() {
        LoginUser user = currentUser();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }
}
