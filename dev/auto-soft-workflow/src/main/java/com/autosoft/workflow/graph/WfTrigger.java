package com.autosoft.workflow.graph;

import java.util.LinkedHashMap;
import java.util.Map;

public class WfTrigger {

    private String type;
    private Map<String, String> inputSchema = new LinkedHashMap<>();
    private String app;
    private String entity;
    private String cron;
    private Integer enabled;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, String> getInputSchema() { return inputSchema; }
    public void setInputSchema(Map<String, String> inputSchema) {
        this.inputSchema = inputSchema == null ? new LinkedHashMap<>() : inputSchema;
    }
    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
