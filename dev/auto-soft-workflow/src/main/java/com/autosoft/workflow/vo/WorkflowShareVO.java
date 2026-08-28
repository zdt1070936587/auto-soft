package com.autosoft.workflow.vo;

import java.time.Instant;
import java.util.Map;

public class WorkflowShareVO {

    private String token;
    private String permission;
    private Instant expireAt;
    private String name;
    private String code;
    private Map<String, Object> graph;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Instant getExpireAt() { return expireAt; }
    public void setExpireAt(Instant expireAt) { this.expireAt = expireAt; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Map<String, Object> getGraph() { return graph; }
    public void setGraph(Map<String, Object> graph) { this.graph = graph; }
}
