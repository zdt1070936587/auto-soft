package com.autosoft.common.utils;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;

/**
 * 断言工具，失败时抛出 BizException。
 */
public final class AssertUtils {

    private AssertUtils() {
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new BizException(ResultCode.BAD_REQUEST, message);
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BizException(ResultCode.BAD_REQUEST, message);
        }
    }

    public static void notBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, message);
        }
    }
}
