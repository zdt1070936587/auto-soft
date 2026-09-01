/**
 * 模块说明。
 *
 * @author zhaodt
 * @since 2026-08-20
 */
package com.autosoft.framework.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限码校验。SUPER_ADMIN 绕过。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    String value();
}
