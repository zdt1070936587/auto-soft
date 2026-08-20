package com.autosoft.agent.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ai_message")
public class AiMessageDO extends BaseDO {

    private Long sessionId;
    private String role;
    private String content;
    private String toolName;
    private String toolCallId;
    private Integer tokens;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }
}
