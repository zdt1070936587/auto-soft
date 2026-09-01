package com.autosoft.common.core;

/**
 * 统一 API 返回包装。Controller 必须返回本类型，禁止裸 Map。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class R<T> {

    private Integer code;
    private String msg;
    private T data;
    private String traceId;

    public R() {
    }

    public R(Integer code, String msg, T data, String traceId) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return fail(resultCode, resultCode.getMsg());
    }

    public static <T> R<T> fail(ResultCode resultCode, String msg) {
        return new R<>(resultCode.getCode(), msg, null, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
