package com.autosoft.workflow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * WfDefinitionVersion实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("wf_definition_version")
public class WfDefinitionVersionDO extends BaseDO {

    private Long definitionId;
    private Integer version;
    private String graphJson;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getGraphJson() { return graphJson; }
    public void setGraphJson(String graphJson) { this.graphJson = graphJson; }
}
