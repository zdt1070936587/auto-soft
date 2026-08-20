package com.autosoft.common.exception;

import com.autosoft.common.core.ResultCode;

/**
 * 业务异常。由 Service / Manager 抛出，GlobalExceptionHandler 统一转换为 R。
 */
public class BizException extends RuntimeException {

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
