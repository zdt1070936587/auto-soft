package com.autosoft.workflow.vo;

import java.util.Map;

public class WorkflowDefinitionVO {

    private Long id;
    private Long appId;
    private String code;
    private String name;
    private String status;
    private Integer version;
    private String grantRoles;
    private String appKind;
    private boolean published;
    private Map<String, Object> graph;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getGrantRoles() { return grantRoles; }
    public void setGrantRoles(String grantRoles) { this.grantRoles = grantRoles; }
    public String getAppKind() { return appKind; }
    public void setAppKind(String appKind) { this.appKind = appKind; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Map<String, Object> getGraph() { return graph; }
    public void setGraph(Map<String, Object> graph) { this.graph = graph; }
}
