package com.autosoft.framework.security;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 校验 @RequiresPermission。
 */
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requiresPermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        LoginUser user = SecurityUtils.requireUser();
        if (user.isSuperAdmin()) {
            return joinPoint.proceed();
        }
        String permission = requiresPermission.value();
        if (user.getPermissions() == null || !user.getPermissions().contains(permission)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限");
        }
        return joinPoint.proceed();
    }
}
