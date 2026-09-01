package com.autosoft.agent.vo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * AiMessage视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class AiMessageVO {

    private Long id;
    private String role;
    private String content;
    private String toolName;
    private Instant createdAt;
    private List<AiAttachmentVO> attachments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<AiAttachmentVO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AiAttachmentVO> attachments) {
        this.attachments = attachments;
    }
}
