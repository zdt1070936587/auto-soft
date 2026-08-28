package com.autosoft.workflow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("wf_run")
public class WfRunDO extends BaseDO {

    public static final String RUNNING = "running";
    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";
    public static final String PAUSED = "paused";

    private Long definitionId;
    private Integer version;
    private Integer dryRun;
    private String status;
    private String triggerJson;
    private String currentNodeId;
    private Long tokenInput;
    private Long tokenOutput;
    private String errorMsg;
    private Long startUserId;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Integer getDryRun() { return dryRun; }
    public void setDryRun(Integer dryRun) { this.dryRun = dryRun; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTriggerJson() { return triggerJson; }
    public void setTriggerJson(String triggerJson) { this.triggerJson = triggerJson; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }
    public Long getTokenInput() { return tokenInput; }
    public void setTokenInput(Long tokenInput) { this.tokenInput = tokenInput; }
    public Long getTokenOutput() { return tokenOutput; }
    public void setTokenOutput(Long tokenOutput) { this.tokenOutput = tokenOutput; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Long getStartUserId() { return startUserId; }
    public void setStartUserId(Long startUserId) { this.startUserId = startUserId; }
}
