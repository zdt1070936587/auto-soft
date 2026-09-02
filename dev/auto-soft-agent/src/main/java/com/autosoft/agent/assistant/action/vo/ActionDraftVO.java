package com.autosoft.agent.assistant.action.vo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作草稿视图对象。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
public class ActionDraftVO {

    private String draftId;
    private Long sessionId;
    private String capabilityId;
    private String status;
    private String label;
    private String targetPath;
    private String targetType;
    private String modalKey;
    private String permission;
    private Map<String, Object> values = new LinkedHashMap<>();
    private Map<String, Object> displayValues = new LinkedHashMap<>();
    private List<String> missing = new ArrayList<>();
    private List<String> unknown = new ArrayList<>();
    private Instant expiresAt;

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values == null ? new LinkedHashMap<>() : values;
    }

    public Map<String, Object> getDisplayValues() {
        return displayValues;
    }

    public void setDisplayValues(Map<String, Object> displayValues) {
        this.displayValues = displayValues == null ? new LinkedHashMap<>() : displayValues;
    }

    public List<String> getMissing() {
        return missing;
    }

    public void setMissing(List<String> missing) {
        this.missing = missing == null ? new ArrayList<>() : missing;
    }

    public List<String> getUnknown() {
        return unknown;
    }

    public void setUnknown(List<String> unknown) {
        this.unknown = unknown == null ? new ArrayList<>() : unknown;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
