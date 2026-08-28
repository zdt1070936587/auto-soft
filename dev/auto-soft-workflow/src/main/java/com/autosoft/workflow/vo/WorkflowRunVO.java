package com.autosoft.workflow.vo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WorkflowRunVO {

    private Long id;
    private Long definitionId;
    private String definitionCode;
    private Integer version;
    private boolean dryRun;
    private String status;
    private String currentNodeId;
    private String errorMsg;
    private Long tokenInput;
    private Long tokenOutput;
    private Instant createdAt;
    private List<WorkflowStepVO> steps = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getDefinitionCode() { return definitionCode; }
    public void setDefinitionCode(String definitionCode) { this.definitionCode = definitionCode; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Long getTokenInput() { return tokenInput; }
    public void setTokenInput(Long tokenInput) { this.tokenInput = tokenInput; }
    public Long getTokenOutput() { return tokenOutput; }
    public void setTokenOutput(Long tokenOutput) { this.tokenOutput = tokenOutput; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<WorkflowStepVO> getSteps() { return steps; }
    public void setSteps(List<WorkflowStepVO> steps) { this.steps = steps; }
}
