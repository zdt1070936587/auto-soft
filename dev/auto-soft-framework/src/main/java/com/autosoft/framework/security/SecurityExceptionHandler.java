package com.autosoft.framework.security;

import com.autosoft.common.core.R;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 401/403 输出统一 R，不返回默认登录 HTML。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    private final JsonMapper jsonMapper;

    public SecurityExceptionHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ResultCode.UNAUTHORIZED, ResultCode.UNAUTHORIZED.getMsg());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ResultCode.FORBIDDEN, "无权限");
    }

    public static void writeBiz(HttpServletResponse response, JsonMapper jsonMapper, BizException ex)
            throws IOException {
        ResultCode code = ex.getResultCode();
        HttpStatus status = httpStatusOf(code);
        write(response, jsonMapper, status, code, ex.getMessage());
    }

    private void write(HttpServletResponse response, HttpStatus status, ResultCode code, String msg)
            throws IOException {
        write(response, jsonMapper, status, code, msg);
    }

    private static void write(HttpServletResponse response, JsonMapper jsonMapper, HttpStatus status,
                              ResultCode code, String msg) throws IOException {
        if (response.isCommitted()) {
            log.warn("response already committed, skip writing R");
            return;
        }
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(R.fail(code, msg)));
    }

    public static HttpStatus httpStatusOf(ResultCode code) {
        int value = code.getCode();
        if (value == ResultCode.UNAUTHORIZED.getCode() || value == ResultCode.USERNAME_PASSWORD_ERROR.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (value == ResultCode.FORBIDDEN.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (value == ResultCode.BAD_REQUEST.getCode()) {
            return HttpStatus.BAD_REQUEST;
        }
        if (value == ResultCode.NOT_FOUND.getCode()) {
            return HttpStatus.NOT_FOUND;
        }
        if (value == ResultCode.SERVER_ERROR.getCode()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }
}
