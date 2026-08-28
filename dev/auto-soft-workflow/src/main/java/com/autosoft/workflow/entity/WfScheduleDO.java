package com.autosoft.workflow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("wf_schedule")
public class WfScheduleDO extends BaseDO {

    private Long definitionId;
    private String cron;
    private Integer enabled;
    private Instant lastRunAt;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Instant getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(Instant lastRunAt) { this.lastRunAt = lastRunAt; }
}
