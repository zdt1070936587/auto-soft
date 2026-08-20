package com.autosoft.flow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("meta_entity_flow")
public class MetaEntityFlowDO extends BaseDO {
    private Long entityId;
    private String flowCode;
    private Long definitionId;
    private String approveRoleCodes;
    private Integer enabled;

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getFlowCode() { return flowCode; }
    public void setFlowCode(String flowCode) { this.flowCode = flowCode; }
    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getApproveRoleCodes() { return approveRoleCodes; }
    public void setApproveRoleCodes(String approveRoleCodes) { this.approveRoleCodes = approveRoleCodes; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
