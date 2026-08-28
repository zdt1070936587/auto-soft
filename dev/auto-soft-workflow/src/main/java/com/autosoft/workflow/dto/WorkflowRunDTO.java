package com.autosoft.workflow.dto;

import java.util.Map;

public class WorkflowRunDTO {

    private Map<String, Object> input;
    private boolean confirm;

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public boolean isConfirm() { return confirm; }
    public void setConfirm(boolean confirm) { this.confirm = confirm; }
}
