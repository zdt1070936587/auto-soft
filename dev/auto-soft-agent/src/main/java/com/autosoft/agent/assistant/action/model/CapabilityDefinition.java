package com.autosoft.agent.assistant.action.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 可执行能力定义。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
public class CapabilityDefinition {

    private String capabilityId;
    private String label;
    private String description;
    private String path;
    private String permission;
    private String targetType;
    private String modalKey;
    private String operation;
    private String apiMethod;
    private String apiPath;
    private List<String> keywords = new ArrayList<>();
    private List<CapabilityField> fields = new ArrayList<>();

    public String getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public void setApiMethod(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords == null ? new ArrayList<>() : keywords;
    }

    public List<CapabilityField> getFields() {
        return fields;
    }

    public void setFields(List<CapabilityField> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }
}
