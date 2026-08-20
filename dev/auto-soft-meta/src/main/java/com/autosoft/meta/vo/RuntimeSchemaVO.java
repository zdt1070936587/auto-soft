package com.autosoft.meta.vo;

import java.util.ArrayList;
import java.util.List;

public class RuntimeSchemaVO {
    private String appCode;
    private String appName;
    private String entityCode;
    private String entityName;
    private boolean published;
    private boolean flowBound;
    private List<MetaFieldVO> fields = new ArrayList<>();

    public String getAppCode() { return appCode; }
    public void setAppCode(String appCode) { this.appCode = appCode; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getEntityCode() { return entityCode; }
    public void setEntityCode(String entityCode) { this.entityCode = entityCode; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public boolean isFlowBound() { return flowBound; }
    public void setFlowBound(boolean flowBound) { this.flowBound = flowBound; }
    public List<MetaFieldVO> getFields() { return fields; }
    public void setFields(List<MetaFieldVO> fields) { this.fields = fields; }
}
