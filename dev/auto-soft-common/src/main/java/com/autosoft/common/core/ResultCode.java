package com.autosoft.common.core;

/**
 * 统一业务状态码。成功固定为 0，前端以 code === 0 判断成功。
 */
public enum ResultCode {

    SUCCESS(0, "ok"),
    FAIL(1, "fail"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    USERNAME_PASSWORD_ERROR(401, "用户名或密码错误"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    SERVER_ERROR(500, "server error");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
