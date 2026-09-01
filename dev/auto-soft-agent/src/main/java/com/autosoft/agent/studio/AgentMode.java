package com.autosoft.agent.studio;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;

import java.util.Set;

/**
 * 工作室工作级别：讨论 / 计划 / 开发。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public enum AgentMode {

    DISCUSS("discuss"),
    PLAN("plan"),
    DEVELOP("develop");

    public static final Set<String> READ_ONLY_TOOLS = Set.of(
            "ask_user", "get_current_schema", "preview_app", "get_workflow_graph", "validate_workflow");

    private final String code;

    AgentMode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AgentMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEVELOP;
        }
        for (AgentMode mode : values()) {
            if (mode.code.equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        throw new BizException(ResultCode.BAD_REQUEST, "无效的工作级别: " + raw);
    }

    public boolean allowsTool(String toolName) {
        if (this == DEVELOP) {
            return true;
        }
        return READ_ONLY_TOOLS.contains(toolName);
    }
}
