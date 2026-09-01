package com.autosoft.workflow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * WfDefinition实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("wf_definition")
public class WfDefinitionDO extends BaseDO {

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";

    private Long appId;
    private String code;
    private String name;
    private String status;
    private String graphJson;
    private Integer version;
    private String grantRoles;
    private String visibility;

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGraphJson() { return graphJson; }
    public void setGraphJson(String graphJson) { this.graphJson = graphJson; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getGrantRoles() { return grantRoles; }
    public void setGrantRoles(String grantRoles) { this.grantRoles = grantRoles; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
}
