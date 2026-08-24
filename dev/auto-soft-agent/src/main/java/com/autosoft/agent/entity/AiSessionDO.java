package com.autosoft.agent.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ai_session")
public class AiSessionDO extends BaseDO {

    private Long userId;
    private String title;
    private Long appId;
    private String status;
    private Long tokenInput;
    private Long tokenOutput;
    private String agentMode;

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

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
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

    public String getAgentMode() {
        return agentMode;
    }

    public void setAgentMode(String agentMode) {
        this.agentMode = agentMode;
    }
}
