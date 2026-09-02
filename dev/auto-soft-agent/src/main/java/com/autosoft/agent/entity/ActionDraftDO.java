package com.autosoft.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

/**
 * 助手操作草稿实体。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@TableName("ai_assistant_action_draft")
public class ActionDraftDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private Long sessionId;
    private Long userId;
    private String capabilityId;
    private String status;
    private String targetPath;
    private String targetType;
    private String modalKey;
    private String valuesJson;
    private String displayJson;
    private String missingJson;
    private String unknownJson;
    private Instant expiresAt;
    private Instant consumedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getModalKey() {
        return modalKey;
    }

    public void setModalKey(String modalKey) {
        this.modalKey = modalKey;
    }

    public String getValuesJson() {
        return valuesJson;
    }

    public void setValuesJson(String valuesJson) {
        this.valuesJson = valuesJson;
    }

    public String getDisplayJson() {
        return displayJson;
    }

    public void setDisplayJson(String displayJson) {
        this.displayJson = displayJson;
    }

    public String getMissingJson() {
        return missingJson;
    }

    public void setMissingJson(String missingJson) {
        this.missingJson = missingJson;
    }

    public String getUnknownJson() {
        return unknownJson;
    }

    public void setUnknownJson(String unknownJson) {
        this.unknownJson = unknownJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
