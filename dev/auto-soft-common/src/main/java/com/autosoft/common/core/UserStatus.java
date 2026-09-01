package com.autosoft.common.core;

/**
 * 账号启用状态。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public enum UserStatus {

    DISABLED(0),
    ENABLED(1);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
