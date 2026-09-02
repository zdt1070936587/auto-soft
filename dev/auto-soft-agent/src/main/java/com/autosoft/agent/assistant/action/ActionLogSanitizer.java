package com.autosoft.agent.assistant.action;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 助手工具日志敏感字段脱敏。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class ActionLogSanitizer {

    private static final Pattern PASSWORD_JSON = Pattern.compile(
            "(\"password\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE);

    public String sanitize(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        return PASSWORD_JSON.matcher(json).replaceAll("$1\"***\"");
    }
}
