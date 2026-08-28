package com.autosoft.agent.assistant.vo;

import java.time.Instant;

public class AiAssistantSessionVO {

    private Long id;
    private String title;
    private String status;
    private Long tokenInput;
    private Long tokenOutput;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
