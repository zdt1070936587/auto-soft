package com.autosoft.meta.app;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;

public enum AppKind {

    ADMIN("admin"),
    FRONTEND("frontend"),
    H5("h5");

    private final String code;

    AppKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public boolean needsDdl() {
        return this == ADMIN;
    }

    public static AppKind from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ADMIN;
        }
        for (AppKind kind : values()) {
            if (kind.code.equalsIgnoreCase(raw)) {
                return kind;
            }
        }
        throw new BizException(ResultCode.BAD_REQUEST, "无效的应用类型: " + raw);
    }
}
