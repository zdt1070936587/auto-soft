package com.autosoft.workflow.graph;

import java.util.LinkedHashMap;
import java.util.Map;

public class WfNode {

    private String id;
    private String type;
    private String title;
    private Map<String, Object> config = new LinkedHashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) {
        this.config = config == null ? new LinkedHashMap<>() : config;
    }
}
