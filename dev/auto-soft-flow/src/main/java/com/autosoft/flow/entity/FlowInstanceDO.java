package com.autosoft.flow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_flow_instance")
public class FlowInstanceDO extends BaseDO {
    private Long definitionId;
    private String appCode;
    private String entityCode;
    private Long bizId;
    private String status;
    private Long startUserId;
    private Integer currentLevel;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getAppCode() { return appCode; }
    public void setAppCode(String appCode) { this.appCode = appCode; }
    public String getEntityCode() { return entityCode; }
    public void setEntityCode(String entityCode) { this.entityCode = entityCode; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getStartUserId() { return startUserId; }
    public void setStartUserId(Long startUserId) { this.startUserId = startUserId; }
    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }
}
