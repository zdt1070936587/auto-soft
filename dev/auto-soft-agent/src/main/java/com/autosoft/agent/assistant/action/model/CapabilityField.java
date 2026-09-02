package com.autosoft.agent.assistant.action.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 能力字段定义。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
public class CapabilityField {

    private String key;
    private String label;
    /** string, int, decimal, datetime, enum, role_ref, bool */
    private String type;
    private boolean required;
    private String pattern;
    private String hint;
    private boolean sensitive;
    private boolean multi;
    private Object defaultValue;
    private List<Object> enumValues = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isMulti() {
        return multi;
    }

    public void setMulti(boolean multi) {
        this.multi = multi;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<Object> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<Object> enumValues) {
        this.enumValues = enumValues == null ? new ArrayList<>() : enumValues;
    }
}
