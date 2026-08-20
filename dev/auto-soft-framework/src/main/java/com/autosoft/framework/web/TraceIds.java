package com.autosoft.framework.web;

import org.slf4j.MDC;

/**
 * 当前请求 traceId。
 */
public final class TraceIds {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    private TraceIds() {
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
