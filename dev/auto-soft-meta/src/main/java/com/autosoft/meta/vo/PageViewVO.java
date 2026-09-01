package com.autosoft.meta.vo;

/**
 * PageView视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class PageViewVO {

    private String appCode;
    private String appName;
    private String appKind;
    private String pageCode;
    private String pageTitle;
    private String pageType;
    private String layout;
    private String schemaJson;
    private boolean published;
    private RuntimeSchemaVO crudSchema;
    private String graphJson;
    private Long workflowId;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppKind() {
        return appKind;
    }

    public void setAppKind(String appKind) {
        this.appKind = appKind;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public RuntimeSchemaVO getCrudSchema() {
        return crudSchema;
    }

    public void setCrudSchema(RuntimeSchemaVO crudSchema) {
        this.crudSchema = crudSchema;
    }

    public String getGraphJson() {
        return graphJson;
    }

    public void setGraphJson(String graphJson) {
        this.graphJson = graphJson;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public boolean isLowCodePage() {
        return "PAGE".equalsIgnoreCase(pageType) || (schemaJson != null && !schemaJson.isBlank());
    }
}
