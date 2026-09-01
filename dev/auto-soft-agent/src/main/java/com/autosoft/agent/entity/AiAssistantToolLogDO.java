package com.autosoft.agent.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * AiAssistantToolLog实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("ai_assistant_tool_log")
public class AiAssistantToolLogDO extends BaseDO {

    private Long sessionId;
    private String toolName;
    private String argumentsJson;
    private String resultJson;
    private Integer success;
    private String errorMsg;
    private Integer durationMs;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    public void setArgumentsJson(String argumentsJson) {
        this.argumentsJson = argumentsJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }
}
