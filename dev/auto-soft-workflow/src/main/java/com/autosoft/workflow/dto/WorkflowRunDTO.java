package com.autosoft.workflow.dto;

import java.util.Map;

/**
 * WorkflowRun传输对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class WorkflowRunDTO {

    private Map<String, Object> input;
    private boolean confirm;

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public boolean isConfirm() { return confirm; }
    public void setConfirm(boolean confirm) { this.confirm = confirm; }
}
