package com.autosoft.agent.assistant.action.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段校验结果。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
public class FieldValidationResult {

    private Map<String, Object> values = new LinkedHashMap<>();
    private Map<String, Object> displayValues = new LinkedHashMap<>();
    private List<String> missing = new ArrayList<>();
    private List<String> unknown = new ArrayList<>();
    private Map<String, String> fieldErrors = new LinkedHashMap<>();
    private boolean ready;

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

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors == null ? new LinkedHashMap<>() : fieldErrors;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
