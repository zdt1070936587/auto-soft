package com.autosoft.flow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * FlowDefinition实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("sys_flow_definition")
public class FlowDefinitionDO extends BaseDO {
    private String flowCode;
    private String name;
    private String approveRoleCodes;
    private Integer enabled;

    public String getFlowCode() { return flowCode; }
    public void setFlowCode(String flowCode) { this.flowCode = flowCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApproveRoleCodes() { return approveRoleCodes; }
    public void setApproveRoleCodes(String approveRoleCodes) { this.approveRoleCodes = approveRoleCodes; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
