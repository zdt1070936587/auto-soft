package com.autosoft.workflow.exec;

public class WorkflowPausedException extends RuntimeException {

    private final String nodeId;

    public WorkflowPausedException(String nodeId) {
        super("paused");
        this.nodeId = nodeId;
    }

    public String nodeId() {
        return nodeId;
    }
}
