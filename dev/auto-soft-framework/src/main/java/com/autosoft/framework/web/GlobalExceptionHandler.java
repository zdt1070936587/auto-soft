package com.autosoft.framework.web;

import com.autosoft.common.core.R;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.framework.security.SecurityExceptionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理。记录完整堆栈，响应体不返回堆栈。
 */
@RestControllerAdvice(basePackages = "com.autosoft")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex) {
        log.warn("business error, code={}, msg={}", ex.getResultCode().getCode(), ex.getMessage());
        return ResponseEntity.status(SecurityExceptionHandler.httpStatusOf(ex.getResultCode()))
                .body(withTrace(R.fail(ex.getResultCode(), ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = joinFieldErrors(ex.getBindingResult().getFieldErrors());
        log.warn("argument not valid: {}", msg);
        return ResponseEntity.badRequest().body(withTrace(R.fail(ResultCode.BAD_REQUEST, msg)));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException ex) {
        String msg = joinFieldErrors(ex.getBindingResult().getFieldErrors());
        log.warn("bind error: {}", msg);
        return ResponseEntity.badRequest().body(withTrace(R.fail(ResultCode.BAD_REQUEST, msg)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("constraint violation: {}", msg);
        return ResponseEntity.badRequest().body(withTrace(R.fail(ResultCode.BAD_REQUEST, msg)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        log.error("unhandled exception", ex);
        return ResponseEntity.internalServerError().body(withTrace(R.fail(ResultCode.SERVER_ERROR)));
    }

    private R<Void> withTrace(R<Void> body) {
        body.setTraceId(TraceIds.current());
        return body;
    }

    private String joinFieldErrors(java.util.List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }
}
