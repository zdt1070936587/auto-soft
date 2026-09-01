package com.autosoft.agent.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * AiAssistantSession实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("ai_assistant_session")
public class AiAssistantSessionDO extends BaseDO {

    private Long userId;
    private String title;
    private String status;
    private Long tokenInput;
    private Long tokenOutput;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTokenInput() {
        return tokenInput;
    }

    public void setTokenInput(Long tokenInput) {
        this.tokenInput = tokenInput;
    }

    public Long getTokenOutput() {
        return tokenOutput;
    }

    public void setTokenOutput(Long tokenOutput) {
        this.tokenOutput = tokenOutput;
    }
}
