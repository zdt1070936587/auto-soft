package com.autosoft.agent.dto;

import java.util.List;

/**
 * ChatMessage传输对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class ChatMessageDTO {

    private String message;
    private String agentMode;
    private List<Long> attachmentIds;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAgentMode() {
        return agentMode;
    }

    public void setAgentMode(String agentMode) {
        this.agentMode = agentMode;
    }

    public List<Long> getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(List<Long> attachmentIds) {
        this.attachmentIds = attachmentIds;
    }
}
